package com.bytbeat.mpctrainer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.Executors

/** Browser and Sample Edit use exactly the same PCM engine as the pads. */
class BrowserPreview(private val context:Context,private val engine:UnifiedAudioEngine,private val gain:()->Float={1f}) {
 private val main=Handler(Looper.getMainLooper());private val files=Executors.newSingleThreadExecutor()
 @Volatile private var generation=0;private var sound=0;private var stream=0;@Volatile private var active=false;@Volatile private var closed=false
 @Synchronized fun play(uri:Uri,onReady:()->Unit={},onError:(String)->Unit={}){
  if(closed)return;stop();val token=generation;active=true
  files.execute{var temporary:File?=null
   try{
    val file=if(uri.scheme=="file")File(uri.path?:error("Нет пути к аудио"))else{
     File.createTempFile("audition_",".audio",context.cacheDir).also{out->temporary=out;context.contentResolver.openInputStream(uri)?.use{input->out.outputStream().use{input.copyTo(it)}}?:error("Не удалось открыть файл")}
    }
    main.post{
     if(closed||generation!=token){temporary?.delete();return@post}
     sound=engine.loadPreview(file.absolutePath){id,status->
      temporary?.delete()
      if(closed||generation!=token){engine.unload(id);return@loadPreview}
      if(status!=0){stop();onError("Не удалось декодировать PREVIEW");return@loadPreview}
      onReady();stream=engine.playPreview(id,gain().coerceIn(0f,1f)){if(generation==token)stop()}
     }
    }
   }catch(e:Exception){temporary?.delete();main.post{if(!closed&&generation==token){stop();onError(e.message?:"PREVIEW недоступен")}}}
  }
 }
 @Synchronized fun stop(){generation++;active=false;if(stream!=0)engine.stop(stream);if(sound!=0)engine.unload(sound);stream=0;sound=0}
 @Synchronized fun release(){stop();closed=true;files.shutdownNow();main.removeCallbacksAndMessages(null)}
 fun isActive()=active
}
