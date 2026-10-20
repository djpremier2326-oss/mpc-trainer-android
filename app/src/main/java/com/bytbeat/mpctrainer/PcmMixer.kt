package com.bytbeat.mpctrainer

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor

/** One linear PCM path for preview and pads. No normalization, compression or fixed attenuation. */
class PcmMixer(val outputRate:Int=48000) {
 data class Sample(val data:FloatArray,val channels:Int,val sampleRate:Int){val frames:Int get()=data.size/channels}
 private data class Voice(val id:Int,val sample:Sample,val left:Float,val right:Float,val rate:Float,var position:Double=0.0,val done:(()->Unit)?=null)
 private val voices=mutableListOf<Voice>();private var serial=0
 @Synchronized fun play(sample:Sample,left:Float,right:Float,rate:Float,done:(()->Unit)?=null):Int{
  if(voices.size>=32)voices.removeAt(0)
  val id=++serial;voices+=Voice(id,sample,left.coerceIn(0f,1f),right.coerceIn(0f,1f),rate.coerceIn(.125f,8f),done=done);return id
 }
 @Synchronized fun stop(id:Int){voices.removeAll{it.id==id}}
 @Synchronized fun clear(){voices.clear()}
 @Synchronized fun hasVoices()=voices.isNotEmpty()
 @Synchronized fun render(frames:Int):Pair<FloatArray,List<()->Unit>>{
  val out=FloatArray(frames*2);val finished=mutableListOf<()->Unit>();val it=voices.iterator()
  while(it.hasNext()){
   val v=it.next();val s=v.sample;val advance=s.sampleRate.toDouble()/outputRate*v.rate
   for(f in 0 until frames){if(v.position>=s.frames)break
    val a=floor(v.position).toInt();val b=(a+1).coerceAtMost(s.frames-1);val t=(v.position-a).toFloat()
    val l=s.data[a*s.channels]+(s.data[b*s.channels]-s.data[a*s.channels])*t
    val ch=if(s.channels==2)1 else 0
    val r=s.data[a*s.channels+ch]+(s.data[b*s.channels+ch]-s.data[a*s.channels+ch])*t
    out[f*2]+=l*v.left;out[f*2+1]+=r*v.right;v.position+=advance
   }
   if(v.position>=s.frames){it.remove();v.done?.let{finished+=it}}
  }
  // Float audio's legal full-scale range. Never scale down a single voice to make headroom.
  for(i in out.indices)out[i]=out[i].coerceIn(-1f,1f)
  return out to finished
 }
 companion object {
  fun read(file:File):Sample {
   val info=WavSlicer.inspect(file)
   require(info.frames.toLong()*info.channels<=16L*1024*1024){"Сэмпл слишком большой: максимум 64 МБ декодированного PCM на звук"}
   val pcm=FloatArray(info.frames*info.channels);val bytesPerValue=info.bits/8
   RandomAccessFile(file,"r").use{input->input.seek(info.dataOffset);val block=ByteArray(64*1024/ info.blockAlign*info.blockAlign);var frame=0
    while(frame<info.frames){val count=minOf(block.size/info.blockAlign,info.frames-frame);input.readFully(block,0,count*info.blockAlign);val bb=ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN)
     for(f in 0 until count)for(ch in 0 until info.channels){val offset=f*info.blockAlign+ch*bytesPerValue;val value=when(info.bits){
      8->((block[offset].toInt() and 255)-128)/128f
      16->bb.getShort(offset)/32768f
      24->{val n=(block[offset].toInt() and 255) or ((block[offset+1].toInt() and 255) shl 8) or (block[offset+2].toInt() shl 16);n/8388608f}
      else->bb.getInt(offset)/2147483648f
     };pcm[(frame+f)*info.channels+ch]=value}
     frame+=count
    }
   };return Sample(pcm,info.channels,info.sampleRate)
  }
 }
}
