package com.bytbeat.mpctrainer

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject

/** Adapts the trainer surface to real phone screens without touching audio logic. */
class TrainerApp : Application(), Application.ActivityLifecycleCallbacks {
    override fun onCreate() { super.onCreate(); registerActivityLifecycleCallbacks(this) }

    override fun onActivityResumed(activity: Activity) {
        if (activity !is TrainerActivity) return
        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (content.tag == "phone_layout_fixed") return
        val root = content.getChildAt(0) as? LinearLayout ?: return

        findFirst(root, GridLayout::class.java)?.let { grid ->
            (grid.layoutParams as? LinearLayout.LayoutParams)?.let { lp -> lp.height=ViewGroup.LayoutParams.WRAP_CONTENT; lp.weight=0f; grid.layoutParams=lp }
            for(i in 0 until grid.childCount) grid.getChildAt(i).layoutParams=grid.getChildAt(i).layoutParams.apply{height=activity.dp(66)}
        }
        tuneRows(activity,root)
        addProjectControls(activity,root)

        content.removeView(root)
        val scroll=ScrollView(activity).apply{isFillViewport=true;overScrollMode=View.OVER_SCROLL_IF_CONTENT_SCROLLS;addView(root,ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT))}
        content.addView(scroll,FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT))
        content.tag="phone_layout_fixed"
    }

    private fun addProjectControls(activity:TrainerActivity,root:LinearLayout){
        val row=LinearLayout(activity).apply{orientation=LinearLayout.HORIZONTAL}
        val bpm=Button(activity).apply{text="BPM";textSize=10f;isAllCaps=false;setOnClickListener{showBpm(activity)}}
        val fresh=Button(activity).apply{text="New Project";textSize=10f;isAllCaps=false;setTextColor(Color.rgb(110,25,20));setOnClickListener{confirmNewProject(activity)}}
        row.addView(bpm,LinearLayout.LayoutParams(0,activity.dp(48),1f));row.addView(fresh,LinearLayout.LayoutParams(0,activity.dp(48),1f));root.addView(row)
    }

    private fun showBpm(activity:TrainerActivity){
        val prefs=activity.getSharedPreferences("trainer_v2",Context.MODE_PRIVATE)
        val raw=prefs.getString("project",null)
        val obj=try{if(raw!=null)JSONObject(raw) else JSONObject()}catch(_:Exception){JSONObject()}
        val current=obj.optInt("bpm",90).coerceIn(40,240)
        val box=LinearLayout(activity).apply{orientation=LinearLayout.VERTICAL;setPadding(activity.dp(20),activity.dp(8),activity.dp(20),0)}
        val value=TextView(activity).apply{textSize=22f;text="$current BPM"}
        val seek=SeekBar(activity).apply{max=200;progress=current-40}
        seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,f:Boolean){value.text="${p+40} BPM"};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}})
        box.addView(value);box.addView(seek)
        AlertDialog.Builder(activity).setTitle("TEMPO").setMessage("Set the project tempo. The real MPC uses BPM as the master timing reference for recording, playback, metronome and Note Repeat.").setView(box).setNegativeButton("CANCEL",null).setPositiveButton("SET"){_,_->obj.put("bpm",seek.progress+40);prefs.edit().putString("project",obj.toString()).apply();activity.recreate()}.show()
    }

    private fun confirmNewProject(activity:TrainerActivity){
        AlertDialog.Builder(activity).setTitle("NEW PROJECT").setMessage("Start from an empty project? This clears pad assignments, recorded events and current trainer settings. Imported audio files remain stored on the phone so the operation is safe for the source files.").setNegativeButton("CANCEL",null).setPositiveButton("NEW PROJECT"){_,_->activity.getSharedPreferences("trainer_v2",Context.MODE_PRIVATE).edit().clear().apply();activity.recreate()}.show()
    }

    private fun tuneRows(activity:Activity,root:ViewGroup){
        for(i in 0 until root.childCount){val child=root.getChildAt(i);if(child is LinearLayout&&child.orientation==LinearLayout.HORIZONTAL){child.layoutParams=child.layoutParams.apply{height=activity.dp(48)};for(j in 0 until child.childCount)(child.getChildAt(j) as? Button)?.apply{minHeight=0;minimumHeight=0;setPadding(activity.dp(3),0,activity.dp(3),0);textSize=9f;isAllCaps=false}}}
        findFirst(root,TextView::class.java){it.text?.toString()?.startsWith("MPC TRAINER")==true}?.setPadding(0,activity.dp(4),0,activity.dp(4))
        styleButton(root,"REC",Color.rgb(155,55,42),Color.WHITE);styleButton(root,"OVERDUB",Color.rgb(184,102,35),Color.WHITE);styleButton(root,"PLAY",Color.rgb(63,102,69),Color.WHITE);styleButton(root,"LEARN",Color.rgb(154,126,54),Color.WHITE)
        listOf("BANK A","BANK B","BANK C","BANK D").forEach{styleButton(root,it,Color.rgb(75,73,66),Color.WHITE)}
    }
    private fun styleButton(root:View,label:String,bg:Int,fg:Int){findFirst(root,Button::class.java){it.text?.toString()==label}?.apply{setBackgroundColor(bg);setTextColor(fg)}}
    private fun <T:View> findFirst(root:View,type:Class<T>,predicate:(T)->Boolean={true}):T?{if(type.isInstance(root)){val value=type.cast(root);if(value!=null&&predicate(value))return value};if(root is ViewGroup)for(i in 0 until root.childCount){val found=findFirst(root.getChildAt(i),type,predicate);if(found!=null)return found};return null}
    private fun Activity.dp(value:Int)=(value*resources.displayMetrics.density+0.5f).toInt()
    override fun onActivityCreated(a:Activity,b:Bundle?){};override fun onActivityStarted(a:Activity){};override fun onActivityPaused(a:Activity){};override fun onActivityStopped(a:Activity){};override fun onActivitySaveInstanceState(a:Activity,b:Bundle){};override fun onActivityDestroyed(a:Activity){}
}
