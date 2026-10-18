"""Model-specific command policy. No guessed control keys or generic-family writes."""
import hashlib
import math
import time
from datetime import datetime, timezone, timedelta
from urllib.parse import quote, urlencode
from .core import Fault

CAPS = {
    'power': ('PowerStatus', 1), 'mode': ('ACMode', 0),
    'temperature': ('ACTemperature', 2), 'fan': ('ACFanSpeed', 4),
    'display': ('DisplayControl', 8), 'eco': ('ECOMode', 10),
    'powerful': ('PowerfulMode', 3), 'capacity': ('Convertible', 28),
    'vertical_swing': ('VerticalSwing', 5), 'horizontal_swing': ('HorizontalSwing', 6),
}
CHOICES = {'power': ['on', 'off'], 'mode': ['auto', 'cool', 'dry', 'fan'],
           'fan': ['auto', 'quiet', 'low', 'medium', 'high'], 'display': ['on', 'off'],
           'eco': ['on', 'off'], 'powerful': ['on', 'off'],
           'capacity': [0, 40, 50, 60, 70, 80, 90, 100, 110]}
SWING_ACTIONS = ('vertical_swing', 'horizontal_swing')
STATUS_KEYS = {'ps', 'acmd', 'actmp', 'acfs', 'acdc', 'acem', 'acpm', 'cnv', 'rmtmp',
               'onlineStatus', 'ts', 'rssi', 'acec', 'acsp', 'acjs', 'achs', 'acvs'}

def opaque(value):
    return 'device_' + hashlib.sha256(value.encode()).hexdigest()[:20]

def scalar(value):
    return value if isinstance(value, (str, int, float, bool)) or value is None else None

def availability(value):
    """Normalize explicit cloud availability values; missing/unknown stays unknown."""
    if isinstance(value, str):
        value = value.strip().lower()
        if value in ('online', 'true'):
            return 'online'
        if value in ('offline', 'false'):
            return 'offline'
    elif isinstance(value, (bool, int, float)):
        if value == 1:
            return 'online'
        if value == 0:
            return 'offline'
    return 'unknown'

def fields(raw):
    if not isinstance(raw, dict):
        raise Fault('unexpected_response')
    # Only accept a direct map or a single wrapper-only envelope. Never discard
    # sibling fields by blindly walking every vaguely named dictionary.
    for _ in range(4):
        if len(raw) == 1 and next(iter(raw)) in ('status', 'data', 'params', 'state'):
            raw = next(iter(raw.values()))
            if not isinstance(raw, dict):
                raise Fault('unexpected_response')
        else:
            break
    if not any(k in raw for k in STATUS_KEYS):
        raise Fault('unexpected_response')
    return raw

def same(a, b):
    if a is None or b is None or isinstance(a, bool) or isinstance(b, bool):
        return a is b
    try:
        return math.isfinite(float(a)) and float(a) == float(b)
    except (ValueError, TypeError):
        return str(a).casefold() == str(b).casefold()

