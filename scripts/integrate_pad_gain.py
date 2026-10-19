from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()

old='private fun velocity(i:Int)=if(levels16)(((i+1)*127f/16f).roundToLong().toInt()).coerceIn(1,127) else if(fullLevel)127 else 100\n private fun hit(i:Int,user:Boolean){val b=if(levels16)sourceBank else bank;val p=if(levels16)sourcePad else i;val v=velocity(i);val loaded=pads[b][p]!=null;pads[b][p]?.let{pool.play(it.soundId,v/127f,v/127f,1,0,1f)};'
new='private fun velocity(i:Int)=if(levels16)(((i+1)*127f/16f).roundToLong().toInt()).coerceIn(1,127) else if(fullLevel)127 else 100\n private fun padGain(v:Int)=if(levels16)(v.coerceIn(1,127)/127f) else 1f\n private fun hit(i:Int,user:Boolean){val b=if(levels16)sourceBank else bank;val p=if(levels16)sourcePad else i;val v=velocity(i);val loaded=pads[b][p]!=null;pads[b][p]?.let{val g=padGain(v);pool.play(it.soundId,g,g,1,0,1f)};'
if old not in s: raise SystemExit('manual pad playback anchor not found')
s=s.replace(old,new,1)

old='events.filter{!mute[it.track]&&crossed(lastClockTick,t,it.tick)}.forEach{e->pads[e.bank][e.pad]?.let{val v=e.velocity.coerceIn(1,127)/127f;pool.play(it.soundId,v,v,1,0,1f)}}'
new='events.filter{!mute[it.track]&&crossed(lastClockTick,t,it.tick)}.forEach{e->pads[e.bank][e.pad]?.let{val g=if(e.velocity>=100)1f else e.velocity.coerceIn(1,99)/100f;pool.play(it.soundId,g,g,1,0,1f)}}'
if old not in s: raise SystemExit('sequencer playback anchor not found')
s=s.replace(old,new,1)

p.write_text(s)
print('Pad gain compensation integrated: normal phone hits audition at unity; 16 Levels retain velocity scaling')