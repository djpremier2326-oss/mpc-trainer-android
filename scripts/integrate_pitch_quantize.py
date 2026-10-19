from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
# Pad pitch is a per-pad program parameter in this trainer. +/-12 st maps to SoundPool 0.5x..2x.
old='private var masterVolume=1f;private val padVolumes=Array(4){FloatArray(16){1f}}'
new='private var masterVolume=1f;private val padVolumes=Array(4){FloatArray(16){1f}};private val padPitch=Array(4){IntArray(16){0}}'
if old not in s: raise SystemExit('pitch state anchor missing')
s=s.replace(old,new,1)
# Global TC state: when off, live recording must preserve performed timing.
state='private var bank=0;private var pad=0;private var track=0;private var bpm=90;private var tc=16;private var swing=50'
if state not in s: raise SystemExit('tc state anchor missing')
s=s.replace(state,state+';private var tcEnabled=true',1)
# Preserve requested pitch if the first manual hit occurs before async SoundPool load completes.
s=s.replace('private val readySounds=mutableSetOf<Int>();private val pendingAuditions=mutableMapOf<Int,Float>()','private val readySounds=mutableSetOf<Int>();private val pendingAuditions=mutableMapOf<Int,Pair<Float,Float>>()',1)
s=s.replace('pendingAuditions.remove(id)?.let{g->sp.play(id,g,g,1,0,1f)}','pendingAuditions.remove(id)?.let{pr->sp.play(id,pr.first,pr.first,1,0,pr.second)}',1)
old='private fun playLoadedSound(soundId:Int,gain:Float,queueManual:Boolean){val g=gain.coerceIn(0f,1f);if(readySounds.contains(soundId))pool.play(soundId,g,g,1,0,1f)else if(queueManual)pendingAuditions[soundId]=g}'
new='private fun pitchRate(b:Int,p:Int)=java.lang.Math.pow(2.0,padPitch[b][p]/12.0).toFloat().coerceIn(.5f,2f)\n private fun playLoadedSound(soundId:Int,gain:Float,queueManual:Boolean,rate:Float=1f){val g=gain.coerceIn(0f,1f);val r=rate.coerceIn(.5f,2f);if(readySounds.contains(soundId))pool.play(soundId,g,g,1,0,r)else if(queueManual)pendingAuditions[soundId]=Pair(g,r)}'
if old not in s: raise SystemExit('play helper anchor missing')
s=s.replace(old,new,1)
s=s.replace('playLoadedSound(it.soundId,g,true)','playLoadedSound(it.soundId,g,true,pitchRate(b,p))',1)
s=s.replace('playLoadedSound(it.soundId,g,false)','playLoadedSound(it.soundId,g,false,pitchRate(e.bank,e.pad))',1)
# Record raw timing when Global Timing Correct is off; when on, quantize start timing.
s=s.replace('events+=Event(b,p,quant(tick()),track,v)','events+=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v)',1)
# Newly assigned audio starts at unity volume / natural pitch.
s=s.replace('padVolumes[bank][pad]=1f;saveVolumes();savePads()','padVolumes[bank][pad]=1f;padPitch[bank][pad]=0;saveVolumes();savePads()',1)
s=s.replace('padVolumes[bank][pp]=1f;assigned++','padVolumes[bank][pp]=1f;padPitch[bank][pp]=0;assigned++',1)
# Persist pitch with pad volume state.
old='o.put("pads",a);getSharedPreferences("trainer_v2",MODE_PRIVATE).edit().putString("volumes",o.toString()).apply()'
new='o.put("pads",a);val pitch=JSONArray();for(b in 0..3)for(p in 0..15)pitch.put(padPitch[b][p]);o.put("pitch",pitch);getSharedPreferences("trainer_v2",MODE_PRIVATE).edit().putString("volumes",o.toString()).apply()'
if old not in s: raise SystemExit('save pitch anchor missing')
s=s.replace(old,new,1)
old='for(b in 0..3)for(p in 0..15){val i=b*16+p;if(i<a.length())padVolumes[b][p]=a.optDouble(i,1.0).toFloat().coerceIn(0f,1f)}}catch(_:Exception){}}'
new='for(b in 0..3)for(p in 0..15){val i=b*16+p;if(i<a.length())padVolumes[b][p]=a.optDouble(i,1.0).toFloat().coerceIn(0f,1f)};val pitch=o.optJSONArray("pitch");if(pitch!=null)for(b in 0..3)for(p in 0..15){val i=b*16+p;if(i<pitch.length())padPitch[b][p]=pitch.optInt(i,0).coerceIn(-12,12)}}catch(_:Exception){}}'
if old not in s: raise SystemExit('restore pitch anchor missing')
s=s.replace(old,new,1)
# Persist Global TC state in project JSON. Keep current trainer divisions intentionally focused on 1/8,1/16,1/32.
s=s.replace('.put("bank",bank).put("metro",metronome)','.put("bank",bank).put("tcOn",tcEnabled).put("metro",metronome)',1)
s=s.replace('bank=o.optInt("bank",0).coerceIn(0,3);metronome=', 'bank=o.optInt("bank",0).coerceIn(0,3);tcEnabled=o.optBoolean("tcOn",true);metronome=',1)
anchor='fun showPadVolumeOnDisplay(value:Int){showDisplayMessage("PAD VOL  ${value.coerceIn(0,100)}%")}'
block='''fun showPadVolumeOnDisplay(value:Int){showDisplayMessage("PAD VOL  ${value.coerceIn(0,100)}%")}
 fun adjustSelectedPadPitch(delta:Int):Int{val v=(padPitch[bank][pad]+delta).coerceIn(-12,12);padPitch[bank][pad]=v;saveVolumes();showDisplayMessage("PITCH  ${if(v>0)"+" else ""}$v st");return v}
 fun resetSelectedPadPitch(){padPitch[bank][pad]=0;saveVolumes();showDisplayMessage("PITCH  0 st")}
 fun setTimingCorrectEnabled(on:Boolean){tcEnabled=on;saveProject();showDisplayMessage("TC  ${if(on)"ON" else "OFF"} • 1/$tc • SWING $swing")}
 fun timingCorrectEnabled()=tcEnabled
 fun quantizeCurrentTrack():Int{var changed=0;for(i in events.indices){val e=events[i];if(e.track==track){val q=quant(e.tick);if(q!=e.tick){events[i]=e.copy(tick=q);changed++}}};saveProject();showDisplayMessage("TC DO IT • TRACK ${track+1} • $changed событий");report("tc");return changed}'''
