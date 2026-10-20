from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt');s=p.read_text()
def replace(old,new):
 global s
 if s.count(old)!=1:raise SystemExit('Playback consistency anchor not unique: '+old[:100])
 s=s.replace(old,new,1)
replace('data class Event(val bank:Int,val pad:Int,val tick:Long,val track:Int,val velocity:Int)','data class Event(val bank:Int,val pad:Int,val tick:Long,val track:Int,val velocity:Int,val tuneOffset:Int=0)')
replace('val r=rate.coerceIn(.5f,2f)','val r=rate.coerceIn(.125f,8f)')
replace('private fun levels16TuneSemitones(i:Int)=(i-levels16OriginalPad).coerceIn(-12,12)','private fun levels16TuneSemitones(i:Int)=(i-levels16OriginalPad).coerceIn(-15,15)')
old=next(line for line in s.splitlines() if line.startswith(' private fun hit(i:Int,user:Boolean)'))
new=''' private fun voiceParameters(e:Event)=PadVoice.parameters(e.velocity,padVolumes[e.bank][e.pad],masterVolume,padPitch[e.bank][e.pad],e.tuneOffset)
 private fun playEvent(e:Event,manual:Boolean){TrainerUiState.pulsePad(e.bank,e.pad);val params=voiceParameters(e);pads[e.bank][e.pad]?.let{playLoadedSound(it.soundId,params.gain,manual,params.rate)}}
 private fun hit(i:Int,user:Boolean){if(user)displayOverride=null;val b=if(levels16)sourceBank else bank;val p=if(levels16)sourcePad else i;val loaded=pads[b][p]!=null;val v=effectiveVelocity(velocity(i));val offset=if(levels16&&levels16Type=="TUNE")levels16TuneSemitones(i) else 0;val event=Event(b,p,if(playing){if(tcEnabled)quant(tick())else tick()}else 0L,track,v,offset);playEvent(event,true);if(user&&loaded&&recording&&playing){events+=event;recordedCycle[event]=loopCycle();if(overdub)passOverdub=true else passRecorded=true;saveProject()};if(user&&loaded){if(fullLevel)report("full_level_pad");if(levels16)report("16_levels_pad")};if(user)showDisplayMessage("BANK ${'A'+b} • PAD ${p+1} • ${pads[b][p]?.name?:"EMPTY"}\\n${levelReadout()}")else refreshStatus()}'''
