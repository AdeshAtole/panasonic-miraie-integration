"""Bounded MirAIe REST client, serialized session storage and public errors."""
from contextlib import contextmanager
from pathlib import Path
import json
import math
import os
import secrets
import sys
import threading
import time

import httpx
from filelock import FileLock, Timeout
from platformdirs import user_state_path

AUTH = 'https://auth.miraie.in/simplifi/v1/'
BASE = 'https://app.miraie.in/simplifi/v1/'
CLIENT = '292d99d7-5623-4157-8a71-846ee3ff5dbf'
GUIDANCE = {
    'unconfigured': 'Call begin_setup to open local sign-in. Never send credentials in chat.',
    'expired': 'Call begin_setup to sign in again. No appliance command will be replayed.',
    'offline': 'Check internet access, then run connection_test. Do not retry an uncertain write.',
    'store_unavailable': 'Unlock your OS credential store and run doctor. No file fallback is used.',
    'busy': 'Another local operation or setup is active. Finish or cancel it, then try again.',
    'invalid_credentials': 'Check credentials directly in the local sign-in page and restart setup.',
    'unsupported': 'This device or schema is read-only; inspect capabilities.',
    'invalid_request': 'Check the requested device, advertised values and current reported state.',
    'unexpected_response': 'Service response was not understood. No inferred state is available.',
    'service_error': 'Panasonic rejected the request. Run a read-only connection test later.',
}

class Fault(Exception):
    def __init__(self, code):
        self.code = code
        super().__init__(GUIDANCE.get(code, 'Operation failed safely.'))

    def public(self):
        return {'state': self.code, 'next_action': str(self)}

class Store:
    """Only vetted OS backends. Tokens never fall back to a plaintext file."""
    def __init__(self, directory=None, backend=None):
        self.directory = Path(directory) if directory else user_state_path('openmiraie-mcp', appauthor=False)
        self.backend = backend
        self.mutex = threading.RLock()

    def prepare(self):
        try:
            if self.directory.is_symlink():
                raise OSError()
            self.directory.mkdir(mode=0o700, parents=True, exist_ok=True)
            if os.name != 'nt':
                self.directory.chmod(0o700)
        except OSError:
            raise Fault('store_unavailable') from None

    @contextmanager
    def locked(self):
        self.prepare()
        try:
            with self.mutex, FileLock(str(self.directory / 'session.lock'), timeout=1, mode=0o600):
                yield
        except Timeout:
            raise Fault('busy') from None

    def native(self):
        if self.backend is None:
            try:
                if sys.platform == 'darwin':
                    from keyring.backends.macOS import Keyring
                elif sys.platform == 'win32':
                    from keyring.backends.Windows import WinVaultKeyring as Keyring
                elif sys.platform.startswith('linux'):
                    from keyring.backends.SecretService import Keyring
                else:
                    raise RuntimeError()
                self.backend = Keyring()
            except Exception:
                raise Fault('store_unavailable') from None
        return self.backend

    def load(self):
        try:
            raw = self.native().get_password('openmiraie-mcp', 'session-v1')
            value = json.loads(raw) if raw else {}
            if not isinstance(value, dict):
                raise ValueError()
            return value
        except Exception:
            raise Fault('store_unavailable') from None

    def save(self, data):
        try:
            # One atomic native credential item includes local selection.
            self.native().set_password('openmiraie-mcp', 'session-v1', json.dumps(data))
        except Exception:
            raise Fault('store_unavailable') from None

    def clear(self):
        try:
            if self.native().get_password('openmiraie-mcp', 'session-v1') is not None:
                self.native().delete_password('openmiraie-mcp', 'session-v1')
        except Exception:
            raise Fault('store_unavailable') from None

class HTTP:
    def __init__(self, transport=None):
        self.client = httpx.Client(timeout=15, follow_redirects=False, trust_env=False, transport=transport)

    def request(self, base, path, method='GET', body=None, token=None):
        headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        try:
            with self.client.stream(method, base + path, json=body, headers=headers) as response:
                if response.status_code in (401, 406):
                    raise Fault('expired')
                if not 200 <= response.status_code < 300:
                    raise Fault('service_error')
                raw = bytearray()
                for block in response.iter_bytes():
                    raw.extend(block)
                    if len(raw) > 4 * 1024 * 1024:
                        raise Fault('unexpected_response')
                return json.loads(raw) if raw else {}
        except httpx.HTTPError:
            raise Fault('offline') from None
        except (ValueError, UnicodeError):
            raise Fault('unexpected_response') from None

class API:
    # Caller holds Store.locked for each complete operation, including refresh/readback.
    def __init__(self, store, http=None):
        self.store, self.http = store, http or HTTP()

    def session_from(self, result, previous=None):
        if not isinstance(result, dict) or not isinstance(result.get('accessToken'), str) or not result['accessToken']:
            raise Fault('unexpected_response')
        data = dict(previous or {})
        data['accessToken'] = result['accessToken']
        if isinstance(result.get('refreshToken'), str) and result['refreshToken']:
            data['refreshToken'] = result['refreshToken']
        expiry = result.get('expiresIn')
        try:
            data['expires_at'] = time.time() + max(0, float(expiry)) if expiry is not None else None
        except (ValueError, TypeError):
            raise Fault('unexpected_response') from None
        if data['expires_at'] is not None and not math.isfinite(data['expires_at']):
            raise Fault('unexpected_response')
        data['expired'] = False
        return data

    def login(self, identity, password):
        if not identity or not password or len(identity) > 320 or len(password) > 1024:
            raise Fault('invalid_credentials')
        try:
            result = self.http.request(AUTH, 'userManagement/login', 'POST', {
                'email' if '@' in identity else 'mobile': identity,
                'password': password, 'clientId': CLIENT, 'scope': 'an_' + secrets.token_hex(12)})
        except Fault as exc:
            if exc.code == 'expired':
                raise Fault('invalid_credentials') from None
            raise
        self.store.save(self.session_from(result))

    def refresh(self, session):
        if not session.get('refreshToken'):
            self.expire(session)
        try:
            result = self.http.request(AUTH, 'userManagement/tokenRefresh', 'POST',
                                       {'clientId': CLIENT, 'token': session['refreshToken']})
            updated = self.session_from(result, session)
        except Fault as exc:
            if exc.code in ('expired', 'unexpected_response'):
                self.expire(session)
            raise
        self.store.save(updated)
        return updated

    def expire(self, session):
        session['expired'] = True
        self.store.save(session)
        raise Fault('expired')

    def request(self, path, method='GET', body=None):
        session = self.store.load()
        if not session.get('accessToken'):
            raise Fault('unconfigured')
        if session.get('expired'):
            raise Fault('expired')
        if session.get('expires_at') is not None and session['expires_at'] <= time.time():
            # Never resume an appliance write across session recovery.
            if method != 'GET':
                raise Fault('expired')
            session = self.refresh(session)
        try:
            return self.http.request(BASE, path, method, body, session['accessToken'])
        except Fault as exc:
            if exc.code != 'expired':
                raise
            if method != 'GET':
                self.expire(session)
            session = self.refresh(session)
        try:
            return self.http.request(BASE, path, 'GET', token=session['accessToken'])
        except Fault as exc:
            if exc.code == 'expired':
                self.expire(session)
            raise
