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
 private var downX=0f;private var downY=0f;private var startValue=100
 fun setValueSilently(v:Int){value=v.coerceIn(0,100);invalidate()}
 override fun onMeasure(w:Int,h:Int){val desiredW=(190*resources.displayMetrics.density).toInt();val desiredH=(96*resources.displayMetrics.density).toInt();setMeasuredDimension(resolveSize(desiredW,w),resolveSize(desiredH,h))}
 override fun onDraw(c:Canvas){
  super.onDraw(c)
  val d=resources.displayMetrics.density
  val sd=resources.displayMetrics.scaledDensity
  val r=min(height*.38f,width*.22f)
  val cx=width-r-d*8f
  val cy=height*.50f
  p.style=Paint.Style.FILL;p.color=Color.rgb(53,52,49);c.drawCircle(cx,cy,r,p)
  p.style=Paint.Style.STROKE;p.strokeWidth=d*2.5f;p.color=Color.rgb(132,129,120);c.drawCircle(cx,cy,r,p)
  val a=Math.toRadians((-135.0+270.0*value/100.0));p.color=Color.rgb(224,205,103);p.strokeWidth=d*3.5f
  c.drawLine(cx,cy,cx+cos(a).toFloat()*r*.78f,cy+sin(a).toFloat()*r*.78f,p)
  val tx=(cx-r-d*10f).coerceAtLeast(d*4f)
  p.style=Paint.Style.FILL;p.textAlign=Paint.Align.RIGHT;p.typeface=Typeface.DEFAULT_BOLD;p.color=Color.rgb(43,42,39)
  p.textSize=sd*10f;c.drawText(label,tx,cy-d*3f,p)
  p.textSize=sd*15f;c.drawText("$value%",tx,cy+d*16f,p)
 }
 override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){MotionEvent.ACTION_DOWN->{downX=e.x;downY=e.y;startValue=value;parent?.requestDisallowInterceptTouchEvent(true);return true};MotionEvent.ACTION_MOVE->{val dx=e.x-downX;val dy=downY-e.y;val dominant=if(abs(dx)>abs(dy))dx else dy;val nv=(startValue+dominant/(resources.displayMetrics.density*2.4f)).roundToInt().coerceIn(0,100);if(nv!=value){value=nv;invalidate();onValueChanged?.invoke(value)};return true};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{parent?.requestDisallowInterceptTouchEvent(false);performClick();return true}};return true}
 override fun performClick():Boolean{super.performClick();return true}
}
