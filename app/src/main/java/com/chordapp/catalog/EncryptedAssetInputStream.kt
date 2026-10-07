package com.chordapp.catalog

import java.io.FilterInputStream
import java.io.InputStream

/**
 * FilterInputStream yang men-dekripsi stream data katalog bertopeng secara on-the-fly.
 * Bekerja langsung secara streaming byte-by-byte tanpa memakan RAM tambahan.
 */
class EncryptedAssetInputStream(inputStream: InputStream) : FilterInputStream(inputStream) {

    private val key = OBFUSCATION_KEY
    private var keyIndex = 0

    override fun read(): Int {
        val b = super.read()
        if (b == -1) return -1
        val decrypted = b xor (key[keyIndex % key.size].toInt() and 0xFF)
        keyIndex++
        return decrypted
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val count = super.read(b, off, len)
        if (count == -1) return -1
        for (i in 0 until count) {
            b[off + i] = (b[off + i].toInt() xor (key[keyIndex % key.size].toInt() and 0xFF)).toByte()
            keyIndex++
        }
        return count
    }

    override fun skip(n: Long): Long {
        if (n <= 0) return 0
        val buffer = ByteArray(minOf(n, 8192L).toInt())
        var totalSkipped = 0L
        while (totalSkipped < n) {
            val toRead = minOf(n - totalSkipped, buffer.size.toLong()).toInt()
            val read = read(buffer, 0, toRead)
            if (read == -1) break
            totalSkipped += read
        }
        return totalSkipped
    }

    override fun markSupported(): Boolean = false

    companion object {
        private val OBFUSCATION_KEY = byteArrayOf(
            0x4B, 0x4F, 0x52, 0x44, // K O R D
            0x5F, 0x4B, 0x45, 0x4B, // _ K E K
            0x41, 0x4C, 0x43, 0x4C, // A L C L
            0x49, 0x50, 0x5F, 0x32  // I P _ 2
        )
    }
}
