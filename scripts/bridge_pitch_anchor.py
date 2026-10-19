from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
old='val g=padGain(v,b,p);pads[b][p]?.let{playLoadedSound(it.soundId,g,true)}'
new='val g=padGain(v,b,p);pads[b][p]?.let{playLoadedSound(it.soundId,g,true,pitchRate(b,p))}'
if old not in s:
    if new in s:
        print('Pitch anchor already bridged')
    else:
        raise SystemExit('manual playback bridge anchor missing')
else:
    s=s.replace(old,new,1)
    p.write_text(s)
    print('Manual playback bridged for pitch integration')
