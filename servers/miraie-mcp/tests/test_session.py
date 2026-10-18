import json
import time
import pytest
import httpx
from miraie_mcp.core import API, HTTP, Fault, Store, BASE
from conftest import MemoryKeyring

@pytest.mark.parametrize('status',[401,406])
def test_refresh_read_once_and_rotation(service,status):
    calls=[]
    def handler(request):
        calls.append(request)
        if request.url.path.endswith('tokenRefresh'):
            assert json.loads(request.content)['token']=='synthetic-refresh'
            return httpx.Response(200,json={'accessToken':'rotated','refreshToken':'rotated-refresh','expiresIn':1})
        if len(calls)==1: return httpx.Response(status)
        assert request.headers['Authorization']=='Bearer rotated'
        return httpx.Response(200,json=[])
    service.api.http=HTTP(httpx.MockTransport(handler))
    assert service.api.request('homeManagement/homes')==[]
    assert len(calls)==3
    assert service.store.load()['refreshToken']=='rotated-refresh'

def test_write_auth_failure_never_replayed(service):
    calls=[]
    def handler(request):
        calls.append(request)
        return httpx.Response(401)
    service.api.http=HTTP(httpx.MockTransport(handler))
    with pytest.raises(Fault,match='sign in again'): service.api.request('deviceManagement/control','POST',{})
    assert len(calls)==1 and service.store.load()['expired']

def test_write_timeout_is_not_retried(service):
    calls=[]
    def handler(request):
        calls.append(request)
        raise httpx.ReadTimeout('sensitive upstream message')
    service.api.http=HTTP(httpx.MockTransport(handler))
    with pytest.raises(Fault) as exc: service.api.request('deviceManagement/control','POST',{})
    assert exc.value.code=='offline' and 'sensitive' not in str(exc.value)
    assert len(calls)==1

def test_expired_no_refresh_and_wrong_credentials(service):
    service.store.save({'accessToken':'a','expires_at':0})
    with pytest.raises(Fault): service.api.request('homeManagement/homes')
    assert service.session_status()['state']=='expired'
    service.api.http.fail='expired'
    with pytest.raises(Fault) as exc: service.api.login('test@example.invalid','synthetic')
    assert exc.value.code=='invalid_credentials'

def test_password_not_saved(service):
    service.api.login('test@example.invalid','synthetic-password')
    assert 'synthetic-password' not in str(service.store.load())
    assert 'test@example.invalid' not in str(service.store.load())
    assert 'deviceId' not in service.store.load()

def test_store_failure_is_safe(tmp_path):
    class Broken(MemoryKeyring):
        def set_password(self,*args): raise RuntimeError('secret-value')
    store=Store(tmp_path, Broken())
    with pytest.raises(Fault) as exc: store.save({'accessToken':'secret-value'})
    assert exc.value.code=='store_unavailable' and 'secret-value' not in str(exc.value)

def test_http_redirect_not_followed_and_body_not_exposed():
    client=HTTP(httpx.MockTransport(lambda r:httpx.Response(302,headers={'Location':'https://evil.invalid'},text='secret')))
    with pytest.raises(Fault) as exc: client.request(BASE,'x')
    assert exc.value.code=='service_error' and 'secret' not in str(exc.value)

def test_no_credentials_is_not_crash(service):
    service.store.clear()
    assert service.session_status()['state']=='unconfigured'
    with pytest.raises(Fault) as exc: service.discovery()
    assert exc.value.code=='unconfigured'

def test_cross_process_lock_blocks_other_client(service):
    import subprocess,sys
    with service.store.locked():
        result=subprocess.run([sys.executable,'-c', '''
from miraie_mcp.core import Store, Fault
import sys
try:
 with Store(sys.argv[1]).locked(): print('unexpected')
except Fault as exc: print(exc.code)
''',str(service.store.directory)],capture_output=True,text=True,timeout=5)
    assert result.stdout.strip()=='busy'

def test_refresh_failure_once_and_expired_marked(service):
    calls=[]
    def handler(request):
        calls.append(request)
        return httpx.Response(401)
    service.api.http=HTTP(httpx.MockTransport(handler))
    with pytest.raises(Fault): service.api.request('homeManagement/homes')
    assert len(calls)==2 and service.store.load()['expired']

def test_offline_does_not_destroy_session(service):
    service.api.http.fail='offline'
    with pytest.raises(Fault): service.discovery()
    assert service.store.load()['accessToken']=='synthetic-access'
