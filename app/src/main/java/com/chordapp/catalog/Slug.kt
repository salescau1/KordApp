package com.chordapp.catalog

/**
 * Slug identik dengan yang dipakai crawler saat mengisi `title_slug` / `artist_slug`
 * di catalog.db. Aturan (DIVERIFIKASI ulang atas 184.527 baris catalog.db asli):
 * huruf-kecil; huruf/angka dipertahankan (termasuk unicode); PEMISAH = spasi,
 * `-`, dan `_` (jadi satu `-`); SEMUA tanda baca lain DIBUANG tanpa pemisah
 * (mis. "AC/DC"->"acdc", "B.B. King"->"bb-king", "I'm"->"im", "P!nk"->"pnk");
 * lalu `-` beruntun dirapatkan & dipangkas. Contoh: "Menari-Nari"->"menari-nari",
 * "Jangan Paksa Rindu (Beda)"->"jangan-paksa-rindu-beda".
 *
 * Dipakai untuk mencocokkan entri chart (teks "artist"/"title" bebas) ke katalog
 * lewat kolom ber-index `artist_slug`/`title_slug` — jauh lebih cepat daripada
 * `lower(trim(artist))=...` yang memaksa full-table scan.
 */
fun catalogSlug(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input.lowercase()) {
        when {
            ch.isWhitespace() || ch == '-' || ch == '_' -> sb.append('-')
            ch.isLetterOrDigit() -> sb.append(ch)
            // tanda baca lain dibuang tanpa pemisah
        }
    }
    return sb.toString().replace(Regex("-+"), "-").trim('-')
}
