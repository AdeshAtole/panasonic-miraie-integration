"""Single-owner Alexa Smart Home v3 skill. Invocation is restricted by skill ID."""
import hashlib
import json
import math
import time
import uuid
from datetime import datetime, timezone
from urllib.parse import urlencode
from protocol import AC, Fault, request

ENDPOINT = 'openmiraie-ac-display'


def response(d, name, payload=None, namespace='Alexa', state=None):
    header = {'namespace': namespace, 'name': name, 'payloadVersion': '3',
              'messageId': str(uuid.uuid4())}
    if d.get('header', {}).get('correlationToken'):
        header['correlationToken'] = d['header']['correlationToken']
    event = {'header': header, 'payload': payload or {}}
    if 'endpoint' in d:
        event['endpoint'] = {'endpointId': d['endpoint'].get('endpointId', ENDPOINT)}
    result = {'event': event}
    if state is not None:
        result['context'] = {'properties': [{
            'namespace': 'Alexa.PowerController', 'name': 'powerState',
            'value': state.upper(), 'timeOfSample': datetime.now(timezone.utc).isoformat(timespec='milliseconds').replace('+00:00', 'Z'),
            'uncertaintyInMilliseconds': 1000}]}
    return result


def error(d, code):
    messages = {
        'INVALID_AUTHORIZATION_CREDENTIAL': 'Link the authorized Amazon account.',
        'EXPIRED_AUTHORIZATION_CREDENTIAL': 'Reconnect the AC account with the local setup tool.',
        'NO_SUCH_ENDPOINT': 'Unknown display device.',
        'INVALID_DIRECTIVE': 'Unsupported display request.',
        'INVALID_VALUE': 'This model does not support the reviewed display control.',
        'ENDPOINT_UNREACHABLE': 'Display state could not be confirmed. Check the AC and integration.',
        'INTERNAL_ERROR': 'The display integration needs attention.'}
    return response(d, 'ErrorResponse', {'type': code, 'message': messages[code]})


def authorize(d, config, http=request):
    scope = d.get('endpoint', {}).get('scope') or d.get('payload', {}).get('scope', {})
    if scope.get('type') != 'BearerToken' or not isinstance(scope.get('token'), str) or not scope['token']:
        raise Fault('INVALID_AUTHORIZATION_CREDENTIAL')
    try:
        info = http('https://api.amazon.com/auth/o2/tokeninfo?' + urlencode({'access_token': scope['token']}))
        expiry = float(info.get('exp', info.get('expires_in', 0)))
        if (info.get('aud') != config['lwa_client_id'] or info.get('user_id') != config['owner_id']
                or not math.isfinite(expiry) or expiry <= 0):
            raise Fault('INVALID_AUTHORIZATION_CREDENTIAL')
    except (Fault, TypeError, ValueError):
        raise Fault('INVALID_AUTHORIZATION_CREDENTIAL') from None


def discovery(d, name):
    return response(d, 'Discover.Response', {'endpoints': [{
        'endpointId': ENDPOINT, 'manufacturerName': 'openMirAIe', 'friendlyName': name,
        'description': 'Panasonic AC display light only', 'displayCategories': ['SWITCH'],
        'capabilities': [
            {'type': 'AlexaInterface', 'interface': 'Alexa', 'version': '3'},
            {'type': 'AlexaInterface', 'interface': 'Alexa.PowerController', 'version': '3',
             'properties': {'supported': [{'name': 'powerState'}], 'proactivelyReported': False,
                            'retrievable': True}}]}]}, 'Alexa.Discovery')


