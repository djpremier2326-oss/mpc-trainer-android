package com.bytbeat.mpctrainer

import android.app.Application
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class TrainerBehaviorTest {
 private fun field(a:TrainerActivity,name:String)=TrainerActivity::class.java.getDeclaredField(name).apply{isAccessible=true}.get(a)
 private fun set(a:TrainerActivity,name:String,value:Any){TrainerActivity::class.java.getDeclaredField(name).apply{isAccessible=true}.set(a,value)}
 private fun invoke(a:TrainerActivity,name:String){TrainerActivity::class.java.getDeclaredMethod(name).apply{isAccessible=true}.invoke(a)}
 @Suppress("UNCHECKED_CAST") private fun events(a:TrainerActivity)=field(a,"events") as MutableList<TrainerActivity.Event>
 private fun find(root:View,label:String):Button?{if(root is Button&&root.text.toString()==label)return root;if(root is ViewGroup)for(i in 0 until root.childCount)find(root.getChildAt(i),label)?.let{return it};return null}
 private fun press(a:TrainerActivity,label:String){val b=find(a.window.decorView,label)?:error("Missing $label");b.performClick()}
 private fun dialog()=ShadowDialog.getLatestDialog() as AlertDialog
 private fun text(v:View):String {val own=if(v is TextView)v.text.toString() else "";return own+if(v is ViewGroup)(0 until v.childCount).joinToString(" "){text(v.getChildAt(it))}else ""}
 private fun open():org.robolectric.android.controller.ActivityController<TrainerActivity>{
  val app=org.robolectric.RuntimeEnvironment.getApplication()
  for(name in listOf("mpc","trainer_v2","learn"))app.getSharedPreferences(name,0).edit().clear().commit()
  return Robolectric.buildActivity(TrainerActivity::class.java).setup()
 }
 @Test fun learnExplainsConceptsBeforePracticeAndChecksUnderstanding(){
  val c=open();val a=c.get()
  try{
   press(a,"LEARN");var d=dialog();val t=text(d.window!!.decorView)
   assertTrue(t.contains("Что это"));assertTrue(t.contains("Sample"));assertTrue(t.contains("Зачем это нужно"));assertTrue(t.contains("Что услышать"));assertTrue(t.contains("Практика"))
   assertFalse(field(a,"lessonReady") as Boolean)
   d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();d=dialog()
   // Wrong answer explains the mistake and never arms practice.
   d.listView.performItemClick(null,1,1L);shadowOf(Looper.getMainLooper()).idle();d=dialog();assertTrue(text(d.window!!.decorView).contains("Разберёмся"));assertFalse(field(a,"lessonReady") as Boolean)
   d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();d=dialog();d.listView.performItemClick(null,0,0L);shadowOf(Looper.getMainLooper()).idle()
   dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(field(a,"lessonReady") as Boolean)
   press(a,"MAIN");assertEquals(1,(field(a,"engine") as LearnEngine).lesson)
  }finally{c.pause().stop().destroy()}
 }
 @Test fun recArmsAndPlayBeginsRecordingWhileOverdubPreservesEvents(){
  val c=open();val a=c.get()
  try{
   set(a,"countIn",false)
   events(a).add(TrainerActivity.Event(0,0,24,0,127))
   press(a,"REC");assertFalse(field(a,"playing") as Boolean);assertFalse(field(a,"recording") as Boolean);assertEquals(1,events(a).size)
   press(a,"PLAY");assertTrue(field(a,"recording") as Boolean);assertTrue(events(a).isEmpty())
   press(a,"STOP");events(a).add(TrainerActivity.Event(0,0,24,0,127))
   press(a,"PLAY");press(a,"OVERDUB");assertTrue(field(a,"overdub") as Boolean);assertTrue(field(a,"recording") as Boolean);assertEquals(1,events(a).size)
   press(a,"OVERDUB");assertFalse(field(a,"recording") as Boolean);assertTrue(field(a,"playing") as Boolean);press(a,"STOP")
  }finally{c.pause().stop().destroy()}
 }
 @Test fun stopCancelsCountInAndPlayResumesPosition(){
  val c=open();val a=c.get()
  try{
   press(a,"REC");press(a,"PLAY");press(a,"STOP");shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));assertFalse(field(a,"playing") as Boolean)
   set(a,"countIn",false);press(a,"PLAY");shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));press(a,"STOP");val position=field(a,"pausedTick") as Long;assertTrue(position>0)
   press(a,"PLAY");val tick=TrainerActivity::class.java.getDeclaredMethod("tick").apply{isAccessible=true}.invoke(a) as Long;assertTrue(kotlin.math.abs(tick-position)<=1);press(a,"STOP")
  }finally{c.pause().stop().destroy()}
 }
 @Test fun practiceKitNeverOverwritesLoadedPadsAndMuteIsNonDestructive(){
  val c=open();val a=c.get()
  try{
   invoke(a,"addTeachingKit");val count=(field(a,"pads") as Array<*>).sumOf{(it as Array<*>).count{x->x!=null}};assertEquals(4,count)
   invoke(a,"addTeachingKit");val count2=(field(a,"pads") as Array<*>).sumOf{(it as Array<*>).count{x->x!=null}};assertEquals(8,count2)
   events(a).add(TrainerActivity.Event(0,0,0,0,127));press(a,"TRACK MUTE");assertEquals(1,events(a).size);assertTrue((field(a,"mute") as BooleanArray)[0]);press(a,"TRACK MUTE");assertFalse((field(a,"mute") as BooleanArray)[0])
  }finally{c.pause().stop().destroy()}
 }
 @Test fun repeatWritesMultipleEventsAndVelocityLevelsRemainDistinct(){
  val c=open();val a=c.get()
  try{
   invoke(a,"addTeachingKit");set(a,"countIn",false);press(a,"REC");press(a,"PLAY");press(a,"NOTE REPEAT")
   invoke(a,"startRepeat");shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(520));invoke(a,"stopRepeat");assertTrue(events(a).size>=3)
   press(a,"STOP");events(a).clear();set(a,"levels16",true);set(a,"levels16Type","VELOCITY");set(a,"sourceBank",0);set(a,"sourcePad",0)
   press(a,"REC");press(a,"PLAY")
   val hit=TrainerActivity::class.java.getDeclaredMethod("hit",Int::class.javaPrimitiveType,Boolean::class.javaPrimitiveType).apply{isAccessible=true}
   hit.invoke(a,0,true);hit.invoke(a,7,true);hit.invoke(a,15,true)
   assertEquals(listOf(8,64,127),events(a).map{it.velocity});press(a,"STOP")
  }finally{c.pause().stop().destroy()}
 }
 @Test fun tcChangesOnlyCurrentTrackAndPitchSurvivesRelaunch(){
  var c=open();var a=c.get()
  try{
   invoke(a,"addTeachingKit");a.adjustSelectedPadPitch(-3)
   events(a).add(TrainerActivity.Event(0,0,10,0,127));events(a).add(TrainerActivity.Event(0,0,10,1,127));set(a,"tc",16);set(a,"tcShift",5)
   a.quantizeCurrentTrack();assertEquals(5L,events(a)[0].tick);assertEquals(10L,events(a)[1].tick)
   c.pause().stop().destroy();c=Robolectric.buildActivity(TrainerActivity::class.java).setup();a=c.get()
   @Suppress("UNCHECKED_CAST") val pitch=field(a,"padPitch") as Array<IntArray>;assertEquals(-3,pitch[0][0]);assertEquals(2,events(a).size)
  }finally{c.pause().stop().destroy()}
 }
 @Test fun emptyOverdubCannotPassRecordingLesson(){
  val c=open();val a=c.get()
  try{
   val e=field(a,"engine") as LearnEngine;e.selectLesson(4,LearnEngine.Snapshot(loadedPads=1,eventCount=1));set(a,"learn",true);set(a,"lessonReady",true);set(a,"countIn",false)
   events(a).add(TrainerActivity.Event(0,0,24,0,127));press(a,"OVERDUB");press(a,"PLAY");press(a,"STOP");assertEquals(4,e.lesson)
  }finally{c.pause().stop().destroy()}
 }
}
