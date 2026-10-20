package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import kotlin.math.*

class PcmMixerTest {
 private fun wav(file:File,bits:Int,frames:Int,channels:Int=2,rate:Int=48000):FloatArray{
  val size=frames*channels*(bits/8);val b=ByteBuffer.allocate(44+size).order(ByteOrder.LITTLE_ENDIAN)
  b.put("RIFF".toByteArray());b.putInt(36+size);b.put("WAVEfmt ".toByteArray());b.putInt(16);b.putShort(1);b.putShort(channels.toShort());b.putInt(rate);b.putInt(rate*channels*bits/8);b.putShort((channels*bits/8).toShort());b.putShort(bits.toShort());b.put("data".toByteArray());b.putInt(size)
  val expected=FloatArray(frames*channels)
  for(i in expected.indices){val value=(if(i%channels==0).8 else -.4)*sin(2*PI*(i/channels)/48.0);expected[i]=value.toFloat()
   when(bits){8->b.put((value*128+128).roundToInt().coerceIn(0,255).toByte());16->b.putShort((value*32768).roundToInt().toShort());24->{val n=(value*8388608).roundToInt();b.put(n.toByte());b.put((n shr 8).toByte());b.put((n shr 16).toByte())};32->b.putInt((value*2147483648.0).toLong().toInt())}
  };file.writeBytes(b.array());return expected
 }
 @Test fun allPcmDepthsKeepAmplitudeStereoAndSourceBytes(){
  val dir=Files.createTempDirectory("pcm").toFile()
  try{for(bits in listOf(8,16,24,32)){val f=File(dir,"$bits.audio");val expected=wav(f,bits,1024);val before=f.readBytes();val s=PcmMixer.read(f);assertEquals(1024,s.frames);assertEquals(2,s.channels)
    val tolerance=if(bits==8).0041f else if(bits==16).00002f else .000001f
    assertArrayEquals(expected,s.data,tolerance);assertArrayEquals(before,f.readBytes())
    val mix=PcmMixer();mix.play(s,1f,1f,1f);val out=mix.render(1024).first;assertArrayEquals(s.data,out,0f)
   }}finally{dir.deleteRecursively()}
 }
 @Test fun previewAndPadAreIdenticalAtUnityGainWithoutSoundPoolTruncation(){
  val dir=Files.createTempDirectory("long-pcm").toFile()
  try{val f=File(dir,"long.wav");wav(f,16,48000*8);assertTrue(f.length()>1024*1024);val s=PcmMixer.read(f)
   val preview=PcmMixer();val pad=PcmMixer();preview.play(s,1f,1f,1f);pad.play(s,1f,1f,1f)
   var frames=0
   while(preview.hasVoices()){val a=preview.render(256).first;val b=pad.render(256).first;assertArrayEquals(a,b,0f);frames+=256}
   assertTrue(frames>=48000*8);assertFalse(pad.hasVoices())
  }finally{dir.deleteRecursively()}
 }
 @Test fun pitchChangesRateWithoutChangingPeakAndVolumesAreLinear(){
  val s=PcmMixer.Sample(FloatArray(1024){.5f},1,48000)
  val m=PcmMixer();m.play(s,.5f,.5f,2f);val out=m.render(512).first;assertTrue(out.all{it==.25f});assertFalse(m.hasVoices())
 }
 @Test fun simultaneousPadsSumWithoutFixedHeadroomAttenuation(){
  val s=PcmMixer.Sample(FloatArray(32){.2f},1,48000);val m=PcmMixer();m.play(s,1f,1f,1f);m.play(s,1f,1f,1f);assertTrue(m.render(16).first.all{abs(it-.4f)<.000001f})
 }
}
