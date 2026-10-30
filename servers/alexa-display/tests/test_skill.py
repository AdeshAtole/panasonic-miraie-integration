import copy
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'src'))
from protocol import AC, AUTH, BASE, Fault, fields, validate
from skill import ENDPOINT, Journal, authorize, handle

DEVICE = {'deviceId': 'synthetic-ac', 'modelId': '140013', 'category': 'AC'}
MODEL = {'modelId': '140013', 'controlCapabilities': [
    {'capability': 'DisplayControl', 'attributes': [{'key': 'acdc', 'values': ['on', 'off']}]}]}
CONFIG = {'device': DEVICE, 'session': {'accessToken': 'synthetic', 'refreshToken': 'synthetic-refresh',
                                     'expires_at': 9999999999},
          'lwa_client_id': 'synthetic-client', 'owner_id': 'synthetic-owner'}


def directive(name='TurnOff', namespace='Alexa.PowerController', message='request-1'):
    return {'directive': {'header': {'namespace': namespace, 'name': name, 'messageId': message,
                                    'payloadVersion': '3', 'correlationToken': 'synthetic-correlation'},
                          'endpoint': {'endpointId': ENDPOINT, 'scope': {'type': 'BearerToken', 'token': 'synthetic'}},
                          'payload': {}}}


class MemoryJournal:
    def __init__(self):
        self.items, self.pending = {}, None

    def claim(self, key, target):
        prior = self.items.get(key)
        if not prior:
            self.items[key] = {'target': target}
        return prior

    def finish(self, key, target, result):
        self.items[key] = {'target': target, 'response': result}

    def blocked(self):
        return self.pending

    def block(self, key, target):
        self.pending = {'request': key, 'target': target}

    def unblock(self, key):
        if self.pending['request'] != key:
            raise AssertionError('Wrong lock owner')
        self.pending = None


class FakeAC:
    def __init__(self):
        self.state, self.writes, self.failure = 'on', [], False

    def prepare(self):
        return {'acdc': self.state}

    def set_display(self, target):
        self.writes.append(target)
        if self.failure:
            raise Fault('ENDPOINT_UNREACHABLE')
        self.state = target

    def verify(self, target):
        return {'acdc': self.state}


