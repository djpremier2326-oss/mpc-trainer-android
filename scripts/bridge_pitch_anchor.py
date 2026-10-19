from pathlib import Path
import re
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
target='val g=padGain(v,b,p);pads[b][p]?.let{playLoadedSound(it.soundId,g,true,pitchRate(b,p))}'
if target in s:
    print('Pitch anchor already bridged')
else:
    # The earlier audit/volume scripts can express this path in several equivalent
    # forms. Locate the manual playback statement semantically and normalize it to
    # the exact input expected by the pitch integration.
    patterns=[
      r'val g=padGain\(v,b,p\);pads\[b\]\[p\]\?\.let\{playLoadedSound\(it\.soundId,g,true\)\}',
      r'val g=\(padGain\(v\)\*padVolumes\[b\]\[p\]\*masterVolume\)\.coerceIn\(0f,1f\);pads\[b\]\[p\]\?\.let\{playLoadedSound\(it\.soundId,g,true\)\}',
      r'pads\[b\]\[p\]\?\.let\{val g=\(padGain\(v\)\*padVolumes\[b\]\[p\]\*masterVolume\)\.coerceIn\(0f,1f\);playLoadedSound\(it\.soundId,g,true\)\}',
      r'pads\[b\]\[p\]\?\.let\{val g=\(padGain\(v\)\*padVolumes\[b\]\[p\]\*masterVolume\)\.coerceIn\(0f,1f\);pool\.play\(it\.soundId,g,g,1,0,1f\)\}'
    ]
    for pat in patterns:
        ns,n=re.subn(pat,target,s,count=1)
        if n:
            s=ns;p.write_text(s);print('Manual playback normalized for pitch integration');break
    else:
        # Print only the relevant source fragment into CI when a future transform
        # changes it again, so the next failure is immediately diagnosable.
        m=re.search(r'.{0,180}pads\[b\]\[p\].{0,260}',s)
        if m: print('CURRENT MANUAL PATH:',m.group(0))
        raise SystemExit('manual playback bridge anchor missing')
