import subprocess,time,re,pathlib
out=pathlib.Path('renderer-evidence');out.mkdir(exist_ok=True)
def adb(*args):return subprocess.run(['adb',*args],check=True,capture_output=True,timeout=40).stdout.decode(errors='replace')
def logs():return adb('logcat','-d','-t','4000')
try:
 adb('root');adb('wait-for-device');adb('install','-r','candidate/raspisanie.apk');adb('logcat','-c')
 adb('shell','am','start','-W','-n','com.bytbeat.dayplan/.PlannerActivity')
 deadline=time.monotonic()+40
 while 'web frame:' not in logs():
  assert time.monotonic()<deadline,'Initial page did not load'
  time.sleep(.25)
 host=adb('shell','pidof','com.bytbeat.dayplan').strip()
 text=logs();matches=re.findall(r'Start proc (\d+):[^\n]*sandboxed_process[^\n]*for[^\n]*\{com\.bytbeat\.dayplan/',text)
 assert matches,'Could not identify this app renderer'
 adb('shell','kill','-9',matches[-1])
 deadline=time.monotonic()+40
 while True:
  text=logs()
  if 'Renderer exited;' in text and text.count('web frame:')>=2:break
  assert time.monotonic()<deadline,'Renderer was not restored'
  time.sleep(.25)
 assert adb('shell','pidof','com.bytbeat.dayplan').strip()==host,'Host app process exited'
 assert 'com.bytbeat.dayplan/.PlannerActivity' in adb('shell','dumpsys','activity','activities'),'Activity missing'
 # Exercise actual touch scrolling after recovery.
 for i in range(6):adb('shell','input','swipe','500','1450','500','650','300')
 assert adb('shell','pidof','com.bytbeat.dayplan').strip()==host,'Host exited during scrolling'
 (out/'result.txt').write_text('PASS: renderer killed, host survives, page reload completes, six scroll gestures do not exit host')
finally:
 try:(out/'logcat.txt').write_text(logs())
 except Exception as e:(out/'error.txt').write_text(str(e))
 subprocess.run(['adb','exec-out','screencap','-p'],stdout=(out/'screen.png').open('wb'),timeout=20)
