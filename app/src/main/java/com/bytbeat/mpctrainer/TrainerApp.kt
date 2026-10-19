package com.bytbeat.mpctrainer

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject

/** MPC Live III inspired phone surface. Pads stay below the display for phone ergonomics. */
class TrainerApp:Application(),Application.ActivityLifecycleCallbacks{
 private val ui=Handler(Looper.getMainLooper());private var pulse:Runnable?=null;private var lastPadSerial=-1L;private var litPad=-1
 override fun onCreate(){super.onCreate();registerActivityLifecycleCallbacks(this)}
 override fun onActivityResumed(activity:Activity){if(activity !is TrainerActivity)return;val content=activity.findViewById<FrameLayout>(android.R.id.content)?:return;if(content.tag!="phone_ui_v9"){val root=content.getChildAt(0) as? LinearLayout?:return;rebuild(activity,root);content.tag="phone_ui_v9"};startIndicators(activity)}
 override fun onActivityPaused(a:Activity){stopIndicators()}
 private fun rebuild(a:TrainerActivity,root:LinearLayout){
  root.setPadding(a.dp(8),a.dp(4),a.dp(8),a.dp(4));root.setBackgroundColor(Color.rgb(218,214,202))
  findText(root){it.text?.toString()?.startsWith("MPC TRAINER")==true}?.apply{text="MPC LIVE III • TRAINER";textSize=12f;gravity=Gravity.CENTER_VERTICAL;setTextColor(Color.rgb(66,60,55));setPadding(a.dp(3),0,0,a.dp(2));layoutParams=layoutParams.apply{height=a.dp(28)}}
  findText(root){it.text?.toString()?.startsWith("SEQ")==true}?.apply{textSize=9.5f;minHeight=0;setPadding(a.dp(8),a.dp(3),a.dp(8),a.dp(3));layoutParams=layoutParams.apply{height=a.dp(42)};background=rounded(Color.rgb(25,34,31),Color.rgb(12,17,15),a.dp(1),a.dp(3));setTextColor(Color.rgb(204,235,194))}
  val rows=(0 until root.childCount).mapNotNull{root.getChildAt(it) as? LinearLayout}.filter{it.orientation==LinearLayout.HORIZONTAL}
  rows.forEach{row->val labels=(0 until row.childCount).mapNotNull{(row.getChildAt(it) as? Button)?.text?.toString()};val transport=labels.any{it in setOf("REC","OVERDUB","STOP","PLAY")};row.layoutParams=row.layoutParams.apply{height=if(transport)a.dp(48) else a.dp(44)};for(i in 0 until row.childCount)(row.getChildAt(i) as? Button)?.apply{minHeight=0;minimumHeight=0;textSize=if(transport)9f else 8.3f;isAllCaps=false;setPadding(a.dp(2),0,a.dp(2),0);background=buttonSelector(a);setTextColor(Color.rgb(48,46,43))}}
  val grid=findFirst(root,GridLayout::class.java)
  if(grid!=null){val size=(a.resources.displayMetrics.widthPixels-a.dp(16)).coerceAtMost(a.dp(390));(grid.layoutParams as? LinearLayout.LayoutParams)?.let{lp->lp.height=size;lp.weight=0f;lp.topMargin=a.dp(5);lp.bottomMargin=a.dp(5);grid.layoutParams=lp};val gap=a.dp(3);val cell=(size-gap*3)/4;for(i in 0 until grid.childCount)(grid.getChildAt(i) as? Button)?.apply{minHeight=0;minimumHeight=0;textSize=8.5f;setPadding(0,0,0,0);setTextColor(Color.rgb(236,235,229));gravity=Gravity.CENTER;background=padSelector(a);layoutParams=GridLayout.LayoutParams().apply{width=cell;height=cell;columnSpec=GridLayout.spec(i%4);rowSpec=GridLayout.spec(i/4);setMargins(if(i%4==0)0 else gap,if(i/4==0)0 else gap,0,0)}}}
  val restart=findButton(root,"RESTART COURSE");val guide=findButton(root,"CONTROL GUIDE");restart?.visibility=View.GONE;guide?.visibility=View.GONE;rows.firstOrNull{row->(0 until row.childCount).any{row.getChildAt(it)===restart||row.getChildAt(it)===guide}}?.visibility=View.GONE;addUtilityBar(a,root,restart,guide)
 }
 private fun addUtilityBar(a:TrainerActivity,root:LinearLayout,restart:Button?,guide:Button?){val bar=LinearLayout(a).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(0,a.dp(3),0,0)};val tempo=Button(a).apply{text="TEMPO";textSize=8.5f;isAllCaps=false;background=buttonSelector(a);setOnClickListener{showBpm(a)}};val menu=Button(a).apply{text="MENU";textSize=8.5f;isAllCaps=false;background=buttonSelector(a);setOnClickListener{showMenu(a,restart,guide)}};bar.addView(tempo,LinearLayout.LayoutParams(0,a.dp(40),1f));bar.addView(menu,LinearLayout.LayoutParams(0,a.dp(40),1f));root.addView(bar)}
 private fun showMenu(a:TrainerActivity,restart:Button?,guide:Button?){val items=arrayOf("CONTROL GUIDE","RESTART COURSE","NEW PROJECT");AlertDialog.Builder(a).setTitle("MPC TRAINER MENU").setItems(items){_,which->when(which){0->guide?.performClick();1->restart?.performClick();2->confirmNewProject(a)}}.setNegativeButton("CLOSE",null).show()}
 private fun startIndicators(a:TrainerActivity){stopIndicators();val root=a.findViewById<ViewGroup>(android.R.id.content)?:return;val grid=findFirst(root,GridLayout::class.java);val r=object:Runnable{override fun run(){val s=TrainerUiState.snapshot;for(i in 0..3)setLed(a,root,"BANK ${'A'+i}",s.bank==i,Color.rgb(31,154,205));setLed(a,root,"REC",s.recording&&!s.overdub,Color.rgb(219,49,42));setLed(a,root,"OVERDUB",s.recording&&s.overdub,Color.rgb(231,116,28));setLed(a,root,"PLAY",s.playing&&!s.recording,Color.rgb(53,176,82));setLed(a,root,"NOTE REPEAT",s.noteRepeat,Color.rgb(220,53,137));setLed(a,root,"FULL LEVEL",s.fullLevel,Color.rgb(224,167,43));setLed(a,root,"16 LEVELS",s.levels16,Color.rgb(34,176,201));setLed(a,root,"TRACK MUTE",s.trackMuted,Color.rgb(213,56,52));if(s.activeSerial!=lastPadSerial){lastPadSerial=s.activeSerial;if(s.activeBank==s.bank&&s.activePad in 0..15){litPad=s.activePad;paintPad(a,grid,litPad,true);ui.postDelayed({if(litPad==s.activePad){paintPad(a,grid,litPad,false);litPad=-1}},85)}};ui.postDelayed(this,35)}};pulse=r;ui.post(r)}
 private fun paintPad(a:Activity,grid:GridLayout?,i:Int,on:Boolean){(grid?.getChildAt(i) as? Button)?.background=if(on)rounded(Color.rgb(72,74,68),Color.rgb(29,190,210),a.dp(3),a.dp(3)) else padSelector(a)}
 private fun setLed(a:Activity,root:View,label:String,on:Boolean,c:Int){findButton(root,label)?.apply{background=if(on)ledButton(a,c) else buttonSelector(a);setTextColor(if(on)Color.rgb(25,25,23) else Color.rgb(48,46,43))}}
 private fun stopIndicators(){pulse?.let{ui.removeCallbacks(it)};pulse=null;litPad=-1}
 private fun padSelector(a:Activity)=StateListDrawable().apply{addState(intArrayOf(android.R.attr.state_pressed),rounded(Color.rgb(67,66,62),Color.rgb(29,190,210),a.dp(3),a.dp(3)));addState(intArrayOf(),rounded(Color.rgb(50,49,47),Color.rgb(77,75,70),a.dp(1),a.dp(3)))}
 private fun buttonSelector(a:Activity)=StateListDrawable().apply{addState(intArrayOf(android.R.attr.state_pressed),rounded(Color.rgb(176,176,169),Color.rgb(106,104,98),a.dp(1),a.dp(3)));addState(intArrayOf(),rounded(Color.rgb(205,205,199),Color.rgb(151,149,141),a.dp(1),a.dp(3)))}
 private fun ledButton(a:Activity,c:Int)=rounded(Color.rgb(188,198,197),c,a.dp(3),a.dp(3))
 private fun showBpm(a:TrainerActivity){val prefs=a.getSharedPreferences("trainer_v2",Context.MODE_PRIVATE);val raw=prefs.getString("project",null);val obj=try{if(raw!=null)JSONObject(raw)else JSONObject()}catch(_:Exception){JSONObject()};val current=obj.optInt("bpm",90).coerceIn(40,240);val box=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;setPadding(a.dp(20),a.dp(8),a.dp(20),0)};val value=TextView(a).apply{textSize=22f;text="$current BPM"};val seek=SeekBar(a).apply{max=200;progress=current-40};seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,f:Boolean){value.text="${p+40} BPM"};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}});box.addView(value);box.addView(seek);AlertDialog.Builder(a).setTitle("TEMPO").setView(box).setNegativeButton("CANCEL",null).setPositiveButton("SET"){_,_->obj.put("bpm",seek.progress+40);prefs.edit().putString("project",obj.toString()).apply();a.recreate()}.show()}
 private fun confirmNewProject(a:TrainerActivity){AlertDialog.Builder(a).setTitle("NEW PROJECT").setMessage("Clear pad assignments, sequence events and project settings? Imported source audio stays on the phone.").setNegativeButton("CANCEL",null).setPositiveButton("NEW PROJECT"){_,_->val e=a.getSharedPreferences("trainer_v2",Context.MODE_PRIVATE).edit();ProjectReset.preferenceKeys().forEach(e::remove);e.apply();a.recreate()}.show()}
 private fun rounded(fill:Int,stroke:Int,strokeWidth:Int,radius:Int)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;setColor(fill);setStroke(strokeWidth,stroke);cornerRadius=radius.toFloat()}
 private fun findButton(root:View,label:String)=findFirst(root,Button::class.java){it.text?.toString()==label};private fun findText(root:View,p:(TextView)->Boolean)=findFirst(root,TextView::class.java,p)
 private fun <T:View> findFirst(root:View,type:Class<T>,p:(T)->Boolean={true}):T?{if(type.isInstance(root)){val v=type.cast(root);if(v!=null&&p(v))return v};if(root is ViewGroup)for(i in 0 until root.childCount){val f=findFirst(root.getChildAt(i),type,p);if(f!=null)return f};return null}
 private fun Activity.dp(v:Int)=(v*resources.displayMetrics.density+0.5f).toInt()
 override fun onActivityCreated(a:Activity,b:Bundle?){};override fun onActivityStarted(a:Activity){};override fun onActivityStopped(a:Activity){};override fun onActivitySaveInstanceState(a:Activity,b:Bundle){};override fun onActivityDestroyed(a:Activity){stopIndicators()}
}