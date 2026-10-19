from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
old='private var playing=false;private var recording=false;private var overdub=false;private var noteRepeat=false;private var fullLevel=false;private var levels16=false'
new='private var playing=false;private var recording=false;private var overdub=false;private var noteRepeat=false;private var fullLevel=false;private var levels16=false;private var levels16Type="VELOCITY";private var levels16OriginalPad=7'
if old not in s: raise SystemExit('16 levels state anchor missing')
s=s.replace(old,new,1)
# Phone UI audit rewrites the visible button label to a two-line form before this script runs.
button_candidates=[
 'btn("16 LEVELS"){if(!levels16){sourceBank=bank;sourcePad=pad};levels16=!levels16;report("16_levels");refreshPads();refreshStatus()}',
 'btn("16\nLEVELS"){if(!levels16){sourceBank=bank;sourcePad=pad};levels16=!levels16;report("16_levels");refreshPads();refreshStatus()}'
]
found=next((x for x in button_candidates if x in s),None)
if found is None: raise SystemExit('16 levels button anchor missing after phone UI transform')
s=s.replace(found,found.split('{',1)[0]+'{show16Levels()}',1)
old='private fun velocity(i:Int)=if(levels16)(((i+1)*127f/16f).roundToLong().toInt()).coerceIn(1,127) else if(fullLevel)127 else 100'
new='private fun velocity(i:Int)=if(levels16&&levels16Type=="VELOCITY")(((i+1)*127f/16f).roundToLong().toInt()).coerceIn(1,127) else if(fullLevel)127 else 100\n private fun levels16TuneSemitones(i:Int)=(i-levels16OriginalPad).coerceIn(-12,12)\n private fun levels16Rate(i:Int)=java.lang.Math.pow(2.0,levels16TuneSemitones(i)/12.0).toFloat().coerceIn(.5f,2f)'
if old not in s: raise SystemExit('velocity anchor missing')
s=s.replace(old,new,1)
old='val g=padGain(v,b,p);pads[b][p]?.let{playLoadedSound(it.soundId,g,true,pitchRate(b,p))}'
new='val g=padGain(v,b,p);val rate=if(levels16&&levels16Type=="TUNE")levels16Rate(i) else pitchRate(b,p);pads[b][p]?.let{playLoadedSound(it.soundId,g,true,rate)}'
if old not in s: raise SystemExit('manual playback pitch anchor missing')
s=s.replace(old,new,1)
old='if(user&&loaded&&recording&&playing){events+=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);saveProject()}'
new='if(user&&loaded&&recording&&playing){if(levels16&&levels16Type=="TUNE"){showDisplayMessage("16 LEVELS • TUNE • запись pitch-событий пока недоступна")}else{events+=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);saveProject()}}'
if old not in s: raise SystemExit('record anchor missing')
s=s.replace(old,new,1)
old='private val repeater=object:Runnable{override fun run(){if(noteRepeat){hit(pad,true);h.postDelayed(this,(60000.0/bpm*(4.0/tc)).toLong().coerceAtLeast(25))}}};private fun startRepeat(){h.removeCallbacks(repeater);h.post(repeater)};private fun stopRepeat(){h.removeCallbacks(repeater)}'
new='private fun noteRepeatIntervalMs()=(60000.0/bpm*(4.0/tc.coerceAtLeast(1))).toLong().coerceAtLeast(25)\n private val repeater=object:Runnable{override fun run(){if(noteRepeat){hit(pad,true);h.postDelayed(this,noteRepeatIntervalMs())}}};private fun startRepeat(){h.removeCallbacks(repeater);h.post(repeater)};private fun stopRepeat(){h.removeCallbacks(repeater)}'
if old not in s: raise SystemExit('note repeat anchor missing')
s=s.replace(old,new,1)
old='private fun redrawSteps(page:Int,label:TextView,bs:List<Button>){label.text="BAR ${page+1} • TRACK ${track+1} • PAD ${pad+1}";bs.forEachIndexed{i,b->val t=page*384L+i*24L;val on=events.any{it.track==track&&it.bank==bank&&it.pad==pad&&it.tick==t};b.text="${i+1}\\n${if(on)"●" else "○"}";b.setOnClickListener{val ix=events.indexOfFirst{it.track==track&&it.bank==bank&&it.pad==pad&&it.tick==t};if(ix>=0)events.removeAt(ix)else{events+=Event(bank,pad,t,track,127);report("step_added")};saveProject();redrawSteps(page,label,bs)}}}'
new='private fun redrawSteps(page:Int,label:TextView,bs:List<Button>){label.text="BAR ${page+1} • TRACK ${track+1} • BANK ${\'A\'+bank} • PAD ${pad+1} • 1/16";bs.forEachIndexed{i,b->val t=page*384L+i*24L;val ix=events.indexOfFirst{it.track==track&&it.bank==bank&&it.pad==pad&&it.tick==t};val on=ix>=0;val vel=if(on)events[ix].velocity else 100;b.text="${i+1}\\n${if(on)"● $vel" else "○"}";b.setOnClickListener{val hit=events.indexOfFirst{it.track==track&&it.bank==bank&&it.pad==pad&&it.tick==t};if(hit>=0)events.removeAt(hit)else{events+=Event(bank,pad,t,track,100);report("step_added")};saveProject();redrawSteps(page,label,bs)}}}'
if old not in s: raise SystemExit('step redraw anchor missing')
s=s.replace(old,new,1)
# Pitch state follows the volume integration which runs earlier.
old='private var masterVolume=1f;private val padVolumes=Array(4){FloatArray(16){1f}}'
new='private var masterVolume=1f;private val padVolumes=Array(4){FloatArray(16){1f}};private val padPitch=Array(4){IntArray(16){0}}'
if old not in s: raise SystemExit('pitch state anchor missing')
s=s.replace(old,new,1)
state='private var bank=0;private var pad=0;private var track=0;private var bpm=90;private var tc=16;private var swing=50'
if state not in s: raise SystemExit('tc state anchor missing')
s=s.replace(state,state+';private var tcEnabled=true',1)
s=s.replace('private val readySounds=mutableSetOf<Int>();private val pendingAuditions=mutableMapOf<Int,Float>()','private val readySounds=mutableSetOf<Int>();private val pendingAuditions=mutableMapOf<Int,Pair<Float,Float>>()',1)
s=s.replace('pendingAuditions.remove(id)?.let{g->sp.play(id,g,g,1,0,1f)}','pendingAuditions.remove(id)?.let{pr->sp.play(id,pr.first,pr.first,1,0,pr.second)}',1)
old='private fun playLoadedSound(soundId:Int,gain:Float,queueManual:Boolean){val g=gain.coerceIn(0f,1f);if(readySounds.contains(soundId))pool.play(soundId,g,g,1,0,1f)else if(queueManual)pendingAuditions[soundId]=g}'
new='private fun pitchRate(b:Int,p:Int)=java.lang.Math.pow(2.0,padPitch[b][p]/12.0).toFloat().coerceIn(.5f,2f)\n private fun playLoadedSound(soundId:Int,gain:Float,queueManual:Boolean,rate:Float=1f){val g=gain.coerceIn(0f,1f);val r=rate.coerceIn(.5f,2f);if(readySounds.contains(soundId))pool.play(soundId,g,g,1,0,r)else if(queueManual)pendingAuditions[soundId]=Pair(g,r)}'
if old not in s: raise SystemExit('play helper anchor missing')
s=s.replace(old,new,1)
s=s.replace('playLoadedSound(it.soundId,g,false)','playLoadedSound(it.soundId,g,false,pitchRate(e.bank,e.pad))',1)
s=s.replace('padVolumes[bank][pad]=1f;saveVolumes();savePads()','padVolumes[bank][pad]=1f;padPitch[bank][pad]=0;saveVolumes();savePads()',1)
s=s.replace('padVolumes[bank][pp]=1f;assigned++','padVolumes[bank][pp]=1f;padPitch[bank][pp]=0;assigned++',1)
old='o.put("pads",a);getSharedPreferences("trainer_v2",MODE_PRIVATE).edit().putString("volumes",o.toString()).apply()'
new='o.put("pads",a);val pitch=JSONArray();for(b in 0..3)for(p in 0..15)pitch.put(padPitch[b][p]);o.put("pitch",pitch);getSharedPreferences("trainer_v2",MODE_PRIVATE).edit().putString("volumes",o.toString()).apply()'
if old not in s: raise SystemExit('save pitch anchor missing')
s=s.replace(old,new,1)
old='for(b in 0..3)for(p in 0..15){val i=b*16+p;if(i<a.length())padVolumes[b][p]=a.optDouble(i,1.0).toFloat().coerceIn(0f,1f)}}catch(_:Exception){}}'
new='for(b in 0..3)for(p in 0..15){val i=b*16+p;if(i<a.length())padVolumes[b][p]=a.optDouble(i,1.0).toFloat().coerceIn(0f,1f)};val pitch=o.optJSONArray("pitch");if(pitch!=null)for(b in 0..3)for(p in 0..15){val i=b*16+p;if(i<pitch.length())padPitch[b][p]=pitch.optInt(i,0).coerceIn(-12,12)}}catch(_:Exception){}}'
if old not in s: raise SystemExit('restore pitch anchor missing')
s=s.replace(old,new,1)
save_anchor='.put("bank",bank).put("metro",metronome)'
if save_anchor not in s: raise SystemExit('TC save persistence anchor missing')
s=s.replace(save_anchor,'.put("bank",bank).put("tcOn",tcEnabled).put("metro",metronome)',1)
restore_anchor='bank=o.optInt("bank",0).coerceIn(0,3);metronome='
if restore_anchor not in s: raise SystemExit('TC restore persistence anchor missing')
s=s.replace(restore_anchor,'bank=o.optInt("bank",0).coerceIn(0,3);tcEnabled=o.optBoolean("tcOn",true);metronome=',1)
tc_candidates=['btn("TC"){tc=when(tc){8->16;16->32;else->8};saveProject();report("tc");refreshStatus()}','btn("TC"){showTimingCorrect()}']
tc_found=next((x for x in tc_candidates if x in s),None)
if tc_found is None: raise SystemExit('TC button anchor missing')
if 'showTimingCorrect()' not in tc_found:s=s.replace(tc_found,'btn("TC"){showTimingCorrect()}',1)
anchor='fun showPadVolumeOnDisplay(value:Int){showDisplayMessage("PAD VOL  ${value.coerceIn(0,100)}%")}'
block='''fun showPadVolumeOnDisplay(value:Int){showDisplayMessage("PAD VOL  ${value.coerceIn(0,100)}%")}
 fun adjustSelectedPadPitch(delta:Int):Int{val v=(padPitch[bank][pad]+delta).coerceIn(-12,12);padPitch[bank][pad]=v;saveVolumes();showDisplayMessage("PITCH  ${if(v>0)"+" else ""}$v st");return v}
 fun resetSelectedPadPitch(){padPitch[bank][pad]=0;saveVolumes();showDisplayMessage("PITCH  0 st")}
 fun setTimingCorrectEnabled(on:Boolean){tcEnabled=on;saveProject();showDisplayMessage("TC  ${if(on)"ON" else "OFF"} • 1/$tc • SWING $swing")}
 fun timingCorrectEnabled()=tcEnabled
 fun quantizeCurrentTrack():Int{var changed=0;for(i in events.indices){val e=events[i];if(e.track==track){val q=quant(e.tick);if(q!=e.tick){events[i]=e.copy(tick=q);changed++}}};saveProject();showDisplayMessage("TC DO IT • TRACK ${track+1} • $changed событий");report("tc");return changed}
 private fun show16Levels(){
  if(!levels16){sourceBank=bank;sourcePad=pad}
  val types=arrayOf("VELOCITY","TUNE");var selected=if(levels16Type=="TUNE")1 else 0
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(8),dp(18),dp(4))}
  val active=CheckBox(this).apply{text="ACTIVE";isChecked=levels16};val source=TextView(this).apply{text="PAD   ${'A'+sourceBank}${(sourcePad+1).toString().padStart(2,'0')}";textSize=16f}
  val choose=RadioGroup(this).apply{orientation=RadioGroup.HORIZONTAL};types.forEachIndexed{i,name->choose.addView(RadioButton(this).apply{text=name;id=View.generateViewId();isChecked=i==selected;setOnClickListener{selected=i}})}
  val original=SeekBar(this).apply{max=15;progress=levels16OriginalPad};val originalLabel=TextView(this).apply{text="ORIGINAL PAD   ${levels16OriginalPad+1}"}
  original.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,p:Int,f:Boolean){originalLabel.text="ORIGINAL PAD   ${p+1}"};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}})
  box.addView(active);box.addView(source);box.addView(choose);box.addView(originalLabel);box.addView(original);box.addView(TextView(this).apply{text="VELOCITY: выбранный PAD на 16 уровнях громкости.\nTUNE: тот же PAD на 16 высотах; ORIGINAL PAD задаёт исходную высоту."})
  AlertDialog.Builder(this).setTitle("16 LEVELS").setView(box).setNegativeButton("CANCEL",null).setPositiveButton("DONE"){_,_->levels16=active.isChecked;levels16Type=types[selected];levels16OriginalPad=original.progress;report("16_levels");showDisplayMessage("16 LEVELS ${if(levels16)"ON" else "OFF"} • $levels16Type");refreshPads();refreshStatus()}.show()
 }
 private fun showTimingCorrect(){
  val divisions=intArrayOf(4,8,16,32,64);var selected=divisions.indexOf(tc).let{if(it<0)2 else it};val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(8),dp(18),dp(4))}
  val enabled=CheckBox(this).apply{text="GLOBAL TIMING CORRECT";isChecked=tcEnabled};val divLabel=TextView(this).apply{text="TIME DIVISION   1/${divisions[selected]}"};val div=SeekBar(this).apply{max=divisions.lastIndex;progress=selected};val swingLabel=TextView(this).apply{text="SWING   $swing%"};val sw=SeekBar(this).apply{max=25;progress=(swing-50).coerceIn(0,25)}
  div.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,p:Int,f:Boolean){selected=p.coerceIn(0,divisions.lastIndex);divLabel.text="TIME DIVISION   1/${divisions[selected]}"};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}});sw.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,p:Int,f:Boolean){swingLabel.text="SWING   ${50+p}%"};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}})
  box.addView(enabled);box.addView(divLabel);box.addView(div);box.addView(swingLabel);box.addView(sw);box.addView(TextView(this).apply{text="TC выравнивает сыгранные события по выбранной сетке. DO IT применяет настройки к уже записанному текущему TRACK."})
  AlertDialog.Builder(this).setTitle("TIMING CORRECT").setView(box).setNegativeButton("CANCEL",null).setNeutralButton("DO IT"){_,_->tc=divisions[selected];swing=50+sw.progress;tcEnabled=enabled.isChecked;saveProject();quantizeCurrentTrack();refreshStatus()}.setPositiveButton("DONE"){_,_->tc=divisions[selected];swing=50+sw.progress;tcEnabled=enabled.isChecked;saveProject();report("tc");showDisplayMessage("TC ${if(tcEnabled)"ON" else "OFF"} • 1/$tc • SWING $swing");refreshStatus()}.show()
 }'''
