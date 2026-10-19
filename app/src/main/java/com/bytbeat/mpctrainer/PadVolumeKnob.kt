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
 private var lastY=0f
 fun setValueSilently(v:Int){value=v.coerceIn(0,100);invalidate()}
 override fun onMeasure(w:Int,h:Int){val desired=(64*resources.displayMetrics.density).toInt();setMeasuredDimension(resolveSize(desired,w),resolveSize(desired,h))}
 override fun onDraw(c:Canvas){super.onDraw(c);val cx=width/2f;val cy=height*.43f;val r=min(width,height)*.27f;p.style=Paint.Style.FILL;p.color=Color.rgb(53,52,49);c.drawCircle(cx,cy,r,p);p.style=Paint.Style.STROKE;p.strokeWidth=resources.displayMetrics.density*2;p.color=Color.rgb(132,129,120);c.drawCircle(cx,cy,r,p);val a=Math.toRadians((-135.0+270.0*value/100.0));p.color=Color.rgb(224,205,103);p.strokeWidth=resources.displayMetrics.density*3;c.drawLine(cx,cy,(cx+cos(a).toFloat()*r*.78f),(cy+sin(a).toFloat()*r*.78f),p);p.style=Paint.Style.FILL;p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=resources.displayMetrics.scaledDensity*8;p.color=Color.rgb(43,42,39);c.drawText(label,cx,height*.82f,p);p.textSize=resources.displayMetrics.scaledDensity*7;c.drawText("$value%",cx,height*.96f,p)}
 override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){MotionEvent.ACTION_DOWN->{lastY=e.y;parent?.requestDisallowInterceptTouchEvent(true);return true};MotionEvent.ACTION_MOVE->{val dy=lastY-e.y;if(abs(dy)>=1f){val nv=(value+dy/(resources.displayMetrics.density*1.3f)).roundToInt().coerceIn(0,100);lastY=e.y;if(nv!=value){value=nv;invalidate();onValueChanged?.invoke(value)}};return true};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{parent?.requestDisallowInterceptTouchEvent(false);performClick();return true}};return super.onTouchEvent(e)}
 override fun performClick():Boolean{super.performClick();return true}
}