class Profile:
    def __init__(self, model, device):
        if isinstance(model, dict) and 'controlCapabilities' not in model and isinstance(model.get('data'), dict):
            model = model['data']
        if not isinstance(model, dict):
            raise Fault('unexpected_response')
        self.model, self.device = model, device
        self.supported = (str(device.get('modelId')) == '140013'
                          and str(model.get('modelId', '140013')) == '140013'
                          and device.get('category') == 'AC'
                          and not device.get('thirdPartyService') and not device.get('parentDeviceId')
                          and not model.get('thirdPartyService'))
        self.attrs = {}
        for cap in model.get('controlCapabilities', []):
            if not isinstance(cap, dict):
                continue
            attrs = cap.get('attributes', [])
            if len(attrs) == 1 and isinstance(attrs[0], dict) and isinstance(attrs[0].get('key'), str):
                if attrs[0]['key'] not in ('cnt', 'sid', 'ki', 'deviceId'):
                    self.attrs[cap.get('capability')] = attrs[0]

    def attr(self, action):
        if not self.supported or CAPS[action][0] not in self.attrs:
            raise Fault('unsupported')
        return self.attrs[CAPS[action][0]]

    def value(self, state, action):
        return state.get(self.attr(action)['key'])

    def validate(self, action, value, mode=None):
        attr = self.attr(action)
        if action in SWING_ACTIONS:
            if type(value) is not int or value not in self.swing_positions(action):
                raise Fault('invalid_request')
            return value
        if action == 'temperature':
            if isinstance(value, bool) or not isinstance(value, (float, int)) or not math.isfinite(value):
                raise Fault('invalid_request')
            minimum, maximum, step = self.bounds(mode)
            if not minimum <= value <= maximum or abs((value-minimum)/step-round((value-minimum)/step)) > 1e-6:
                raise Fault('invalid_request')
            return str(float(value))
        choices = attr.get('intValues' if action == 'capacity' else 'values', [])
        if isinstance(value, bool) or value not in CHOICES[action] or value not in choices:
            raise Fault('invalid_request')
        return int(value) if action == 'capacity' else value

    def swing_positions(self, action):
        """Model-advertised integer positions; no inferred physical direction labels."""
        attr = self.attr(action)
        def integer(value):
            if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value) or int(value) != value:
                raise Fault('unsupported')
            return int(value)
        positions = None
        if 'range' in attr:
            ranges = attr['range']
            if isinstance(ranges, list):
                if len(ranges) != 1:
                    raise Fault('unsupported')
                ranges = ranges[0]
            if not isinstance(ranges, dict) or 'mode' in ranges:
                raise Fault('unsupported')
            def bound(name):
                entries = ranges.get(name)
                if not isinstance(entries, list) or len(entries) != 1 or not isinstance(entries[0], dict) or entries[0].get('unit'):
                    raise Fault('unsupported')
                return integer(entries[0].get('value'))
            low, high = bound('min'), bound('max')
            # The shipped SwingControlViewHolder increments by one when no
            # explicit integer list is present. Honor an advertised step too.
            step = bound('increment') if 'increment' in ranges else 1
            if not 0 <= low <= high <= 5 or step < 1:
                raise Fault('unsupported')
            positions = list(range(low, high + 1, step))
        if 'intValues' in attr:
            values = attr['intValues']
            if not isinstance(values, list) or not values or len(values) > 6:
                raise Fault('unsupported')
            values = [integer(v) for v in values]
            if len(set(values)) != len(values) or any(v < 0 or v > 5 for v in values):
                raise Fault('unsupported')
            if positions is not None and any(v not in positions for v in values):
                raise Fault('unsupported')
            positions = values
        if positions is None:
            raise Fault('unsupported')
        return positions

    def bounds(self, mode):
        attr = self.attr('temperature')
        ranges = attr.get('range', [])
        ranges = [ranges] if isinstance(ranges, dict) else ranges
        for item in ranges:
            if not isinstance(item, dict) or item.get('mode', mode) != mode:
                continue
            result = []
            for name in ('min', 'max', 'increment'):
                found = None
                for value in item.get(name, []):
                    if isinstance(value, dict) and str(value.get('unit', '')).lower() in ('celcius','celsius','c','°c'):
                        found = float(value['value'])
                if found is None or not math.isfinite(found):
                    raise Fault('unsupported')
                result.append(found)
            lo, hi, step = result
            if lo < 16 or hi > 30 or lo > hi or step <= 0:
                raise Fault('unsupported')
            return result
        raise Fault('unsupported')

    def swing_guidance(self, action):
        positions = self.swing_positions(action)
        result = {'positions': positions, 'direction_mapping': 'unverified',
                  'evidence': 'static_only',
                  'next_action': 'Use an explicitly requested numeric position; horizontal direction labels remain unverified.'}
        if action == 'vertical_swing':
            observed = {
                0: 'Continuous vertical oscillation, observed by the owner.',
                1: 'Starting position in the tested sequence; absolute angle unverified.',
                2: 'Moved downward from position 1, observed by the owner.',
                3: 'Moved farther downward from position 2, observed by the owner.',
                4: 'Moved farther downward from position 3, observed by the owner.',
                5: 'Moved farther downward from position 4 and stayed fixed; lowest tested position.',
            }
            result.update({
                'direction_mapping': 'partially_verified',
                'evidence': 'live_readback_and_owner_observation',
                'observed_on': '2026-10-19',
                'scope': 'One model 140013 unit; other firmware/units and exact angles are unverified. This is prior evidence, not physical confirmation of a new command.',
                'position_observations': {str(v): observed[v] for v in positions},
                'next_action': 'On the tested model use 0 for oscillation or 5 for the lowest tested fixed position, only if advertised. Codes 1 through 5 were progressively downward. Verify new commands by readback and owner observation; inspect horizontal observations separately.',
            })
        elif action == 'horizontal_swing':
            observed = {
                0: 'Continuous horizontal oscillation, observed by the owner.',
                1: 'Appeared centered / straight ahead on owner retest from position 2; exact angle and fixedness not separately established.',
                2: 'Owner observed left-facing airflow while facing the AC; relative ordering versus position 3 remains unverified.',
                3: 'Moved slightly left from owner-observed centered position 1 while facing the AC. Earlier center label is withdrawn; exact angle unverified.',
                4: 'Moved to the observer right from position 3 while facing the AC.',
                5: 'Moved farther to the observer right from position 4 while facing the AC and stayed fixed; rightmost tested position.',
            }
            result.update({
                'direction_mapping': 'partially_verified',
                'evidence': 'live_readback_and_owner_observation',
                'observed_on': '2026-10-19',
                'scope': 'One model 140013 unit; observer faces the AC. Other firmware/units and exact angles are unverified. Prior evidence is not physical confirmation of a new command.',
                'observer_perspective': 'Facing the air conditioner; left/right are the observer left/right.',
                'position_observations': {str(v): observed[v] for v in positions},
                'next_action': 'On the tested model use 0 for horizontal oscillation, 1 for owner-observed centered airflow, 2 or 3 for observed left-facing airflow, or 5 for the rightmost tested fixed position, only if advertised. Transitions 3 to 4 and 4 to 5 moved right while facing the AC. The latest 1 to 3 comparison supersedes the earlier center label for 3. Do not infer the relative order of 2 and 3 or the full range ordering.',
            })
        return result

    def inspect(self):
        # Export schema fields, never arbitrary raw metadata (may contain secrets).
        sections = {}
        for section in ('controlCapabilities', 'statusCapabilitiesV2', 'configCapabilities'):
            sections[section] = []
            for cap in self.model.get(section, []):
                if not isinstance(cap, dict):
                    continue
                attrs = []
                for attr in cap.get('attributes', []):
                    if isinstance(attr, dict):
                        attrs.append({k: schema_value(v) for k, v in attr.items()
                                      if k in ('key', 'values', 'intValues', 'range')})
                sections[section].append({'capability': scalar(cap.get('capability')), 'attributes': attrs})
        swing = {}
        for action in SWING_ACTIONS:
            if not self.supported or CAPS[action][0] not in self.attrs:
                continue
            try:
                swing[action] = self.swing_guidance(action)
            except Fault:
                swing[action] = {'positions': [], 'direction_mapping': 'unverified',
                                 'evidence': 'static_only', 'next_action': 'Unsupported swing schema; read-only inspection only.'}
        return {'support': 'limited' if self.supported else 'unsupported',
                'evidence': 'Model 140013: live display/temperature/both-swing-axis readbacks; owner observed display-on and scoped swing behavior on one unit. Exact swing angles, the relative horizontal order of 2/3 and other controls remain unverified physically; see testing.md for scope and limits.',
                'writes': [a for a, (name, _) in CAPS.items() if name in self.attrs
                           and (a not in SWING_ACTIONS or swing.get(a, {}).get('positions'))] if self.supported else [],
                'swing': swing, 'schema': sections}

