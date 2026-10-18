"""Static-evidence swing builders with synthetic model metadata and readbacks."""
import pytest
from miraie_mcp.core import Fault
from miraie_mcp.domain import SWING_ACTIONS
from test_domain import writes


def attribute(service, action):
    name = 'VerticalSwing' if action == 'vertical_swing' else 'HorizontalSwing'
    return next(c['attributes'][0] for c in service.api.http.model['controlCapabilities'] if c['capability'] == name)


@pytest.mark.parametrize('action,key,index', [('vertical_swing','acvs',5), ('horizontal_swing','achs',6)])
@pytest.mark.parametrize('position', range(6))
def test_swing_payload_and_axis_independence(service, action, key, index, position):
    baseline = dict(service.api.http.state)
    result = service.control(action, position)
    assert result['complete']
    assert writes(service) == [{'cnt':'an','sid':'0',key:position,'ki':index}]
    assert service.api.http.state == {**baseline, key:position}
    assert result['physical_confirmation'] is False


@pytest.mark.parametrize('action', SWING_ACTIONS)
@pytest.mark.parametrize('value', [-1, 6, True, False, 2.0, 2.5, '2', 'down', 'auto', float('nan'), float('inf')])
def test_invalid_position_never_writes(service, action, value):
    with pytest.raises(Fault):
        service.control(action, value)
    assert not writes(service)


@pytest.mark.parametrize('action', SWING_ACTIONS)
@pytest.mark.parametrize('state', [{'ps':'off'}, {'onlineStatus':'false'}, {'onlineStatus':None}, {'acec':'on'}, {'acec':None}])
def test_power_availability_and_cleaning_guards(service, action, state):
    service.api.http.state.update(state)
    with pytest.raises(Fault):
        service.control(action, 3)
    assert not writes(service)


@pytest.mark.parametrize('schema', [
    {}, {'range':{}}, {'range':None}, {'range':[]},
    {'range':{'min':[{'value':0}], 'max':[{'value':6}]}},
    {'range':{'min':[{'value':4}], 'max':[{'value':2}]}},
    {'range':{'min':[{'value':False}], 'max':[{'value':5}]}},
    {'range':{'min':[{'value':0}], 'max':[{'value':5}], 'increment':[{'value':0}]}},
    {'range':{'min':[{'value':0}], 'max':[{'value':5}], 'increment':[{'value':0.5}]}},
    {'range':{'min':[{'value':0,'unit':'degrees'}], 'max':[{'value':5}]}},
    {'intValues':[0,True]}, {'intValues':[0,6]}, {'intValues':[0,0]},
    {'intValues':[]}, {'intValues':'0,5'}, {'intValues':[float('nan')]},
])
def test_malformed_schema_fails_closed(service, schema):
    attr = attribute(service, 'vertical_swing')
    attr.clear(); attr.update({'key':'acvs', **schema})
    with pytest.raises(Fault):
        service.control('vertical_swing', 3)
    assert not writes(service)
    info = service.profile(service.device()).inspect()
    assert 'vertical_swing' not in info['writes']
    assert info['swing']['vertical_swing']['positions'] == []


def test_narrow_range_and_advertised_step(service):
    attribute(service, 'vertical_swing')['range'] = {'min':[{'value':1}], 'max':[{'value':5}], 'increment':[{'value':2}]}
    info = service.profile(service.device()).inspect()
    assert info['swing']['vertical_swing']['positions'] == [1,3,5]
    with pytest.raises(Fault): service.control('vertical_swing', 2)
    assert not writes(service)
    assert service.control('vertical_swing', 3)['complete']


def test_explicit_integer_choices_and_conflicting_metadata(service):
    attr = attribute(service, 'horizontal_swing')
    attr.pop('range'); attr['intValues'] = [0,5]
    assert service.profile(service.device()).inspect()['swing']['horizontal_swing']['positions'] == [0,5]
    with pytest.raises(Fault): service.control('horizontal_swing', 3)
    assert not writes(service)
    assert service.control('horizontal_swing', 5)['complete']
    service.api.http.calls.clear()
    attr['range'] = {'min':[{'value':1}], 'max':[{'value':5}]}
    with pytest.raises(Fault): service.control('horizontal_swing', 5)
    assert not writes(service)


def test_missing_axis_and_unsupported_model(service):
    service.api.http.model['controlCapabilities'] = [c for c in service.api.http.model['controlCapabilities'] if c['capability'] != 'VerticalSwing']
    with pytest.raises(Fault): service.control('vertical_swing', 5)
    assert 'vertical_swing' not in service.profile(service.device()).inspect()['writes']
    service.api.http.devices[0]['modelId'] = 'unverified-model'
    with pytest.raises(Fault): service.control('horizontal_swing', 5)
    assert not writes(service)


def test_swing_key_derived_and_unconfirmed_command_not_resent(service):
    attribute(service, 'vertical_swing')['key'] = 'model_swing_key'
    assert service.control('vertical_swing', 5)['complete']
    assert writes(service)[0]['model_swing_key'] == 5
    service.api.http.calls.clear()
    service.api.http.match = False
    waits = []; service.sleep = waits.append
    result = service.control('horizontal_swing', 5)
    assert result['steps'][0]['accepted'] and not result['complete']
    assert not result['steps'][0]['reported_matching']
    assert len(writes(service)) == 1
    assert waits == [3.0,1.2,1.8]


def test_swing_metadata_does_not_guess_direction(service):
    info = service.profile(service.device()).inspect()
    for action in SWING_ACTIONS:
        assert action in info['writes']
        assert info['swing'][action]['positions'] == list(range(6))
    vertical = info['swing']['vertical_swing']
    assert vertical['direction_mapping'] == 'partially_verified'
    assert vertical['evidence'] == 'live_readback_and_owner_observation'
    assert 'oscillation' in vertical['position_observations']['0']
    assert 'stayed fixed' in vertical['position_observations']['5']
    assert 'position 4' in vertical['position_observations']['5']
    assert 'One model 140013 unit' in vertical['scope']
    horizontal = info['swing']['horizontal_swing']
    assert horizontal['direction_mapping'] == 'partially_verified'
    assert horizontal['evidence'] == 'live_readback_and_owner_observation'
    assert 'Facing the air conditioner' in horizontal['observer_perspective']
    assert 'oscillation' in horizontal['position_observations']['0']
    assert 'stayed fixed' in horizontal['position_observations']['5']
    assert 'from position 4' in horizontal['position_observations']['5']
    assert 'Appeared centered' in horizontal['position_observations']['1']
    assert 'relative ordering versus position 3 remains unverified' in horizontal['position_observations']['2']


@pytest.mark.parametrize('action', SWING_ACTIONS)
def test_position_observations_follow_actual_advertised_positions(service, action):
    attr = attribute(service, action)
    attr.pop('range'); attr['intValues'] = [1,3]
    guidance = service.profile(service.device()).inspect()['swing'][action]
    assert guidance['positions'] == [1,3]
    assert set(guidance['position_observations']) == {'1','3'}
    result = service.control(action, 3)
    assert result['swing_guidance'] == guidance
    assert result['physical_confirmation'] is False
