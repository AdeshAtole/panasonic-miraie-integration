import copy
import pytest
from miraie_mcp.core import Store, API, Fault
from miraie_mcp.domain import Service, CAPS, CHOICES

class MemoryKeyring:
    value = None
    def get_password(self, *args): return self.value
    def set_password(self, service, key, value): self.value = value
    def delete_password(self, *args): self.value = None

MODEL = {'modelId':'140013', 'controlCapabilities':[
    {'capability':name, 'attributes':[{'key':key, **({'range':{'min':[{'unit':'celcius','value':16}],
        'max':[{'unit':'celcius','value':30}], 'increment':[{'unit':'celcius','value':1}]}}
        if action == 'temperature' else {'intValues' if action == 'capacity' else 'values':CHOICES[action]})}]}
    for (action,(name,index)),key in zip(CAPS.items(), ['ps','acmd','actmp','acfs','acdc','acem','acpm','cnv'])]}

class FakeHTTP:
    def __init__(self):
        self.calls=[]
        self.model=copy.deepcopy(MODEL)
        self.devices=[{'deviceId':'synthetic-device', 'deviceName':'Test AC','modelId':'140013','category':'AC'}]
        self.state={'ps':'on','acmd':'cool','actmp':'24','acfs':'auto','acdc':'on','acem':'off',
                    'acpm':'off','cnv':0,'acec':'off','acsp':'none','onlineStatus':'online','rmtmp':22.61}
        self.match=True
        self.fail=None
    def request(self, base, path, method='GET', body=None, token=None):
        self.calls.append((path, method, body, token))
        if self.fail: raise Fault(self.fail)
        if path.endswith('/login') or path.endswith('/tokenRefresh'):
            return {'accessToken':'synthetic-access', 'refreshToken':'synthetic-refresh','expiresIn':3600}
        if path == 'homeManagement/homes':
            return [{'homeName':'Test Home','spaces':[{'spaceName':'Test Room','devices':self.devices}]}]
        if 'deviceModels' in path: return self.model
        if '/mobile/status' in path: return dict(self.state)
        if path == 'deviceManagement/control':
            if self.match:
                self.state.update({k:v for k,v in body['control_params'].items() if k not in ('cnt','sid','ki')})
            return {}
        if 'powerConsumption' in path: return [{'day':'19102026','power':'0.123'}]
        raise AssertionError(path)

@pytest.fixture
def service(tmp_path):
    store=Store(tmp_path/'state', MemoryKeyring())
    store.save({'accessToken':'synthetic-access','refreshToken':'synthetic-refresh'})
    http=FakeHTTP()
    service=Service(store, API(store,http), sleep=lambda _:None)
    service.select(service.discovery()['devices'][0]['device'])
    http.calls.clear()
    return service
