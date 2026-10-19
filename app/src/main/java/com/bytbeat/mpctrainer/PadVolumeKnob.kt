package com.bytbeat.mpctrainer

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class PadVolumeKnob(context:Context):View(context){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 var value:Int=100; private set
 var label:String="PAD VOL"
 var onValueChanged:((Int)->Unit)?=null
 private var lastAngle=0.0
 private var accumulated=0.0
 private var dragging=false
 fun setValueSilently(v:Int){value=v.coerceIn(0,100);invalidate()}
 override fun onMeasure(w:Int,h:Int){val d=resources.displayMetrics.density;setMeasuredDimension(resolveSize((190*d).toInt(),w),resolveSize((82*d).toInt(),h))}
 private fun geometry():FloatArray{val d=resources.displayMetrics.density;val r=min(height*.42f,width*.205f);val cx=width-r-d*4f;val cy=height*.50f;return floatArrayOf(cx,cy,r)}
 private fun touchAngle(x:Float,y:Float,cx:Float,cy:Float)=Math.toDegrees(atan2((y-cy).toDouble(),(x-cx).toDouble()))
 override fun onDraw(c:Canvas){
  super.onDraw(c);val d=resources.displayMetrics.density;val sd=resources.displayMetrics.scaledDensity;val g=geometry();val cx=g[0];val cy=g[1];val r=g[2]
  p.style=Paint.Style.FILL;p.color=Color.rgb(53,52,49);c.drawCircle(cx,cy,r,p)
  p.style=Paint.Style.STROKE;p.strokeWidth=d*2.5f;p.color=Color.rgb(132,129,120);c.drawCircle(cx,cy,r,p)
  val a=Math.toRadians(-135.0+270.0*value/100.0);p.color=Color.rgb(224,205,103);p.strokeWidth=d*3.5f;c.drawLine(cx,cy,cx+cos(a).toFloat()*r*.78f,cy+sin(a).toFloat()*r*.78f,p)
  val tx=(cx-r-d*10f).coerceAtLeast(d*4f);p.style=Paint.Style.FILL;p.textAlign=Paint.Align.RIGHT;p.typeface=Typeface.DEFAULT_BOLD;p.color=Color.rgb(43,42,39)
  p.textSize=sd*10f;c.drawText("PAD VOL",tx,cy-d*4f,p);p.textSize=sd*15f;c.drawText("$value%",tx,cy+d*16f,p)
 }
 override fun onTouchEvent(e:MotionEvent):Boolean{
  val g=geometry();val cx=g[0];val cy=g[1];val r=g[2]
  when(e.actionMasked){
   MotionEvent.ACTION_DOWN->{val dx=e.x-cx;val dy=e.y-cy;if(dx*dx+dy*dy>(r*1.55f)*(r*1.55f))return false;lastAngle=touchAngle(e.x,e.y,cx,cy);accumulated=0.0;dragging=true;parent?.requestDisallowInterceptTouchEvent(true);return true}
   MotionEvent.ACTION_MOVE->{if(!dragging)return false;val now=touchAngle(e.x,e.y,cx,cy);var delta=now-lastAngle;if(delta>180)delta-=360.0;if(delta< -180)delta+=360.0;lastAngle=now;accumulated+=delta;val steps=(accumulated/2.7).toInt();if(steps!=0){accumulated-=steps*2.7;val nv=(value+steps).coerceIn(0,100);if(nv!=value){value=nv;invalidate();onValueChanged?.invoke(value)}};return true}
   MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{if(dragging){dragging=false;parent?.requestDisallowInterceptTouchEvent(false);performClick();return true}}
  };return false
 }
 override fun performClick():Boolean{super.performClick();return true}
}
