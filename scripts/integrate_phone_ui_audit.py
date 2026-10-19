from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt')
s=p.read_text()
s=s.replace('content.tag!="phone_ui_v18"','content.tag!="phone_ui_v23"').replace('content.tag="phone_ui_v18"','content.tag="phone_ui_v23"')
old='findText(root){it.text?.toString()?.startsWith("SEQ")==true}?.apply{textSize=10.5f;typeface=Typeface.MONOSPACE;gravity=Gravity.TOP or Gravity.START;minHeight=0;setPadding(a.dp(12),a.dp(9),a.dp(12),a.dp(8));layoutParams=layoutParams.apply{height=a.dp(190)};background=rounded(Color.rgb(17,28,25),Color.rgb(6,10,9),a.dp(2),a.dp(4));setTextColor(Color.rgb(190,235,179))}'
new='val displayText=findText(root){it.text?.toString()?.startsWith("SEQ")==true};val displayH=a.dp(190);displayText?.apply{textSize=10.5f;typeface=Typeface.MONOSPACE;gravity=Gravity.TOP or Gravity.START;minHeight=0;setPadding(a.dp(12),a.dp(9),a.dp(12),a.dp(8));layoutParams=layoutParams.apply{height=-1};background=rounded(Color.rgb(17,28,25),Color.rgb(6,10,9),a.dp(2),a.dp(4));setTextColor(Color.rgb(190,235,179))};(displayText?.parent as? FrameLayout)?.layoutParams=(displayText?.parent as? FrameLayout)?.layoutParams?.apply{height=displayH}'
if old not in s: raise SystemExit('display sizing anchor missing')
s=s.replace(old,new,1)
old='val gridSize=(a.resources.displayMetrics.widthPixels-a.dp(16)).coerceAtMost(a.dp(410))'
new='val gridSize=(a.resources.displayMetrics.widthPixels-a.dp(16)).coerceAtMost(a.dp(460))'
if old not in s: raise SystemExit('grid sizing anchor missing')
s=s.replace(old,new,1)
# Preserve display and pad grid. Reclaim the missing height only from surrounding controls and gaps.
s=s.replace('root.setPadding(a.dp(8),a.dp(3),a.dp(8),a.dp(4))','root.setPadding(a.dp(8),0,a.dp(8),0)',1)
s=s.replace('layoutParams=layoutParams.apply{height=a.dp(26)}','layoutParams=layoutParams.apply{height=a.dp(22)}',1)
s=s.replace('row.layoutParams=row.layoutParams.apply{height=if(transport)a.dp(44) else if(bankRow)a.dp(38) else if(topModes)a.dp(38) else a.dp(36)}','row.layoutParams=row.layoutParams.apply{height=if(transport)a.dp(34) else if(bankRow)a.dp(30) else if(topModes)a.dp(30) else a.dp(27)}',1)
s=s.replace('lp.topMargin=a.dp(5);lp.bottomMargin=a.dp(5)','lp.topMargin=a.dp(1);lp.bottomMargin=a.dp(1)',1)
start=s.index(' private fun addUtilityBar(');end=s.index('\n private fun syncKnob',start)
newbar=''' private fun addUtilityBar(a:TrainerActivity,root:LinearLayout,restart:Button?,guide:Button?){val panel=LinearLayout(a).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(0,0,0,0)};val left=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER};val menu=Button(a).apply{text="MENU";textSize=8.4f;typeface=Typeface.DEFAULT_BOLD;isAllCaps=false;minHeight=0;minimumHeight=0;setPadding(0,0,0,0);background=darkUtilitySelector(a);setTextColor(Color.rgb(42,41,38));setOnClickListener{showMenu(a,restart,guide)}};val tempo=Button(a).apply{text="TEMPO";textSize=8.4f;typeface=Typeface.DEFAULT_BOLD;isAllCaps=false;minHeight=0;minimumHeight=0;setPadding(0,0,0,0);background=darkUtilitySelector(a);setTextColor(Color.rgb(42,41,38));setOnClickListener{showBpm(a)}};left.addView(menu,LinearLayout.LayoutParams(-1,0,1f).apply{bottomMargin=a.dp(2)});left.addView(tempo,LinearLayout.LayoutParams(-1,0,1f));padKnob=PadVolumeKnob(a).apply{label="PAD VOL";onValueChanged={a.setSelectedPadVolumePercent(it)}};panel.addView(left,LinearLayout.LayoutParams(0,a.dp(66),0.82f).apply{rightMargin=a.dp(6)});panel.addView(padKnob,LinearLayout.LayoutParams(0,a.dp(66),1.18f));root.addView(panel,LinearLayout.LayoutParams(-1,a.dp(66)))}'''
s=s[:start]+newbar+s[end:]
# REC must stay red even when the indicator refresh loop paints the transport row.
oldset='setTextColor(if(on)Color.rgb(18,18,17)else Color.rgb(42,41,38))'
newset='setTextColor(if(on)Color.rgb(18,18,17)else if(l=="REC")Color.rgb(190,35,32)else Color.rgb(42,41,38))'
if oldset not in s: raise SystemExit('LED text color anchor missing')
s=s.replace(oldset,newset,1)
start=s.index(' private fun showBpm(a:TrainerActivity){');end=s.index('\n private fun confirmNewProject',start)
newfun=''' private fun showBpm(a:TrainerActivity){val cur=a.currentBpm();val title=TextView(a).apply{text="TEMPO   $cur BPM";textSize=22f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setPadding(a.dp(12),a.dp(14),a.dp(12),a.dp(8));setTextColor(Color.rgb(42,41,38))};val seek=SeekBar(a).apply{max=200;progress=cur-40;setPadding(a.dp(18),0,a.dp(18),0)};val box=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;addView(title,LinearLayout.LayoutParams(-1,a.dp(58)));addView(seek,LinearLayout.LayoutParams(-1,a.dp(54)))};seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,progress:Int,from:Boolean){val x=progress+40;title.text="TEMPO   $x BPM";if(from)a.setTempoBpm(x)};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}});AlertDialog.Builder(a).setView(box).setNegativeButton("ОТМЕНА"){_,_->a.setTempoBpm(cur)}.setPositiveButton("ГОТОВО"){_,_->a.setTempoBpm(seek.progress+40)}.setOnCancelListener{a.setTempoBpm(cur)}.show()}'''
s=s[:start]+newfun+s[end:]
p.write_text(s)
print('Preserved display/pads; compacted only surrounding controls; REC stays red')