from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()

old=' private fun hit(i:Int,user:Boolean){val b=if(levels16)sourceBank else bank;val p=if(levels16)sourcePad else i;val v=velocity(i);'
new=' private fun hit(i:Int,user:Boolean){val b=if(levels16)sourceBank else bank;val p=if(levels16)sourcePad else i;TrainerUiState.pulsePad(b,p);val v=velocity(i);'
if 'TrainerUiState.pulsePad(b,p)' not in s:
    if old not in s: raise SystemExit('hit anchor not found')
    s=s.replace(old,new,1)

old='events.filter{!mute[it.track]&&crossed(lastClockTick,t,it.tick)}.forEach{e->pads[e.bank][e.pad]?.let{val v=e.velocity.coerceIn(1,127)/127f;pool.play(it.soundId,v,v,1,0,1f)}}'
new='events.filter{!mute[it.track]&&crossed(lastClockTick,t,it.tick)}.forEach{e->TrainerUiState.pulsePad(e.bank,e.pad);pads[e.bank][e.pad]?.let{val v=e.velocity.coerceIn(1,127)/127f;pool.play(it.soundId,v,v,1,0,1f)}}'
if 'forEach{e->TrainerUiState.pulsePad(e.bank,e.pad)' not in s:
    if old in s:
        s=s.replace(old,new,1)
    else:
        marker='events.filter{!mute[it.track]&&crossed(lastClockTick,t,it.tick)}.forEach{e->'
        if marker not in s: raise SystemExit('clock anchor not found')
        s=s.replace(marker,marker+'TrainerUiState.pulsePad(e.bank,e.pad);',1)

# refreshStatus may already be extended by earlier commits; inject publish at function entry instead of matching its entire body.
if 'TrainerUiState.publish(bank,pad,track' not in s:
    marker=' private fun refreshStatus(){'
    if marker not in s: raise SystemExit('status function anchor not found')
    s=s.replace(marker,marker+'TrainerUiState.publish(bank,pad,track,playing,recording,overdub,noteRepeat,fullLevel,levels16,mute[track]);',1)

p.write_text(s)
print('Explicit trainer UI state bridge integrated')