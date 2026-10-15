package com.chordapp.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * VisualTransformation kustom untuk input Kode Lisensi KordApp.
 * Mengubah input alfanumerik mentah (misal: "KORDABCD0001SGHI" atau "KORD78294410")
 * menjadi format bersekat dash otomatis secara real-time:
 * - 3 Segmen (12 karakter): "XXXX-XXXX-XXXX" (contoh: "KORD-7829-4410")
 * - 4 Segmen (16 karakter): "XXXX-XXXX-XXXX-XXXX" (contoh: "KORD-ABCD-0001-SGHI")
 *
 * Dilengkapi OffsetMapping presisi agar kursor dan tombol backspace tidak melompat.
 */
class LicenseKeyVisualTransformation(
    val segmentSize: Int = 4,
    val separator: Char = '-'
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = StringBuilder()

        for (i in raw.indices) {
            out.append(raw[i])
            if ((i + 1) % segmentSize == 0 && (i + 1) < raw.length) {
                out.append(separator)
            }
        }

        val transformedString = out.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceIn(0, raw.length)
                // Jumlah separator yang berada sebelum posisi kursor
                val separators = if (clamped == raw.length) {
                    if (raw.isEmpty()) 0 else (raw.length - 1) / segmentSize
                } else {
                    clamped / segmentSize
                }
                return (clamped + separators).coerceIn(0, transformedString.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceIn(0, transformedString.length)
                // Dalam teks transformasi, setiap blok memiliki panjang (segmentSize + 1), misal 5 untuk segmentSize=4
                val separators = clamped / (segmentSize + 1)
                return (clamped - separators).coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(transformedString), offsetMapping)
    }

    companion object {
        /**
         * Membersihkan input pengguna: hanya izinkan alfanumerik [A-Za-z0-9],
         * otomatis dikonversi ke uppercase, dan dibatasi maksimal maxLength karakter.
         */
        fun sanitize(input: String, maxLength: Int = 16): String {
            return input.filter { it.isLetterOrDigit() }.uppercase().take(maxLength)
        }

        /**
         * Memformat string alfanumerik mentah menjadi format bertanda hubung (-)
         * sesuai segmentSize (default per 4 karakter).
         */
        fun formatWithDashes(raw: String, segmentSize: Int = 4, separator: Char = '-'): String {
            val clean = sanitize(raw)
            if (clean.isEmpty()) return ""
            return clean.chunked(segmentSize).joinToString(separator.toString())
        }
    }
}