def schema_value(value):
    if isinstance(value, list):
        return [schema_value(v) for v in value]
    if isinstance(value, dict):
        return {k: schema_value(v) for k, v in value.items() if k in ('mode','min','max','increment','unit','value')}
    return scalar(value)

class Service:
    def __init__(self, store, api, sleep=time.sleep):
        self.store, self.api, self.sleep = store, api, sleep

    def session_status(self):
        session = self.store.load()
        state = 'configured' if session.get('accessToken') else 'unconfigured'
        if session.get('expired') or (session.get('expires_at') is not None and session['expires_at'] <= time.time()):
            state = 'expired'
        return {'state': state, 'default_device': opaque(session['deviceId']) if session.get('deviceId') else None,
                'next_action': ('Run connection_test for read-only connectivity, then discover_devices and select_default_device.'
                                if state == 'configured' else 'Call begin_setup; enter credentials only in the local browser.'),
                'network': 'not_checked', 'storage': 'native OS credential store; no password saved'}

    def discover(self):
        homes = self.api.request('homeManagement/homes')
        if not isinstance(homes, list):
            raise Fault('unexpected_response')
        devices = []
        for home in homes:
            for space in home.get('spaces', []):
                for device in space.get('devices', []):
                    if not isinstance(device.get('deviceId'), str) or not device['deviceId']:
                        continue
                    devices.append({**device, '_home': home.get('homeName', 'Home'),
                                    '_space': space.get('spaceName', 'Space')})
        return devices

    def discovery(self):
        devices = self.discover()
        return {'devices': [{'device': opaque(d['deviceId']), 'name': scalar(d.get('deviceName', 'Appliance')),
                             'home': scalar(d['_home']), 'space': scalar(d['_space']),
                             'category': scalar(d.get('category')), 'modelId': scalar(d.get('modelId')),
                             'modelNumber': scalar(d.get('modelNumber')),
                             'parent': opaque(d['parentDeviceId']) if d.get('parentDeviceId') else None,
                             'partner_service': bool(d.get('thirdPartyService')),
                             'support': 'inspect capabilities before use'} for d in devices],
                'next_action': 'Choose a device explicitly with select_default_device.' if devices else
                'No devices found. Check this MirAIe India account in the official app; setup never pairs appliances.'}

    def device(self, ref=None):
        if ref is None:
            selected = self.store.load().get('deviceId')
            ref = opaque(selected) if selected else None
        for device in self.discover():
            if ref == opaque(device['deviceId']):
                return device
        raise Fault('invalid_request')

    def profile(self, device):
        model_id = device.get('modelId')
        if not model_id:
            raise Fault('unsupported')
        return Profile(self.api.request('deviceManagement/deviceModels/modelId/' + quote(str(model_id), safe='')), device)

    def select(self, ref):
        device = self.device(ref)
        profile = self.profile(device)
        session = self.store.load()
        session['deviceId'] = device['deviceId']
        self.store.save(session)
        return {'device': ref, **profile.inspect(), 'next_action': 'Run reported_status for a read-only snapshot.'}

    def read(self, device):
        return fields(self.api.request('deviceManagement/devices/' + quote(device['deviceId'], safe='') + '/mobile/status'))

    def status(self, ref=None):
        device = self.device(ref)
        raw = self.read(device)
        reported = {k: scalar(v) for k,v in raw.items() if k in STATUS_KEYS}
        room = reported.get('rmtmp')
        try:
            room = float(room)
            if room == -1 or not math.isfinite(room):
                room = None
        except (TypeError, ValueError):
            room = None
        return {'device': opaque(device['deviceId']), 'reported': reported, 'requested': None,
                'observed_at': datetime.now(timezone.utc).isoformat(), 'source': 'REST mobile/status',
                'room_temperature_c': room, 'fan_setting': reported.get('acfs'),
                'measured_fan_rpm': None, 'physical_confirmation': False,
                'availability': availability(raw.get('onlineStatus'))}

    def energy(self, ref=None, days=7):
        if not 1 <= days <= 31:
            raise Fault('invalid_request')
        device = self.device(ref)
        if not self.profile(device).supported:
            raise Fault('unsupported')
        today = datetime.now(timezone(timedelta(hours=5, minutes=30))).date()
        query = urlencode({'grain':'Daily', 'startDate':(today-timedelta(days=days-1)).strftime('%d%m%Y'),
                           'endDate':today.strftime('%d%m%Y')})
        data = self.api.request('powerConsumption/devices/' + quote(device['deviceId'], safe='') + '?' + query)
        if isinstance(data, dict):
            data = data.get('data')
        if not isinstance(data, list):
            raise Fault('unexpected_response')
        return {'unit':'kWh', 'today_may_be_partial':True, 'instantaneous_power':False,
                'days':[{'day':scalar(x.get('day')), 'energy_kwh':scalar(x.get('power'))} for x in data if isinstance(x,dict)]}

    def control(self, action, value, ref=None):
        device = self.device(ref)
        profile = self.profile(device)
        state = self.read(device)
        mode = profile.value(state, 'mode')
        target = profile.validate(action, value, mode)
        # Unknown availability is not a license to actuate.
        if availability(state.get('onlineStatus')) != 'online':
            raise Fault('invalid_request')
        if action not in ('power', 'display') and not same(profile.value(state, 'power'), 'on'):
            raise Fault('invalid_request')
        if action in ('temperature', 'fan', 'powerful', *SWING_ACTIONS) and state.get('acec') not in ('off', False, 0):
            raise Fault('invalid_request')
        if action == 'temperature' and mode not in ('auto', 'cool', 'dry'):
            raise Fault('invalid_request')
        if action == 'fan' and mode not in ('auto', 'cool', 'fan'):
            raise Fault('invalid_request')
        if action in ('eco', 'powerful') and mode != 'cool':
            raise Fault('invalid_request')
        # Active sleep reset/JetStream semantics are not implemented.
        if action in ('power', 'eco', 'powerful') and state.get('acsp') not in (None, 'off', 'none'):
            raise Fault('unsupported')
        plan = []
        if action == 'eco' and value == 'on':
            plan = [('eco', 'on'), ('temperature', profile.validate('temperature', 26, mode))]
        elif action == 'powerful' and value == 'on':
            capacity = profile.value(state, 'capacity')
            if capacity is None:
                raise Fault('invalid_request')
            if not same(capacity, 0):
                plan.append(('capacity', profile.validate('capacity', 0, mode)))
            plan.append((action, target))
        elif action == 'capacity' and value != 0:
            if state.get('acjs') not in (None, 'off'):
                raise Fault('unsupported')
            powerful = profile.value(state, 'powerful')
            if powerful is None:
                raise Fault('invalid_request')
            if same(powerful, 'on'):
                plan.append(('powerful', profile.validate('powerful', 'off', mode)))
            plan.append((action, target))
        else:
            plan = [(action, target)]
        # Preflight every step before any write, including Eco's temperature side effect.
        for planned_action, _ in plan:
            if planned_action in ('temperature', 'fan', 'powerful', *SWING_ACTIONS) and state.get('acec') not in ('off', False, 0):
                raise Fault('invalid_request')
            if planned_action == 'powerful' and mode != 'cool':
                raise Fault('invalid_request')
        original_token = self.store.load().get('accessToken')
        steps = []
        for step_action, step_value in plan:
            if steps and (self.store.load().get('accessToken') != original_token
                          or availability(state.get('onlineStatus')) != 'online'
                          or not same(profile.value(state, 'power'), 'on')
                          or profile.value(state, 'mode') != mode
                          or (step_action in ('temperature', 'fan', 'powerful', *SWING_ACTIONS) and state.get('acec') not in ('off', False, 0))):
                break
            key = profile.attr(step_action)['key']
            step = {'action':step_action, 'requested':step_value, 'accepted':False,
                    'reported_matching':False, 'reported':None}
            steps.append(step)
            try:
                self.api.request('deviceManagement/control', 'POST', {'deviceId':device['deviceId'],
                    'control_params':{'cnt':'an','sid':'0', key:step_value, 'ki':CAPS[step_action][1]}})
                step['accepted'] = True
                for delay in (3.0, 1.2, 1.8):
                    self.sleep(delay)
                    state = self.read(device)
                    step['reported'] = scalar(state.get(key))
                    if same(state.get(key), step_value):
                        step['reported_matching'] = True
                        break
                if not step['reported_matching']:
                    break
            except Fault as exc:
                step['error'] = exc.public()
                step['acceptance_unknown'] = not step['accepted'] and exc.code == 'offline'
                break
        complete = len(steps) == len(plan) and all(s['reported_matching'] for s in steps)
        result = {'complete':complete, 'steps':steps, 'pending_steps':len(plan)-len(steps),
                  'physical_confirmation':False, 'next_action': 'Read reported_status before any new command; commands are never retried automatically.'}
        if action in SWING_ACTIONS:
            result['swing_guidance'] = profile.swing_guidance(action)
        return result
