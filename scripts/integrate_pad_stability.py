from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
# Guard SoundPool play calls used by the final playback helper. A bad/stale sound id or
# platform audio failure must not take the Activity down while finger-drumming.
old='private fun playLoadedSound(soundId:Int,gain:Float,queueManual:Boolean,rate:Float=1f){val g=gain.coerceIn(0f,1f);val r=rate.coerceIn(.5f,2f);if(readySounds.contains(soundId))pool.play(soundId,g,g,1,0,r)else if(queueManual)pendingAuditions[soundId]=Pair(g,r)}'
new='private fun playLoadedSound(soundId:Int,gain:Float,queueManual:Boolean,rate:Float=1f){if(soundId<=0)return;val g=gain.coerceIn(0f,1f);val r=rate.coerceIn(.5f,2f);try{if(readySounds.contains(soundId)){pool.play(soundId,g,g,1,0,r)}else if(queueManual){pendingAuditions[soundId]=Pair(g,r)}}catch(_:IllegalStateException){readySounds.remove(soundId);pendingAuditions.remove(soundId)}catch(_:RuntimeException){readySounds.remove(soundId);pendingAuditions.remove(soundId)}}'
if old in s:
    s=s.replace(old,new,1)
elif new not in s:
    raise SystemExit('final playLoadedSound anchor missing')

callback_candidates=[
 'pool.setOnLoadCompleteListener{sp,id,status->if(status==0){readySounds.add(id);pendingAuditions.remove(id)?.let{pr->sp.play(id,pr.first,pr.first,1,0,pr.second)}}}',
 'pool.setOnLoadCompleteListener{sp,id,statusCode->runOnUiThread{if(statusCode==0){readySounds.add(id);pendingAuditions.remove(id)?.let{pr->sp.play(id,pr.first,pr.first,1,0,pr.second)}}else{pendingAuditions.remove(id)}}}',
]
guarded='pool.setOnLoadCompleteListener{sp,id,statusCode->runOnUiThread{if(statusCode==0){readySounds.add(id);pendingAuditions.remove(id)?.let{pr->try{sp.play(id,pr.first,pr.first,1,0,pr.second)}catch(_:RuntimeException){readySounds.remove(id);pendingAuditions.remove(id)}}}else{readySounds.remove(id);pendingAuditions.remove(id)}}}'
if guarded not in s:
    found=next((x for x in callback_candidates if x in s),None)
    if found is None: raise SystemExit('load callback anchor missing')
    s=s.replace(found,guarded,1)

# Remove only the redundant full refresh at the end of a raw hit. Recording monitoring may
# have already wrapped the event in an Event variable and recordedCycle bookkeeping.
record_candidates=[
 'if(user&&loaded&&recording&&playing){if(levels16&&levels16Type=="TUNE"){showDisplayMessage("16 LEVELS • TUNE • запись pitch-событий пока недоступна")}else{events+=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);saveProject()}};refreshStatus()',
 'if(user&&loaded&&recording&&playing){if(levels16&&levels16Type=="TUNE"){showDisplayMessage("16 LEVELS • TUNE • запись pitch-событий пока недоступна")}else{val e=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);events+=e;recordedCycle[e]=loopCycle();saveProject()}};refreshStatus()',
]
found=next((x for x in record_candidates if x in s),None)
if found is not None:
    s=s.replace(found,found[:-len(';refreshStatus()')],1)
elif 'recordedCycle[e]=loopCycle();saveProject()}}' not in s and 'events+=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);saveProject()}}' not in s:
    raise SystemExit('hit recording anchor missing')

release='h.removeCallbacksAndMessages(null);try{pool.setOnLoadCompleteListener(null)}catch(_:Exception){};try{pool.release()}catch(_:Exception){};try{tone.release()}catch(_:Exception){};'
if release not in s:
    old='override fun onDestroy(){'
    if old not in s: raise SystemExit('onDestroy anchor missing')
    s=s.replace(old,'override fun onDestroy(){'+release,1)

p.write_text(s)
print('Rapid pad playback hardened: guarded SoundPool, monitored record path, lighter hit path, deterministic release')