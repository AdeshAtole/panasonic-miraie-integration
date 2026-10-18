"""Real subprocess JSON-RPC handshake, tool discovery and unauthenticated use."""
import json
import os
from pathlib import Path
import subprocess
import sys

def test_stdio_initialization_and_errors(tmp_path):
    runner=tmp_path/'runner.py'
    runner.write_text('''
from miraie_mcp.core import Store, API
from miraie_mcp.domain import Service
from miraie_mcp.server import create_server
from pathlib import Path
class Memory:
 def get_password(self,*args): return None
store=Store(Path(__file__).parent/'state', Memory())
create_server(Service(store, API(store))).run(transport='stdio')
''')
    messages=[{'jsonrpc':'2.0','id':1,'method':'initialize','params':{'protocolVersion':'2025-11-25','capabilities':{},'clientInfo':{'name':'synthetic','version':'1'}}},
              {'jsonrpc':'2.0','method':'notifications/initialized'},
              {'jsonrpc':'2.0','id':2,'method':'tools/list'},
              {'jsonrpc':'2.0','id':3,'method':'tools/call','params':{'name':'session_status','arguments':{}}},
              {'jsonrpc':'2.0','id':4,'method':'tools/call','params':{'name':'discover_devices','arguments':{}}},
              {'jsonrpc':'2.0','id':5,'method':'resources/read','params':{'uri':'miraie://onboarding'}},
              {'jsonrpc':'2.0','id':6,'method':'tools/call','params':{'name':'control_ac','arguments':{'action':'swing','value':'on'}}}]
    process=subprocess.Popen([sys.executable,str(runner)],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
    responses={}
    try:
        for message in messages:
            process.stdin.write(json.dumps(message)+'\n'); process.stdin.flush()
            if 'id' in message:
                import select
                ready,_,_=select.select([process.stdout],[],[],10)
                assert ready, 'MCP response timed out'
                row=json.loads(process.stdout.readline())
                responses[row['id']]=row
    finally:
        process.stdin.close()
        process.wait(timeout=10)
    assert process.returncode==0, process.stderr.read()
    assert 'serverInfo' in responses[1]['result']
    tools={t['name']:t for t in responses[2]['result']['tools']}
    assert len(tools)==11
    assert tools['begin_setup']['inputSchema'].get('properties',{})=={}
    assert tools['control_ac']['annotations']['readOnlyHint'] is False
    for name in ('control_ac', 'reported_status'):
        assert 'wait at least 3 seconds' in tools[name]['description']
    assert 'unconfigured' in str(responses[3])
    assert responses[4]['result']['isError']
    assert 'unconfigured' in str(responses[5])
    assert responses[6]['result']['isError'] or 'error' in responses[6]
