package com.chordapp.catalog

object SongVersionCurator {

    /**
     * Menyederhanakan dan menyortir daftar versi lagu agar tidak membingungkan:
     * - GT: Maksimal 2 versi -> "Original" dan "Easy" (tampil sebagai GT Original & GT Easy)
     * - UG: Maksimal 2 versi -> Top Votes & Best Rating (misal "4 (Top Votes)" & "5 (Best Rating)")
     * - CI: Maksimal 1 versi -> "Standard" (tampil sebagai CI Standard)
     * - CT: Maksimal 1 versi -> "Standard" (tampil sebagai CT Standard)
     */
    fun curate(versions: List<CatalogSongEntity>): List<CatalogSongEntity> {
        if (versions.isEmpty()) return emptyList()

        val result = mutableListOf<CatalogSongEntity>()

        // 1. Process GT (GuitarTuna) - Maksimal 2 versi: Original & Easy
        val gtVersions = versions.filter { isGt(it.source) }
        if (gtVersions.isNotEmpty()) {
            val original = gtVersions.firstOrNull { 
                it.versionName?.contains("Original", ignoreCase = true) == true ||
                it.sourceUrl.contains("original", ignoreCase = true)
            } ?: gtVersions.firstOrNull {
                it.versionName?.contains("Easy", ignoreCase = true) != true &&
                it.difficulty?.equals("Beginner", ignoreCase = true) != true
            } ?: gtVersions.first()

            result.add(original.copy(versionName = "Original"))

            val easy = gtVersions.firstOrNull {
                it.sourceUrl != original.sourceUrl && (
                    it.versionName?.contains("Easy", ignoreCase = true) == true ||
                    it.difficulty?.equals("Beginner", ignoreCase = true) == true ||
                    it.sourceUrl.contains("easy", ignoreCase = true)
                )
            }
            if (easy != null) {
                result.add(easy.copy(versionName = "Easy"))
            }
        }

        // 2. Process UG (Ultimate Guitar) - Maksimal 2 versi: Top Votes & Best Rating
        val ugVersions = versions.filter { isUg(it.source) }
        if (ugVersions.isNotEmpty()) {
            // Versi 1: Top Votes
            val topVotes = ugVersions.maxByOrNull { it.votes ?: 0 } ?: ugVersions.first()
            val topVotesNum = extractVersionNumber(topVotes)
            val topVotesLabel = if (topVotesNum != null) "$topVotesNum (Top Votes)" else "(Top Votes)"
            result.add(topVotes.copy(versionName = topVotesLabel))

            // Versi 2: Best Rating (dari sisa versi)
            val remainingUg = ugVersions.filter { it.sourceUrl != topVotes.sourceUrl }
            if (remainingUg.isNotEmpty()) {
                val bestRating = remainingUg.maxWithOrNull(
                    compareBy<CatalogSongEntity> { it.rating ?: 0f }
                        .thenBy { it.votes ?: 0 }
                ) ?: remainingUg.first()
                val bestRatingNum = extractVersionNumber(bestRating)
                val bestRatingLabel = if (bestRatingNum != null) "$bestRatingNum (Best Rating)" else "(Best Rating)"
                result.add(bestRating.copy(versionName = bestRatingLabel))
            }
        }

        // 3. Process CI (ChordIndonesia) - Maksimal 1 versi: Standard
        val ciVersions = versions.filter { isCi(it.source) }
        if (ciVersions.isNotEmpty()) {
            val ciBest = ciVersions.maxByOrNull { it.votes ?: 0 } ?: ciVersions.first()
            result.add(ciBest.copy(versionName = "Standard"))
        }

        // 4. Process CT (ChordTela) - Maksimal 1 versi: Standard
        val ctVersions = versions.filter { isCt(it.source) }
        if (ctVersions.isNotEmpty()) {
            val ctBest = ctVersions.maxByOrNull { it.votes ?: 0 } ?: ctVersions.first()
            result.add(ctBest.copy(versionName = "Standard"))
        }

        // 5. Sumber lainnya
        val otherVersions = versions.filter { 
            !isGt(it.source) && !isUg(it.source) && !isCi(it.source) && !isCt(it.source) 
        }
        for (v in otherVersions.take(2)) {
            result.add(v)
        }

        return result
    }

    private fun isGt(source: String): Boolean =
        source.contains("GuitarTuna", ignoreCase = true) || source.contains("Guitar Tuna", ignoreCase = true) || source.equals("GT", ignoreCase = true)

    private fun isUg(source: String): Boolean =
        source.contains("Ultimate", ignoreCase = true) || source.equals("UG", ignoreCase = true)

    private fun isCi(source: String): Boolean =
        source.contains("ChordIndonesia", ignoreCase = true) || source.contains("Chord Indonesia", ignoreCase = true) || source.equals("CI", ignoreCase = true)

    private fun isCt(source: String): Boolean =
        source.contains("ChordTela", ignoreCase = true) || source.contains("Chord Tela", ignoreCase = true) || source.equals("CT", ignoreCase = true)

    private fun extractVersionNumber(entity: CatalogSongEntity): String? {
        val nameMatch = Regex("(?i)Ver\\.?\\s*(\\d+)").find(entity.versionName ?: "")
        if (nameMatch != null) return nameMatch.groupValues[1]

        val anyDigitInName = Regex("\\b(\\d+)\\b").find(entity.versionName ?: "")
        if (anyDigitInName != null) return anyDigitInName.groupValues[1]

        val urlMatch = Regex("(?i)(?:-chords-|_chords_|_|ver-)(\\d+)").find(entity.sourceUrl)
        if (urlMatch != null) return urlMatch.groupValues[1]

        return null
    }
}
