package com.chordapp.promo

import org.json.JSONObject

/** What tapping the promo popup does. */
sealed interface PromoAction {
    /** Opens an external link (Spotify/YouTube/dll) in the browser. */
    data class External(val url: String) : PromoAction

    /** Opens a song already present in the local catalog. */
    data class Catalog(val artistSlug: String, val titleSlug: String) : PromoAction
}

/**
 * One promotional item shown as a popup on Home.
 *
 * Isi berasal dari repo GitHub publik (salescau1/KordApp-Content, `promo.json`).
 * Tidak pernah ada data placeholder: jika file gagal dimuat / offline / tidak ada
 * entri `active`, popup tidak ditampilkan sama sekali.
 */
data class Promo(
    val id: String,
    val imageUrl: String,
    /** Judul/subjudul tidak lagi dirender di popup (full-image tanpa teks), tetap diparse untuk kompatibilitas. */
    val title: String?,
    val subtitle: String?,
    /** Aksi saat GAMBAR jadi area tap — hanya dipakai bila kedua tombol stream kosong. */
    val action: PromoAction?,
    /** Tombol stream opsional, dikonfigurasi dari promo.json. Null = tombol tidak ditampilkan. */
    val youtubeUrl: String?,
    val spotifyUrl: String?,
    /** Durasi kunci countdown dalam detik (0..15). 0 = tanpa kunci. */
    val countdownSeconds: Int
)

object PromoParser {
    private const val JSDELIVR_BASE =
        "https://cdn.jsdelivr.net/gh/salescau1/KordApp-Content@main/"

    /**
     * Parse promo.json dan kembalikan promo AKTIF pertama, atau null bila tidak
     * ada yang valid/aktif. Toleran: entri cacat dilewati, bukan bikin crash.
     */
    fun parseActive(json: String): Promo? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val promos = root.optJSONArray("promos") ?: return null
        for (i in 0 until promos.length()) {
            val obj = promos.optJSONObject(i) ?: continue
            if (!obj.optBoolean("active", false)) continue
            val promo = fromJson(obj) ?: continue
            return promo
        }
        return null
    }

    private fun fromJson(obj: JSONObject): Promo? {
        val id = obj.optString("id").trim().takeIf { it.isNotEmpty() } ?: return null
        val image = obj.optString("image").trim().takeIf { it.isNotEmpty() } ?: return null
        val title = obj.optString("title").trim().takeIf { it.isNotEmpty() }
        val subtitle = obj.optString("subtitle").trim().takeIf { it.isNotEmpty() }
        // countdownSeconds: default 3, clamp 0..15. Absen -> default.
        val countdown = obj.optInt("countdownSeconds", 3).coerceIn(0, 15)
        return Promo(
            id = id,
            imageUrl = resolveImage(image),
            title = title,
            subtitle = subtitle,
            action = parseAction(obj.optJSONObject("action")),
            youtubeUrl = parseHttpUrl(obj.optString("youtubeUrl")),
            spotifyUrl = parseHttpUrl(obj.optString("spotifyUrl")),
            countdownSeconds = countdown
        )
    }

    /** Kembalikan URL hanya bila http(s) valid; selain itu null (tombol tidak tampil). */
    private fun parseHttpUrl(raw: String?): String? {
        val url = raw?.trim().orEmpty()
        return if (url.startsWith("https://") || url.startsWith("http://")) url else null
    }

    private fun parseAction(obj: JSONObject?): PromoAction? {
        obj ?: return null
        return when (obj.optString("type").trim().lowercase()) {
            "external" -> {
                val url = obj.optString("url").trim()
                if (url.startsWith("https://") || url.startsWith("http://")) {
                    PromoAction.External(url)
                } else null
            }
            "catalog" -> {
                val a = obj.optString("artistSlug").trim()
                val t = obj.optString("titleSlug").trim()
                if (a.isNotEmpty() && t.isNotEmpty()) PromoAction.Catalog(a, t) else null
            }
            else -> null
        }
    }

    /** Path relatif (mis. `images/x.jpg`) -> URL jsDelivr; URL absolut dibiarkan. */
    private fun resolveImage(image: String): String =
        if (image.startsWith("http://") || image.startsWith("https://")) image
        else JSDELIVR_BASE + image.trimStart('/')
}
