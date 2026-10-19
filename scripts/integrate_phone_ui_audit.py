from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt')
s=p.read_text()
s=s.replace('content.tag!="phone_ui_v18"','content.tag!="phone_ui_v26"').replace('content.tag="phone_ui_v18"','content.tag="phone_ui_v26"')
old='findText(root){it.text?.toString()?.startsWith("SEQ")==true}?.apply{textSize=10.5f;typeface=Typeface.MONOSPACE;gravity=Gravity.TOP or Gravity.START;minHeight=0;setPadding(a.dp(12),a.dp(9),a.dp(12),a.dp(8));layoutParams=layoutParams.apply{height=a.dp(190)};background=rounded(Color.rgb(17,28,25),Color.rgb(6,10,9),a.dp(2),a.dp(4));setTextColor(Color.rgb(190,235,179))}'
new='val displayText=findText(root){it.text?.toString()?.startsWith("SEQ")==true};val displayH=a.dp(190);displayText?.apply{textSize=10.5f;typeface=Typeface.MONOSPACE;gravity=Gravity.TOP or Gravity.START;minHeight=0;setPadding(a.dp(12),a.dp(9),a.dp(12),a.dp(8));layoutParams=layoutParams.apply{height=-1};background=rounded(Color.rgb(17,28,25),Color.rgb(6,10,9),a.dp(2),a.dp(4));setTextColor(Color.rgb(190,235,179))};(displayText?.parent as? FrameLayout)?.layoutParams=(displayText?.parent as? FrameLayout)?.layoutParams?.apply{height=displayH}'
if old not in s: raise SystemExit('display sizing anchor missing')
s=s.replace(old,new,1)
old='val gridSize=(a.resources.displayMetrics.widthPixels-a.dp(16)).coerceAtMost(a.dp(410))'
new='val gridSize=(a.resources.displayMetrics.widthPixels-a.dp(16)).coerceAtMost(a.dp(460))'
if old not in s: raise SystemExit('grid sizing anchor missing')
s=s.replace(old,new,1)
s=s.replace('root.setPadding(a.dp(8),a.dp(3),a.dp(8),a.dp(4))','root.setPadding(a.dp(8),0,a.dp(8),0)',1)
s=s.replace('layoutParams=layoutParams.apply{height=a.dp(26)}','layoutParams=layoutParams.apply{height=a.dp(22)}',1)
s=s.replace('row.layoutParams=row.layoutParams.apply{height=if(transport)a.dp(44) else if(bankRow)a.dp(38) else if(topModes)a.dp(38) else a.dp(36)}','row.layoutParams=row.layoutParams.apply{height=if(transport)a.dp(36) else if(bankRow)a.dp(32) else if(topModes)a.dp(32) else a.dp(30)}',1)
s=s.replace('lp.topMargin=a.dp(5);lp.bottomMargin=a.dp(5)','lp.topMargin=a.dp(2);lp.bottomMargin=a.dp(2)',1)
start=s.index(' private fun addUtilityBar(');end=s.index('\n private fun syncKnob',start)
newbar=''' private fun addUtilityBar(a:TrainerActivity,root:LinearLayout,restart:Button?,guide:Button?){
  val panel=LinearLayout(a).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  fun utility(label:String,action:()->Unit)=Button(a).apply{text=label;textSize=9f;typeface=Typeface.DEFAULT_BOLD;isAllCaps=false;minHeight=0;minimumHeight=0;setPadding(0,0,0,0);background=darkUtilitySelector(a);setTextColor(Color.rgb(42,41,38));setOnClickListener{action()}}
  val left=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL}
  left.addView(utility("MENU"){showMenu(a,restart,guide)},LinearLayout.LayoutParams(-1,a.dp(35)).apply{bottomMargin=a.dp(3)})
  left.addView(utility("TEMPO"){showBpm(a)},LinearLayout.LayoutParams(-1,a.dp(35)))
  val right=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL}
  right.addView(utility("VOL  −"){val v=a.adjustSelectedPadVolume(-5);a.showPadVolumeOnDisplay(v)},LinearLayout.LayoutParams(-1,a.dp(35)).apply{bottomMargin=a.dp(3)})
  right.addView(utility("VOL  +"){val v=a.adjustSelectedPadVolume(5);a.showPadVolumeOnDisplay(v)},LinearLayout.LayoutParams(-1,a.dp(35)))
  panel.addView(left,LinearLayout.LayoutParams(0,a.dp(73),1f).apply{rightMargin=a.dp(4)})
  panel.addView(right,LinearLayout.LayoutParams(0,a.dp(73),1f))
  root.addView(panel,LinearLayout.LayoutParams(-1,a.dp(73)).apply{topMargin=a.dp(5)})
 }'''
s=s[:start]+newbar+s[end:]
oldset='setTextColor(if(on)Color.rgb(18,18,17)else Color.rgb(42,41,38))'
newset='setTextColor(if(on)Color.rgb(18,18,17)else if(l=="REC")Color.rgb(190,35,32)else Color.rgb(42,41,38))'
if oldset not in s: raise SystemExit('LED text color anchor missing')
s=s.replace(oldset,newset,1)
start=s.index(' private fun showBpm(a:TrainerActivity){');end=s.index('\n private fun confirmNewProject',start)
newfun=''' private fun showBpm(a:TrainerActivity){val cur=a.currentBpm();val title=TextView(a).apply{text="TEMPO   $cur BPM";textSize=22f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setPadding(a.dp(12),a.dp(14),a.dp(12),a.dp(8));setTextColor(Color.rgb(42,41,38))};val seek=SeekBar(a).apply{max=200;progress=cur-40;setPadding(a.dp(18),0,a.dp(18),0)};val box=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;addView(title,LinearLayout.LayoutParams(-1,a.dp(58)));addView(seek,LinearLayout.LayoutParams(-1,a.dp(54)))};seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,progress:Int,from:Boolean){val x=progress+40;title.text="TEMPO   $x BPM";if(from)a.setTempoBpm(x)};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){}});AlertDialog.Builder(a).setView(box).setNegativeButton("ОТМЕНА"){_,_->a.setTempoBpm(cur)}.setPositiveButton("ГОТОВО"){_,_->a.setTempoBpm(seek.progress+40)}.setOnCancelListener{a.setTempoBpm(cur)}.show()}'''
s=s[:start]+newfun+s[end:]
p.write_text(s)

q=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
t=q.read_text()
insert='''
 fun showPadVolumeOnDisplay(value:Int){showDisplayMessage("PAD VOL  ${value.coerceIn(0,100)}%")}
'''
marker='\n override fun onCreate('
if marker not in t: raise SystemExit('onCreate anchor missing')
t=t.replace(marker,insert+marker,1)
q.write_text(t)
print('Dedicated stacked VOL -/+ buttons replace swipe; stable PAD VOL display; row spacing restored')