from pathlib import Path

p = Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s = p.read_text()
def replace(old, new):
    global s
    if s.count(old) != 1:
        raise SystemExit('Expected exactly one anchor: ' + old[:110])
    s = s.replace(old, new, 1)

# Pitch and post-record TC lessons existed, but their real controls never emitted
# the events expected by LearnEngine. Empty pads/tracks cannot complete a lesson.
replace('fun adjustSelectedPadPitch(delta:Int):Int{val v=(padPitch[bank][pad]+delta).coerceIn(-12,12);padPitch[bank][pad]=v;saveVolumes();',
        'fun adjustSelectedPadPitch(delta:Int):Int{val old=padPitch[bank][pad];val v=(old+delta).coerceIn(-12,12);padPitch[bank][pad]=v;saveVolumes();if(v!=old&&pads[bank][pad]!=null)report("pitch_changed");')
replace('fun quantizeCurrentTrack():Int{var changed=0;',
        'fun quantizeCurrentTrack():Int{if(events.none{it.track==track}){showDisplayMessage("TC DO IT • текущий TRACK пуст");return 0};var changed=0;')
replace('report("tc");return changed}', 'report("tc");report("tc_do_it");return changed}')

# Velocity is stored as the value actually auditioned, independent of the UI mode
# that happens to be selected later during playback.
replace('private fun padGain(v:Int,b:Int,p:Int)=(if(levels16&&levels16Type=="VELOCITY")v.coerceIn(1,127)/127f else 1f)*padVolumes[b][p]*masterVolume',
        'private fun effectiveVelocity(v:Int)=if(levels16&&levels16Type=="VELOCITY")v else 127\n private fun padGain(v:Int,b:Int,p:Int)=TrainerPlayback.gain(effectiveVelocity(v),padVolumes[b][p],masterVolume)')
replace('val base=if(e.velocity>=100)1f else e.velocity.coerceIn(1,99)/100f;val g=(base*padVolumes[e.bank][e.pad]*masterVolume).coerceIn(0f,1f)',
        'val g=TrainerPlayback.gain(e.velocity,padVolumes[e.bank][e.pad],masterVolume)')
replace('Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v)',
        'Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,effectiveVelocity(v))')

# First tick must play events at the start. Suppression applies only to the current
# uninterrupted recording pass, never to the next press of PLAY.
replace('private fun crossed(prev:Long,now:Long,target:Long)=if(prev<0)false else if(now>=prev)target>prev&&target<=now else target>prev||target<=now',
        'private fun crossed(prev:Long,now:Long,target:Long)=TrainerPlayback.crossed(prev,now,target)')
replace('private fun startClock(){lastBeat=-1;lastClockTick=tick();h.removeCallbacks(clock);h.post(clock)}',
        'private fun startClock(){lastBeat=-1;lastClockTick=-1;recordedCycle.clear();h.removeCallbacks(clock);h.post(clock)}')
replace('if(lastClockTick>=0&&t!=lastClockTick){', 'if(t!=lastClockTick){')

# Preserve musical position when changing tempo while the transport is running.
replace('fun setTempoBpm(value:Int){bpm=value.coerceIn(40,240);saveProject();refreshStatus()}',
        'fun setTempoBpm(value:Int){val now=SystemClock.elapsedRealtime();val next=value.coerceIn(40,240);if(playing)startMs=now-TrainerPlayback.retimedElapsed(now-startMs,bpm,next);bpm=next;saveProject();refreshStatus()}')

# A successful chop is a real completion even if it replaced occupied slots.
replace('report("sample_edit_chop")', 'report("chopped")')
# Use a unique directory for every operation: two chops of the same source must
# never rewrite the disk files referenced by earlier pad assignments.
replace('val outs=WavSlicer.chop(src,File(filesDir,"samples/chops"),start.progress/100f,end.progress/100f,pieces.progress+2,"${label}_slice")',
        'val count=pieces.progress+2;if(count>16-pad){Toast.makeText(this,"Недостаточно пэдов: нужно $count, доступно ${16-pad}",Toast.LENGTH_LONG).show();return@setOnClickListener};val chopDir=File(filesDir,"samples/chops_${java.util.UUID.randomUUID()}");val outs=WavSlicer.chop(src,chopDir,start.progress/100f,end.progress/100f,count,"slice")')
replace('Toast.makeText(this,"Assigned $assigned slices from PAD ${pad+1}",Toast.LENGTH_LONG).show()',
        'Toast.makeText(this,"Назначено $assigned слайсов с PAD ${pad+1}",Toast.LENGTH_LONG).show()')

# The initial press is not yet proof that Note Repeat generated any repeats.
replace('if(noteRepeat)report("note_repeat_pad");', '')
replace('if(noteRepeat){hit(pad,true);h.postDelayed(this,noteRepeatIntervalMs())}',
        'if(noteRepeat){hit(pad,true);val repeatBank=if(levels16)sourceBank else bank;val repeatPad=if(levels16)sourcePad else pad;if(pads[repeatBank][repeatPad]!=null)report("note_repeat_pad");h.postDelayed(this,noteRepeatIntervalMs())}')

# Release each audio resource once, including the streaming preview player.
old=next(line for line in s.splitlines() if line.startswith(' override fun onDestroy(){'))
replace(old,' override fun onDestroy(){countInToken++;h.removeCallbacksAndMessages(null);browserPreview.release();try{pool.setOnLoadCompleteListener(null)}catch(_:Exception){};try{pool.release()}catch(_:Exception){};try{tone.release()}catch(_:Exception){};readySounds.clear();pendingAuditions.clear();super.onDestroy()}')
p.write_text(s)
print('Verified fixes: chop completion/storage/capacity, velocity, first tick, tempo continuity, repeat validation, audio cleanup')
