package com.bytbeat.mpctrainer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import java.io.File
import kotlin.math.abs

/** Native SAMPLE EDIT waveform with draggable Start/End markers. */
class WaveformView(context: Context) : View(context) {
    private val wavePaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(151,224,176);strokeWidth=2f}
    private val centerPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(67,91,78);strokeWidth=1f}
    private val markerPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(239,190,75);strokeWidth=4f}
    private val shadePaint=Paint().apply{color=Color.argb(105,0,0,0)}
    private var peaks=FloatArray(0)
    private var dragging=0
    var startFraction=0f; private set
    var endFraction=1f; private set
    var onSelectionChanged:((Float,Float)->Unit)?=null

    fun setSelection(start:Float,end:Float){
        startFraction=start.coerceIn(0f,.99f);endFraction=end.coerceIn(startFraction+.001f,1f);invalidate()
    }

    fun loadPcm16Wav(file:File,points:Int=720){peaks=try{WaveformPeaks.read(file,points)}catch(_:Exception){FloatArray(0)};invalidate()}

    override fun onDraw(c:Canvas){
        super.onDraw(c);c.drawColor(Color.rgb(31,42,36));val mid=height/2f;c.drawLine(0f,mid,width.toFloat(),mid,centerPaint)
        if(peaks.isNotEmpty()){val dx=width.toFloat()/peaks.size;for(i in peaks.indices){val amp=peaks[i]*height*.45f;val x=(i+.5f)*dx;c.drawLine(x,mid-amp,x,mid+amp,wavePaint)}}
        val sx=width*startFraction;val ex=width*endFraction
        if(sx>0)c.drawRect(0f,0f,sx,height.toFloat(),shadePaint);if(ex<width)c.drawRect(ex,0f,width.toFloat(),height.toFloat(),shadePaint)
        c.drawLine(sx,0f,sx,height.toFloat(),markerPaint);c.drawLine(ex,0f,ex,height.toFloat(),markerPaint)
    }

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(width<=0)return false
        val f=(e.x/width).coerceIn(0f,1f)
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN->{dragging=if(abs(f-startFraction)<=abs(f-endFraction))1 else 2;parent?.requestDisallowInterceptTouchEvent(true);moveMarker(f);return true}
            MotionEvent.ACTION_MOVE->{moveMarker(f);return true}
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{moveMarker(f);dragging=0;parent?.requestDisallowInterceptTouchEvent(false);performClick();return true}
        }
        return super.onTouchEvent(e)
    }
    override fun performClick():Boolean{super.performClick();return true}
    private fun moveMarker(f:Float){
        if(dragging==1)startFraction=f.coerceIn(0f,(endFraction-.01f).coerceAtLeast(0f))
        else if(dragging==2)endFraction=f.coerceIn((startFraction+.01f).coerceAtMost(1f),1f)
        invalidate();onSelectionChanged?.invoke(startFraction,endFraction)
    }
}
