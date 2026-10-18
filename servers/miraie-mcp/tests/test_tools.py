"""Exercise every registered tool through the official SDK with synthetic state."""
import json
import pytest
from miraie_mcp.onboarding import Setup
from miraie_mcp.server import create_server

TOOL_NAMES = {
    'session_status', 'begin_setup', 'cancel_setup', 'logout', 'connection_test',
    'discover_devices', 'select_default_device', 'capabilities', 'reported_status',
    'daily_energy', 'control_ac',
}

async def invoke(server, name, arguments=None, error=False):
    result = await server.call_tool(name, arguments or {})
    assert result.is_error is error, result
    return json.loads(result.content[0].text)

@pytest.mark.asyncio
async def test_every_registered_tool_and_onboarding_resource(service):
    setup = Setup(service, opener=lambda _: True)
    server = create_server(service, setup)
    seen = set()
    async def call(name, arguments=None, error=False):
        seen.add(name)
        return await invoke(server, name, arguments, error)
    try:
        assert {tool.name for tool in await server.list_tools()} == TOOL_NAMES
        assert (await call('session_status'))['state'] == 'configured'
        discovered = await call('discover_devices')
        handle = discovered['devices'][0]['device']
        assert (await call('connection_test'))['devices_found'] == 1
        assert (await call('select_default_device', {'device': handle}))['device'] == handle
        assert 'display' in (await call('capabilities'))['writes']
        assert (await call('reported_status'))['availability'] == 'online'
        assert (await call('daily_energy', {'days': 7}))['unit'] == 'kWh'
        assert (await call('control_ac', {'action':'display', 'value':'off'}))['complete']
        assert (await call('begin_setup'))['state'] == 'setup_opened'
        assert (await call('begin_setup'))['state'] == 'setup_active'
        assert (await call('cancel_setup'))['state'] == 'setup_cancelled'
        assert not setup.active
        assert (await call('session_status'))['state'] == 'configured'
        resources = await server.list_resources()
        assert [str(r.uri) for r in resources] == ['miraie://onboarding']
        resource = list(await server.read_resource('miraie://onboarding'))
        assert json.loads(resource[0].content)['state'] == 'configured'
        assert (await call('logout'))['state'] == 'unconfigured'
        assert service.store.load() == {}
        assert (await call('session_status'))['default_device'] is None
        assert (await call('discover_devices', error=True))['state'] == 'unconfigured'
        resource = list(await server.read_resource('miraie://onboarding'))
        assert json.loads(resource[0].content)['state'] == 'unconfigured'
        assert seen == TOOL_NAMES
    finally:
        setup.cancel()

@pytest.mark.asyncio
@pytest.mark.parametrize('action,value', [
    ('power','off'), ('mode','dry'), ('temperature',26), ('fan','high'),
    ('display','off'), ('eco','on'), ('powerful','on'), ('capacity',80),
    ('vertical_swing',5), ('horizontal_swing',0),
])
async def test_all_control_actions_through_sdk(service, action, value):
    server = create_server(service)
    result = await invoke(server, 'control_ac', {'action': action, 'value': value})
    assert result['complete']
    assert all(step['accepted'] and step['reported_matching'] for step in result['steps'])
    assert result['physical_confirmation'] is False

@pytest.mark.asyncio
async def test_tool_errors_are_safe_and_do_not_write(service):
    server = create_server(service)
    assert (await invoke(server, 'select_default_device', {'device':'invalid-handle'}, error=True))['state'] == 'invalid_request'
    assert (await invoke(server, 'daily_energy', {'days':32}, error=True))['state'] == 'invalid_request'
    assert (await invoke(server, 'control_ac', {'action':'temperature', 'value':31}, error=True))['state'] == 'invalid_request'
    assert not any(path == 'deviceManagement/control' for path, *_ in service.api.http.calls)


@pytest.mark.asyncio
async def test_swing_observations_reach_agents_through_sdk(service):
    server = create_server(service)
    capabilities = await invoke(server, 'capabilities')
    vertical = capabilities['swing']['vertical_swing']
    assert 'oscillation' in vertical['position_observations']['0']
    assert 'lowest tested position' in vertical['position_observations']['5']
    result = await invoke(server, 'control_ac', {'action':'vertical_swing', 'value':0})
    assert result['swing_guidance'] == vertical
    assert result['physical_confirmation'] is False


@pytest.mark.asyncio
async def test_horizontal_observations_reach_agents_without_inventing_positions(service):
    server = create_server(service)
    capabilities = await invoke(server, 'capabilities')
    horizontal = capabilities['swing']['horizontal_swing']
    assert 'Moved slightly left' in horizontal['position_observations']['3']
    assert 'Earlier center label is withdrawn' in horizontal['position_observations']['3']
    assert 'observer right' in horizontal['position_observations']['4']
    assert 'stayed fixed' in horizontal['position_observations']['5']
    assert 'oscillation' in horizontal['position_observations']['0']
    assert 'Facing the air conditioner' in horizontal['observer_perspective']
    assert 'Appeared centered' in horizontal['position_observations']['1']
    assert 'relative ordering versus position 3 remains unverified' in horizontal['position_observations']['2']
    result = await invoke(server, 'control_ac', {'action':'horizontal_swing', 'value':0})
    assert result['swing_guidance'] == horizontal
    assert result['physical_confirmation'] is False
