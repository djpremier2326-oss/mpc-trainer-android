package com.bytbeat.mpctrainer

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject

/** Compact phone presentation. TrainerActivity remains the source of sequencer/audio behaviour. */
class TrainerApp:Application(),Application.ActivityLifecycleCallbacks{
 override fun onCreate(){super.onCreate();registerActivityLifecycleCallbacks(this)}
 override fun onActivityResumed(activity:Activity){
  if(activity !is TrainerActivity)return
  val content=activity.findViewById<FrameLayout>(android.R.id.content)?:return
  if(content.tag=="phone_ui_v2")return
  val root=content.getChildAt(0) as? LinearLayout?:return
  rebuild(activity,root)
  content.tag="phone_ui_v2"
 }
 private fun rebuild(a:TrainerActivity,root:LinearLayout){
  root.setPadding(a.dp(8),a.dp(6),a.dp(8),a.dp(6));root.setBackgroundColor(Color.rgb(211,205,190))
  val header=findText(root){it.text?.toString()?.startsWith("MPC TRAINER")==true}
  header?.apply{text="MPC TRAINER";textSize=13f;gravity=Gravity.CENTER_VERTICAL;setPadding(a.dp(4),0,0,a.dp(3))}
  val status=findText(root){it.text?.toString()?.startsWith("SEQ")==true}
  status?.apply{textSize=10.5f;minHeight=0;setPadding(a.dp(8),a.dp(4),a.dp(8),a.dp(4));layoutParams=layoutParams.apply{height=a.dp(44)}}
  val grid=findFirst(root,GridLayout::class.java)
  if(grid!=null){
   (grid.layoutParams as? LinearLayout.LayoutParams)?.let{it.height=0;it.weight=1f;it.topMargin=a.dp(5);it.bottomMargin=a.dp(5);grid.layoutParams=it}
   for(i in 0 until grid.childCount)(grid.getChildAt(i) as? Button)?.apply{minHeight=0;minimumHeight=0;textSize=9f;setPadding(0,0,0,0);setTextColor(Color.WHITE);setBackgroundColor(Color.rgb(57,56,52));layoutParams=layoutParams.apply{height=0}}
  }
  val rows=(0 until root.childCount).mapNotNull{root.getChildAt(it) as? LinearLayout}.filter{it.orientation==LinearLayout.HORIZONTAL}
  rows.forEach{row->row.layoutParams=row.layoutParams.apply{height=a.dp(35)};for(i in 0 until row.childCount)(row.getChildAt(i) as? Button)?.apply{minHeight=0;minimumHeight=0;textSize=8f;isAllCaps=false;setPadding(a.dp(2),0,a.dp(2),0)}}
  // Surface hierarchy: navigation + banks + pads + performance + transport stay visible. Utility rows become compact.
  style(root,"REC",Color.rgb(154,54,42),Color.WHITE);style(root,"OVERDUB",Color.rgb(181,96,31),Color.WHITE);style(root,"PLAY",Color.rgb(55,103,67),Color.WHITE);style(root,"LEARN",Color.rgb(153,125,49),Color.WHITE)
  listOf("BANK A","BANK B","BANK C","BANK D").forEach{style(root,it,Color.rgb(72,71,66),Color.WHITE)}
  addBottomBar(a,root)
 }
 private fun addBottomBar(a:TrainerActivity,root:LinearLayout){
  val bar=LinearLayout(a).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(0,a.dp(3),0,0)}
  val bpm=Button(a).apply{text="BPM";textSize=8f;isAllCaps=false;minHeight=0;minimumHeight=0;setOnClickListener{showBpm(a)}}
  val fresh=Button(a).apply{text="NEW";textSize=8f;isAllCaps=false;minHeight=0;minimumHeight=0;setTextColor(Color.rgb(120,35,28));setOnClickListener{confirmNewProject(a)}}
  bar.addView(bpm,LinearLayout.LayoutParams(0,a.dp(34),1f));bar.addView(fresh,LinearLayout.LayoutParams(0,a.dp(34),1f));root.addView(bar)
 }
 private fun showBpm(a:TrainerActivity){val prefs=a.getSharedPreferences("trainer_v2",Context.MODE_PRIVATE);val raw=prefs.getString("project",null);val obj=try{if(raw!=null)JSONObject(raw)else JSONObject()}catch(_:Exception){JSONObject()};val current=obj.optInt("bpm",90).coerceIn(40,240);val box=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;setPadding(a.dp(20),a.dp(8),a.dp(20),0)};val value=TextView(a).apply{textSize=22f;text="$current BPM"};val seek=SeekBar(a).apply{max=200;progress=current-40};seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,f:Boolean){value.text="${p+40} BPM"};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}});box.addView(value);box.addView(seek);AlertDialog.Builder(a).setTitle("TEMPO").setView(box).setNegativeButton("CANCEL",null).setPositiveButton("SET"){_,_->obj.put("bpm",seek.progress+40);prefs.edit().putString("project",obj.toString()).apply();a.recreate()}.show()}
 private fun confirmNewProject(a:TrainerActivity){AlertDialog.Builder(a).setTitle("NEW PROJECT").setMessage("Clear pad assignments, sequence events and project settings? Imported source audio stays on the phone.").setNegativeButton("CANCEL",null).setPositiveButton("NEW PROJECT"){_,_->val e=a.getSharedPreferences("trainer_v2",Context.MODE_PRIVATE).edit();ProjectReset.preferenceKeys().forEach(e::remove);e.apply();a.recreate()}.show()}
 private fun style(root:View,label:String,bg:Int,fg:Int){findFirst(root,Button::class.java){it.text?.toString()==label}?.apply{setBackgroundColor(bg);setTextColor(fg)}}
 private fun findText(root:View,p:(TextView)->Boolean)=findFirst(root,TextView::class.java,p)
 private fun <T:View> findFirst(root:View,type:Class<T>,p:(T)->Boolean={true}):T?{if(type.isInstance(root)){val v=type.cast(root);if(v!=null&&p(v))return v};if(root is ViewGroup)for(i in 0 until root.childCount){val f=findFirst(root.getChildAt(i),type,p);if(f!=null)return f};return null}
 private fun Activity.dp(v:Int)=(v*resources.displayMetrics.density+0.5f).toInt()
 override fun onActivityCreated(a:Activity,b:Bundle?){};override fun onActivityStarted(a:Activity){};override fun onActivityPaused(a:Activity){};override fun onActivityStopped(a:Activity){};override fun onActivitySaveInstanceState(a:Activity,b:Bundle){};override fun onActivityDestroyed(a:Activity){}
}
