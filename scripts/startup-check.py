"""Same emulator, same installed data: baseline APK then signed upgrade.

Keep raw evidence: timings, logs, UI dumps, screenshots and hot-return video.
The WebView-ready log is checked in addition to am's native-window timing.
"""
import json
import pathlib
import re
import statistics
import subprocess
import time
from PIL import Image
import urllib.request
import websocket

OUT = pathlib.Path('startup-evidence')
OUT.mkdir(exist_ok=True)
PACKAGE = 'com.bytbeat.dayplan'
COMPONENT = PACKAGE + '/.PlannerActivity'

def adb(*args, binary=False):
    result = subprocess.run(['adb', *args], check=True, capture_output=True, timeout=60)
    return result.stdout if binary else result.stdout.decode(errors='replace')

def launch():
    return adb('shell', 'am', 'start', '-W', '-a', 'android.intent.action.MAIN',
               '-c', 'android.intent.category.LAUNCHER', '-n', COMPONENT)

def logs():
    return adb('logcat', '-d', '-v', 'threadtime')

def ready():
    deadline = time.monotonic() + 25
    while time.monotonic() < deadline:
        text = logs()
        match = re.search(r'web frame: (\d+)ms', text)
        if match:
            return int(match.group(1)), text
        if 'FATAL EXCEPTION' in text:
            raise RuntimeError(text)
        time.sleep(.15)
    raise RuntimeError('Planner did not become ready:\n' + logs())

def screenshot(name):
    (OUT / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p', binary=True))

def inspect_baseline():
    # v86 is debuggable: inspect its actual DOM rather than guessing from blank pixels.
    pid = adb('shell', 'pidof', PACKAGE).strip()
    try:
        adb('forward', 'tcp:9222', 'localabstract:webview_devtools_remote_' + pid)
        targets = json.load(urllib.request.urlopen('http://127.0.0.1:9222/json'))
        (OUT / 'baseline-devtools-targets.json').write_text(json.dumps(targets))
        ws = websocket.create_connection(targets[0]['webSocketDebuggerUrl'], timeout=8, suppress_origin=True)
        expression = "JSON.stringify({ready:document.documentElement.dataset.appReady,visibility:document.visibilityState,width:innerWidth,height:innerHeight,bodyHeight:document.body.getBoundingClientRect().height,mainHeight:document.querySelector('main').getBoundingClientRect().height,mainVisibility:getComputedStyle(document.querySelector('main')).visibility,rows:document.querySelectorAll('[data-ice-slot]').length})"
        ws.send(json.dumps({'id': 1, 'method': 'Runtime.evaluate', 'params': {'expression': expression, 'returnByValue': True}}))
        while True:
            result = json.loads(ws.recv())
            if result.get('id') == 1:
                (OUT / 'baseline-dom.json').write_text(json.dumps(result))
                break
        ws.close()
    except Exception as error:
        (OUT / 'baseline-devtools-error.txt').write_text(str(error))

report = {'android': adb('shell', 'getprop', 'ro.build.version.release').strip(),
          'webview': adb('shell', 'dumpsys', 'webviewupdate')}
try:
    for version in ['baseline', 'candidate']:
        adb('install', '-r', version + '/raspisanie.apk')
        samples = []
        report[version] = {'cold_web_ready_ms': samples}
        for trial in range(5):
            # Finish the previous task transition before starting a new process.
            # Killing/restarting the foreground Activity back-to-back races WM teardown.
            adb('shell', 'input', 'keyevent', 'KEYCODE_HOME')
            time.sleep(.7)
            adb('shell', 'am', 'force-stop', PACKAGE)
            time.sleep(.5)
            adb('logcat', '-c')
            start = launch()
            elapsed, log = ready()
            samples.append(elapsed)
            (OUT / f'{version}-cold-{trial}.log').write_text(start + '\n' + log)
            time.sleep(.3)
            screenshot(f'{version}-cold-{trial}')
        time.sleep(3)
        screenshot(f'{version}-settled')
        if version == 'baseline':
            inspect_baseline()
        if version == 'candidate':
            im = Image.open(OUT / f'{version}-settled.png').convert('RGB')
            center = im.crop((0, im.height // 5, im.width, im.height * 4 // 5))
            assert len(center.getcolors(center.width * center.height)) > 1000, 'Ready signal fired but screen is blank'
        # uiautomator's waitForIdle can time out on animated WebView content.
        # Preserve its result, but don't abort comparison of both APKs on this.
        dump = adb('shell', 'uiautomator', 'dump', '/sdcard/planner-ui.xml')
        (OUT / f'{version}-uiautomator.txt').write_text(dump)
        try:
            ui = adb('shell', 'cat', '/sdcard/planner-ui.xml')
            (OUT / f'{version}-ui.xml').write_text(ui)
        except subprocess.CalledProcessError:
            report[version]['uiautomator'] = 'idle timeout; review screenshots/video'

        pid = adb('shell', 'pidof', PACKAGE).strip()
        adb('logcat', '-c')
        recording = subprocess.Popen(['adb', 'shell', 'screenrecord', '--time-limit', '30',
                                      '/sdcard/planner-hot.mp4'])
        for trial in range(10):
            adb('shell', 'input', 'keyevent', 'KEYCODE_HOME')
            time.sleep(.35)
            launch()
            time.sleep(.15)
            screenshot(f'{version}-hot-{trial}')
            assert adb('shell', 'pidof', PACKAGE).strip() == pid, 'Process restarted on return'
        if recording.poll() is None:
            subprocess.run(['adb', 'shell', 'pkill', '-INT', 'screenrecord'], check=False)
        recording.wait(timeout=15)
        adb('pull', '/sdcard/planner-hot.mp4', str(OUT / f'{version}-hot.mp4'))
        hot_log = logs()
        (OUT / f'{version}-hot.log').write_text(hot_log)
        assert 'web frame:' not in hot_log, 'WebView loaded again during hot return'
        assert 'FATAL EXCEPTION' not in hot_log, 'Crash on return'
        report[version].update(median_web_ready_ms=statistics.median(samples),
                               hot_returns_without_reload=10)
        print(version, report[version], flush=True)
finally:
    (OUT / 'results.json').write_text(json.dumps(report, indent=2))
    (OUT / 'last-logcat.txt').write_text(logs())
    (OUT / 'activity-state.txt').write_text(adb('shell', 'dumpsys', 'activity', 'activities'))
    (OUT / 'window-state.txt').write_text(adb('shell', 'dumpsys', 'window', 'windows'))
