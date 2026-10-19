from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
anchor='fun selectedPadVolumePercent():Int='
if 'fun currentBpm():Int=' not in s:
    if anchor not in s: raise SystemExit('tempo api anchor missing')
    s=s.replace(anchor,'fun currentBpm():Int=bpm.coerceIn(40,240)\n fun setTempoBpm(value:Int){bpm=value.coerceIn(40,240);saveProject();refreshStatus()}\n '+anchor,1)
old='private fun startRepeat(){h.removeCallbacks(repeater);h.post(repeater)};private fun stopRepeat(){h.removeCallbacks(repeater)}'
new='private fun startRepeat(){h.removeCallbacks(repeater);h.postDelayed(repeater,(60000.0/bpm*(4.0/tc)).toLong().coerceAtLeast(25))};private fun stopRepeat(){h.removeCallbacks(repeater)}'
if old in s:s=s.replace(old,new,1)
old='private fun record(od:Boolean){if(playing)return;countInToken++;if(!od){events.removeAll{it.track==track};recordedCycle.keys.removeAll{it.track==track}};overdub=od;if(countIn){recording=false;playing=false;val token=countInToken;countDown(4,token){recording=true;playing=true;startMs=SystemClock.elapsedRealtime();startClock();refreshStatus()}}else{recording=true;playing=true;startMs=SystemClock.elapsedRealtime();startClock()};saveProject();refreshStatus()}'
new='private fun clearCurrentTrackForRecord(){events.removeAll{it.track==track};recordedCycle.keys.removeAll{it.track==track}}\n private fun record(od:Boolean){if(playing)return;displayOverride=null;countInToken++;overdub=od;if(countIn){recording=false;playing=false;val token=countInToken;countDown(4,token){if(!od)clearCurrentTrackForRecord();recording=true;playing=true;startMs=SystemClock.elapsedRealtime();startClock();saveProject();refreshStatus()}}else{if(!od)clearCurrentTrackForRecord();recording=true;playing=true;startMs=SystemClock.elapsedRealtime();startClock();saveProject()};refreshStatus()}'
if old in s:s=s.replace(old,new,1)
s=s.replace('${s?.name?.take(8)?:"EMPTY"}','${s?.name?:"EMPTY"}')
s=s.replace('"${label.take(7)}_${i+1}"','"${label}_${i+1}"')
s=s.replace('width=150;height=105','width=(resources.displayMetrics.widthPixels-(64*resources.displayMetrics.density).toInt())/4;height=(56*resources.displayMetrics.density).toInt()')
repls={'"Project saved"':'"Проект сохранён"','"Lesson complete"':'"Урок завершён"','"Load a WAV or MP3 to a pad first"':'"Сначала загрузите WAV или MP3 на пэд"','"Only $room pad slots remain in this bank"':'"В этом банке осталось только $room свободных пэдов"','"$count chops assigned to pads"':'"$count фрагментов назначено на пэды"','"Set Start/End, then chop the selected range evenly across pads."':'"Установите Start/End, затем равномерно разрежьте выбранный диапазон по пэдам."','"NUMBER OF CHOPS"':'"КОЛИЧЕСТВО CHOPS"'}
for a,b in repls.items():s=s.replace(a,b)

field=' private lateinit var pool:SoundPool\n'
if 'private val readySounds=' not in s:
    if field not in s: raise SystemExit('SoundPool field anchor missing')
    s=s.replace(field,field+' private val readySounds=mutableSetOf<Int>();private val pendingAuditions=mutableMapOf<Int,Float>()\n',1)
create='pool=SoundPool.Builder().setMaxStreams(24).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).build();restorePads()'
create_new='pool=SoundPool.Builder().setMaxStreams(24).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).build();pool.setOnLoadCompleteListener{sp,id,statusCode->runOnUiThread{if(statusCode==0){readySounds.add(id);pendingAuditions.remove(id)?.let{g->sp.play(id,g,g,1,0,1f)}}else{pendingAuditions.remove(id)}}};restorePads()'
if create in s:s=s.replace(create,create_new,1)
elif 'pool.setOnLoadCompleteListener' not in s: raise SystemExit('SoundPool create anchor missing')
helper_anchor=' private fun hit(i:Int,user:Boolean)'
if 'private fun playLoadedSound(' not in s:
    helper=' private fun playLoadedSound(soundId:Int,gain:Float,keepPending:Boolean){val g=gain.coerceIn(0f,1f);if(readySounds.contains(soundId))pool.play(soundId,g,g,1,0,1f)else if(keepPending)pendingAuditions[soundId]=g}\n'
    if helper_anchor not in s: raise SystemExit('hit anchor missing')
    s=s.replace(helper_anchor,helper+helper_anchor,1)
manual='pool.play(it.soundId,g,g,1,0,1f)'
if manual in s:s=s.replace(manual,'playLoadedSound(it.soundId,g,true)',1)
else: raise SystemExit('manual SoundPool play anchor missing')
if manual in s:s=s.replace(manual,'playLoadedSound(it.soundId,g,true)',1)
else: raise SystemExit('sequencer SoundPool play anchor missing')

# Persistent PAD VOL/readout messages should survive clock refreshes, but real user actions
# must replace them. Clear only on user-originated pad hits and transport actions; sequencer
# playback calls hit(..., false) and therefore cannot erase the readout by itself.
hit_sig='private fun hit(i:Int,user:Boolean){'
if hit_sig in s and 'if(user)displayOverride=null;' not in s:
    s=s.replace(hit_sig,'private fun hit(i:Int,user:Boolean){if(user)displayOverride=null;',1)
play_sig='private fun play(){if(playing)return;'
if play_sig in s:s=s.replace(play_sig,'private fun play(){if(playing)return;displayOverride=null;',1)
stop_sig='private fun stopTransport(){countInToken++;'
if stop_sig in s:s=s.replace(stop_sig,'private fun stopTransport(){displayOverride=null;countInToken++;',1)

# Akai documents TC as snapping notes to the selected grid and Swing as a timing offset.
# Keep those controls visible in MAIN immediately after the user changes them.
for old,new in [
 ('btn("TC"){tc=','btn("TC"){displayOverride=null;tc='),
 ('btn("SWING"){swing=','btn("SWING"){displayOverride=null;swing='),
 ('btn("TRACK −"){track=','btn("TRACK −"){displayOverride=null;track='),
 ('btn("TRACK +"){track=','btn("TRACK +"){displayOverride=null;track='),
 ('btn("METRO"){metronome=','btn("METRO"){displayOverride=null;metronome='),
 ('btn("COUNT IN"){countIn=','btn("COUNT IN"){displayOverride=null;countIn='),
 ('btn("TRACK MUTE"){mute[track]=','btn("TRACK MUTE"){displayOverride=null;mute[track]='),
 ('btn("NOTE REPEAT"){noteRepeat=','btn("NOTE REPEAT"){displayOverride=null;noteRepeat='),
 ('btn("FULL LEVEL"){fullLevel=','btn("FULL LEVEL"){displayOverride=null;fullLevel='),
 ('btn("16 LEVELS"){if(!levels16)','btn("16 LEVELS"){displayOverride=null;if(!levels16)')]:
    if old in s:s=s.replace(old,new,1)

destroy='override fun onDestroy(){'
if destroy in s and 'readySounds.clear();pendingAuditions.clear();' not in s:
    s=s.replace(destroy,'override fun onDestroy(){readySounds.clear();pendingAuditions.clear();',1)
p.write_text(s)
print('Audit fixes applied; audio readiness protected and stale display messages clear on meaningful controls')