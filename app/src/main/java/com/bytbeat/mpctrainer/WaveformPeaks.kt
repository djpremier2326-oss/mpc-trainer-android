package com.bytbeat.mpctrainer

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.max

/** Streaming PCM16 WAV peak extraction. Keeps long samples out of RAM. */
object WaveformPeaks {
    fun read(file: File, points: Int = 720): FloatArray {
        require(points > 0)
        RandomAccessFile(file, "r").use { r ->
            if (r.length() < 44) return FloatArray(0)
            val head = ByteArray(12); r.readFully(head)
            if (String(head, 0, 4) != "RIFF" || String(head, 8, 4) != "WAVE") return FloatArray(0)
            var channels = 1; var bits = 16; var dataStart = -1L; var dataSize = 0L
            while (r.filePointer + 8 <= r.length()) {
                val idb = ByteArray(4); r.readFully(idb); val id = String(idb)
                val size = readLeInt(r).toLong() and 0xffffffffL; val chunk = r.filePointer
                if (id == "fmt " && size >= 16) {
                    readLeShort(r); channels = readLeShort(r).coerceAtLeast(1)
                    r.skipBytes(4 + 4 + 2); bits = readLeShort(r)
                }
                if (id == "data") { dataStart = chunk; dataSize = size; break }
                r.seek((chunk + size + (size and 1)).coerceAtMost(r.length()))
            }
            if (dataStart < 0 || bits != 16) return FloatArray(0)
            val frameBytes = channels * 2
            val frames = (dataSize / frameBytes).coerceAtLeast(1)
            val actualPoints = minOf(points.toLong(), frames).toInt().coerceAtLeast(1)
            val out = FloatArray(actualPoints)
            r.seek(dataStart)
            var frame = 0L
            for (p in 0 until actualPoints) {
                val targetEnd = ((p + 1L) * frames / actualPoints).coerceAtLeast(frame + 1)
                var peak = 0
                while (frame < targetEnd && frame < frames) {
                    var samplePeak = 0
                    repeat(channels) {
                        val lo = r.read(); val hi = r.read()
                        if (lo >= 0 && hi >= 0) samplePeak = max(samplePeak, abs(((hi shl 8) or lo).toShort().toInt()))
                    }
                    peak = max(peak, samplePeak); frame++
                }
                out[p] = (peak / 32768f).coerceIn(0f, 1f)
            }
            return out
        }
    }

    private fun readLeShort(r: RandomAccessFile): Int { val a=r.read(); val b=r.read(); return a or (b shl 8) }
    private fun readLeInt(r: RandomAccessFile): Int { val a=r.read(); val b=r.read(); val c=r.read(); val d=r.read(); return a or (b shl 8) or (c shl 16) or (d shl 24) }
}
