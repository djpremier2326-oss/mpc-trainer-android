from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
# Per-pad pitch state, persisted with project volumes.
old='private var masterVolume=1f;private val padVolumes=Array(4){FloatArray(16){1f}}'
new='private var masterVolume=1f;private val padVolumes=Array(4){FloatArray(16){1f}};private val padPitch=Array(4){IntArray(16){0}}'
if old not in s: raise SystemExit('pitch state anchor missing')
s=s.replace(old,new,1)
# SoundPool rate: +/-12 semitones => 0.5x..2x, matching SoundPool limits.
old='private fun playLoadedSound(soundId:Int,gain:Float,queueManual:Boolean){val g=gain.coerceIn(0f,1f);if(readySounds.contains(soundId))pool.play(soundId,g,g,1,0,1f)else if(queueManual)pendingAuditions[soundId]=g}'
new='private fun pitchRate(b:Int,p:Int)=kotlin.math.pow(2.0,padPitch[b][p]/12.0).toFloat().coerceIn(.5f,2f)\n private fun playLoadedSound(soundId:Int,gain:Float,queueManual:Boolean,rate:Float=1f){val g=gain.coerceIn(0f,1f);if(readySounds.contains(soundId))pool.play(soundId,g,g,1,0,rate.coerceIn(.5f,2f))else if(queueManual)pendingAuditions[soundId]=g}'
if old not in s: raise SystemExit('play helper anchor missing')
s=s.replace(old,new,1)
# Manual and sequencer playback use pitch of the actual source pad.
s=s.replace('playLoadedSound(it.soundId,g,true)','playLoadedSound(it.soundId,g,true,pitchRate(b,p))',1)
s=s.replace('playLoadedSound(it.soundId,g,false)','playLoadedSound(it.soundId,g,false,pitchRate(e.bank,e.pad))',1)
# Newly loaded/chopped samples start at natural pitch.
s=s.replace('padVolumes[bank][pad]=1f;saveVolumes();savePads()','padVolumes[bank][pad]=1f;padPitch[bank][pad]=0;saveVolumes();savePads()',1)
s=s.replace('padVolumes[bank][pp]=1f;assigned++','padVolumes[bank][pp]=1f;padPitch[bank][pp]=0;assigned++',1)
# Persist pitch beside volume.
old='o.put("pads",a);getSharedPreferences("trainer_v2",MODE_PRIVATE).edit().putString("volumes",o.toString()).apply()'
new='o.put("pads",a);val pitch=JSONArray();for(b in 0..3)for(p in 0..15)pitch.put(padPitch[b][p]);o.put("pitch",pitch);getSharedPreferences("trainer_v2",MODE_PRIVATE).edit().putString("volumes",o.toString()).apply()'
if old not in s: raise SystemExit('save pitch anchor missing')
s=s.replace(old,new,1)
old='for(b in 0..3)for(p in 0..15){val i=b*16+p;if(i<a.length())padVolumes[b][p]=a.optDouble(i,1.0).toFloat().coerceIn(0f,1f)}}catch(_:Exception){}}'
new='for(b in 0..3)for(p in 0..15){val i=b*16+p;if(i<a.length())padVolumes[b][p]=a.optDouble(i,1.0).toFloat().coerceIn(0f,1f)};val pitch=o.optJSONArray("pitch");if(pitch!=null)for(b in 0..3)for(p in 0..15){val i=b*16+p;if(i<pitch.length())padPitch[b][p]=pitch.optInt(i,0).coerceIn(-12,12)}}catch(_:Exception){}}'
if old not in s: raise SystemExit('restore pitch anchor missing')
s=s.replace(old,new,1)
# Public UI actions.
anchor='fun showPadVolumeOnDisplay(value:Int){showDisplayMessage("PAD VOL  ${value.coerceIn(0,100)}%")}'
block='''fun showPadVolumeOnDisplay(value:Int){showDisplayMessage("PAD VOL  ${value.coerceIn(0,100)}%")}
 fun adjustSelectedPadPitch(delta:Int):Int{val v=(padPitch[bank][pad]+delta).coerceIn(-12,12);padPitch[bank][pad]=v;saveVolumes();showDisplayMessage("PITCH  ${if(v>0)"+" else ""}$v st");return v}
 fun resetSelectedPadPitch(){padPitch[bank][pad]=0;saveVolumes();showDisplayMessage("PITCH  0 st")}
 fun quantizeCurrentTrack():Int{var changed=0;for(i in events.indices){val e=events[i];if(e.track==track){val q=quant(e.tick);if(q!=e.tick){events[i]=e.copy(tick=q);changed++}}};saveProject();showDisplayMessage("TC QUANTIZE • TRACK ${track+1} • $changed событий");report("tc");return changed}'''
if anchor not in s: raise SystemExit('public UI anchor missing')
s=s.replace(anchor,block,1)
p.write_text(s)

# Add visible phone controls without shrinking the 190dp display or pad grid.
a=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt')
t=a.read_text()
old='right.addView(utility("VOL  +"){val v=a.adjustSelectedPadVolume(5);a.showPadVolumeOnDisplay(v)},LinearLayout.LayoutParams(-1,a.dp(35)))'
new='right.addView(utility("VOL  +"){val v=a.adjustSelectedPadVolume(5);a.showPadVolumeOnDisplay(v)},LinearLayout.LayoutParams(-1,a.dp(35)))'
if old not in t: raise SystemExit('utility bar anchor missing')
# Pitch and quantize live in MENU to avoid stealing pad area; TC remains physical button.
t=t.replace('val items=arrayOf("MIXER / MASTER VOLUME","CONTROL GUIDE","RESTART COURSE","NEW PROJECT")','val items=arrayOf("PITCH −","PITCH +","PITCH RESET","TC • QUANTIZE TRACK","MIXER / MASTER VOLUME","CONTROL GUIDE","RESTART COURSE","NEW PROJECT")',1)
t=t.replace('when(w){0->{a.setWorkspaceMode("MIXER");a.showVolumeMixer()};1->guide?.performClick();2->restart?.performClick();3->confirmNewProject(a)}','when(w){0->a.adjustSelectedPadPitch(-1);1->a.adjustSelectedPadPitch(1);2->a.resetSelectedPadPitch();3->a.quantizeCurrentTrack();4->{a.setWorkspaceMode("MIXER");a.showVolumeMixer()};5->guide?.performClick();6->restart?.performClick();7->confirmNewProject(a)}',1)
a.write_text(t)
print('Pitch +/-12 semitones and post-record TC quantize integrated')