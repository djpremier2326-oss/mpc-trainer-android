from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()

velocity='private fun velocity(i:Int)=if(levels16)(((i+1)*127f/16f).roundToLong().toInt()).coerceIn(1,127) else if(fullLevel)127 else 100'
if 'private fun padGain(v:Int)' not in s:
    if velocity not in s: raise SystemExit('velocity anchor not found')
    s=s.replace(velocity,velocity+'\n private fun padGain(v:Int)=if(levels16)(v.coerceIn(1,127)/127f) else 1f',1)

old='pads[b][p]?.let{pool.play(it.soundId,v/127f,v/127f,1,0,1f)}'
new='pads[b][p]?.let{val g=padGain(v);pool.play(it.soundId,g,g,1,0,1f)}'
if old in s:
    s=s.replace(old,new,1)
elif 'val g=padGain(v);pool.play(it.soundId,g,g,1,0,1f)' not in s:
    raise SystemExit('manual pad playback anchor not found')

old='pads[e.bank][e.pad]?.let{val v=e.velocity.coerceIn(1,127)/127f;pool.play(it.soundId,v,v,1,0,1f)}'
new='pads[e.bank][e.pad]?.let{val g=if(e.velocity>=100)1f else e.velocity.coerceIn(1,99)/100f;pool.play(it.soundId,g,g,1,0,1f)}'
if old in s:
    s=s.replace(old,new,1)
elif 'val g=if(e.velocity>=100)1f else e.velocity.coerceIn(1,99)/100f' not in s:
    raise SystemExit('sequencer playback anchor not found')

p.write_text(s)
print('Pad gain compensation integrated: normal phone hits audition at unity; 16 Levels retain velocity scaling')