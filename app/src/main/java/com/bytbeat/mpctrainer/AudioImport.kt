package com.bytbeat.mpctrainer

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Normalizes Android-supported audio to an editable PCM16 WAV when needed. */
object AudioImport {
 fun toEditableWav(input:File,output:File):File{
  if(input.extension.equals("wav",true)){try{val i=WavSlicer.inspect(input);if(i.bits==16)return input}catch(_:Exception){}}
  output.parentFile?.mkdirs();val temp=File(output.parentFile?:output.absoluteFile.parentFile,".${output.name}.pcm.tmp");val ex=MediaExtractor();var dec:MediaCodec?=null
  try{
   ex.setDataSource(input.absolutePath);var track=-1;var src:MediaFormat?=null
   for(i in 0 until ex.trackCount){val f=ex.getTrackFormat(i);if(f.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")){track=i;src=f;break}}
   require(track>=0&&src!=null){"Не удалось прочитать аудиофайл"};ex.selectTrack(track);val fmt=src!!;val mime=fmt.getString(MediaFormat.KEY_MIME)?:error("Неизвестный аудиоформат")
   dec=MediaCodec.createDecoderByType(mime);dec.configure(fmt,null,null,0);dec.start();var rate=fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);var channels=fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);var enc=AudioFormat.ENCODING_PCM_16BIT;var inDone=false;var outDone=false;var total=0L;val info=MediaCodec.BufferInfo()
   FileOutputStream(temp).use{out->while(!outDone){if(!inDone){val ix=dec.dequeueInputBuffer(10000);if(ix>=0){val b=dec.getInputBuffer(ix)?:error("Ошибка декодера");val n=ex.readSampleData(b,0);if(n<0){dec.queueInputBuffer(ix,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inDone=true}else{dec.queueInputBuffer(ix,0,n,ex.sampleTime,0);ex.advance()}}};val ox=dec.dequeueOutputBuffer(info,10000);if(ox>=0){if(info.size>0){val b=dec.getOutputBuffer(ox)?:error("Ошибка декодера");b.position(info.offset);b.limit(info.offset+info.size);val data=ByteArray(info.size);b.get(data);out.write(data);total+=data.size;if(total>512L*1024L*1024L)error("Сэмпл слишком большой для Sample Edit")};outDone=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0;dec.releaseOutputBuffer(ox,false)}else if(ox==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){val f=dec.outputFormat;rate=f.getInteger(MediaFormat.KEY_SAMPLE_RATE);channels=f.getInteger(MediaFormat.KEY_CHANNEL_COUNT);if(f.containsKey(MediaFormat.KEY_PCM_ENCODING))enc=f.getInteger(MediaFormat.KEY_PCM_ENCODING)}}}
   if(enc!=AudioFormat.ENCODING_PCM_16BIT)error("Телефон не смог преобразовать этот аудиофайл в PCM16");if(channels !in 1..2||rate<=0||total<=0||total>Int.MAX_VALUE)error("Неподдерживаемый формат аудио")
   FileOutputStream(output).use{out->val bytes=total.toInt();val h=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);h.put("RIFF".toByteArray());h.putInt(36+bytes);h.put("WAVEfmt ".toByteArray());h.putInt(16);h.putShort(1);h.putShort(channels.toShort());h.putInt(rate);h.putInt(rate*channels*2);h.putShort((channels*2).toShort());h.putShort(16);h.put("data".toByteArray());h.putInt(bytes);out.write(h.array());temp.inputStream().use{it.copyTo(out)}};return output
  }finally{try{dec?.stop()}catch(_:Exception){};try{dec?.release()}catch(_:Exception){};try{ex.release()}catch(_:Exception){};temp.delete()}
 }
}
