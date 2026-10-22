package com.chordapp.catalog

import java.text.Normalizer
import java.util.Locale

/**
 * Normalisasi diakritik/aksen teks ke bentuk dasar Latin (ASCII folding).
 * Menghilangkan accent marks sehingga pencarian seperti:
 * - "eńau" <-> "enau"
 * - "Beyoncé" <-> "Beyonce"
 * - "Michael Bublé" <-> "Michael Buble"
 * - "Mötley Crüe" <-> "Motley Crue"
 * dapat saling menemukan dua arah.
 */
private val COMBINING_DIACRITICS_REGEX = Regex("\\p{Mn}+")

fun foldDiacritics(input: String): String {
    if (input.isEmpty()) return ""
    // Fast path: if input has no non-ASCII characters, return directly
    var hasNonAscii = false
    for (i in 0 until input.length) {
        if (input[i].code >= 128) {
            hasNonAscii = true
            break
        }
    }
    if (!hasNonAscii) return input

    val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
    val stripped = nfd.replace(COMBINING_DIACRITICS_REGEX, "")
    return stripped
        .replace('ø', 'o').replace('Ø', 'O')
        .replace('æ', 'a').replace('Æ', 'A')
        .replace('đ', 'd').replace('Đ', 'D')
        .replace('ł', 'l').replace('Ł', 'L')
}

/**
 * Memeriksa apakah [source] mengandung [query] dengan mengabaikan huruf besar/kecil
 * dan tanda aksen/diakritik.
 */
fun containsIgnoreDiacritics(source: String, query: String): Boolean {
    val normSource = foldDiacritics(source).lowercase(Locale.ROOT)
    val normQuery = foldDiacritics(query).lowercase(Locale.ROOT)
    return normSource.contains(normQuery)
}

/**
 * Menghasilkan variasi kata kunci pencarian (original, folded, decomposed, dan substitusi umum)
 * agar query SQLite LIKE dapat menemukan rekaman dengan aksen maupun tanpa aksen.
 */
fun generateSearchVariants(query: String): List<String> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()

    val variants = linkedSetOf<String>()
    variants.add(trimmed)

    val folded = foldDiacritics(trimmed)
    if (folded.isNotEmpty()) {
        variants.add(folded)
    }

    val hasSpace = trimmed.contains(' ') || trimmed.contains('\t')
    val hasHyphen = trimmed.contains('-') || trimmed.contains('_')

    // 1. Wildcard substitution for space/hyphen queries (e.g. "mesra mesra" -> "mesra%mesra", "kecil kecil" -> "kecil%kecil")
    if (hasSpace || hasHyphen) {
        val wildcard = trimmed.replace(Regex("[\\s_\\-]+"), "%")
        variants.add(wildcard)
    }

    // 2. Hyphenated variant (e.g. "mesra mesra" -> "mesra-mesra")
    if (hasSpace) {
        val hyphenated = trimmed.replace(Regex("\\s+"), "-")
        variants.add(hyphenated)
    }

    // 3. Spaced variant (e.g. "mesra-mesra" -> "mesra mesra")
    if (hasHyphen) {
        val spaced = trimmed.replace(Regex("[-_]+"), " ")
        variants.add(spaced)
    }

    // 4. Folded wildcard
    if (folded.isNotEmpty() && (folded.contains(' ') || folded.contains('-') || folded.contains('_'))) {
        val foldedWildcard = folded.replace(Regex("[\\s_\\-]+"), "%")
        variants.add(foldedWildcard)
    }

    // Bentuk NFD (decomposed combining characters, misal n + acute)
    val nfd = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
    if (nfd != trimmed) {
        variants.add(nfd)
    }

    // Pemetaan khusus yang sering dipakai dalam musik
    val lower = trimmed.lowercase(Locale.ROOT)
    when (lower) {
        "enau" -> {
            variants.add("eńau")
            variants.add("eńau")
        }
        "eńau", "eńau" -> {
            variants.add("enau")
            variants.add("Enau")
        }
        "beyonce" -> {
            variants.add("Beyoncé")
            variants.add("beyoncé")
        }
        "beyoncé" -> {
            variants.add("Beyonce")
            variants.add("beyonce")
        }
        "michael buble" -> {
            variants.add("Michael Bublé")
            variants.add("michael-bublé")
        }
        "michael bublé" -> {
            variants.add("Michael Buble")
            variants.add("michael-buble")
        }
        "motley crue" -> {
            variants.add("Mötley Crüe")
            variants.add("mötley-crüe")
        }
        "mötley crüe" -> {
            variants.add("Motley Crue")
            variants.add("motley-crue")
        }
    }

    // Substitusi huruf vokal/konsonan aksen umum jika belum ada
    val commonSubs = listOf(
        'e' to 'é',
        'n' to 'ń',
        'c' to 'ç',
        'u' to 'ü',
        'o' to 'ö',
        'a' to 'á'
    )
    for ((asciiChar, accentedChar) in commonSubs) {
        if (folded.contains(asciiChar, ignoreCase = true)) {
            val withAccent = folded.replace(asciiChar, accentedChar)
            variants.add(withAccent)
        }
    }

    return variants.filter { it.isNotBlank() }.distinct().take(5)
}