if anchor not in s: raise SystemExit('public UI anchor missing')
s=s.replace(anchor,block,1)
p.write_text(s)

a=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt');t=a.read_text()
old='''  val right=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL}
  right.addView(utility("VOL  −"){val v=a.adjustSelectedPadVolume(-5);a.showPadVolumeOnDisplay(v)},LinearLayout.LayoutParams(-1,a.dp(35)).apply{bottomMargin=a.dp(3)})
  right.addView(utility("VOL  +"){val v=a.adjustSelectedPadVolume(5);a.showPadVolumeOnDisplay(v)},LinearLayout.LayoutParams(-1,a.dp(35)))'''
new='''  val right=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL}
  fun pairRow(leftButton:Button,rightButton:Button)=LinearLayout(a).apply{orientation=LinearLayout.HORIZONTAL;addView(leftButton,LinearLayout.LayoutParams(0,a.dp(35),1f).apply{rightMargin=a.dp(3)});addView(rightButton,LinearLayout.LayoutParams(0,a.dp(35),1f))}
  right.addView(pairRow(utility("VOL −"){val v=a.adjustSelectedPadVolume(-5);a.showPadVolumeOnDisplay(v)},utility("PITCH −"){a.adjustSelectedPadPitch(-1)}),LinearLayout.LayoutParams(-1,a.dp(35)).apply{bottomMargin=a.dp(3)})
  right.addView(pairRow(utility("VOL +"){val v=a.adjustSelectedPadVolume(5);a.showPadVolumeOnDisplay(v)},utility("PITCH +"){a.adjustSelectedPadPitch(1)}),LinearLayout.LayoutParams(-1,a.dp(35)))'''
if old not in t: raise SystemExit('right utility cluster anchor missing')
t=t.replace(old,new,1)
t=t.replace('val items=arrayOf("MIXER / MASTER VOLUME","CONTROL GUIDE","RESTART COURSE","NEW PROJECT")','val items=arrayOf("PITCH RESET","TC ON / OFF","TC • DO IT (CURRENT TRACK)","MIXER / MASTER VOLUME","CONTROL GUIDE","RESTART COURSE","NEW PROJECT")',1)
t=t.replace('when(w){0->{a.setWorkspaceMode("MIXER");a.showVolumeMixer()};1->guide?.performClick();2->restart?.performClick();3->confirmNewProject(a)}','when(w){0->a.resetSelectedPadPitch();1->a.setTimingCorrectEnabled(!a.timingCorrectEnabled());2->a.quantizeCurrentTrack();3->{a.setWorkspaceMode("MIXER");a.showVolumeMixer()};4->guide?.performClick();5->restart?.performClick();6->confirmNewProject(a)}',1)
a.write_text(t)
print('Pitch, TC, Note Repeat, Step Sequencer and 16 Levels integrated after phone UI transform')