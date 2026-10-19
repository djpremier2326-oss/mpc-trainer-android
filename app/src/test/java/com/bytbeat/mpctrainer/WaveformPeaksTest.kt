package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

class WaveformPeaksTest {
    @Test fun extractsNormalizedPeaksAcrossWholeWav() {
        val f=File.createTempFile("waveform_", ".wav")
        try {
            val rate=8000; val frames=8000; val pcm=ByteArray(frames*2)
            for(i in 0 until frames){val amp=if(i<frames/2)4000 else 24000;val v=(sin(i*.12)*amp).toInt().toShort().toInt();pcm[i*2]=(v and 255).toByte();pcm[i*2+1]=((v shr 8) and 255).toByte()}
            FileOutputStream(f).use{out->
                fun le16(v:Int){out.write(v and 255);out.write((v shr 8) and 255)}
                fun le32(v:Int){repeat(4){out.write((v shr (it*8)) and 255)}}
                out.write("RIFF".toByteArray());le32(36+pcm.size);out.write("WAVEfmt ".toByteArray());le32(16);le16(1);le16(1);le32(rate);le32(rate*2);le16(2);le16(16);out.write("data".toByteArray());le32(pcm.size);out.write(pcm)
            }
            val p=WaveformPeaks.read(f,100)
            assertEquals(100,p.size);assertTrue(p.all{it in 0f..1f})
            assertTrue(p.take(50).average()<p.drop(50).average())
            assertTrue(p.maxOrNull()!!>.6f)
        } finally { f.delete() }
    }

    @Test fun rejectsNonWavWithoutCrashing(){val f=File.createTempFile("not_wave_", ".bin");try{f.writeText("hello");assertEquals(0,WaveformPeaks.read(f,20).size)}finally{f.delete()}}
}
