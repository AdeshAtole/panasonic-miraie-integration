"""Official MCP SDK entry point; stdout is reserved for JSON-RPC."""
import argparse
from functools import wraps
import getpass
import json
import logging
import sys
from typing import Literal

from mcp.server import MCPServer
from mcp.types import ToolAnnotations, CallToolResult, TextContent
from .core import API, Fault, Store
from .domain import Service
from .onboarding import Setup

# HTTP trace logging could contain tokens or credentials. Only safe fixed errors
# are exposed; debugging payloads is intentionally not a supported setting.
logging.getLogger('httpx').setLevel(logging.CRITICAL)
logging.getLogger('httpcore').setLevel(logging.CRITICAL)

def create_server(service=None, setup=None):
    store = service.store if service else Store()
    service = service or Service(store, API(store))
    setup = setup or Setup(service)
    mcp = MCPServer('openMirAIe', version='0.1.0', log_level='CRITICAL',
                    instructions='Credentials belong only in the local setup browser or hidden-input CLI. Call session_status first. Appliance writes require an explicit user request. Inspect capabilities for model-specific swing positions and observed meanings. Horizontal left/right is from an observer facing the AC; never infer untested positions.')
    read = ToolAnnotations(readOnlyHint=True, destructiveHint=False, openWorldHint=True)
    local = ToolAnnotations(readOnlyHint=False, destructiveHint=False, openWorldHint=False)
    write = ToolAnnotations(readOnlyHint=False, destructiveHint=True, idempotentHint=False, openWorldHint=True)

    def safe(function):
        @wraps(function)
        def wrapped(*args, **kwargs):
            try:
                with store.locked():
                    result = function(*args, **kwargs)
                    # Defense in depth: even a token echoed into a friendly upstream
                    # label must not leave via a tool result.
                    session = store.load() if function.__name__ not in ('begin_setup', 'cancel_setup', 'logout') else {}
                    def redact(value):
                        if isinstance(value, str):
                            for name in ('accessToken', 'refreshToken'):
                                secret = session.get(name)
                                if secret:
                                    value = value.replace(secret, '[redacted]')
                            return value
                        if isinstance(value, list):
                            return [redact(v) for v in value]
                        if isinstance(value, dict):
                            return {k:redact(v) for k,v in value.items()}
                        return value
                    return redact(result)
            except Fault as exc:
                return CallToolResult(isError=True, content=[TextContent(type='text', text=json.dumps(exc.public()))])
            except Exception:
                return CallToolResult(isError=True, content=[TextContent(type='text', text=json.dumps({'state':'unexpected_response','next_action':'Run doctor; no raw error details are exposed.'}))])
        return wrapped

    @mcp.tool(annotations=read)
    @safe
    def session_status():
        """Report local configuration and exact next action without a network request."""
        return service.session_status()

    @mcp.tool(annotations=local)
    @safe
    def begin_setup():
        """Open a five-minute local browser sign-in. No credentials in tool arguments."""
        return setup.begin()

    @mcp.tool(annotations=local)
    @safe
    def cancel_setup():
        """Close this process's local setup page; retain any session already saved."""
        return setup.cancel()

    @mcp.tool(annotations=local)
    @safe
    def logout():
        """Remove only this server's local session and default selection. Does not revoke other apps."""
        setup.cancel()
        store.clear()
        return {'state':'unconfigured','next_action':'Call begin_setup to sign in again.'}

    @mcp.tool(annotations=read)
    @safe
    def connection_test():
        """Read discovery to test authentication/connectivity; never operate an appliance."""
        result = service.discovery()
        return {'state':'configured','devices_found':len(result['devices']), 'next_action':result['next_action']}

    @mcp.tool(annotations=read)
    @safe
    def discover_devices():
        """List homes/spaces/devices with opaque device handles; no automatic default selection."""
        return service.discovery()

    @mcp.tool(annotations=local)
    @safe
    def select_default_device(device: str):
        """Explicitly select a handle returned by discover_devices; no appliance writes."""
        return service.select(device)

    @mcp.tool(annotations=read)
    @safe
    def capabilities(device: str | None = None):
        """Inspect model capability schemas, support boundaries and model-specific swing position observations. For tested model 140013 vertical swing: 0 oscillates; positions 1 through 5 move progressively downward; 5 is the lowest tested fixed position. For horizontal swing, 0 oscillates, 1 appeared centered on retest, 2 points observer-left, and 3 moved slightly left from 1 while facing the AC; 3 to 4 to 5 moves right while facing the AC, and 5 stays fixed. The earlier center label for 3 is withdrawn. Relative order of 2/3 and exact angles remain unverified. Default must be selected explicitly."""
        return service.profile(service.device(device)).inspect()

    @mcp.tool(annotations=read)
    @safe
    def reported_status(device: str | None = None):
        """Fetch one cloud snapshot. After a change, wait at least 3 seconds before checking reported status. For observed swing-code meanings, call capabilities. Fractional room temperature is separate from fan setting; no RPM inference."""
        return service.status(device)

    @mcp.tool(annotations=read)
    @safe
    def daily_energy(device: str | None = None, days: int = 7):
        """On-demand Daily energy in kWh, 1–31 days; today's value may be partial."""
        return service.energy(device, days)

    @mcp.tool(annotations=write)
    @safe
    def control_ac(action: Literal['power','mode','temperature','fan','display','eco','powerful','capacity','vertical_swing','horizontal_swing'],
                   value: str | int | float, device: str | None = None):
        """Explicit AC command only. Send once; wait at least 3 seconds after each change before checking reported status. The server waits before its bounded readback; report partial completion and never automatically resend unconfirmed commands. Set mode separately and wait for confirmation before dependent settings. Swing actions vertical_swing/horizontal_swing require an integer position advertised by capabilities. On one tested model 140013 unit, vertical 0 oscillates, 1 through 5 move progressively downward, and 5 is the lowest tested fixed position. Check capabilities for scope and allowed positions; horizontal 0 oscillates, 1 appeared centered on retest, 2 points observer-left, and 3 moved slightly left from 1 while facing the AC; 3 to 4 to 5 moves right while facing the AC, and 5 is the rightmost tested fixed position. The earlier center label for 3 is withdrawn. Relative order of 2/3 and exact angles remain unverified. Prior observations are not physical confirmation of this command."""
        return service.control(action, value, device)

    @mcp.resource('miraie://onboarding')
    def onboarding_resource() -> str:
        """Safe first-run and session recovery information."""
        result = session_status()
        if isinstance(result, CallToolResult):
            return result.content[0].text
        return json.dumps(result)
    return mcp