if anchor not in s: raise SystemExit('public UI anchor missing')
s=s.replace(anchor,block,1)
p.write_text(s)

# UI: keep the 190dp display and pad grid unchanged. Split existing right utility column into a 2x2 VOL/PITCH cluster.
a=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt')
t=a.read_text()
old='''  val right=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL}
  right.addView(utility("VOL  −"){val v=a.adjustSelectedPadVolume(-5);a.showPadVolumeOnDisplay(v)},LinearLayout.LayoutParams(-1,a.dp(35)).apply{bottomMargin=a.dp(3)})
  right.addView(utility("VOL  +"){val v=a.adjustSelectedPadVolume(5);a.showPadVolumeOnDisplay(v)},LinearLayout.LayoutParams(-1,a.dp(35)))'''
new='''  val right=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL}
  fun pairRow(leftButton:Button,rightButton:Button)=LinearLayout(a).apply{orientation=LinearLayout.HORIZONTAL;addView(leftButton,LinearLayout.LayoutParams(0,a.dp(35),1f).apply{rightMargin=a.dp(3)});addView(rightButton,LinearLayout.LayoutParams(0,a.dp(35),1f))}
  right.addView(pairRow(utility("VOL −"){val v=a.adjustSelectedPadVolume(-5);a.showPadVolumeOnDisplay(v)},utility("PITCH −"){a.adjustSelectedPadPitch(-1)}),LinearLayout.LayoutParams(-1,a.dp(35)).apply{bottomMargin=a.dp(3)})
  right.addView(pairRow(utility("VOL +"){val v=a.adjustSelectedPadVolume(5);a.showPadVolumeOnDisplay(v)},utility("PITCH +"){a.adjustSelectedPadPitch(1)}),LinearLayout.LayoutParams(-1,a.dp(35)))'''
if old not in t: raise SystemExit('right utility cluster anchor missing')
t=t.replace(old,new,1)
# MENU retains reset and TC Do It/config actions. TC On/Off is explicit, matching Live III concept.
t=t.replace('val items=arrayOf("MIXER / MASTER VOLUME","CONTROL GUIDE","RESTART COURSE","NEW PROJECT")','val items=arrayOf("PITCH RESET","TC ON / OFF","TC • DO IT (CURRENT TRACK)","MIXER / MASTER VOLUME","CONTROL GUIDE","RESTART COURSE","NEW PROJECT")',1)
t=t.replace('when(w){0->{a.setWorkspaceMode("MIXER");a.showVolumeMixer()};1->guide?.performClick();2->restart?.performClick();3->confirmNewProject(a)}','when(w){0->a.resetSelectedPadPitch();1->a.setTimingCorrectEnabled(!a.timingCorrectEnabled());2->a.quantizeCurrentTrack();3->{a.setWorkspaceMode("MIXER");a.showVolumeMixer()};4->guide?.performClick();5->restart?.performClick();6->confirmNewProject(a)}',1)
a.write_text(t)
print('MPC-aligned TC on/off + Do It and visible per-pad pitch controls integrated')