"""Exercise the signed APK on an emulator, including upgrade and process restarts."""
import pathlib
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PACKAGE = 'cz.honestlead.pause'
ACTIVITY = PACKAGE + '/cz.honestlead.mezera.ui.MainActivity'
APK = pathlib.Path('android/app/build/outputs/apk/release/app-release.apk')
OUT = pathlib.Path('android/test-results')
OUT.mkdir(exist_ok=True)
API = subprocess.check_output(['adb', 'shell', 'getprop', 'ro.build.version.sdk'], text=True).strip()


def adb(*args):
    return subprocess.check_output(['adb', *args], text=True, timeout=60)


def tree():
    for _ in range(8):
        try:
            adb('shell', 'uiautomator', 'dump', '/sdcard/pause-test.xml')
            return ET.fromstring(adb('shell', 'cat', '/sdcard/pause-test.xml'))
        except (ET.ParseError, subprocess.CalledProcessError):
            time.sleep(1)
    raise AssertionError('Could not read UI hierarchy')


def find(label):
    for _ in range(12):
        nodes = tree()
        for node in nodes.iter('node'):
            if label in (node.get('text'), node.get('content-desc')):
                return node
        time.sleep(1)
    (OUT / f'failure-{API}.xml').write_bytes(ET.tostring(nodes))
    raise AssertionError(f'UI label not found: {label}')


def reveal(label):
    for _ in range(6):
        for node in tree().iter('node'):
            if label in (node.get('text'), node.get('content-desc')):
                return node
        size = adb('shell', 'wm', 'size')
        width, height = map(int, re.findall(r'(\d+)x(\d+)', size)[-1])
        adb('shell', 'input', 'swipe', str(width//2), str(height*4//5), str(width//2), str(height//3), '400')
    return find(label)


def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(1)


def launch():
    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('shell', 'am', 'start', '-W', '-n', ACTIVITY)
    time.sleep(2)


def screenshot(name):
    with (OUT / f'{API}-{name}.png').open('wb') as output:
        subprocess.run(['adb', 'exec-out', 'screencap', '-p'], stdout=output, check=True)


def check_switch():
    assert find('App monitoring').get('checked') == 'false', 'Upgrade reset the monitoring preference'


try:
    adb('shell', 'input', 'keyevent', '82')
    adb('install', '-r', '-g', 'previous.apk')
    launch()
    # Existing installs must keep their non-language preferences through the update.
    switches = [node for node in tree().iter('node') if node.get('checkable') == 'true']
    assert switches, 'Old app did not launch'
    if switches[0].get('checked') == 'true':
        tap(switches[0])
    adb('install', '-r', '-g', str(APK))
    launch()
    find('Settings')
    check_switch()
    screenshot('english')
    tap(find('Settings'))
    find('Language')
    screenshot('language-picker')
    tap(find('Čeština'))
    find('Nastavení')
    launch()
    find('Nastavení')
    screenshot('czech-restarted')
    # Reinstall in place to prove the explicit choice survives an APK update.
    adb('install', '-r', '-g', str(APK))
    launch()
    find('Nastavení')
    tap(find('Nastavení'))
    tap(find('English'))
    find('Settings')
    launch()
    find('Settings')
    check_switch()
    tap(reveal('Statistics'))
    reveal('Your most opened apps')
    screenshot('english-statistics')
    print(f'PASS API {API}: upgrade defaults to English, CZ/EN switch, process restart, reinstall, preserved monitoring preference, stats UI')
except Exception:
    # Preserve the original failure even if the emulator itself has stopped.
    for capture in (
        lambda: (OUT / f'failure-{API}.xml').write_bytes(ET.tostring(tree())),
        lambda: screenshot('failure'),
        lambda: (OUT / f'logcat-{API}.txt').write_text(adb('logcat', '-d', '-t', '1500'), encoding='utf-8'),
    ):
        try:
            capture()
        except Exception as diagnostic_error:
            print(f'Diagnostic unavailable: {diagnostic_error}', file=sys.stderr)
    raise