def main():
    parser = argparse.ArgumentParser(description='Local MirAIe India MCP server')
    parser.add_argument('command', nargs='?', choices=['serve','doctor','login','config'], default='serve')
    args = parser.parse_args()
    if args.command == 'serve':
        create_server().run(transport='stdio')
        return
    if args.command == 'config':
        print(json.dumps({'mcpServers':{'miraie':{'command':sys.executable, 'args':['-m','miraie_mcp.server']}}}, indent=2))
        return
    store = Store()
    service = Service(store, API(store))
    try:
        with store.locked():
            if args.command == 'doctor':
                print(json.dumps({'python':sys.version.split()[0], **service.session_status(),
                                  'connection_test':'Use the MCP connection_test tool for an explicit read-only network check.'}, indent=2))
            else:
                if not sys.stdin.isatty():
                    raise Fault('invalid_request')
                identity = input('MirAIe India email or mobile: ')
                password = getpass.getpass('Password (hidden, never saved): ')
                service.api.login(identity, password)
                del password
                devices = service.discovery()['devices']
                for index, device in enumerate(devices, 1):
                    print(index, device['home'], '/', device['space'], '/', device['name'])
                if not devices:
                    print('No devices found. Check the account in the official app.')
                    return
                choice = int(input('Select a default appliance number (Ctrl-C cancels): '))
                if choice < 1 or choice > len(devices):
                    raise Fault('invalid_request')
                result = service.select(devices[choice-1]['device'])
                print('Selected. Support:', result['support'], 'Use reported_status for a read-only check.')
    except Fault as exc:
        print(json.dumps(exc.public()), file=sys.stderr)
        raise SystemExit(2)
    except (KeyboardInterrupt, EOFError, ValueError):
        print('Setup cancelled. Any successful login remains; use logout to remove it.', file=sys.stderr)
        raise SystemExit(2)

if __name__ == '__main__':
    main()
