from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt')
s=p.read_text()
s=s.replace('content.tag!="phone_ui_v18"','content.tag!="phone_ui_v20"').replace('content.tag="phone_ui_v18"','content.tag="phone_ui_v20"')
# Size the actual display container, not only its TextView child.
old='findText(root){it.text?.toString()?.startsWith("SEQ")==true}?.apply{textSize=10.5f;typeface=Typeface.MONOSPACE;gravity=Gravity.TOP or Gravity.START;minHeight=0;setPadding(a.dp(12),a.dp(9),a.dp(12),a.dp(8));layoutParams=layoutParams.apply{height=a.dp(190)};background=rounded(Color.rgb(17,28,25),Color.rgb(6,10,9),a.dp(2),a.dp(4));setTextColor(Color.rgb(190,235,179))}'
new='val displayText=findText(root){it.text?.toString()?.startsWith("SEQ")==true};val displayH=(a.resources.displayMetrics.heightPixels*0.16f).toInt().coerceIn(a.dp(118),a.dp(155));displayText?.apply{textSize=10.5f;typeface=Typeface.MONOSPACE;gravity=Gravity.TOP or Gravity.START;minHeight=0;setPadding(a.dp(12),a.dp(9),a.dp(12),a.dp(8));layoutParams=layoutParams.apply{height=-1};background=rounded(Color.rgb(17,28,25),Color.rgb(6,10,9),a.dp(2),a.dp(4));setTextColor(Color.rgb(190,235,179))};(displayText?.parent as? FrameLayout)?.layoutParams=(displayText?.parent as? FrameLayout)?.layoutParams?.apply{height=displayH}'
if old not in s: raise SystemExit('display sizing anchor missing')
s=s.replace(old,new,1)
# Pads are sacred: always size from screen width. If vertical space is tight, compact the surrounding controls, never the pads.
old='val gridSize=(a.resources.displayMetrics.widthPixels-a.dp(16)).coerceAtMost(a.dp(410))'
new='val gridSize=(a.resources.displayMetrics.widthPixels-a.dp(16)).coerceAtMost(a.dp(460))'
if old not in s: raise SystemExit('grid sizing anchor missing')
s=s.replace(old,new,1)
# Replace bottom row with a hardware-like panel: TEMPO and MENU stacked left, one large volume knob right.
start=s.index(' private fun addUtilityBar(')
end=s.index('\n private fun syncKnob',start)
newbar=''' private fun addUtilityBar(a:TrainerActivity,root:LinearLayout,restart:Button?,guide:Button?){val panel=LinearLayout(a).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(0,a.dp(4),0,a.dp(12))};val left=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER};val tempo=Button(a).apply{text="TEMPO";textSize=8.2f;typeface=Typeface.DEFAULT_BOLD;isAllCaps=false;background=darkUtilitySelector(a);setTextColor(Color.rgb(42,41,38));setOnClickListener{showBpm(a)}};val menu=Button(a).apply{text="MENU";textSize=8.2f;typeface=Typeface.DEFAULT_BOLD;isAllCaps=false;background=darkUtilitySelector(a);setTextColor(Color.rgb(42,41,38));setOnClickListener{showMenu(a,restart,guide)}};left.addView(tempo,LinearLayout.LayoutParams(-1,0,1f).apply{bottomMargin=a.dp(3)});left.addView(menu,LinearLayout.LayoutParams(-1,0,1f));padKnob=PadVolumeKnob(a).apply{label="PAD VOL";onValueChanged={a.setSelectedPadVolumePercent(it)}};panel.addView(left,LinearLayout.LayoutParams(0,a.dp(112),0.9f).apply{rightMargin=a.dp(10)});panel.addView(padKnob,LinearLayout.LayoutParams(0,a.dp(112),1.55f));root.addView(panel)}'''
s=s[:start]+newbar+s[end:]
# Tempo updates in place. No Activity recreate = no SoundPool teardown/reload or screen flash.
start=s.index(' private fun showBpm(a:TrainerActivity){')
end=s.index('\n private fun confirmNewProject',start)
newfun=''' private fun showBpm(a:TrainerActivity){val cur=a.currentBpm();val title=TextView(a).apply{text="TEMPO   $cur BPM";textSize=22f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setPadding(a.dp(12),a.dp(14),a.dp(12),a.dp(8));setTextColor(Color.rgb(42,41,38))};val seek=SeekBar(a).apply{max=200;progress=cur-40;setPadding(a.dp(18),0,a.dp(18),0)};val box=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;addView(title,LinearLayout.LayoutParams(-1,a.dp(58)));addView(seek,LinearLayout.LayoutParams(-1,a.dp(54)))};seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,progress:Int,from:Boolean){val x=progress+40;title.text="TEMPO   $x BPM";if(from)a.setTempoBpm(x)};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}});AlertDialog.Builder(a).setView(box).setNegativeButton("ОТМЕНА"){_,_->a.setTempoBpm(cur)}.setPositiveButton("ГОТОВО"){_,_->a.setTempoBpm(seek.progress+40)}.setOnCancelListener{a.setTempoBpm(cur)}.show()}'''
s=s[:start]+newfun+s[end:]
p.write_text(s)
print('Full-width pads and left-stacked TEMPO/MENU with large right volume knob integrated')