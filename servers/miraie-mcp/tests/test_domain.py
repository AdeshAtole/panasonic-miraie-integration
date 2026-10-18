import pytest
from miraie_mcp.core import Fault
from miraie_mcp.domain import opaque

def writes(s): return [x[2]['control_params'] for x in s.api.http.calls if x[0]=='deviceManagement/control']

def test_discovery_selection_and_hierarchy(service):
    service.api.http.devices.append({'deviceId':'second','deviceName':'Fan','category':'FAN','modelId':'other'})
    result=service.discovery()
    assert len(result['devices'])==2
    assert result['devices'][0]['home']=='Test Home'
    assert 'synthetic-device' not in str(result)
    session=service.store.load(); session.pop('deviceId'); service.store.save(session)
    with pytest.raises(Fault): service.status()
    assert service.select(opaque('second'))['support']=='unsupported'
    assert not writes(service)

def test_no_devices(service):
    service.api.http.devices=[]
    assert service.discovery()['devices']==[]

def test_fractional_temperature_and_no_rpm(service):
    assert service.status()['room_temperature_c']==22.61
    assert service.status()['measured_fan_rpm'] is None
    service.api.http.state['rmtmp']=-1
    assert service.status()['room_temperature_c'] is None
    service.api.http.state.pop('onlineStatus')
    assert service.status()['availability']=='unknown'

@pytest.mark.parametrize('action,value', [('mode','heat'),('temperature',15),('temperature',25.5),('fan','turbo'),('capacity',55),('power',True)])
def test_invalid_values_never_write(service,action,value):
    with pytest.raises(Fault): service.control(action,value)
    assert not writes(service)

@pytest.mark.parametrize('state,action,value', [({'ps':'off'},'mode','cool'),({'acmd':'fan'},'temperature',24),
    ({'acmd':'dry'},'fan','low'),({'acec':'on'},'temperature',24),({'acec':'on'},'powerful','on'),
    ({'acmd':'auto'},'eco','on'),({'onlineStatus':'offline'},'display','off'),({'acsp':'active'},'power','on')])
def test_guards(service,state,action,value):
    service.api.http.state.update(state)
    with pytest.raises(Fault): service.control(action,value)
    assert not writes(service)

def test_power_and_display_are_independent(service):
    service.control('display','off')
    assert writes(service)==[{'cnt':'an','sid':'0','acdc':'off','ki':8}]
    assert service.api.http.state['ps']=='on'
    service.control('power','off')
    assert writes(service)[1]['ki']==1
    assert service.api.http.state['actmp']=='24'

def test_dry_does_not_force_low(service):
    assert service.control('mode','dry')['complete']
    assert len(writes(service))==1
    assert service.api.http.state['acfs']=='auto'

def test_eco_sequence_and_capacity_exclusion(service):
    assert service.control('eco','on')['complete']
    assert [x['ki'] for x in writes(service)]==[10,2]
    assert service.api.http.state['actmp']=='26.0'
    service.api.http.calls.clear()
    service.api.http.state['cnv']=80
    service.control('powerful','on')
    assert [x['ki'] for x in writes(service)]==[28,3]
    service.api.http.calls.clear()
    service.control('capacity',80)
    assert [x['ki'] for x in writes(service)]==[3,28]

def test_unconfirmed_stops_coupled_steps(service):
    service.api.http.match=False
    result=service.control('eco','on')
    assert not result['complete'] and result['pending_steps']==1
    assert result['steps'][0]['accepted'] and not result['steps'][0]['reported_matching']
    assert len(writes(service))==1
    assert len([c for c in service.api.http.calls if '/mobile/status' in c[0]])==4

def test_unsupported_partner_or_model(service):
    service.api.http.devices[0]['thirdPartyService']='partner'
    with pytest.raises(Fault): service.control('power','on')
    assert not writes(service)

def test_model_key_is_derived(service):
    service.api.http.model['controlCapabilities'][4]['attributes'][0]['key']='display_key'
    result=service.control('display','off')
    assert result['complete'] and writes(service)[0]['display_key']=='off'

def test_energy_preserves_decimal_and_partial(service):
    result=service.energy(days=7)
    assert result['days'][0]['energy_kwh']=='0.123'
    assert result['today_may_be_partial']

def test_redaction_allowlists(service):
    service.api.http.state.update({'password':'never-output','udpControlKey':'never-output', 'topic':'never-output'})
    service.api.http.model['privateToken']='never-output'
    service.api.http.model['controlCapabilities'][0]['attributes'][0]['registrationToken']='never-output'
    assert 'never-output' not in str(service.status())
    assert 'never-output' not in str(service.profile(service.device()).inspect())

def test_eco_preflight_does_not_toggle_during_cleaning(service):
    service.api.http.state['acec']='on'
    with pytest.raises(Fault): service.control('eco','on')
    assert writes(service)==[]

def test_session_change_stops_next_step(service):
    original=service.read
    count=0
    def read(device):
        nonlocal count
        count+=1
        if count==2:
            session=service.store.load(); session['accessToken']='rotated'; service.store.save(session)
        return original(device)
    service.read=read
    result=service.control('eco','on')
    assert not result['complete'] and result['pending_steps']==1
    assert len(writes(service))==1

def test_timeout_reports_unknown_acceptance(service):
    original=service.api.http.request
    def request(base,path,*args,**kwargs):
        if path=='deviceManagement/control': raise Fault('offline')
        return original(base,path,*args,**kwargs)
    service.api.http.request=request
    result=service.control('power','off')
    assert not result['complete'] and result['steps'][0]['acceptance_unknown']

def test_missing_range_disables_temperature(service):
    service.api.http.model['controlCapabilities'][2]['attributes'][0].pop('range')
    with pytest.raises(Fault): service.control('temperature',24)
    assert not writes(service)


@pytest.mark.parametrize('online', ['true', 'TRUE', True, 1, 'online'])
def test_online_forms_allow_display_and_coupled_steps(service, online):
    service.api.http.state.update({'onlineStatus': online, 'acdc': 'off'})
    assert service.status()['availability'] == 'online'
    result = service.control('display', 'on')
    assert result['complete']
    assert writes(service) == [{'cnt':'an', 'sid':'0', 'acdc':'on', 'ki':8}]
    assert service.api.http.state['ps'] == 'on'
    service.api.http.calls.clear()
    assert service.control('eco', 'on')['complete']
    assert [x['ki'] for x in writes(service)] == [10, 2]


@pytest.mark.parametrize('value,expected', [
    ('false','offline'), ('FALSE','offline'), (False,'offline'), (0,'offline'),
    ('offline','offline'), (None,'unknown'), ('unknown','unknown'),
    ('','unknown'), ('1','unknown'), (2,'unknown'), ({},'unknown'), ([], 'unknown')])
def test_offline_and_unknown_forms_never_write(service, value, expected):
    service.api.http.state['onlineStatus'] = value
    assert service.status()['availability'] == expected
    with pytest.raises(Fault):
        service.control('display', 'on')
    assert writes(service) == []


def test_first_readback_waits_three_seconds_after_change(service):
    delays = []
    service.sleep = delays.append
    original = service.read
    def read(device):
        if writes(service):
            assert delays and delays[0] >= 3
        return original(device)
    service.read = read
    assert service.control('temperature', 26)['complete']
    assert delays == [3.0]
    assert len(writes(service)) == 1
