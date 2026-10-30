"""One-shot, loopback-only Amazon authorization-code sign-in for setup."""
import getpass
from http.server import BaseHTTPRequestHandler, HTTPServer
import secrets
import time
from urllib.parse import parse_qs, urlencode, urlsplit
import webbrowser

from protocol import Fault, request

RETURN_URL = 'http://localhost:8765/callback'


def callback_code(host, path, state):
    parsed = urlsplit(path)
    values = parse_qs(parsed.query)
    if (host != 'localhost:8765' or parsed.path != '/callback'
            or values.get('state') != [state] or len(values.get('code', [])) != 1):
        return None
    return values['code'][0]


def login_owner(client):
    secret = getpass.getpass('Login with Amazon client secret (hidden, used once): ')
    state = secrets.token_urlsafe(32)
    received = []

    class Callback(BaseHTTPRequestHandler):
        def log_message(self, *_):
            pass  # Never log authorization codes or query strings.

        def do_GET(self):
            code = callback_code(self.headers.get('Host'), self.path, state)
            if code:
                received.append(code)
            self.send_response(200 if code else 400)
            self.send_header('Content-Type', 'text/plain; charset=utf-8')
            self.send_header('Cache-Control', 'no-store')
            self.send_header('Referrer-Policy', 'no-referrer')
            self.send_header('Content-Security-Policy', "default-src 'none'")
            self.end_headers()
            self.wfile.write(b'You can close this window and return to setup.' if code else b'Invalid sign-in response.')

    with HTTPServer(('127.0.0.1', 8765), Callback) as server:
        server.timeout = 1
        url = 'https://www.amazon.com/ap/oa?' + urlencode({
            'client_id': client, 'scope': 'profile:user_id', 'response_type': 'code',
            'redirect_uri': RETURN_URL, 'state': state})
        if not webbrowser.open(url):
            raise Fault('INTERNAL_ERROR')
        print('Finish Amazon sign-in in the browser. Waiting up to five minutes.')
        deadline = time.monotonic() + 300
        while not received and time.monotonic() < deadline:
            server.handle_request()
    if not received:
        raise Fault('INVALID_AUTHORIZATION_CREDENTIAL')
    token = request('https://api.amazon.com/auth/o2/token', {
        'grant_type': 'authorization_code', 'code': received[0], 'client_id': client,
        'client_secret': secret, 'redirect_uri': RETURN_URL}, timeout=15, form=True)['access_token']
    del secret
    info = request('https://api.amazon.com/auth/o2/tokeninfo?' + urlencode({'access_token': token}), timeout=15)
    if info.get('aud') != client or not info.get('user_id'):
        raise Fault('INVALID_AUTHORIZATION_CREDENTIAL')
    return info['user_id']
