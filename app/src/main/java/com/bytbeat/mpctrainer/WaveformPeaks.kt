package com.bytbeat.mpctrainer

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.max

/** Streaming integer-PCM WAV peak extraction. Keeps long samples out of RAM. */
object WaveformPeaks {
    fun read(file: File, points: Int = 720): FloatArray {
        require(points > 0)
        RandomAccessFile(file, "r").use { r ->
            if (r.length() < 44) return FloatArray(0)
            val head = ByteArray(12); r.readFully(head)
            if (String(head, 0, 4) != "RIFF" || String(head, 8, 4) != "WAVE") return FloatArray(0)
            var format = 0; var channels = 1; var bits = 16; var dataStart = -1L; var dataSize = 0L
            while (r.filePointer + 8 <= r.length()) {
                val idb = ByteArray(4); r.readFully(idb); val id = String(idb)
                val size = readLeInt(r).toLong() and 0xffffffffL; val chunk = r.filePointer
                if (id == "fmt " && size >= 16) {
                    format = readLeShort(r); channels = readLeShort(r).coerceAtLeast(1)
                    r.skipBytes(4 + 4 + 2); bits = readLeShort(r)
                }
                if (id == "data") { dataStart = chunk; dataSize = size; break }
                r.seek((chunk + size + (size and 1)).coerceAtMost(r.length()))
            }
            if (dataStart < 0 || format != 1 || bits !in setOf(8,16,24,32)) return FloatArray(0)
            val bytesPerSample = bits / 8
            val frameBytes = channels * bytesPerSample
            val frames = (dataSize / frameBytes).coerceAtLeast(1)
            val actualPoints = minOf(points.toLong(), frames).toInt().coerceAtLeast(1)
            val out = FloatArray(actualPoints)
            r.seek(dataStart)
            var frame = 0L
            val normalizer = when(bits){8->128.0;16->32768.0;24->8388608.0;else->2147483648.0}
            for (p in 0 until actualPoints) {
                val targetEnd = ((p + 1L) * frames / actualPoints).coerceAtLeast(frame + 1)
                var peak = 0.0
                while (frame < targetEnd && frame < frames) {
                    var samplePeak = 0.0
                    repeat(channels) {
                        val magnitude = when(bits){
                            8 -> { val v=r.read(); if(v<0) 0.0 else abs(v-128).toDouble() }
                            16 -> { val lo=r.read();val hi=r.read();if(lo<0||hi<0)0.0 else abs(((hi shl 8) or lo).toShort().toInt()).toDouble() }
                            24 -> { val b0=r.read();val b1=r.read();val b2=r.read();if(b0<0||b1<0||b2<0)0.0 else {var v=b0 or (b1 shl 8) or (b2 shl 16);if((v and 0x800000)!=0)v=v or -0x1000000;abs(v.toDouble())} }
                            else -> { val b0=r.read();val b1=r.read();val b2=r.read();val b3=r.read();if(b0<0||b1<0||b2<0||b3<0)0.0 else abs((b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)).toDouble()) }
                        }
                        samplePeak=max(samplePeak,magnitude)
                    }
                    peak=max(peak,samplePeak);frame++
                }
                out[p]=(peak/normalizer).toFloat().coerceIn(0f,1f)
            }
            return out
        }
    }

    private fun readLeShort(r: RandomAccessFile): Int { val a=r.read(); val b=r.read(); return a or (b shl 8) }
    private fun readLeInt(r: RandomAccessFile): Int { val a=r.read(); val b=r.read(); val c=r.read(); val d=r.read(); return a or (b shl 8) or (c shl 16) or (d shl 24) }
}
