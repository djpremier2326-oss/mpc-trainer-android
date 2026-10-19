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
new='private fun clearCurrentTrackForRecord(){events.removeAll{it.track==track};recordedCycle.keys.removeAll{it.track==track}}\n private fun record(od:Boolean){if(playing)return;countInToken++;overdub=od;if(countIn){recording=false;playing=false;val token=countInToken;countDown(4,token){if(!od)clearCurrentTrackForRecord();recording=true;playing=true;startMs=SystemClock.elapsedRealtime();startClock();saveProject();refreshStatus()}}else{if(!od)clearCurrentTrackForRecord();recording=true;playing=true;startMs=SystemClock.elapsedRealtime();startClock();saveProject()};refreshStatus()}'
if old in s:s=s.replace(old,new,1)
s=s.replace('${s?.name?.take(8)?:"EMPTY"}','${s?.name?:"EMPTY"}')
s=s.replace('"${label.take(7)}_${i+1}"','"${label}_${i+1}"')
s=s.replace('width=150;height=105','width=(resources.displayMetrics.widthPixels-(64*resources.displayMetrics.density).toInt())/4;height=(56*resources.displayMetrics.density).toInt()')
repls={'"Project saved"':'"Проект сохранён"','"Lesson complete"':'"Урок завершён"','"Load a WAV or MP3 to a pad first"':'"Сначала загрузите WAV или MP3 на пэд"','"Only $room pad slots remain in this bank"':'"В этом банке осталось только $room свободных пэдов"','"$count chops assigned to pads"':'"$count фрагментов назначено на пэды"','"Set Start/End, then chop the selected range evenly across pads."':'"Установите Start/End, затем равномерно разрежьте выбранный диапазон по пэдам."','"NUMBER OF CHOPS"':'"КОЛИЧЕСТВО CHOPS"'}
for a,b in repls.items():s=s.replace(a,b)
p.write_text(s)
print('Audit fixes applied')