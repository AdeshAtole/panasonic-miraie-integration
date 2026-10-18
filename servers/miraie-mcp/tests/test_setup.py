import re
import time
import httpx
import pytest
from miraie_mcp.core import Fault
from miraie_mcp.onboarding import Setup

def csrf(page): return re.search('name="csrf" value="([^"]+)"',page).group(1)

@pytest.fixture
def setup(service):
    service.store.clear()
    urls=[]
    setup=Setup(service,opener=lambda url:urls.append(url) or True)
    result=setup.begin()
    assert 'http' not in str(result) and setup.bootstrap not in str(result)
    setup.test_url=urls[0]
    yield setup
    setup.cancel()
    for _ in range(30):
        if not setup.lock.is_locked: break
        time.sleep(.02)

def test_secure_lifecycle(setup):
    with httpx.Client(follow_redirects=True,trust_env=False) as client:
        assert client.get(setup.origin).status_code==403
        page=client.get(setup.test_url)
        assert page.status_code==200 and 'Password' in page.text
        assert 'no-store' in page.headers['cache-control']
        assert page.headers['referrer-policy']=='same-origin'
        token=csrf(page.text)
        assert client.get(setup.test_url).status_code==403 # bootstrap is single use
        assert client.get(setup.origin,headers={'Host':'evil.invalid'}).status_code==403
        bad=client.post(setup.origin+'/',data={'csrf':token,'action':'login'},headers={'Origin':'https://evil.invalid'})
        assert bad.status_code==403
        headers={'Origin':setup.origin}
        page=client.post(setup.origin+'/',data={'csrf':token,'action':'login','identity':'test@example.invalid','password':'synthetic-password'},headers=headers)
        assert 'Choose your default' in page.text and 'synthetic-password' not in page.text
        assert 'deviceId' not in setup.service.store.load()
        assert client.post(setup.origin+'/',data={'csrf':token,'action':'login'},headers=headers).status_code==403
        device=setup.service.discovery()['devices'][0]['device']
        page=client.post(setup.origin+'/',data={'csrf':csrf(page.text),'action':'select','device':device},headers=headers)
        assert 'Limited AC controls' in page.text
        page=client.post(setup.origin+'/',data={'csrf':csrf(page.text),'action':'status'},headers=headers)
        assert 'Cloud status read' in page.text
        assert not any(c[0]=='deviceManagement/control' for c in setup.service.api.http.calls)
        page=client.post(setup.origin+'/',data={'csrf':csrf(page.text),'action':'finish'},headers=headers)
        assert 'Setup complete' in page.text

def test_multiple_process_setup_lock(setup):
    other=Setup(setup.service,opener=lambda _:True)
    with pytest.raises(Fault) as exc: other.begin()
    assert exc.value.code=='busy'

def test_expiry_and_restart(service):
    setup=Setup(service,opener=lambda _:True,lifetime=.05)
    setup.begin()
    time.sleep(.4)
    assert not setup.active and not setup.lock.is_locked
    assert setup.begin()['state']=='setup_opened'
    setup.cancel()

def test_browser_failure_fallback(service):
    setup=Setup(service,opener=lambda _:False)
    assert setup.begin()['state']=='browser_unavailable'
    time.sleep(.4)
    assert not setup.lock.is_locked

def test_wrong_credentials_not_echoed(setup):
    setup.service.api.http.fail='expired'
    with httpx.Client(follow_redirects=True,trust_env=False) as client:
        page=client.get(setup.test_url)
        page=client.post(setup.origin+'/',data={'csrf':csrf(page.text),'action':'login','identity':'test@example.invalid','password':'synthetic-secret'},headers={'Origin':setup.origin})
        assert 'Check credentials' in page.text
        assert 'synthetic-secret' not in page.text and 'test@example.invalid' not in page.text
        assert setup.service.store.load()=={}


def test_null_origin_still_rejected_and_has_recovery(setup):
    with httpx.Client(follow_redirects=True,trust_env=False) as client:
        page=client.get(setup.test_url)
        response=client.post(setup.origin+'/',data={'csrf':csrf(page.text),'action':'login','identity':'synthetic@example.invalid','password':'synthetic'},headers={'Origin':'null'})
        assert response.status_code==403
        assert 'local security check' in response.text
        assert not setup.service.api.http.calls


def test_stale_form_is_explained_without_echoing_credentials(setup):
    with httpx.Client(follow_redirects=True,trust_env=False) as client:
        client.get(setup.test_url)
        page=client.post(setup.origin+'/',data={'csrf':'stale','action':'login','identity':'synthetic@example.invalid','password':'private-input'},headers={'Origin':setup.origin})
        assert page.status_code==403 and 'form is out of date' in page.text
        assert 'private-input' not in page.text
        assert 'Return to setup' in page.text


def test_success_redirect_prevents_password_resubmission(setup):
    with httpx.Client(trust_env=False) as client:
        client.get(setup.test_url)
        page=client.get(setup.origin+'/')
        response=client.post(setup.origin+'/',data={'csrf':csrf(page.text),'action':'login','identity':'synthetic@example.invalid','password':'private-input'},headers={'Origin':setup.origin})
        assert response.status_code==303 and response.headers['location']=='/'
        client.get(setup.origin+'/'); client.get(setup.origin+'/')
        assert len([c for c in setup.service.api.http.calls if c[0].endswith('/login')])==1