class SkillTests(unittest.TestCase):
    def setUp(self):
        self.ac, self.journal = FakeAC(), MemoryJournal()

    def run_event(self, event):
        return handle(event, CONFIG, self.journal, self.ac, auth=lambda *_: None)

    def test_discovery_is_one_display_switch_without_actuation(self):
        event = directive('Discover', 'Alexa.Discovery')
        result = self.run_event(event)
        endpoints = result['event']['payload']['endpoints']
        self.assertEqual(len(endpoints), 1)
        self.assertEqual(endpoints[0]['friendlyName'], 'AC display')
        self.assertEqual(endpoints[0]['displayCategories'], ['SWITCH'])
        self.assertEqual([x['interface'] for x in endpoints[0]['capabilities']], ['Alexa', 'Alexa.PowerController'])
        self.assertEqual(self.ac.writes, [])

    def test_turn_off_and_on_with_correct_correlation(self):
        result = self.run_event(directive())
        self.assertEqual(result['context']['properties'][0]['value'], 'OFF')
        self.assertEqual(result['event']['header']['correlationToken'], 'synthetic-correlation')
        self.assertNotIn('scope', result['event']['endpoint'])
        self.run_event(directive('TurnOn', message='request-2'))
        self.assertEqual(self.ac.writes, ['off', 'on'])

    def test_duplicate_is_not_resent_even_after_later_command(self):
        event = directive()
        first = self.run_event(event)
        self.run_event(directive('TurnOn', message='request-2'))
        self.assertEqual(self.run_event(event), first)
        self.assertEqual(self.ac.writes, ['off', 'on'])

    def test_reused_message_with_changed_intent_rejected(self):
        self.run_event(directive())
        result = self.run_event(directive('TurnOn'))
        self.assertEqual(result['event']['payload']['type'], 'INVALID_DIRECTIVE')
        self.assertEqual(self.ac.writes, ['off'])

    def test_ambiguous_failure_blocks_new_requests_and_replays(self):
        self.ac.failure = True
        result = self.run_event(directive())
        self.assertEqual(result['event']['payload']['type'], 'ENDPOINT_UNREACHABLE')
        self.assertTrue(self.journal.pending)
        self.run_event(directive())
        self.run_event(directive('TurnOn', message='request-2'))
        self.assertEqual(self.ac.writes, ['off'])

    def test_crash_pending_record_cannot_replay(self):
        import hashlib
        self.journal.claim(hashlib.sha256(b'request-1').hexdigest(), 'off')
        self.run_event(directive())
        self.assertEqual(self.ac.writes, [])

    def test_state_report_and_noop_never_write(self):
        report = self.run_event(directive('ReportState', 'Alexa'))
        self.assertEqual(report['event']['header']['name'], 'StateReport')
        self.run_event(directive('TurnOn'))
        self.assertEqual(self.ac.writes, [])

    def test_wrong_endpoint_and_unsupported_directives(self):
        for name, namespace in [('SetTargetTemperature', 'Alexa.ThermostatController'),
                                ('Toggle', 'Alexa.PowerController')]:
            result = self.run_event(directive(name, namespace))
            self.assertEqual(result['event']['payload']['type'], 'INVALID_DIRECTIVE')
        event = directive()
        event['directive']['endpoint']['endpointId'] = 'main-ac'
        self.assertEqual(self.run_event(event)['event']['payload']['type'], 'NO_SUCH_ENDPOINT')
        self.assertEqual(self.ac.writes, [])

    def test_auth_is_before_any_device_read(self):
        def deny(*_):
            raise Fault('INVALID_AUTHORIZATION_CREDENTIAL')
        with patch.object(self.ac, 'prepare', side_effect=AssertionError('Must not read')):
            result = handle(directive(), CONFIG, self.journal, self.ac, auth=deny)
        self.assertEqual(result['event']['payload']['type'], 'INVALID_AUTHORIZATION_CREDENTIAL')

    def test_authorize_checks_owner_audience_expiry_and_discovery_payload(self):
        valid = {'aud': CONFIG['lwa_client_id'], 'user_id': CONFIG['owner_id'], 'exp': 100}
        authorize(directive()['directive'], CONFIG, http=lambda *_: valid)
        discovery = directive('Discover', 'Alexa.Discovery')['directive']
        discovery['payload']['scope'] = discovery.pop('endpoint')['scope']
        authorize(discovery, CONFIG, http=lambda *_: valid)
        for updates in ({'aud': 'other'}, {'user_id': 'other'}, {'exp': 0}, {'exp': None}, {'exp': 'nan'}):
            with self.assertRaises(Fault):
                authorize(directive()['directive'], CONFIG, http=lambda *_: dict(valid, **updates))


