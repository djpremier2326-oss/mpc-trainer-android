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
    helper=' private fun playLoadedSound(soundId:Int,gain:Float,queueManual:Boolean){val g=gain.coerceIn(0f,1f);if(readySounds.contains(soundId))pool.play(soundId,g,g,1,0,1f)else if(queueManual)pendingAuditions[soundId]=g}\n private fun unloadPadSound(sample:PadSample?){sample?:return;pendingAuditions.remove(sample.soundId);readySounds.remove(sample.soundId);pool.unload(sample.soundId)}\n'
    if helper_anchor not in s: raise SystemExit('hit anchor missing')
    s=s.replace(helper_anchor,helper+helper_anchor,1)
manual='pool.play(it.soundId,g,g,1,0,1f)'
if manual in s:s=s.replace(manual,'playLoadedSound(it.soundId,g,true)',1)
else: raise SystemExit('manual SoundPool play anchor missing')
if manual in s:s=s.replace(manual,'playLoadedSound(it.soundId,g,false)',1)
else: raise SystemExit('sequencer SoundPool play anchor missing')
load_anchor='pads[bank][pad]=PadSample(pool.load(f.absolutePath,1),pendingName.substringBeforeLast(\'.\'),f.absolutePath);padVolumes[bank][pad]=1f;saveVolumes();savePads();refreshPads();report("loaded")'
load_new='unloadPadSound(pads[bank][pad]);pads[bank][pad]=PadSample(pool.load(f.absolutePath,1),pendingName.substringBeforeLast(\'.\'),f.absolutePath);padVolumes[bank][pad]=1f;saveVolumes();savePads();refreshPads();report("loaded")'
if load_anchor in s:s=s.replace(load_anchor,load_new,1)
elif 'unloadPadSound(pads[bank][pad]);pads[bank][pad]=PadSample' not in s: raise SystemExit('pad replacement anchor missing')
chop_prefix='pads[bank][pp]=PadSample(pool.load(f.absolutePath,1),'
if 'unloadPadSound(pads[bank][pp]);pads[bank][pp]=PadSample' not in s:
    if chop_prefix not in s: raise SystemExit('chop replacement anchor missing')
    s=s.replace(chop_prefix,'unloadPadSound(pads[bank][pp]);'+chop_prefix,1)
chop_done=';assigned++}};savePads();refreshPads();report("sample_edit_chop")'
if chop_done in s:s=s.replace(chop_done,';padVolumes[bank][pp]=1f;assigned++}};saveVolumes();savePads();refreshPads();report("sample_edit_chop")',1)
hit_sig='private fun hit(i:Int,user:Boolean){'
if hit_sig in s and 'if(user)displayOverride=null;' not in s:s=s.replace(hit_sig,'private fun hit(i:Int,user:Boolean){if(user)displayOverride=null;',1)
# A manual pad hit should identify the actual source sample on the MPC display. In 16 LEVELS the source pad is used.
hit_refresh=';refreshStatus()}\n private fun loopTicks()'
if 'PAD ${p+1} • ${pads[b][p]?.name?:"EMPTY"}' not in s:
    if hit_refresh not in s: raise SystemExit('pad display anchor missing')
    s=s.replace(hit_refresh,';if(user)showDisplayMessage("BANK ${\'A\'+b} • PAD ${p+1} • ${pads[b][p]?.name?:\"EMPTY\"}");else refreshStatus()}\n private fun loopTicks()',1)
