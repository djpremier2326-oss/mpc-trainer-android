from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
state='private var tcEnabled=true'
if state not in s: raise SystemExit('tc enabled state missing')
s=s.replace(state,state+';private var tcTriplet=false;private var tcShift=0',1)
quant_candidates=['private fun quant(t:Long):Long{val g=384L/tc;var q=((t+g/2)/g)*g;if(swing>50&&((q/g)%2L)==1L)q+=((swing-50)/25.0*g/2.0).roundToLong();return q.coerceIn(0,767)}','private fun quant(raw:Long):Long{val s=(384L/tc).coerceAtLeast(1);var q=(raw.toDouble()/s).roundToLong()*s;if((q/s)%2L==1L&&swing>50)q+=((s*(swing-50))/50.0).roundToLong();return q%loopTicks()}']
old=next((x for x in quant_candidates if x in s),None)
if old is None: raise SystemExit('quant anchor missing')
new='private fun tcGridTicks():Long{val straight=(384L/tc.coerceAtLeast(1)).coerceAtLeast(1L);return if(tcTriplet)((straight*2L)/3L).coerceAtLeast(1L) else straight}\n private fun quant(t:Long):Long{val g=tcGridTicks();var q=((t+g/2)/g)*g;if(!tcTriplet&&swing>50&&((q/g)%2L)==1L)q+=((swing-50)/25.0*g/2.0).roundToLong();q+=tcShift;return ((q%loopTicks())+loopTicks())%loopTicks()}'
s=s.replace(old,new,1)
save='.put("bank",bank).put("tcOn",tcEnabled).put("metro",metronome)'
if save not in s: raise SystemExit('tc persistence save anchor missing')
s=s.replace(save,'.put("bank",bank).put("tcOn",tcEnabled).put("tcTriplet",tcTriplet).put("tcShift",tcShift).put("metro",metronome)',1)
restore='bank=o.optInt("bank",0).coerceIn(0,3);tcEnabled=o.optBoolean("tcOn",true);metronome='
if restore not in s: raise SystemExit('tc persistence restore anchor missing')
s=s.replace(restore,'bank=o.optInt("bank",0).coerceIn(0,3);tcEnabled=o.optBoolean("tcOn",true);tcTriplet=o.optBoolean("tcTriplet",false);tcShift=o.optInt("tcShift",0).coerceIn(-96,96);metronome=',1)
old='private fun noteRepeatIntervalMs()=(60000.0/bpm*(4.0/tc.coerceAtLeast(1))).toLong().coerceAtLeast(25)'
new='private fun noteRepeatIntervalMs()=(60000.0/bpm*(tcGridTicks()/96.0)).toLong().coerceAtLeast(25)'
if old not in s: raise SystemExit('note repeat interval anchor missing')
s=s.replace(old,new,1)
old='val enabled=CheckBox(this).apply{text="GLOBAL TIMING CORRECT";isChecked=tcEnabled};val divLabel=TextView(this).apply{text="TIME DIVISION   1/${divisions[selected]}"};val div=SeekBar(this).apply{max=divisions.lastIndex;progress=selected};val swingLabel=TextView(this).apply{text="SWING   $swing%"};val sw=SeekBar(this).apply{max=25;progress=(swing-50).coerceIn(0,25)}'
new='val enabled=CheckBox(this).apply{text="GLOBAL TIMING CORRECT";isChecked=tcEnabled};val triplet=CheckBox(this).apply{text="TRIPLET";isChecked=tcTriplet};val divLabel=TextView(this).apply{text="TIME DIVISION   1/${divisions[selected]}${if(tcTriplet)"T" else ""}"};val div=SeekBar(this).apply{max=divisions.lastIndex;progress=selected};val swingLabel=TextView(this).apply{text="SWING   $swing%"};val sw=SeekBar(this).apply{max=25;progress=(swing-50).coerceIn(0,25)};val shiftLabel=TextView(this).apply{text="SHIFT TIMING   $tcShift ticks"};val shift=SeekBar(this).apply{max=192;progress=tcShift+96}'
if old not in s: raise SystemExit('tc dialog controls anchor missing')
s=s.replace(old,new,1)
old='div.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,p:Int,f:Boolean){selected=p.coerceIn(0,divisions.lastIndex);divLabel.text="TIME DIVISION   1/${divisions[selected]}"};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}});sw.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,p:Int,f:Boolean){swingLabel.text="SWING   ${50+p}%"};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}})'
new='fun refreshDiv(){divLabel.text="TIME DIVISION   1/${divisions[selected]}${if(triplet.isChecked)"T" else ""}"};triplet.setOnCheckedChangeListener{_,_->refreshDiv()};div.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,p:Int,f:Boolean){selected=p.coerceIn(0,divisions.lastIndex);refreshDiv()};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}});sw.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,p:Int,f:Boolean){swingLabel.text="SWING   ${50+p}%"};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}});shift.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,p:Int,f:Boolean){shiftLabel.text="SHIFT TIMING   ${p-96} ticks"};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}})'
if old not in s: raise SystemExit('tc dialog listeners anchor missing')
s=s.replace(old,new,1)
old='box.addView(enabled);box.addView(divLabel);box.addView(div);box.addView(swingLabel);box.addView(sw);box.addView(TextView(this).apply{text="TC выравнивает сыгранные события по выбранной сетке. DO IT применяет настройки к уже записанному текущему TRACK."})'
new='box.addView(enabled);box.addView(divLabel);box.addView(div);box.addView(triplet);box.addView(swingLabel);box.addView(sw);box.addView(shiftLabel);box.addView(shift);box.addView(TextView(this).apply{text="TC выравнивает события по сетке. TRIPLET включает триольную сетку. SHIFT TIMING сдвигает результат вперёд или назад в ticks. DO IT применяет настройки к уже записанному текущему TRACK."})'
if old not in s: raise SystemExit('tc dialog view anchor missing')
s=s.replace(old,new,1)
old='tc=divisions[selected];swing=50+sw.progress;tcEnabled=enabled.isChecked;saveProject();quantizeCurrentTrack();refreshStatus()'
new='tc=divisions[selected];swing=50+sw.progress;tcEnabled=enabled.isChecked;tcTriplet=triplet.isChecked;tcShift=shift.progress-96;saveProject();quantizeCurrentTrack();refreshStatus()'
if old not in s: raise SystemExit('tc do it anchor missing')
s=s.replace(old,new,1)
old='tc=divisions[selected];swing=50+sw.progress;tcEnabled=enabled.isChecked;saveProject();report("tc");showDisplayMessage("TC ${if(tcEnabled)"ON" else "OFF"} • 1/$tc • SWING $swing");refreshStatus()'
new='tc=divisions[selected];swing=50+sw.progress;tcEnabled=enabled.isChecked;tcTriplet=triplet.isChecked;tcShift=shift.progress-96;saveProject();report("tc");showDisplayMessage("TC ${if(tcEnabled)"ON" else "OFF"} • 1/$tc${if(tcTriplet)"T" else ""} • SWING $swing • SHIFT $tcShift");refreshStatus()'
if old not in s: raise SystemExit('tc done anchor missing')
s=s.replace(old,new,1)
p.write_text(s)
print('Timing Correct triplets, Shift Timing, persistence, and Note Repeat grid integrated')