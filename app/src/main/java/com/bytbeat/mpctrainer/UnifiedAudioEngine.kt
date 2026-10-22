package com.bytbeat.mpctrainer

import android.content.Context
import android.media.*
import android.os.Handler
import android.os.Looper
import android.os.Process
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Shared stereo float PCM playback for pad voices and Browser audition. */
class UnifiedAudioEngine(private val context:Context) {
 private val main=Handler(Looper.getMainLooper());private val worker=Executors.newSingleThreadExecutor()
 private val next=AtomicInteger();private val allocated=mutableSetOf<Int>();private val samples=mutableMapOf<Int,PcmMixer.Sample>()
 private val outputRate=(context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()?.takeIf{it in 8000..192000}?:48000
 private val burstFrames=(context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull()?.takeIf{it in 64..2048}?:192
 private val mixer=PcmMixer(outputRate);@Volatile private var closed=false;@Volatile private var output:AudioTrack?=null;private var thread:Thread?=null
 private var listener:((UnifiedAudioEngine,Int,Int)->Unit)?=null
 fun setOnLoadCompleteListener(value:((UnifiedAudioEngine,Int,Int)->Unit)?){listener=value}
 fun load(path:String,priority:Int):Int=loadInternal(path,true,null)
 fun loadPreview(path:String,ready:(Int,Int)->Unit):Int=loadInternal(path,false,ready)
 private fun loadInternal(path:String,notify:Boolean,ready:((Int,Int)->Unit)?):Int{
  if(closed)return 0
  ensureOutput() // Start the output while decoding, before the first pad hit.
  val id=next.incrementAndGet();synchronized(samples){allocated+=id}
  worker.execute{var status=0
   try{val input=File(path);val editable=try{WavSlicer.inspect(input);input}catch(_:Exception){AudioImport.toEditableWav(input,File(context.cacheDir,"pcm_engine/${java.util.UUID.randomUUID()}.wav"))};val sample=PcmMixer.read(editable)
    synchronized(samples){if(!closed&&id in allocated){val used=samples.values.sumOf{it.data.size.toLong()*4};require(used+sample.data.size.toLong()*4<=128L*1024*1024){"Недостаточно памяти для набора"};samples[id]=sample}}
    if(editable!=input)editable.delete()
   }catch(_:Exception){status=-1}
   main.post{if(!closed&&synchronized(samples){id in allocated}){if(notify)listener?.invoke(this,id,status);ready?.invoke(id,status)}}
  };return id
 }
 fun play(soundId:Int,left:Float,right:Float,priority:Int,loop:Int,rate:Float):Int=playVoice(soundId,left,right,rate,null)
 fun playPreview(soundId:Int,gain:Float,done:()->Unit):Int=playVoice(soundId,gain,gain,1f,done)
 private fun playVoice(soundId:Int,left:Float,right:Float,rate:Float,done:(()->Unit)?):Int{
  if(closed)return 0;val sample=synchronized(samples){samples[soundId]}?:return 0
  val voice=mixer.play(sample,left,right,rate){if(done!=null)main.post{if(!closed)done()}}
  ensureOutput();return voice
 }
 @Synchronized private fun ensureOutput(){if(thread?.isAlive==true||closed)return
  thread=Thread({
   try{Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
    val min=AudioTrack.getMinBufferSize(outputRate,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_FLOAT).coerceAtLeast(4096)
    val track=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(outputRate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()).setBufferSizeInBytes(min).setTransferMode(AudioTrack.MODE_STREAM).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build()
    if(closed){track.release();return@Thread};output=track;track.setBufferSizeInFrames(burstFrames*2);track.setVolume(1f);track.play()
    while(!closed){val (audio,completed)=mixer.render(burstFrames);var offset=0;while(offset<audio.size&&!closed){val n=track.write(audio,offset,audio.size-offset,AudioTrack.WRITE_BLOCKING);if(n<=0)break;offset+=n};completed.forEach{it()};if(offset==0&&!closed)break}
   }catch(_:Exception){main.post{if(!closed)android.widget.Toast.makeText(context,"Не удалось запустить аудиовыход",android.widget.Toast.LENGTH_LONG).show()}}
   finally{val track=output;output=null;try{track?.stop()}catch(_:Exception){};try{track?.release()}catch(_:Exception){}}
  },"MPC-PCM").apply{isDaemon=true;start()}
 }
 fun stop(stream:Int){mixer.stop(stream)}
 fun stopAll(){mixer.clear()}
 fun unload(id:Int):Boolean=synchronized(samples){allocated.remove(id);samples.remove(id)!=null}
 fun release(){closed=true;listener=null;worker.shutdownNow();mixer.clear();synchronized(samples){samples.clear();allocated.clear()};try{output?.pause();output?.flush()}catch(_:Exception){};main.removeCallbacksAndMessages(null)}
}
