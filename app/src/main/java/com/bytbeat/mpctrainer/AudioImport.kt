package com.bytbeat.mpctrainer

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Decodes Android-supported compressed audio (including MP3) to 16-bit PCM WAV. */
object AudioImport {
    fun toEditableWav(input: File, output: File): File {
        if (input.extension.equals("wav", true)) return input
        val extractor = MediaExtractor()
        extractor.setDataSource(input.absolutePath)
        var track = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) { track = i; format = f; break }
        }
        require(track >= 0 && format != null) { "No audio track" }
        extractor.selectTrack(track)
        val source = format!!
        val mime = source.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(source, null, null, 0)
        codec.start()
        val pcm = ArrayList<ByteArray>()
        var total = 0
        var inputDone = false
        var outputDone = false
        var sampleRate = source.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = source.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val info = MediaCodec.BufferInfo()
        while (!outputDone) {
            if (!inputDone) {
                val ix = codec.dequeueInputBuffer(10_000)
                if (ix >= 0) {
                    val buf = codec.getInputBuffer(ix)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(ix, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(ix, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val ox = codec.dequeueOutputBuffer(info, 10_000)
            if (ox >= 0) {
                if (info.size > 0) {
                    val b = codec.getOutputBuffer(ox)!!
                    b.position(info.offset); b.limit(info.offset + info.size)
                    val data = ByteArray(info.size); b.get(data)
                    pcm += data; total += data.size
                }
                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                codec.releaseOutputBuffer(ox, false)
            } else if (ox == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val out = codec.outputFormat
                sampleRate = out.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                channels = out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            }
        }
        codec.stop(); codec.release(); extractor.release()
        output.parentFile?.mkdirs()
        FileOutputStream(output).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray()); header.putInt(36 + total); header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray()); header.putInt(16); header.putShort(1); header.putShort(channels.toShort())
            header.putInt(sampleRate); header.putInt(sampleRate * channels * 2); header.putShort((channels * 2).toShort()); header.putShort(16)
            header.put("data".toByteArray()); header.putInt(total); out.write(header.array())
            pcm.forEach(out::write)
        }
        return output
    }
}
