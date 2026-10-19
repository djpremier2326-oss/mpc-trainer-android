package com.bytbeat.mpctrainer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.max

/** Lightweight native waveform renderer for PCM16 WAV files used by SAMPLE EDIT. */
class WaveformView(context: Context) : View(context) {
    private val wavePaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(151,224,176);strokeWidth=2f}
    private val centerPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(67,91,78);strokeWidth=1f}
    private val markerPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(239,190,75);strokeWidth=4f}
    private val shadePaint=Paint().apply{color=Color.argb(105,0,0,0)}
    private var peaks=FloatArray(0)
    var startFraction=0f; private set
    var endFraction=1f; private set

    fun setSelection(start:Float,end:Float){startFraction=start.coerceIn(0f,.99f);endFraction=end.coerceIn(startFraction+.001f,1f);invalidate()}

    fun loadPcm16Wav(file:File,points:Int=720){
        peaks=try{readPeaks(file,points)}catch(_:Exception){FloatArray(0)}
        invalidate()
    }

    override fun onDraw(c:Canvas){
        super.onDraw(c);c.drawColor(Color.rgb(31,42,36));val mid=height/2f;c.drawLine(0f,mid,width.toFloat(),mid,centerPaint)
        if(peaks.isNotEmpty()){val dx=width.toFloat()/peaks.size;for(i in peaks.indices){val amp=peaks[i]*height*.45f;val x=(i+.5f)*dx;c.drawLine(x,mid-amp,x,mid+amp,wavePaint)}}
        val sx=width*startFraction;val ex=width*endFraction
        if(sx>0)c.drawRect(0f,0f,sx,height.toFloat(),shadePaint);if(ex<width)c.drawRect(ex,0f,width.toFloat(),height.toFloat(),shadePaint)
        c.drawLine(sx,0f,sx,height.toFloat(),markerPaint);c.drawLine(ex,0f,ex,height.toFloat(),markerPaint)
    }

    private fun readPeaks(file:File,points:Int):FloatArray{
        RandomAccessFile(file,"r").use{r->
            if(r.length()<44)return FloatArray(0)
            val head=ByteArray(12);r.readFully(head);if(String(head,0,4)!="RIFF"||String(head,8,4)!="WAVE")return FloatArray(0)
            var channels=1;var bits=16;var dataStart=-1L;var dataSize=0L
            while(r.filePointer+8<=r.length()){
                val idb=ByteArray(4);r.readFully(idb);val id=String(idb);val size=readLeInt(r).toLong() and 0xffffffffL;val chunk=r.filePointer
                if(id=="fmt "&&size>=16){readLeShort(r);channels=readLeShort(r).coerceAtLeast(1);r.skipBytes(4+4+2);bits=readLeShort(r)}
                if(id=="data"){dataStart=chunk;dataSize=size;break}
                r.seek((chunk+size+(size and 1)).coerceAtMost(r.length()))
            }
            if(dataStart<0||bits!=16)return FloatArray(0)
            val frameBytes=channels*2;val frames=(dataSize/frameBytes).coerceAtLeast(1);val out=FloatArray(points);val framesPerPoint=max(1L,frames/points)
            r.seek(dataStart);var p=0;var frame=0L
            while(p<points&&frame<frames){var peak=0;var n=0L;while(n<framesPerPoint&&frame<frames){var samplePeak=0;repeat(channels){val lo=r.read();val hi=r.read();if(lo<0||hi<0)return@repeat;val v=((hi shl 8) or lo).toShort().toInt();samplePeak=max(samplePeak,abs(v))};peak=max(peak,samplePeak);n++;frame++};out[p]=(peak/32768f).coerceIn(0f,1f);p++}
            return if(p==points)out else out.copyOf(p)
        }
    }
    private fun readLeShort(r:RandomAccessFile):Int{val a=r.read();val b=r.read();return a or (b shl 8)}
    private fun readLeInt(r:RandomAccessFile):Int{val a=r.read();val b=r.read();val c=r.read();val d=r.read();return a or (b shl 8) or (c shl 16) or (d shl 24)}
}
