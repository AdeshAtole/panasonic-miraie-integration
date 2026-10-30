"""Narrow MirAIe India adapter. No MCP runtime or guessed generic controls."""
import http.client
import json
import math
import secrets
import time
from urllib.parse import quote, urlencode, urlsplit

AUTH = 'https://auth.miraie.in/simplifi/v1/'
BASE = 'https://app.miraie.in/simplifi/v1/'
CLIENT = '292d99d7-5623-4157-8a71-846ee3ff5dbf'


class Fault(Exception):
    """Only fixed, safe error codes cross the public boundary."""


def request(url, body=None, token=None, timeout=1.0, form=False):
    """TLS only, no redirects/retries/proxies, bounded response; never log URLs."""
    target = urlsplit(url)
    if target.scheme != 'https' or target.hostname not in (
            'auth.miraie.in', 'app.miraie.in', 'api.amazon.com'):
        raise Fault('INTERNAL_ERROR')
    headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
    if form:
        headers['Content-Type'] = 'application/x-www-form-urlencoded'
    if token:
        headers['Authorization'] = 'Bearer ' + token
    connection = http.client.HTTPSConnection(target.hostname, timeout=timeout)
    try:
        connection.request('GET' if body is None else 'POST',
                           target.path + ('?' + target.query if target.query else ''),
                           None if body is None else (urlencode(body) if form else json.dumps(body)), headers)
        response = connection.getresponse()
        raw = response.read(4 * 1024 * 1024 + 1)
        if response.status in (401, 406):
            raise Fault('EXPIRED_AUTHORIZATION_CREDENTIAL')
        if not 200 <= response.status < 300 or len(raw) > 4 * 1024 * 1024:
            raise Fault('ENDPOINT_UNREACHABLE')
        return json.loads(raw) if raw else {}
    except (OSError, ValueError, http.client.HTTPException):
        raise Fault('ENDPOINT_UNREACHABLE') from None
    finally:
        connection.close()


def session(result, previous=None):
    if not isinstance(result, dict) or not isinstance(result.get('accessToken'), str) or not result['accessToken']:
        raise Fault('INTERNAL_ERROR')
    value = dict(previous or {})
    value['accessToken'] = result['accessToken']
    if result.get('refreshToken'):
        value['refreshToken'] = result['refreshToken']
    try:
        expiry = float(result['expiresIn'])
        if not math.isfinite(expiry) or expiry <= 0:
            raise ValueError()
    except (KeyError, TypeError, ValueError):
        raise Fault('INTERNAL_ERROR') from None
    value['expires_at'] = time.time() + expiry
    return value


def login(identity, password):
    return session(request(AUTH + 'userManagement/login', {
        'email' if '@' in identity else 'mobile': identity, 'password': password,
        'clientId': CLIENT, 'scope': 'an_' + secrets.token_hex(12)}, timeout=15))


def fields(raw):
    for _ in range(4):
        if isinstance(raw, dict) and len(raw) == 1 and next(iter(raw)) in ('status', 'data', 'params', 'state'):
            raw = next(iter(raw.values()))
        else:
            break
    if not isinstance(raw, dict) or raw.get('acdc') not in ('on', 'off'):
        raise Fault('ENDPOINT_UNREACHABLE')
    online = raw.get('onlineStatus')
    if not (online is True or type(online) in (int, float) and online == 1
            or isinstance(online, str) and online.strip().lower() in ('online', 'true')):
        raise Fault('ENDPOINT_UNREACHABLE')
    return raw


def validate(device, model):
    if isinstance(model, dict) and 'controlCapabilities' not in model and isinstance(model.get('data'), dict):
        model = model['data']
    if (not isinstance(model, dict) or str(device.get('modelId')) != '140013'
            or str(model.get('modelId', '140013')) != '140013'
            or device.get('category') != 'AC' or device.get('parentDeviceId')
            or device.get('thirdPartyService') or model.get('thirdPartyService')):
        raise Fault('INVALID_VALUE')
    caps = [c for c in model.get('controlCapabilities', [])
            if isinstance(c, dict) and c.get('capability') == 'DisplayControl']
    # Allow extra metadata and enum ordering, but require one exact key.
    attrs = caps[0].get('attributes', []) if len(caps) == 1 else []
    if (not isinstance(attrs, list) or len(attrs) != 1 or not isinstance(attrs[0], dict)
            or attrs[0].get('key') != 'acdc'
            or not isinstance(attrs[0].get('values'), list)
            or sorted(attrs[0]['values'], key=str) != ['off', 'on']):
        raise Fault('INVALID_VALUE')


class AC:
    def __init__(self, config, save, http=request, clock=time.monotonic):
        self.config, self.save, self.http, self.clock = config, save, http, clock
        self.deadline = clock() + 6.8

    def call(self, path, body=None):
        remaining = self.deadline - self.clock()
        if remaining <= 0.15:
            raise Fault('ENDPOINT_UNREACHABLE')
        s = self.config['session']
        # Refresh only before a new operation, never in response to a failed write.
        return self.http(BASE + path, body, s['accessToken'], timeout=min(0.9, remaining))

    def prepare(self):
        s = self.config['session']
        if s.get('expires_at', 0) <= time.time() + 15:
            if not s.get('refreshToken'):
                raise Fault('EXPIRED_AUTHORIZATION_CREDENTIAL')
            updated = self.http(AUTH + 'userManagement/tokenRefresh',
                                {'clientId': CLIENT, 'token': s['refreshToken']}, timeout=0.9)
            self.config['session'] = session(updated, s)
            self.save(self.config)
        device = self.config['device']
        model = self.call('deviceManagement/deviceModels/modelId/' + quote(str(device['modelId']), safe=''))
        validate(device, model)
        return self.read()

    def read(self):
        return fields(self.call('deviceManagement/devices/' +
                               quote(self.config['device']['deviceId'], safe='') + '/mobile/status'))

    def set_display(self, target):
        if target not in ('on', 'off'):
            raise Fault('INVALID_VALUE')
        if self.deadline - self.clock() < 4.1:
            raise Fault('ENDPOINT_UNREACHABLE')
        self.call('deviceManagement/control', {
            'deviceId': self.config['device']['deviceId'],
            'control_params': {'cnt': 'an', 'sid': '0', 'acdc': target, 'ki': 8}})

    def verify(self, target, sleep=time.sleep):
        sleep(3.0)
        state = self.read()
        if state['acdc'] != target:
            raise Fault('ENDPOINT_UNREACHABLE')
        return state