class ProtocolTests(unittest.TestCase):
    def test_strict_model_capabilities(self):
        validate(DEVICE, MODEL)
        for device, model in [(dict(DEVICE, modelId='other'), MODEL),
                              (dict(DEVICE, thirdPartyService='partner'), MODEL),
                              (DEVICE, dict(MODEL, modelId='other')),
                              (DEVICE, {'controlCapabilities': []})]:
            with self.assertRaises(Fault):
                validate(device, model)
        for attr in ({'key': 'ps', 'values': ['on', 'off']}, {'key': 'acdc', 'values': ['on']}):
            model = copy.deepcopy(MODEL)
            model['controlCapabilities'][0]['attributes'] = [attr]
            with self.assertRaises(Fault):
                validate(DEVICE, model)

    def test_offline_unknown_and_missing_status_fail_closed(self):
        for online in (False, 0, 'offline', 'false', None, 'unknown'):
            with self.assertRaises(Fault):
                fields({'acdc': 'on', 'onlineStatus': online})
        self.assertEqual(fields({'data': {'acdc': 'off', 'onlineStatus': 'online'}})['acdc'], 'off')

    def test_control_body_has_display_only_and_delayed_readback(self):
        calls, sleeps = [], []

        def http(url, body=None, token=None, **kwargs):
            calls.append((url, body))
            return {'acdc': 'off', 'ps': 'on', 'onlineStatus': True}

        ac = AC(copy.deepcopy(CONFIG), lambda _: None, http=http)
        ac.set_display('off')
        ac.verify('off', sleep=sleeps.append)
        self.assertEqual(calls[0], (BASE + 'deviceManagement/control', {
            'deviceId': 'synthetic-ac', 'control_params': {'cnt': 'an', 'sid': '0', 'acdc': 'off', 'ki': 8}}))
        self.assertEqual(sleeps, [3.0])
        self.assertEqual(len(calls), 2)

    def test_failed_write_is_not_retried_or_refreshed(self):
        calls = []

        def http(url, *args, **kwargs):
            calls.append(url)
            raise Fault('EXPIRED_AUTHORIZATION_CREDENTIAL')

        ac = AC(copy.deepcopy(CONFIG), lambda _: None, http=http)
        with self.assertRaises(Fault):
            ac.set_display('off')
        self.assertEqual(calls, [BASE + 'deviceManagement/control'])

    def test_refresh_is_persisted_before_reads(self):
        config = copy.deepcopy(CONFIG)
        config['session']['expires_at'] = 0
        sequence = []

        def http(url, *args, **kwargs):
            sequence.append(url)
            if url.startswith(AUTH):
                return {'accessToken': 'rotated', 'refreshToken': 'rotated-refresh', 'expiresIn': 3600}
            if 'modelId' in url:
                return MODEL
            return {'acdc': 'on', 'onlineStatus': True}

        ac = AC(config, lambda _: sequence.append('saved'), http=http)
        ac.prepare()
        self.assertEqual(sequence[:2], [AUTH + 'userManagement/tokenRefresh', 'saved'])

    def test_insufficient_deadline_sends_nothing(self):
        ac = AC(copy.deepcopy(CONFIG), lambda _: None, http=lambda *_: self.fail('Must not send'))
        ac.deadline = ac.clock() + 2
        with self.assertRaises(Fault):
            ac.set_display('off')


class JournalTests(unittest.TestCase):
    def test_durable_conditional_claim_and_collision(self):
        class ConditionalFailure(Exception):
            response = {'Error': {'Code': 'ConditionalCheckFailedException'}}

        class Table:
            def __init__(self):
                self.items = {}

            def put_item(self, Item, **kwargs):
                if kwargs.get('ConditionExpression') and Item['id'] in self.items:
                    raise ConditionalFailure()
                self.items[Item['id']] = Item

            def get_item(self, Key, ConsistentRead):
                self_consistent = ConsistentRead
                assert self_consistent is True
                return {'Item': self.items.get(Key['id'])}

        table = Table()
        journal = Journal(table)
        self.assertIsNone(journal.claim('hash', 'off'))
        self.assertEqual(journal.claim('hash', 'off'), {'id': 'hash', 'target': 'off'})
        journal.finish('hash', 'off', {'event': {}})
        self.assertEqual(journal.claim('hash', 'off')['response'], {'event': {}})
        journal.block('hash', 'off')
        self.assertTrue(journal.blocked())
        with self.assertRaises(ConditionalFailure):
            journal.block('other', 'on')


class OwnerLoginTests(unittest.TestCase):
    def test_callback_rejects_wrong_state_host_path_and_duplicate_codes(self):
        sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
        from owner_login import callback_code
        self.assertEqual(callback_code('localhost:8765', '/callback?state=random&code=synthetic', 'random'), 'synthetic')
        for host, path in [('other:8765', '/callback?state=random&code=x'),
                           ('localhost:8765', '/callback?state=wrong&code=x'),
                           ('localhost:8765', '/other?state=random&code=x'),
                           ('localhost:8765', '/callback?state=random&code=x&code=y')]:
            self.assertIsNone(callback_code(host, path, 'random'))


if __name__ == '__main__':
    unittest.main()
