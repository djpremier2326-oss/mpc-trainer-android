package com.bytbeat.mpctrainer

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Decodes Android-supported compressed audio (including MP3) to 16-bit PCM WAV. */
object AudioImport {
    fun toEditableWav(input:File,output:File):File {
        if(input.extension.equals("wav",true)) return input
        output.parentFile?.mkdirs()
        val temp=File(output.parentFile ?: output.absoluteFile.parentFile,".${output.name}.pcm.tmp")
        val extractor=MediaExtractor()
        var codec:MediaCodec?=null
        try {
            extractor.setDataSource(input.absolutePath)
            var track=-1
            var source:MediaFormat?=null
            for(i in 0 until extractor.trackCount){
                val f=extractor.getTrackFormat(i)
                val mime=f.getString(MediaFormat.KEY_MIME).orEmpty()
                if(mime.startsWith("audio/")){track=i;source=f;break}
            }
            require(track>=0 && source!=null){"No audio track"}
            extractor.selectTrack(track)
            val src=source!!
            val mime=src.getString(MediaFormat.KEY_MIME) ?: error("Unknown audio format")
            codec=MediaCodec.createDecoderByType(mime)
            codec.configure(src,null,null,0)
            codec.start()
            var sampleRate=src.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels=src.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var pcmEncoding=AudioFormat.ENCODING_PCM_16BIT
            var inputDone=false
            var outputDone=false
            var total=0L
            val info=MediaCodec.BufferInfo()
            FileOutputStream(temp).use { pcmOut ->
                while(!outputDone){
                    if(!inputDone){
                        val ix=codec.dequeueInputBuffer(10_000)
                        if(ix>=0){
                            val buf=codec.getInputBuffer(ix) ?: error("Decoder input buffer unavailable")
                            val size=extractor.readSampleData(buf,0)
                            if(size<0){codec.queueInputBuffer(ix,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true}
                            else{codec.queueInputBuffer(ix,0,size,extractor.sampleTime,0);extractor.advance()}
                        }
                    }
                    val ox=codec.dequeueOutputBuffer(info,10_000)
                    when {
                        ox>=0 -> {
                            if(info.size>0){
                                val b=codec.getOutputBuffer(ox) ?: error("Decoder output buffer unavailable")
                                b.position(info.offset);b.limit(info.offset+info.size)
                                val data=ByteArray(info.size);b.get(data);pcmOut.write(data);total+=data.size
                                if(total>512L*1024L*1024L) error("Audio is too large for basic Sample Edit")
                            }
                            outputDone=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(ox,false)
                        }
                        ox==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val f=codec.outputFormat
                            sampleRate=f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels=f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            if(f.containsKey(MediaFormat.KEY_PCM_ENCODING)) pcmEncoding=f.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                    }
                }
            }
            if(pcmEncoding!=AudioFormat.ENCODING_PCM_16BIT) error("This audio decoder did not return 16-bit PCM")
            if(channels !in 1..2 || sampleRate<=0) error("Unsupported decoded audio format")
            if(total<=0L || total>Int.MAX_VALUE) error("Decoded audio is empty or too large")
            FileOutputStream(output).use { out ->
                val bytes=total.toInt()
                val header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray(Charsets.US_ASCII));header.putInt(36+bytes);header.put("WAVE".toByteArray(Charsets.US_ASCII))
                header.put("fmt ".toByteArray(Charsets.US_ASCII));header.putInt(16);header.putShort(1);header.putShort(channels.toShort())
                header.putInt(sampleRate);header.putInt(sampleRate*channels*2);header.putShort((channels*2).toShort());header.putShort(16)
                header.put("data".toByteArray(Charsets.US_ASCII));header.putInt(bytes);out.write(header.array())
                temp.inputStream().use { it.copyTo(out) }
            }
            return output
        } finally {
            try{codec?.stop()}catch(_:Exception){}
            try{codec?.release()}catch(_:Exception){}
            try{extractor.release()}catch(_:Exception){}
            temp.delete()
        }
    }
}
