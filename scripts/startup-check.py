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

report = {'android': adb('shell', 'getprop', 'ro.build.version.release').strip(),
          'webview': adb('shell', 'dumpsys', 'webviewupdate')}
try:
    for version in ['baseline', 'candidate']:
        adb('install', '-r', version + '/raspisanie.apk')
        samples = []
        for trial in range(5):
            adb('shell', 'am', 'force-stop', PACKAGE)
            adb('logcat', '-c')
            start = launch()
            elapsed, log = ready()
            samples.append(elapsed)
            (OUT / f'{version}-cold-{trial}.log').write_text(start + '\n' + log)
            time.sleep(.3)
            screenshot(f'{version}-cold-{trial}')
        adb('shell', 'uiautomator', 'dump', '/sdcard/planner-ui.xml')
        ui = adb('shell', 'cat', '/sdcard/planner-ui.xml')
        (OUT / f'{version}-ui.xml').write_text(ui)
        assert 'Сегодня' in ui and 'Календарь' in ui, 'Main planner UI missing'

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
        adb('shell', 'pkill', '-INT', 'screenrecord')
        recording.wait(timeout=15)
        adb('pull', '/sdcard/planner-hot.mp4', str(OUT / f'{version}-hot.mp4'))
        hot_log = logs()
        (OUT / f'{version}-hot.log').write_text(hot_log)
        assert 'web frame:' not in hot_log, 'WebView loaded again during hot return'
        assert 'FATAL EXCEPTION' not in hot_log, 'Crash on return'
        report[version] = {'cold_web_ready_ms': samples,
                           'median_web_ready_ms': statistics.median(samples),
                           'hot_returns_without_reload': 10}
        print(version, report[version], flush=True)
finally:
    (OUT / 'results.json').write_text(json.dumps(report, indent=2))
    (OUT / 'last-logcat.txt').write_text(logs())
