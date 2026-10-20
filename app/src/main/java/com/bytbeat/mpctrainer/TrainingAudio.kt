package com.bytbeat.mpctrainer

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*
import java.util.Random

/** Self-contained audible demonstrations. No project events or user audio are changed. */
object TrainingAudio {
 private const val rate=22050
 data class Hit(val seconds:Double,val sound:Int,val gain:Double=1.0,val speed:Double=1.0)
 fun example(lesson:Int,directory:File):File {
  val beat=60.0/90.0
  val hits=mutableListOf<Hit>()
  fun drums(start:Double,hats:Boolean=true,hatGain:Double=.3){
   for(i in 0..7){val t=start+i*beat; if(i%4==0||i%4==2)hits+=Hit(t,0,.8);if(i%4==1||i%4==3)hits+=Hit(t,1,.6)}
   if(hats)for(i in 0..15)hits+=Hit(start+i*beat/2,2,hatGain)
  }
  when(lesson){
   5,16->{for(i in 0..15){hits+=Hit(i*beat/2+if(i==0)0.0 else if(i%2==0).065 else -.04,2,.5);hits+=Hit(8*beat+i*beat/2,2,.5)}}
   6->{for(i in 0..15){hits+=Hit(i*beat/2,2,.5);hits+=Hit(8*beat+i*beat/2+if(i%2==1)beat*.12 else 0.0,2,.5)}}
   7->{for(i in 0..7)hits+=Hit(i*beat,2,.5);for(i in 0..15)hits+=Hit(8*beat+i*beat/2,2,.5)}
   8,9->{for(i in 0..7)hits+=Hit(i*beat,1,(i+1)/8.0*.7);for(i in 0..7)hits+=Hit(8*beat+i*beat,3,.45,2.0.pow((i-3)/12.0))}
   15->{hits+=Hit(0.0,3,.6);hits+=Hit(4*beat,3,.6,.5);hits+=Hit(8*beat,3,.6,2.0)}
   13->{val order=listOf(0,1,2,3,2,0,3,1);order.forEachIndexed{i,n->hits+=Hit(i*beat*2,3,.5,2.0.pow(n*2/12.0))}}
   4->{drums(0.0,false);drums(8*beat,true)}
   12->{drums(0.0,true);drums(8*beat,false)}
   14->{drums(0.0,true,.7);drums(8*beat,true,.16)}
   3->{for(i in 0..3)hits+=Hit(i*beat,2,.4);drums(4*beat,true);}
   1,2->{hits+=Hit(0.0,0,.8);hits+=Hit(2*beat,1,.6);hits+=Hit(4*beat,2,.5);drums(8*beat,true)}
   else->{drums(0.0,true);drums(8*beat,true)}
  }
  return render(File(directory,"lesson_${lesson}_90bpm.wav"),hits,16*beat+.5)
 }
 fun kit(directory:File):List<Pair<String,File>> = listOf(
  "Учебная бочка" to render(File(directory,"kick.wav"),listOf(Hit(0.0,0)),.5),
  "Учебный малый" to render(File(directory,"snare.wav"),listOf(Hit(0.0,1)),.5),
  "Учебный хэт" to render(File(directory,"hat.wav"),listOf(Hit(0.0,2)),.25),
  "Учебный сэмпл" to render(File(directory,"melody.wav"),(0..3).map{Hit(it*.5,3,.6,2.0.pow(it*2/12.0))},2.0)
 )
 private fun render(file:File,hits:List<Hit>,duration:Double):File {
  val data=FloatArray((duration*rate).toInt())
  for(hit in hits){val random=Random(47);val start=(hit.seconds*rate).toInt();val length=(rate*.48/hit.speed).toInt()
   for(i in 0 until length){val target=start+i;if(target !in data.indices)continue;val t=i.toDouble()/rate*hit.speed
    val noise=random.nextDouble()*2-1
    val sample=when(hit.sound){
     0->sin(2*PI*(48*t+90*.03*(1-exp(-t/.03))))*exp(-t*13)
     1->(.8*noise+.2*sin(2*PI*180*t))*exp(-t*22)
     2->noise*exp(-t*90)
     else->sin(2*PI*220*t)*exp(-t*8)
    };data[target]+=(sample*hit.gain*.55).toFloat()
   }
  }
  val pcm=ByteBuffer.allocate(data.size*2).order(ByteOrder.LITTLE_ENDIAN)
  data.forEach{pcm.putShort((it.coerceIn(-1f,1f)*32767).toInt().toShort())}
  val header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
  header.put("RIFF".toByteArray());header.putInt(36+pcm.capacity());header.put("WAVEfmt ".toByteArray());header.putInt(16);header.putShort(1);header.putShort(1);header.putInt(rate);header.putInt(rate*2);header.putShort(2);header.putShort(16);header.put("data".toByteArray());header.putInt(pcm.capacity())
  file.parentFile?.mkdirs();file.outputStream().use{it.write(header.array());it.write(pcm.array())};return file
 }
}