replace(old,new)
old='e->TrainerUiState.pulsePad(e.bank,e.pad);pads[e.bank][e.pad]?.let{val g=TrainerPlayback.gain(e.velocity,padVolumes[e.bank][e.pad],masterVolume);playLoadedSound(it.soundId,g,false,pitchRate(e.bank,e.pad))}'
replace(old,'e->playEvent(e,false)')
# New step hits and regular phone hits have the same strength. Existing intentional
# 16 Levels dynamics are retained; only the old fixed "100" baseline is migrated.
replace('Event(bank,pad,t,track,100)','Event(bank,pad,t,track,127)')
replace('val vel=if(on)events[ix].velocity else 100','val vel=if(on)events[ix].velocity else 127')
replace('put("bpm",bpm).put("tc",tc)','put("velocitySchema",2).put("bpm",bpm).put("tc",tc)')
replace('put("v",it.velocity))','put("v",it.velocity).put("tune",it.tuneOffset))')
replace('val ev=e.optInt("v",100).coerceIn(1,127)','val ev=PadVoice.restoredVelocity(e.optInt("v",127),o.optInt("velocitySchema",1))')
replace('Event(eb,ep,et%loopTicks(),er,ev)','Event(eb,ep,et%loopTicks(),er,ev,e.optInt("tune",0).coerceIn(-15,15))')
# Hardware volume keys explicitly target music, not the ringtone stream.
replace('super.onCreate(savedInstanceState);pool=UnifiedAudioEngine(this);','super.onCreate(savedInstanceState);volumeControlStream=AudioManager.STREAM_MUSIC;pool=UnifiedAudioEngine(this);')
replace('fun showPadVolumeOnDisplay(value:Int){showDisplayMessage("PAD VOL  ${value.coerceIn(0,100)}%")}','fun showPadVolumeOnDisplay(value:Int){showDisplayMessage(levelReadout())}')
block=''' private fun levelReadout():String="MASTER ${(masterVolume*100).roundToLong()}% • PAD ${(padVolumes[bank][pad]*100).roundToLong()}% • PITCH ${padPitch[bank][pad]} st${if(levels16)" • 16 LEVELS $levels16Type" else ""}"
 fun restoreNormalLevels(){masterVolume=1f;padVolumes[bank][pad]=1f;levels16=false;fullLevel=false;noteRepeat=false;stopRepeat();saveVolumes();refreshPads();showDisplayMessage("Обычная громкость: MASTER 100% / PAD 100%\\nPITCH сохранён: ${padPitch[bank][pad]} st")}
 private fun showLevelHelp(){AlertDialog.Builder(this).setTitle("Громкость звука").setMessage("${levelReadout()}\\n\\nMASTER управляет всем проектом. PAD — выбранным звуком. Если оба уровня уменьшены, их ослабление складывается. 16 LEVELS VELOCITY намеренно даёт разную силу ударов.\\nКнопки громкости телефона теперь управляют именно мультимедиа.").setPositiveButton("MASTER / PAD 100%"){_,_->restoreNormalLevels()}.setNegativeButton("Закрыть",null).show()}
'''
replace(' private fun velocity(i:Int)',block+' private fun velocity(i:Int)')
# The current levels must stay visible, even during a long theory explanation.
replace('private fun refreshStatus(){TrainerUiState.publish','private fun refreshStatus(){displayInfo?.apply{visibility=View.VISIBLE;layoutParams=FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);setBackgroundColor(Color.rgb(17,28,25));text=levelReadout();setOnClickListener{showLevelHelp()}};TrainerUiState.publish')
replace('fun showVolumeMixer(){val box=', 'fun showVolumeMixer(){val box=')
replace('setTitle("MIXER / VOLUME").setView(box).setNegativeButton','setTitle("MIXER / VOLUME").setView(box).setNeutralButton("100% / 100%"){_,_->restoreNormalLevels()}.setNegativeButton')
# A second STOP is the hardware's all-sounds-off operation. PLAY START also clears
# the old tail so repeated audition does not stack it with a new pass.
replace('private var lessonReady=false;','private var lastStopAt=-1000L;private var lessonReady=false;')
replace('private fun stopTransport(){displayOverride=null;','private fun stopTransport(){val now=SystemClock.elapsedRealtime();if(now-lastStopAt<500L)pool.stopAll();lastStopAt=now;displayOverride=null;')
replace('setOnLongClickListener{playing=false;recording=false;','setOnLongClickListener{pool.stopAll();playing=false;recording=false;')
s=s.replace('MPC УЧЁБА · 228','MPC УЧЁБА · 229')
p.write_text(s)
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt');s=p.read_text().replace('MPC УЧЁБА · 228','MPC УЧЁБА · 229');p.write_text(s)
p=Path('app/src/main/java/com/bytbeat/mpctrainer/LessonContent.kt');s=p.read_text().replace('Запись TUNE пока недоступна.','TUNE также записывается в события.').replace('В этой версии TUNE можно прослушивать, запись мелодии ещё не реализована.','В этой версии TUNE можно прослушивать и записывать в REC или OVERDUB.');p.write_text(s)
p=Path('app/src/main/java/com/bytbeat/mpctrainer/LearnEngine.kt');s=p.read_text().replace('Запись TUNE пока недоступна.','TUNE также записывается в события.');p.write_text(s)
# Drop the old source-string assertion which enshrined missing Tune recording.
p=Path('app/src/test/java/com/bytbeat/mpctrainer/PitchTimingCorrectIntegrationTest.kt');s=p.read_text().replace('basic_16_levels_exposes_velocity_and_tune_without_fake_tune_recording','basic_16_levels_exposes_velocity_and_persisted_tune_recording').replace('assertTrue(s.contains("запись pitch-событий пока недоступна"))','assertTrue(s.contains("val tuneOffset:Int=0")); assertTrue(s.contains("put(\\"tune\\",it.tuneOffset)"))');p.write_text(s)
p=Path('app/src/test/java/com/bytbeat/mpctrainer/TrainerBehaviorTest.kt');s=p.read_text().replace('MPC УЧЁБА · 228','MPC УЧЁБА · 229');p.write_text(s)
print('Live and recorded events share gain and pitch; legacy baseline fixed; visible levels and unity reset')
