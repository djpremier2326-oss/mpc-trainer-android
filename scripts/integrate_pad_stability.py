from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
# Guard SoundPool play calls used by the final playback helper. A bad/stale sound id or
# platform audio failure must not take the Activity down while finger-drumming.
old='private fun playLoadedSound(soundId:Int,gain:Float,queueManual:Boolean,rate:Float=1f){val g=gain.coerceIn(0f,1f);val r=rate.coerceIn(.5f,2f);if(readySounds.contains(soundId))pool.play(soundId,g,g,1,0,r)else if(queueManual)pendingAuditions[soundId]=Pair(g,r)}'
new='private fun playLoadedSound(soundId:Int,gain:Float,queueManual:Boolean,rate:Float=1f){if(soundId<=0)return;val g=gain.coerceIn(0f,1f);val r=rate.coerceIn(.5f,2f);try{if(readySounds.contains(soundId)){pool.play(soundId,g,g,1,0,r)}else if(queueManual){pendingAuditions[soundId]=Pair(g,r)}}catch(_:IllegalStateException){readySounds.remove(soundId);pendingAuditions.remove(soundId)}catch(_:RuntimeException){readySounds.remove(soundId);pendingAuditions.remove(soundId)}}'
if old not in s: raise SystemExit('final playLoadedSound anchor missing')
s=s.replace(old,new,1)
# Load callback can race with replacement/unload. Never audition a stale id blindly.
old='pool.setOnLoadCompleteListener{sp,id,status->if(status==0){readySounds.add(id);pendingAuditions.remove(id)?.let{pr->sp.play(id,pr.first,pr.first,1,0,pr.second)}}}'
new='pool.setOnLoadCompleteListener{sp,id,status->if(status==0){readySounds.add(id);pendingAuditions.remove(id)?.let{pr->try{sp.play(id,pr.first,pr.first,1,0,pr.second)}catch(_:RuntimeException){readySounds.remove(id)}}}else{readySounds.remove(id);pendingAuditions.remove(id)}}'
if old not in s: raise SystemExit('load callback anchor missing')
s=s.replace(old,new,1)
# Do not do a full status/workspace refresh for every raw finger-down. That refresh walks
# project/UI state and is unnecessary for ordinary auditioning; selection UI is refreshed by touch handler.
old='if(user&&loaded&&recording&&playing){if(levels16&&levels16Type=="TUNE"){showDisplayMessage("16 LEVELS • TUNE • запись pitch-событий пока недоступна")}else{events+=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);saveProject()}};refreshStatus()'
new='if(user&&loaded&&recording&&playing){if(levels16&&levels16Type=="TUNE"){showDisplayMessage("16 LEVELS • TUNE • запись pitch-событий пока недоступна")}else{events+=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);saveProject()}}'
if old not in s: raise SystemExit('hit refresh anchor missing')
s=s.replace(old,new,1)
# Release all callbacks and native audio resources deterministically.
old='override fun onDestroy(){'
new='override fun onDestroy(){h.removeCallbacksAndMessages(null);try{pool.setOnLoadCompleteListener(null)}catch(_:Exception){};try{pool.release()}catch(_:Exception){};try{tone.release()}catch(_:Exception){};'
if old not in s: raise SystemExit('onDestroy anchor missing')
s=s.replace(old,new,1)
p.write_text(s)
print('Rapid pad playback hardened: guarded SoundPool, stale-load handling, lighter hit path, deterministic release')