package com.bytbeat.mpctrainer

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Small offline PCM-WAV slicer for the foundation Sample Edit lesson.
 * It intentionally supports ordinary uncompressed PCM WAV only; unsupported
 * files are left untouched rather than pretending that a chop succeeded.
 */
object WavSlicer {
    data class Info(
        val channels:Int,
        val sampleRate:Int,
        val bits:Int,
        val dataOffset:Long,
        val dataSize:Int,
        val blockAlign:Int
    ) { val frames:Int get() = if(blockAlign==0) 0 else dataSize/blockAlign }

    fun inspect(file:File):Info {
        RandomAccessFile(file,"r").use { r ->
            if(r.length()<44) error("WAV file is too short")
            if(readAscii(r,4)!="RIFF") error("Not a RIFF WAV")
            readLE32(r)
            if(readAscii(r,4)!="WAVE") error("Not a WAV file")
            var channels=0; var rate=0; var bits=0; var align=0; var pcm=false
            var dataOffset=-1L; var dataSize=0
            while(r.filePointer+8<=r.length()) {
                val id=readAscii(r,4); val size=readLE32(r).toInt(); val start=r.filePointer
                when(id) {
                    "fmt " -> if(size>=16) {
                        val format=readLE16(r); channels=readLE16(r); rate=readLE32(r).toInt()
                        readLE32(r); align=readLE16(r); bits=readLE16(r); pcm=format==1
                    }
                    "data" -> { dataOffset=start; dataSize=size; break }
                }
                r.seek(start+size+(size and 1))
            }
            if(!pcm) error("Only PCM WAV is supported in basic Sample Edit")
            if(channels !in 1..2 || bits !in setOf(8,16,24,32) || align<=0 || dataOffset<0) error("Unsupported WAV format")
            val safeSize=minOf(dataSize.toLong(),r.length()-dataOffset).toInt()
            return Info(channels,rate,bits,dataOffset,safeSize,align)
        }
    }

    fun slice(source:File, target:File, startFraction:Float, endFraction:Float):File {
        val info=inspect(source)
        val lo=startFraction.coerceIn(0f,.999f)
        val hi=endFraction.coerceIn(lo+.001f,1f)
        val startFrame=(info.frames*lo).toInt().coerceIn(0,info.frames-1)
        val endFrame=(info.frames*hi).toInt().coerceIn(startFrame+1,info.frames)
        val byteCount=(endFrame-startFrame)*info.blockAlign
        val pcm=ByteArray(byteCount)
        RandomAccessFile(source,"r").use { r -> r.seek(info.dataOffset+startFrame.toLong()*info.blockAlign); r.readFully(pcm) }
        writePcm(target,info,pcm)
        return target
    }

    fun chop(source:File, dir:File, startFraction:Float, endFraction:Float, pieces:Int, baseName:String):List<File> {
        require(pieces in 2..16)
        dir.mkdirs()
        val lo=startFraction.coerceIn(0f,.999f); val hi=endFraction.coerceIn(lo+.001f,1f)
        return (0 until pieces).map { i ->
            val a=lo+(hi-lo)*(i.toFloat()/pieces)
            val b=lo+(hi-lo)*((i+1f)/pieces)
            slice(source,File(dir,"${baseName}_slice_${i+1}.wav"),a,b)
        }
    }

    private fun writePcm(target:File,info:Info,pcm:ByteArray) {
        val byteRate=info.sampleRate*info.blockAlign
        val out=ByteBuffer.allocate(44+pcm.size).order(ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray(Charsets.US_ASCII));out.putInt(36+pcm.size);out.put("WAVE".toByteArray(Charsets.US_ASCII))
        out.put("fmt ".toByteArray(Charsets.US_ASCII));out.putInt(16);out.putShort(1);out.putShort(info.channels.toShort());out.putInt(info.sampleRate);out.putInt(byteRate);out.putShort(info.blockAlign.toShort());out.putShort(info.bits.toShort())
        out.put("data".toByteArray(Charsets.US_ASCII));out.putInt(pcm.size);out.put(pcm)
        target.writeBytes(out.array())
    }
    private fun readAscii(r:RandomAccessFile,n:Int)=String(ByteArray(n).also{r.readFully(it)},Charsets.US_ASCII)
    private fun readLE16(r:RandomAccessFile):Int { val a=r.read();val b=r.read();return a or (b shl 8) }
    private fun readLE32(r:RandomAccessFile):Long { var v=0L; repeat(4){i->v=v or (r.read().toLong() shl (8*i))};return v and 0xffffffffL }
}