play_sig='private fun play(){if(playing)return;'
if play_sig in s:s=s.replace(play_sig,'private fun play(){if(playing)return;displayOverride=null;',1)
stop_sig='private fun stopTransport(){countInToken++;'
if stop_sig in s:s=s.replace(stop_sig,'private fun stopTransport(){displayOverride=null;countInToken++;',1)
for old,new in [('btn("TC"){tc=','btn("TC"){displayOverride=null;tc='),('btn("SWING"){swing=','btn("SWING"){displayOverride=null;swing='),('btn("TRACK −"){track=','btn("TRACK −"){displayOverride=null;track='),('btn("TRACK +"){track=','btn("TRACK +"){displayOverride=null;track='),('btn("METRO"){metronome=','btn("METRO"){displayOverride=null;metronome='),('btn("COUNT IN"){countIn=','btn("COUNT IN"){displayOverride=null;countIn='),('btn("TRACK MUTE"){mute[track]=','btn("TRACK MUTE"){displayOverride=null;mute[track]='),('btn("NOTE REPEAT"){noteRepeat=','btn("NOTE REPEAT"){displayOverride=null;noteRepeat='),('btn("FULL LEVEL"){fullLevel=','btn("FULL LEVEL"){displayOverride=null;fullLevel='),('btn("16 LEVELS"){if(!levels16)','btn("16 LEVELS"){displayOverride=null;if(!levels16)')]:
    if old in s:s=s.replace(old,new,1)
# Save/restore the active bank as project context, and sanitize restored control values.
save_head='JSONObject().put("bpm",bpm).put("tc",tc).put("swing",swing).put("track",track)'
save_head_new='JSONObject().put("bpm",bpm).put("tc",tc).put("swing",swing).put("track",track).put("bank",bank)'
if save_head in s:s=s.replace(save_head,save_head_new,1)
elif '.put("bank",bank)' not in s: raise SystemExit('save project bank anchor missing')
restore_controls='bpm=o.optInt("bpm",90).coerceIn(40,240);tc=o.optInt("tc",16);swing=o.optInt("swing",50);track=o.optInt("track",0).coerceIn(0,3);metronome=o.optBoolean("metro",false);countIn=o.optBoolean("count",true)'
restore_controls_new='bpm=o.optInt("bpm",90).coerceIn(40,240);tc=o.optInt("tc",16).let{if(it in listOf(8,16,32))it else 16};swing=o.optInt("swing",50).coerceIn(50,75);track=o.optInt("track",0).coerceIn(0,3);bank=o.optInt("bank",0).coerceIn(0,3);metronome=o.optBoolean("metro",false);countIn=o.optBoolean("count",true)'
if restore_controls in s:s=s.replace(restore_controls,restore_controls_new,1)
elif 'bank=o.optInt("bank",0).coerceIn(0,3)' not in s: raise SystemExit('restore project controls anchor missing')
# Restore only valid project data. Corrupt/out-of-range events must not crash playback after relaunch.
restore='events+=Event(e.optInt("b"),e.optInt("p"),e.optLong("t"),e.optInt("tr"),e.optInt("v",100))'
restore_new='val eb=e.optInt("b",-1);val ep=e.optInt("p",-1);val et=e.optLong("t",-1L);val er=e.optInt("tr",-1);val ev=e.optInt("v",100).coerceIn(1,127);if(eb in 0..3&&ep in 0..15&&er in 0..3&&et>=0L)events+=Event(eb,ep,et%loopTicks(),er,ev)'
if restore in s:s=s.replace(restore,restore_new,1)
elif 'if(eb in 0..3&&ep in 0..15&&er in 0..3' not in s: raise SystemExit('restore event validation anchor missing')
# A missing sample file should not leave a ghost pad assignment after app relaunch.
restore_pad='if(f.exists())pads[o.getInt("b")][o.getInt("p")]=PadSample(pool.load(f.absolutePath,1),o.getString("n"),f.absolutePath)'
restore_pad_new='val rb=o.optInt("b",-1);val rp=o.optInt("p",-1);if(f.exists()&&rb in 0..3&&rp in 0..15)pads[rb][rp]=PadSample(pool.load(f.absolutePath,1),o.optString("n",f.nameWithoutExtension),f.absolutePath)'
if restore_pad in s:s=s.replace(restore_pad,restore_pad_new,1)
elif 'if(f.exists()&&rb in 0..3&&rp in 0..15)' not in s: raise SystemExit('restore pad validation anchor missing')
destroy='override fun onDestroy(){'
if destroy in s and 'readySounds.clear();pendingAuditions.clear();' not in s:s=s.replace(destroy,'override fun onDestroy(){readySounds.clear();pendingAuditions.clear();',1)
p.write_text(s)
print('Audit fixes applied; project bank/control state persists safely, restored pads/events validated')