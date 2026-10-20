from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt');s=p.read_text()
def replace(old,new):
 global s
 if s.count(old)!=1:raise SystemExit('Unified audio anchor not unique: '+old[:90])
 s=s.replace(old,new,1)
replace('private lateinit var pool:SoundPool','private lateinit var pool:UnifiedAudioEngine')
replace('pool=SoundPool.Builder().setMaxStreams(24).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).build();','pool=UnifiedAudioEngine(this);')
replace('BrowserPreview(this)','BrowserPreview(this,pool){masterVolume}')
# Loading a file resets per-pad pitch and volume. Preview also follows project
# MASTER so a low MASTER cannot produce a deceptive preview/pad comparison.
replace('PREVIEW воспроизводит полный WAV/MP3. После LOAD звук назначается на выбранный пэд.','PREVIEW и пэды используют один аудиодвижок и общий MASTER. После LOAD: PAD VOL 100%, PITCH 0. Полный WAV/MP3 без обрезки SoundPool.')
# A failed load must be visible instead of an apparently assigned silent pad.
old='else{readySounds.remove(id);pendingAuditions.remove(id)}}};restorePads()'
new='else{readySounds.remove(id);pendingAuditions.remove(id);Toast.makeText(this,"Не удалось загрузить аудио: файл слишком большой или формат не поддержан",Toast.LENGTH_LONG).show()}}};restorePads()'
replace(old,new)
p.write_text(s)
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt');s=p.read_text()
old='pairRow(utility("VOL −"){val v=a.adjustSelectedPadVolume(-5);a.showPadVolumeOnDisplay(v)},utility("PITCH −"){a.adjustSelectedPadPitch(-1)})'
new='pairRow(utility("PITCH −"){a.adjustSelectedPadPitch(-1)},utility("VOL −"){val v=a.adjustSelectedPadVolume(-5);a.showPadVolumeOnDisplay(v)})'
if s.count(old)!=1:raise SystemExit('Lower minus button order anchor missing')
s=s.replace(old,new,1)
old='pairRow(utility("VOL +"){val v=a.adjustSelectedPadVolume(5);a.showPadVolumeOnDisplay(v)},utility("PITCH +"){a.adjustSelectedPadPitch(1)})'
new='pairRow(utility("PITCH +"){a.adjustSelectedPadPitch(1)},utility("VOL +"){val v=a.adjustSelectedPadVolume(5);a.showPadVolumeOnDisplay(v)})'
if s.count(old)!=1:raise SystemExit('Lower plus button order anchor missing')
s=s.replace(old,new,1).replace('MPC TRAINER · 225','MPC УЧЁБА · 228');p.write_text(s)
# Keep the automatic welcome and its tests in sync with the visible build.
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt');s=p.read_text().replace('course_welcome",0)>=225','course_welcome",0)>=228').replace('putInt("course_welcome",225)','putInt("course_welcome",228)');p.write_text(s)
p=Path('app/src/test/java/com/bytbeat/mpctrainer/TrainerBehaviorTest.kt');s=p.read_text().replace('putInt("course_welcome",225)','putInt("course_welcome",228)').replace('MPC TRAINER · 225','MPC УЧЁБА · 228');p.write_text(s)
print('Shared float PCM engine for Browser and pads; Pitch left, Volume right; build 228')
