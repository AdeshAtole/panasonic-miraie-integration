"""Real HTML forms: do not inject Origin headers (that hid the original bug)."""
import os
from pathlib import Path
import time
import pytest
from playwright.sync_api import sync_playwright, expect
from miraie_mcp.onboarding import Setup

pytestmark = pytest.mark.skipif(os.environ.get('MIRAIE_BROWSER_TESTS') != '1',
    reason='Run MIRAIE_BROWSER_TESTS=1 after python -m playwright install chromium')

@pytest.fixture
def browser_setup(service):
    service.store.clear()
    urls=[]
    setup=Setup(service,opener=lambda url:urls.append(url) or True)
    setup.begin()
    with sync_playwright() as engine:
        browser=engine.chromium.launch()
        yield setup, urls[0], browser
        browser.close()
    setup.cancel()
    time.sleep(.3)

def shot(page,name):
    directory=os.environ.get('MIRAIE_SCREENSHOTS')
    if directory:
        Path(directory).mkdir(parents=True,exist_ok=True)
        page.screenshot(path=str(Path(directory)/(name+'.png')), full_page=True)

def sign_in(page):
    page.get_by_label('Email or mobile',exact=True).fill('synthetic@example.invalid')
    page.get_by_label('Password',exact=True).fill('synthetic-password')
    with page.expect_response(lambda r:r.request.method=='POST') as result:
        page.get_by_role('button',name='Sign in',exact=True).click()
    return result.value

def test_browser_complete_setup(browser_setup):
    setup,url,browser=browser_setup
    page=browser.new_page(viewport={'width':1280,'height':900})
    errors=[];page.on('pageerror',lambda exc:errors.append(str(exc)))
    response=page.goto(url)
    assert response.headers['referrer-policy']=='same-origin'
    assert page.locator('body').evaluate('(el)=>getComputedStyle(el).backgroundColor')=='rgb(247, 248, 244)'
    shot(page,'setup-sign-in')
    response=sign_in(page)
    assert response.status==303
    assert response.request.headers['origin']==setup.origin
    expect(page.get_by_role('heading',name='Choose your default device')).to_be_visible()
    assert 'deviceId' not in setup.service.store.load()
    shot(page,'setup-devices')
    page.get_by_role('radio').check()
    page.get_by_role('button',name='Use this device',exact=True).click()
    expect(page.get_by_role('heading',name='Your connection is ready')).to_be_visible()
    page.get_by_role('button',name='Read reported status',exact=True).click()
    expect(page.get_by_text('22.61 °C',exact=True)).to_be_visible()
    shot(page,'setup-ready')
    page.reload()
    assert len([c for c in setup.service.api.http.calls if c[0].endswith('/login')])==1
    assert not any(c[0]=='deviceManagement/control' for c in setup.service.api.http.calls)
    page.get_by_role('button',name='Finish setup',exact=True).click()
    expect(page.get_by_role('heading',name='Setup complete')).to_be_visible()
    assert not errors

def test_mobile_wrong_credentials_and_empty_state(browser_setup):
    setup,url,browser=browser_setup
    page=browser.new_page(viewport={'width':390,'height':844},is_mobile=True,has_touch=True)
    page.goto(url)
    shot(page,'setup-mobile')
    assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
    setup.service.api.http.fail='expired'
    sign_in(page)
    expect(page.get_by_role('alert').filter(has_text='Check credentials')).to_be_visible()
    expect(page.get_by_label('Password',exact=True)).to_have_value('')
    assert 'synthetic-password' not in page.content()
    shot(page,'setup-error')
    setup.service.api.http.fail=None
    setup.service.api.http.devices=[]
    sign_in(page)
    expect(page.get_by_text('No devices found',exact=True)).to_be_visible()
    assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
    shot(page,'setup-empty')

def test_legacy_referrer_policy_reproduces_rejected_origin(browser_setup):
    setup,url,browser=browser_setup
    page=browser.new_page()
    def legacy_policy(route):
        response=route.fetch()
        route.fulfill(response=response,headers={**response.headers,'referrer-policy':'no-referrer'})
    page.route('**/*',legacy_policy)
    page.goto(url)
    response=sign_in(page)
    assert response.request.headers['origin']=='null'
    assert response.status==403
    expect(page.get_by_role('heading',name='This page could not submit')).to_be_visible()
    assert not any(c[0].endswith('/login') for c in setup.service.api.http.calls)
