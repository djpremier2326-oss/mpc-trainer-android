from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt')
s=p.read_text()
s=s.replace('content.tag!="phone_ui_v18"','content.tag!="phone_ui_v19"').replace('content.tag="phone_ui_v18"','content.tag="phone_ui_v19"')
# Size the actual display container, not only its TextView child. Keep the whole surface above Android gesture navigation.
old='findText(root){it.text?.toString()?.startsWith("SEQ")==true}?.apply{textSize=10.5f;typeface=Typeface.MONOSPACE;gravity=Gravity.TOP or Gravity.START;minHeight=0;setPadding(a.dp(12),a.dp(9),a.dp(12),a.dp(8));layoutParams=layoutParams.apply{height=a.dp(190)};background=rounded(Color.rgb(17,28,25),Color.rgb(6,10,9),a.dp(2),a.dp(4));setTextColor(Color.rgb(190,235,179))}'
new='val displayText=findText(root){it.text?.toString()?.startsWith("SEQ")==true};val displayH=(a.resources.displayMetrics.heightPixels*0.18f).toInt().coerceIn(a.dp(130),a.dp(170));displayText?.apply{textSize=10.5f;typeface=Typeface.MONOSPACE;gravity=Gravity.TOP or Gravity.START;minHeight=0;setPadding(a.dp(12),a.dp(9),a.dp(12),a.dp(8));layoutParams=layoutParams.apply{height=-1};background=rounded(Color.rgb(17,28,25),Color.rgb(6,10,9),a.dp(2),a.dp(4));setTextColor(Color.rgb(190,235,179))};(displayText?.parent as? FrameLayout)?.layoutParams=(displayText?.parent as? FrameLayout)?.layoutParams?.apply{height=displayH}'
if old not in s: raise SystemExit('display sizing anchor missing')
s=s.replace(old,new,1)
old='val gridSize=(a.resources.displayMetrics.widthPixels-a.dp(16)).coerceAtMost(a.dp(410))'
new='val fixed=a.dp(26+38+38+36+36+36+44+82+28)+displayH;val byHeight=(a.resources.displayMetrics.heightPixels-fixed).coerceAtLeast(a.dp(280));val gridSize=minOf(a.resources.displayMetrics.widthPixels-a.dp(16),a.dp(410),byHeight)'
if old not in s: raise SystemExit('grid sizing anchor missing')
s=s.replace(old,new,1)
# Bottom utility row gets a protected bottom inset so its knob is not sitting in the system gesture strip.
s=s.replace('setPadding(0,a.dp(3),0,a.dp(2))','setPadding(0,a.dp(3),0,a.dp(18))',1)
s=s.replace('bar.addView(tempo,LinearLayout.LayoutParams(0,a.dp(76),0.8f));bar.addView(padKnob,LinearLayout.LayoutParams(0,a.dp(76),1.4f));bar.addView(menu,LinearLayout.LayoutParams(0,a.dp(76),0.8f))','bar.addView(tempo,LinearLayout.LayoutParams(0,a.dp(82),0.8f));bar.addView(padKnob,LinearLayout.LayoutParams(0,a.dp(82),1.4f));bar.addView(menu,LinearLayout.LayoutParams(0,a.dp(82),0.8f))',1)
# Tempo updates in place. No Activity recreate = no SoundPool teardown/reload or screen flash.
start=s.index(' private fun showBpm(a:TrainerActivity){')
end=s.index('\n private fun confirmNewProject',start)
newfun=''' private fun showBpm(a:TrainerActivity){val cur=a.currentBpm();val title=TextView(a).apply{text="TEMPO   $cur BPM";textSize=22f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setPadding(a.dp(12),a.dp(14),a.dp(12),a.dp(8));setTextColor(Color.rgb(42,41,38))};val seek=SeekBar(a).apply{max=200;progress=cur-40;setPadding(a.dp(18),0,a.dp(18),0)};val box=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;addView(title,LinearLayout.LayoutParams(-1,a.dp(58)));addView(seek,LinearLayout.LayoutParams(-1,a.dp(54)))};seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,progress:Int,from:Boolean){val x=progress+40;title.text="TEMPO   $x BPM";if(from)a.setTempoBpm(x)};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}});AlertDialog.Builder(a).setView(box).setNegativeButton("ОТМЕНА"){_,_->a.setTempoBpm(cur)}.setPositiveButton("ГОТОВО"){_,_->a.setTempoBpm(seek.progress+40)}.setOnCancelListener{a.setTempoBpm(cur)}.show()}'''
s=s[:start]+newfun+s[end:]
p.write_text(s)
print('Adaptive display/grid, gesture-safe utility row, and no-recreate live tempo integrated')