"""Five-minute, loopback-only onboarding. No password ever crosses MCP."""
from html import escape
from http.server import BaseHTTPRequestHandler, HTTPServer
import secrets
import threading
import time
from urllib.parse import parse_qs
import webbrowser
from filelock import FileLock, Timeout
from .core import Fault
from .setup_ui import shell, form, CSP

class Setup:
    def __init__(self, service, opener=webbrowser.open, lifetime=300):
        self.service, self.opener, self.lifetime = service, opener, lifetime
        self.server = None
        self.guard = threading.Lock()
        self.active = False

    def begin(self):
        with self.guard:
            if self.active:
                return {'state':'setup_active', 'next_action':'Finish the open local page, or call cancel_setup.'}
            self.service.store.prepare()
            self.lock = FileLock(str(self.service.store.directory / 'setup.lock'), timeout=0,
                                 mode=0o600, thread_local=False)
            try:
                self.lock.acquire()
            except Timeout:
                raise Fault('busy') from None
            try:
                self.bootstrap, self.cookie, self.csrf = [secrets.token_urlsafe(32) for _ in range(3)]
                self.claimed = False
                self.notice = ""
                self.notice_error = False
                self.selected_support = None
                self.selected_support_ref = None
                self.snapshot = None
                self.active = True
                self.deadline = time.monotonic() + self.lifetime
                self.server = HTTPServer(('127.0.0.1', 0), self.handler())
                self.server.timeout = 0.25
                self.origin = 'http://127.0.0.1:' + str(self.server.server_port)
                threading.Thread(target=self.serve, daemon=True).start()
                if not self.opener(self.origin + '/' + self.bootstrap):
                    self.active = False
                    return {'state':'browser_unavailable', 'next_action':'Run miraie-mcp login in a local terminal for hidden-input setup.'}
                return {'state':'setup_opened', 'expires_in_seconds':self.lifetime,
                        'next_action':'Use the local browser page to sign in, explicitly select a device, and optionally read status. Never paste credentials here.'}
            except Exception:
                self.active = False
                self.lock.release()
                raise Fault('offline') from None

    def serve(self):
        try:
            while self.active and time.monotonic() < self.deadline:
                self.server.handle_request()
        finally:
            self.active = False
            self.server.server_close()
            self.lock.release()

    def cancel(self):
        self.active = False
        return {'state':'setup_cancelled', 'next_action':'Call begin_setup to restart. A session already saved by successful login remains; use logout to remove it.'}

    def handler(self):
        setup = self
        class Handler(BaseHTTPRequestHandler):
            def setup(self):
                super().setup()
                self.connection.settimeout(3)

            def log_message(self, *args):
                pass

            def reply(self, code, body='', cookie=False, redirect=False):
                self.send_response(code)
                self.send_header('Content-Type', 'text/html; charset=utf-8')
                self.send_header('Cache-Control', 'no-store')
                # no-referrer makes real HTML POSTs send Origin: null.
                # Keep exact Origin enforcement, and disclose referrers only locally.
                self.send_header('Referrer-Policy', 'same-origin')
                self.send_header('X-Content-Type-Options', 'nosniff')
                self.send_header('Content-Security-Policy', CSP)
                self.send_header('Connection', 'close')
                if cookie:
                    self.send_header('Set-Cookie', 'miraie_setup=' + setup.cookie + '; HttpOnly; SameSite=Strict; Path=/; Max-Age=300')
                if redirect:
                    self.send_header('Location', '/')
                if not body and code >= 400:
                    body = self.problem('This page could not submit', 'The local security check did not pass. Return to your assistant and ask to open MirAIe setup again. Your account details have not been forwarded by this request.')
                data = body.encode()
                self.send_header('Content-Length', str(len(data)))
                self.end_headers()
                self.wfile.write(data)

            def valid(self, post=False):
                if not setup.active or time.monotonic() >= setup.deadline:
                    return False
                if self.headers.get('Host') != setup.origin.removeprefix('http://'):
                    return False
                if post and self.headers.get('Origin') != setup.origin:
                    return False
                if self.headers.get('Sec-Fetch-Site') == 'cross-site':
                    return False
                return True

            def authenticated(self):
                cookies = self.headers.get('Cookie', '').split('; ')
                return any(secrets.compare_digest(c, 'miraie_setup=' + setup.cookie) for c in cookies)

            def form(self, action, body, label, secondary=False):
                return form(setup.csrf, action, body, label, secondary)

            def problem(self, title, detail):
                return shell(title, detail, '<p class="hint">You can return to your assistant to restart setup.</p><a href="/">Return to setup</a>', notice='No appliance command was sent.', error=True)

            def page(self):
                with setup.service.store.locked():
                    session = setup.service.session_status()
                    step = 0
                    if session['state'] != 'configured':
                        title = 'Connect your account'
                        lead = 'Sign in with the account you use in the Panasonic MirAIe India app.'
                        body = self.form('login', '<label class="field" for="identity">Email or mobile</label><input id="identity" name="identity" type="text" required maxlength="320" autocomplete="username" placeholder="Your MirAIe email or mobile"><label class="field" for="password">Password</label><input id="password" name="password" type="password" required maxlength="1024" autocomplete="current-password" placeholder="Your account password"><p class="hint">For mobile sign-in, use the same number format as in the MirAIe app.</p>', 'Sign in')
                        body += '<div class="trust"><strong>Your credentials stay out of chat.</strong>Your password goes directly to Panasonic for sign-in and is never saved. Session tokens are kept in your computer’s credential store.</div>'
                    else:
                        choices = setup.service.discovery()['devices']
                        selected = next((d for d in choices if d['device'] == session.get('default_device')), None)
                        step = 2 if selected else 1
                        title = 'Your connection is ready' if selected else 'Choose your default device'
                        lead = 'Start by reading what your appliance reports. Changes remain your choice.' if selected else 'Choose the appliance your assistant should use when you do not name a device.'
                        options = '<fieldset class="choices"><legend>Your MirAIe devices</legend>'
                        for device in choices:
                            name = escape(str(device.get('name') or 'Appliance'))
                            location = escape(' / '.join(str(device.get(k) or '') for k in ('home','space')))
                            model = escape(str(device.get('modelNumber') or device.get('modelId') or 'Unknown model'))
                            hint = 'AC controls · checked on selection' if device.get('category') == 'AC' and str(device.get('modelId')) == '140013' and not device.get('partner_service') and not device.get('parent') else 'Read-only inspection'
                            options += '<label class="device"><input type="radio" name="device" required value="'+escape(device['device'],quote=True)+'"'+(' checked' if selected and selected['device']==device['device'] else '')+'><span><strong>'+name+'</strong><small>'+location+'</small><small>'+model+'</small><span class="tag">'+hint+'</span></span></label>'
                        options += '</fieldset>'
                        selection = self.form('select', options, 'Use this device') if choices else '<div class="empty"><strong>No devices found</strong><p>Check that this account has an appliance in the official MirAIe India app. Setup does not pair new devices.</p><a href="/">Check again</a></div>'
                        if selected:
                            support = {'limited':'Limited AC controls', 'unsupported':'Read-only inspection'}.get(setup.selected_support if setup.selected_support_ref == selected['device'] else None, 'Inspect capabilities with your assistant')
                            body = '<div class="selected"><span class="eyebrow">Default appliance</span><strong>'+escape(str(selected.get('name') or 'Appliance'))+'</strong><small>'+escape(str(selected.get('home') or ''))+' / '+escape(str(selected.get('space') or ''))+'</small><div class="tag">'+support+'</div></div>'
                            if setup.snapshot and setup.snapshot.get('device') == selected['device']:
                                snap = setup.snapshot
                                room = snap.get('room_temperature_c')
                                body += '<div class="metrics"><div class="metric">Connection<b>'+escape(str(snap.get('availability','unknown')).title())+'</b></div><div class="metric">Room temperature<b>'+escape(str(room)+' °C' if room is not None else 'Not reported')+'</b></div></div><p class="hint">Latest cloud report: '+'<time datetime="'+escape(str(snap.get('observed_at','')),quote=True)+'">'+escape(str(snap.get('observed_at','')))+ '</time>. This does not confirm physical operation.</p>'
                            body += self.form('status', '', 'Read reported status', True)
                            body += self.form('finish', '', 'Finish setup')
                            body += '<details><summary>Choose a different device</summary>'+selection+'</details>'
                            body += '<div class="trust"><strong>Read when you need to.</strong>No background polling or sensor history is enabled. Ask your assistant for a fresh reading, or explicitly request a supported change.</div>'
                        else:
                            body = selection + '<div class="trust"><strong>Support is checked per device.</strong>Known AC controls are limited to the documented model. Other appliances stay available for read-only inspection.</div>'
                return shell(title, lead, body, step, setup.deadline-time.monotonic(), setup.csrf, setup.notice, setup.notice_error)

            def do_GET(self):
                if not self.valid():
                    return self.reply(403)
                if not setup.claimed and secrets.compare_digest(self.path, '/' + setup.bootstrap):
                    setup.claimed = True
                    return self.reply(303, cookie=True, redirect=True)
                if self.path != '/' or not self.authenticated():
                    return self.reply(403)
                try:
                    self.reply(200, self.page())
                except Fault as exc:
                    self.reply(200, self.problem('Connection needs attention', str(exc)))
                except Exception:
                    self.reply(500, self.problem('Something needs attention', 'The response could not be understood. Restart setup from your assistant.'))

            def do_POST(self):
                if not self.valid(True) or self.path != '/' or not self.authenticated():
                    return self.reply(403)
                if self.headers.get('Content-Type') != 'application/x-www-form-urlencoded' or self.headers.get('Transfer-Encoding'):
                    return self.reply(400)
                try:
                    size = int(self.headers.get('Content-Length', '0'))
                    if not 0 < size <= 8192:
                        return self.reply(413)
                    data = parse_qs(self.rfile.read(size).decode(), strict_parsing=True)
                    if any(len(v) != 1 for v in data.values()) or not secrets.compare_digest(data.get('csrf',[''])[0], setup.csrf):
                        return self.reply(403, self.problem('This form is out of date', 'This form was already submitted or replaced. Return to setup for a fresh form.'))
                    setup.csrf = secrets.token_urlsafe(32)  # A form submission cannot be replayed.
                    action = data.get('action',[''])[0]
                    if action in ('cancel', 'finish'):
                        self.reply(200, shell('Setup complete' if action == 'finish' else 'Setup cancelled', 'You can close this tab and return to your assistant.', '<div class="success-icon" aria-hidden="true">✓</div><p>'+('Try asking: <strong>“What is my AC reporting?”</strong>' if action == 'finish' else 'Any completed sign-in remains saved. Ask your assistant to sign out if you want to remove it.')+'</p><div class="trust"><strong>You are in control.</strong>No appliance command was sent during setup.</div>', step=2))
                        setup.active = False
                        return
                    message = ''
                    with setup.service.store.locked():
                        if action == 'login':
                            setup.service.api.login(data.get('identity',[''])[0], data.get('password',[''])[0])
                            message = 'Signed in. Choose a default appliance below.'
                        elif action == 'select':
                            result = setup.service.select(data.get('device',[''])[0])
                            setup.selected_support = result['support']
                            setup.selected_support_ref = result['device']
                            setup.snapshot = None
                            message = 'Default device saved. You can read its reported status below.'
                        elif action == 'status':
                            result = setup.service.status()
                            setup.snapshot = result
                            message = 'Cloud status read. No settings were changed.'
                        else:
                            raise Fault('invalid_request')
                    data.clear()
                    setup.notice, setup.notice_error = message, False
                    self.reply(303, redirect=True)  # Refresh cannot resubmit credentials or actions.
                except Fault as exc:
                    # Safe error page never echoes posted fields or upstream text.
                    setup.notice, setup.notice_error = str(exc), True
                    self.reply(303, redirect=True)
                except Exception:
                    self.reply(400, self.problem('This request could not be processed', 'Return to setup and try a fresh form.'))
        return Handler
