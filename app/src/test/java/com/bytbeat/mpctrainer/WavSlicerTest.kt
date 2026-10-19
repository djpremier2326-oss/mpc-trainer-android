package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavSlicerTest {
    private fun wav(dir:File, frames:Int=4800):File {
        val f=File(dir,"source.wav")
        val channels=1; val rate=48000; val bits=16; val align=2; val bytes=frames*align
        val h=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray());h.putInt(36+bytes);h.put("WAVE".toByteArray())
        h.put("fmt ".toByteArray());h.putInt(16);h.putShort(1);h.putShort(channels.toShort())
        h.putInt(rate);h.putInt(rate*align);h.putShort(align.toShort());h.putShort(bits.toShort())
        h.put("data".toByteArray());h.putInt(bytes)
        FileOutputStream(f).use { out -> out.write(h.array()); out.write(ByteArray(bytes){(it%127).toByte()}) }
        return f
    }

    @Test fun inspectReadsPcmWav() {
        val dir=createTempDir(); try {
            val info=WavSlicer.inspect(wav(dir))
            assertEquals(1,info.channels);assertEquals(48000,info.sampleRate)
            assertEquals(16,info.bits);assertEquals(4800,info.frames)
        } finally { dir.deleteRecursively() }
    }

    @Test fun slicePreservesFormatAndRange() {
        val dir=createTempDir(); try {
            val src=wav(dir);val out=WavSlicer.slice(src,File(dir,"half.wav"),.25f,.75f)
            val info=WavSlicer.inspect(out)
            assertEquals(2400,info.frames);assertEquals(48000,info.sampleRate)
        } finally { dir.deleteRecursively() }
    }

    @Test fun chopCreatesRequestedPlayableSlices() {
        val dir=createTempDir(); try {
            val src=wav(dir);val pieces=WavSlicer.chop(src,File(dir,"chops"),0f,1f,8,"sample")
            assertEquals(8,pieces.size)
            assertTrue(pieces.all{it.exists() && WavSlicer.inspect(it).frames==600})
        } finally { dir.deleteRecursively() }
    }
}