def handle(event, config, journal, ac, auth=authorize):
    d = event.get('directive', {})
    try:
        auth(d, config)
        h = d.get('header', {})
        if h.get('payloadVersion') != '3':
            raise Fault('INVALID_DIRECTIVE')
        kind = (h.get('namespace'), h.get('name'))
        if kind == ('Alexa.Discovery', 'Discover'):
            ac.prepare()  # Discovery is read-only and capability-gated.
            return discovery(d, config.get('friendly_name', 'AC display'))
        if d.get('endpoint', {}).get('endpointId') != ENDPOINT:
            raise Fault('NO_SUCH_ENDPOINT')
        if kind == ('Alexa', 'ReportState'):
            return response(d, 'StateReport', state=ac.prepare()['acdc'])
        if kind not in (('Alexa.PowerController', 'TurnOn'), ('Alexa.PowerController', 'TurnOff')):
            raise Fault('INVALID_DIRECTIVE')
        message = h.get('messageId')
        if not isinstance(message, str) or not 1 <= len(message) <= 256:
            raise Fault('INVALID_DIRECTIVE')
        target = 'on' if kind[1] == 'TurnOn' else 'off'
        key = hashlib.sha256(message.encode()).hexdigest()
        prior = journal.claim(key, target)
        if prior:
            if prior['target'] != target:
                raise Fault('INVALID_DIRECTIVE')
            return prior.get('response') or error(d, 'ENDPOINT_UNREACHABLE')
        try:
            if journal.blocked():
                raise Fault('ENDPOINT_UNREACHABLE')
            state = ac.prepare()
            if state['acdc'] != target:
                journal.block(key, target)  # Durable before sending; no automatic expiry.
                ac.set_display(target)
                state = ac.verify(target)  # One read after at least three seconds.
                journal.unblock(key)
            result = response(d, 'Response', state=state['acdc'])
        except Fault as exc:
            result = error(d, str(exc))
        journal.finish(key, target, result)
        return result
    except Fault as exc:
        return error(d, str(exc))
    except Exception:
        # Never expose backend exceptions, identifiers, URLs, tokens or raw events.
        return error(d, 'INTERNAL_ERROR')


class Journal:
    def __init__(self, table):
        self.table = table

    def get(self, key):
        return self.table.get_item(Key={'id': key}, ConsistentRead=True).get('Item')

    def claim(self, key, target):
        try:
            self.table.put_item(Item={'id': key, 'target': target}, ConditionExpression='attribute_not_exists(id)')
        except Exception as exc:
            if getattr(exc, 'response', {}).get('Error', {}).get('Code') != 'ConditionalCheckFailedException':
                raise
            prior = self.get(key)
            if not prior:
                raise Fault('INTERNAL_ERROR')
            if 'result' in prior:
                prior['response'] = json.loads(prior['result'])
            return prior

    def finish(self, key, target, result):
        self.table.put_item(Item={'id': key, 'target': target, 'result': json.dumps(result)})

    def blocked(self):
        return self.get('uncertain')

    def block(self, key, target):
        self.table.put_item(Item={'id': 'uncertain', 'request': key, 'target': target,
                                 'created': int(time.time())}, ConditionExpression='attribute_not_exists(id)')

    def unblock(self, key):
        self.table.delete_item(Key={'id': 'uncertain'}, ConditionExpression='#r = :r',
                               ExpressionAttributeNames={'#r': 'request'}, ExpressionAttributeValues={':r': key})


def lambda_handler(event, context):
    import os
    import boto3
    from botocore.config import Config
    try:
        options = Config(connect_timeout=0.5, read_timeout=0.5, retries={'total_max_attempts': 1})
        secrets = boto3.client('secretsmanager', config=options)
        arn = os.environ['CONFIG_SECRET']
        config = json.loads(secrets.get_secret_value(SecretId=arn)['SecretString'])

        def save(value):
            secrets.put_secret_value(SecretId=arn, SecretString=json.dumps(value))

        table = boto3.resource('dynamodb', config=options).Table(os.environ['JOURNAL_TABLE'])
        ac = AC(config, save)
        # Share the invocation deadline with authorization and storage overhead.
        ac.deadline = time.monotonic() + min(6.8, context.get_remaining_time_in_millis() / 1000 - 0.5)
        return handle(event, config, Journal(table), ac)
    except Exception:
        return error(event.get('directive', {}), 'INTERNAL_ERROR')
