package com.bytbeat.mpctrainer

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class RecordedAudioBehaviorTest {
 private fun get(a:TrainerActivity,n:String)=TrainerActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun set(a:TrainerActivity,n:String,v:Any){TrainerActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.set(a,v)}
 private fun invoke(a:TrainerActivity,n:String){TrainerActivity::class.java.getDeclaredMethod(n).apply{isAccessible=true}.invoke(a)}
 @Suppress("UNCHECKED_CAST") private fun events(a:TrainerActivity)=get(a,"events") as MutableList<TrainerActivity.Event>
 private fun voice(a:TrainerActivity,e:TrainerActivity.Event)=TrainerActivity::class.java.getDeclaredMethod("voiceParameters",TrainerActivity.Event::class.java).apply{isAccessible=true}.invoke(a,e) as PadVoice.Parameters
 private fun setup(){val app=RuntimeEnvironment.getApplication();for(n in listOf("mpc","trainer_v2","learn"))app.getSharedPreferences(n,0).edit().clear().commit();app.getSharedPreferences("mpc",0).edit().putInt("course_welcome",1000).commit()}
 @Test fun tuneRecordedByActualHitKeepsPitchAndGainAcrossRelaunch(){
  setup();var c=Robolectric.buildActivity(TrainerActivity::class.java).setup();var a=c.get()
  try{
   invoke(a,"addTeachingKit");a.adjustSelectedPadPitch(-3);set(a,"levels16",true);set(a,"levels16Type","TUNE");set(a,"sourceBank",0);set(a,"sourcePad",0);set(a,"levels16OriginalPad",7);set(a,"playing",true);set(a,"recording",true)
   TrainerActivity::class.java.getDeclaredMethod("hit",Int::class.javaPrimitiveType,Boolean::class.javaPrimitiveType).apply{isAccessible=true}.invoke(a,11,true)
   assertEquals(1,events(a).size);val captured=events(a).single();assertEquals(4,captured.tuneOffset);assertEquals(127,captured.velocity);val before=voice(a,captured)
   c.pause().stop().destroy();c=Robolectric.buildActivity(TrainerActivity::class.java).setup();a=c.get()
   assertFalse(get(a,"levels16") as Boolean);val restored=events(a).single();assertEquals(captured,restored);assertEquals(before,voice(a,restored))
  }finally{c.pause().stop().destroy()}
 }
 private fun stepButton(root:View):Button?{if(root is Button&&root.text.toString().startsWith("1\n"))return root;if(root is ViewGroup)for(i in 0 until root.childCount)stepButton(root.getChildAt(i))?.let{return it};return null}
 @Test fun stepSequencerUsesTheSameNormalVelocityAsLivePad(){
  setup();val c=Robolectric.buildActivity(TrainerActivity::class.java).setup();val a=c.get()
  try{invoke(a,"addTeachingKit");invoke(a,"steps");val d=ShadowDialog.getLatestDialog() as AlertDialog;stepButton(d.window!!.decorView)!!.performClick();val e=events(a).single();assertEquals(127,e.velocity);assertEquals(1f,voice(a,e).gain,0f)}finally{c.pause().stop().destroy()}
 }
 @Test fun resetRemovesHiddenVolumeAttenuationButPreservesMusic(){
  setup();val c=Robolectric.buildActivity(TrainerActivity::class.java).setup();val a=c.get()
  try{invoke(a,"addTeachingKit");events(a).add(TrainerActivity.Event(0,0,24,0,64));a.adjustSelectedPadPitch(-4);set(a,"masterVolume",.2f);@Suppress("UNCHECKED_CAST") val levels=get(a,"padVolumes") as Array<FloatArray>;levels[0][0]=.5f;set(a,"levels16",true)
   a.restoreNormalLevels();assertEquals(1f,get(a,"masterVolume") as Float,0f);assertEquals(1f,levels[0][0],0f);assertFalse(get(a,"levels16") as Boolean);assertEquals(64,events(a).single().velocity);@Suppress("UNCHECKED_CAST") val pitch=get(a,"padPitch") as Array<IntArray>;assertEquals(-4,pitch[0][0])
  }finally{c.pause().stop().destroy()}
 }
 @Test fun oldNormalStrengthIsRestoredWithoutFlatteningIntentionalDynamics(){
  setup();RuntimeEnvironment.getApplication().getSharedPreferences("trainer_v2",0).edit().putString("project","""{"events":[{"b":0,"p":0,"t":0,"tr":0,"v":100},{"b":0,"p":0,"t":24,"tr":0,"v":64}]}""").commit()
  val c=Robolectric.buildActivity(TrainerActivity::class.java).setup();val a=c.get()
  try{assertEquals(listOf(127,64),events(a).map{it.velocity})}finally{c.pause().stop().destroy()}
 }
}
