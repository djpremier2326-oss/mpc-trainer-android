package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

class WaveformPeaksTest {
    private fun wav(bits:Int):File{
        val f=File.createTempFile("waveform_${bits}_", ".wav");val rate=8000;val frames=800
        val bytes=bits/8;val pcm=ByteArray(frames*bytes)
        for(i in 0 until frames){val scale=if(i<frames/2).18 else .72;val unit=sin(i*.12)*scale;when(bits){
            8->{pcm[i]=(128+(unit*127).toInt()).coerceIn(0,255).toByte()}
            16->{val v=(unit*32767).toInt();pcm[i*2]=(v and 255).toByte();pcm[i*2+1]=((v shr 8) and 255).toByte()}
            24->{val v=(unit*8388607).toInt();repeat(3){pcm[i*3+it]=((v shr (it*8)) and 255).toByte()}}
            32->{val v=(unit*2147483647.0).toInt();repeat(4){pcm[i*4+it]=((v shr (it*8)) and 255).toByte()}}
        }}
        FileOutputStream(f).use{out->fun le16(v:Int){out.write(v and 255);out.write((v shr 8) and 255)};fun le32(v:Int){repeat(4){out.write((v shr(it*8)) and 255)}};out.write("RIFF".toByteArray());le32(36+pcm.size);out.write("WAVEfmt ".toByteArray());le32(16);le16(1);le16(1);le32(rate);le32(rate*bytes);le16(bytes);le16(bits);out.write("data".toByteArray());le32(pcm.size);out.write(pcm)}
        return f
    }

    @Test fun extractsNormalizedPeaksAcrossEditablePcmDepths(){for(bits in listOf(8,16,24,32)){val f=wav(bits);try{val p=WaveformPeaks.read(f,100);assertEquals("bits=$bits",100,p.size);assertTrue("bits=$bits",p.all{it in 0f..1f});assertTrue("bits=$bits",p.take(50).average()<p.drop(50).average());assertTrue("bits=$bits",p.maxOrNull()!!>.5f)}finally{f.delete()}}}

    @Test fun rejectsNonWavWithoutCrashing(){val f=File.createTempFile("not_wave_", ".bin");try{f.writeText("hello");assertEquals(0,WaveformPeaks.read(f,20).size)}finally{f.delete()}}
}
