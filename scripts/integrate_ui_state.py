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
    if old not in s: raise SystemExit('clock anchor not found')
    s=s.replace(old,new,1)

old=' private fun refreshStatus(){status.text="SEQ 01 • TRACK ${track+1}${if(mute[track])" MUTE" else ""} • BANK ${\'A\'+bank} • PAD ${pad+1} • ${position()}\\n$bpm BPM • TC 1/$tc • SWING $swing% • MET ${if(metronome)"ON" else "OFF"} • COUNT ${if(countIn)"ON" else "OFF"}${if(recording)if(overdub)" • OVERDUB" else " • REC" else if(playing)" • PLAY" else ""}"}'
new=' private fun refreshStatus(){TrainerUiState.publish(bank,pad,track,playing,recording,overdub,noteRepeat,fullLevel,levels16,mute[track]);status.text="SEQ 01 • TRACK ${track+1}${if(mute[track])" MUTE" else ""} • BANK ${\'A\'+bank} • PAD ${pad+1} • ${position()}\\n$bpm BPM • TC 1/$tc • SWING $swing% • MET ${if(metronome)"ON" else "OFF"} • COUNT ${if(countIn)"ON" else "OFF"}${if(recording)if(overdub)" • OVERDUB" else " • REC" else if(playing)" • PLAY" else ""}"}'
if 'TrainerUiState.publish(bank,pad,track' not in s:
    if old not in s: raise SystemExit('status anchor not found')
    s=s.replace(old,new,1)

p.write_text(s)
print('Explicit trainer UI state bridge integrated')