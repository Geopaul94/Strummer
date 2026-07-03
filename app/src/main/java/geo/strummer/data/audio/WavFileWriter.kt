package geo.strummer.data.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Writes a standard 16-bit PCM WAV file incrementally.
//
// The engine hands us float samples in [-1, 1]. We convert to signed 16-bit LE
// and append them. Since we don't know the total length up front, we reserve the
// 44-byte RIFF header on open() and rewrite it with the real sizes on close().
class WavFileWriter(
    private val file: File,
    private val sampleRate: Int,
    private val channels: Int,
) {
    private var raf: RandomAccessFile? = null
    private var dataBytes = 0L

    fun open() {
        val f = RandomAccessFile(file, "rw")
        f.setLength(0)
        // Reserve space for the header; real values written in close().
        f.write(ByteArray(44))
        raf = f
        dataBytes = 0
    }

    // Convert and append [count] float samples (interleaved) as 16-bit PCM.
    fun writeFloats(samples: FloatArray, count: Int) {
        val f = raf ?: return
        val bytes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) {
            val clamped = samples[i].coerceIn(-1f, 1f)
            val s = (clamped * 32767f).toInt().toShort()
            bytes.putShort(s)
        }
        f.write(bytes.array())
        dataBytes += count * 2
    }

    fun close() {
        val f = raf ?: return
        writeHeader(f)
        f.close()
        raf = null
    }

    // Standard 44-byte RIFF/WAVE header for PCM audio.
    private fun writeHeader(f: RandomAccessFile) {
        val byteRate = sampleRate * channels * 2
        val blockAlign = channels * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt((36 + dataBytes).toInt())     // ChunkSize
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)                            // Subchunk1Size (PCM)
        header.putShort(1)                           // AudioFormat = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(16)                          // BitsPerSample
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataBytes.toInt())             // Subchunk2Size
        f.seek(0)
        f.write(header.array())
    }
}
