from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
old='private var playing=false;private var recording=false;private var overdub=false;private var noteRepeat=false;private var fullLevel=false;private var levels16=false'
new='private var playing=false;private var recording=false;private var overdub=false;private var noteRepeat=false;private var fullLevel=false;private var levels16=false;private var levels16Type="VELOCITY";private var levels16OriginalPad=7'
if old not in s: raise SystemExit('16 levels state anchor missing')
s=s.replace(old,new,1)
old='btn("16 LEVELS"){if(!levels16){sourceBank=bank;sourcePad=pad};levels16=!levels16;report("16_levels");refreshPads();refreshStatus()}'
new='btn("16 LEVELS"){show16Levels()}'
if old not in s: raise SystemExit('16 levels button anchor missing')
s=s.replace(old,new,1)
# Preserve velocity ladder and add Tune ladder centered on Original Pad. 16 Levels temporarily maps source pad to all 16 pads.
old='private fun velocity(i:Int)=if(levels16)(((i+1)*127f/16f).roundToLong().toInt()).coerceIn(1,127) else if(fullLevel)127 else 100'
new='private fun velocity(i:Int)=if(levels16&&levels16Type=="VELOCITY")(((i+1)*127f/16f).roundToLong().toInt()).coerceIn(1,127) else if(fullLevel)127 else 100\n private fun levels16TuneSemitones(i:Int)=(i-levels16OriginalPad).coerceIn(-12,12)\n private fun levels16Rate(i:Int)=java.lang.Math.pow(2.0,levels16TuneSemitones(i)/12.0).toFloat().coerceIn(.5f,2f)'
if old not in s: raise SystemExit('velocity anchor missing')
s=s.replace(old,new,1)
# Pitch in Tune mode is a performance/event property in this compact trainer; normal pad pitch remains per-pad.
old='val g=padGain(v,b,p);pads[b][p]?.let{playLoadedSound(it.soundId,g,true,pitchRate(b,p))}'
new='val g=padGain(v,b,p);val rate=if(levels16&&levels16Type=="TUNE")levels16Rate(i) else pitchRate(b,p);pads[b][p]?.let{playLoadedSound(it.soundId,g,true,rate)}'
if old not in s: raise SystemExit('manual playback pitch anchor missing')
s=s.replace(old,new,1)
# Keep event model simple: Tune 16 Levels auditions pitch correctly; recording Tune mode is intentionally blocked rather than recording wrong notes.
old='if(user&&loaded&&recording&&playing){events+=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);saveProject()}'
new='if(user&&loaded&&recording&&playing){if(levels16&&levels16Type=="TUNE"){showDisplayMessage("16 LEVELS • TUNE • запись pitch-событий пока недоступна")}else{events+=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);saveProject()}}'
if old not in s: raise SystemExit('record anchor missing')
s=s.replace(old,new,1)
anchor=' private fun showTimingCorrect(){'
block=''' private fun show16Levels(){
  if(!levels16){sourceBank=bank;sourcePad=pad}
  val types=arrayOf("VELOCITY","TUNE")
  var selected=if(levels16Type=="TUNE")1 else 0
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(8),dp(18),dp(4))}
  val active=CheckBox(this).apply{text="ACTIVE";isChecked=levels16}
  val source=TextView(this).apply{text="PAD   ${'A'+sourceBank}${(sourcePad+1).toString().padStart(2,'0')}";textSize=16f;setPadding(0,dp(8),0,dp(6))}
  val typeLabel=TextView(this).apply{text="TYPE   ${types[selected]}";textSize=16f}
  val choose=RadioGroup(this).apply{orientation=RadioGroup.HORIZONTAL}
  types.forEachIndexed{i,name->choose.addView(RadioButton(this).apply{text=name;id=View.generateViewId();isChecked=i==selected;setOnClickListener{selected=i;typeLabel.text="TYPE   ${types[selected]}"}})}
  val original=SeekBar(this).apply{max=15;progress=levels16OriginalPad}
  val originalLabel=TextView(this).apply{text="ORIGINAL PAD   ${levels16OriginalPad+1}";setPadding(0,dp(8),0,0)}
  original.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,p:Int,f:Boolean){originalLabel.text="ORIGINAL PAD   ${p+1}"};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}})
  box.addView(active);box.addView(source);box.addView(typeLabel);box.addView(choose);box.addView(originalLabel);box.addView(original);box.addView(TextView(this).apply{text="VELOCITY: один выбранный PAD на всех 16 пэдах с возрастающей силой удара.\nTUNE: тот же PAD на 16 высотах; ORIGINAL PAD задаёт пэд с исходной высотой.";setPadding(0,dp(8),0,dp(4))})
  AlertDialog.Builder(this).setTitle("16 LEVELS").setView(box).setNegativeButton("CANCEL",null).setPositiveButton("DONE"){_,_->levels16=active.isChecked;levels16Type=types[selected];levels16OriginalPad=original.progress;report("16_levels");showDisplayMessage("16 LEVELS ${if(levels16)"ON" else "OFF"} • $levels16Type • PAD ${'A'+sourceBank}${sourcePad+1}");refreshPads();refreshStatus()}.show()
 }
'''
if anchor not in s: raise SystemExit('timing dialog anchor missing')
s=s.replace(anchor,block+anchor,1)
p.write_text(s)
print('16 Levels Velocity + Tune modes aligned to MPC concept; unsupported Tune recording fails safely')