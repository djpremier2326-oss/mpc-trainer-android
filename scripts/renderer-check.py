import subprocess,time,re,pathlib
out=pathlib.Path('renderer-evidence');out.mkdir(exist_ok=True)
def adb(*args):
 r=subprocess.run(['adb',*args],capture_output=True,timeout=40)
 if r.returncode:raise RuntimeError(str(args)+': '+r.stderr.decode(errors='replace')+r.stdout.decode(errors='replace'))
 return r.stdout.decode(errors='replace')
def logs():return adb('logcat','-d','-s','PlannerRenderer:I','PlannerStartup:I','ActivityManager:I','AndroidRuntime:E')
try:
 adb('wait-for-device')
 adb('root');adb('wait-for-device')
 assert 'uid=0' in adb('shell','id'),'Rooted test image required for direct renderer eviction'
 # Let post-boot package optimization settle before exercising Activity focus.
 time.sleep(12)
 adb('install','-r','candidate/raspisanie.apk');adb('logcat','-c')
 adb('shell','am','start','-W','-n','com.bytbeat.dayplan/.PlannerActivity')
 subprocess.run(['adb','exec-out','screencap','-p'],stdout=(out/'cold-launch.png').open('wb'),check=True,timeout=20)
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
 # Repeat background renderer loss: every return must recover automatically,
 # and no new renderer/page may be initialized while the Activity is hidden.
 for cycle in range(3):
  before=logs().count('web frame:')
  adb('shell','input','keyevent','KEYCODE_HOME')
  deadline=time.monotonic()+15
  while re.search(r'(?:topResumedActivity|mResumedActivity)[^\n]*com\.bytbeat\.dayplan/',adb('shell','dumpsys','activity','activities')):
   assert time.monotonic()<deadline,'Activity did not enter background'
   time.sleep(.25)
  text=logs();matches=re.findall(r'Start proc (\d+):[^\n]*sandboxed_process[^\n]*for[^\n]*\{com\.bytbeat\.dayplan/',text)
  assert matches,'Could not identify renderer before background exit'
  exits=text.count('Renderer exited;')
  adb('shell','kill','-9',matches[-1])
  # WebView / OS may defer the background crash notification until resume.
  # Do not wait for that notification before bringing the app back.
  time.sleep(.75)
  assert logs().count('web frame:')==before,'Page recreated while hidden'
  adb('shell','am','start','-W','-n','com.bytbeat.dayplan/.PlannerActivity')
  deadline=time.monotonic()+60
  while logs().count('web frame:')<=before or logs().count('Renderer exited;')<=exits:
   assert time.monotonic()<deadline,'Background return stuck instead of automatic recovery'
   time.sleep(.25)
  assert adb('shell','pidof','com.bytbeat.dayplan').strip()==host,'Host exited on background recovery'
 # Exercise actual touch scrolling after recovery.
 for i in range(6):adb('shell','input','swipe','500','1450','500','650','300')
 assert adb('shell','pidof','com.bytbeat.dayplan').strip()==host,'Host exited during scrolling'
 assert re.search(r'(?:topResumedActivity|mResumedActivity)[^\n]*com\.bytbeat\.dayplan/',adb('shell','dumpsys','activity','activities')),'App left foreground during scrolling'
 (out/'result.txt').write_text('PASS: foreground renderer recovery and three background kill/return cycles recover automatically; no hidden recreation or retry button; six scroll gestures keep host foreground')
finally:
 try:(out/'logcat.txt').write_text(logs())
 except Exception as e:(out/'error.txt').write_text(str(e))
 subprocess.run(['adb','exec-out','screencap','-p'],stdout=(out/'screen.png').open('wb'),timeout=20)
