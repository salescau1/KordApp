package com.chordapp.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiacriticsTest {

    @Test
    fun testFoldDiacritics() {
        assertEquals("enau", foldDiacritics("eńau"))
        assertEquals("enau", foldDiacritics("eńau"))
        assertEquals("Beyonce", foldDiacritics("Beyoncé"))
        assertEquals("Michael Buble", foldDiacritics("Michael Bublé"))
        assertEquals("Motley Crue", foldDiacritics("Mötley Crüe"))
        assertEquals("Bjork", foldDiacritics("Björk"))
        assertEquals("Sigur Ros", foldDiacritics("Sigur Rós"))
    }

    @Test
    fun testContainsIgnoreDiacritics() {
        // Search "enau" matches "eńau" and vice versa
        assertTrue(containsIgnoreDiacritics("eńau", "enau"))
        assertTrue(containsIgnoreDiacritics("Enau", "eńau"))
        assertTrue(containsIgnoreDiacritics("Whisnu Santika, eńau", "enau"))
        assertTrue(containsIgnoreDiacritics("eńau feat. Ari Lesmana", "enau"))

        // Search "beyonce" matches "Beyoncé" and vice versa
        assertTrue(containsIgnoreDiacritics("Beyoncé", "beyonce"))
        assertTrue(containsIgnoreDiacritics("Beyonce", "beyoncé"))
        assertTrue(containsIgnoreDiacritics("Beyoncé & Post Malone", "beyonce"))

        // Search "michael buble" matches "Michael Bublé" and vice versa
        assertTrue(containsIgnoreDiacritics("Michael Bublé", "michael buble"))
        assertTrue(containsIgnoreDiacritics("Michael Buble", "michael bublé"))

        // Search "motley crue" matches "Mötley Crüe"
        assertTrue(containsIgnoreDiacritics("Mötley Crüe", "motley crue"))
    }

    @Test
    fun testGenerateSearchVariants() {
        val enauVariants = generateSearchVariants("enau")
        assertTrue(enauVariants.contains("enau"))
        assertTrue(enauVariants.any { it.contains("ń") || it.contains("ń") })

        val enauAccentedVariants = generateSearchVariants("eńau")
        assertTrue(enauAccentedVariants.contains("eńau"))
        assertTrue(enauAccentedVariants.contains("enau"))

        val beyonceVariants = generateSearchVariants("beyonce")
        assertTrue(beyonceVariants.contains("beyonce"))
        assertTrue(beyonceVariants.any { it.contains("é") || it.contains("Beyoncé") })

        val bubleVariants = generateSearchVariants("michael buble")
        assertTrue(bubleVariants.contains("michael buble"))
        assertTrue(bubleVariants.any { it.contains("é") })

        val mesraVariants = generateSearchVariants("mesra mesra")
        assertTrue(mesraVariants.contains("mesra mesra"))
        assertTrue(mesraVariants.contains("mesra%mesra"))
        assertTrue(mesraVariants.contains("mesra-mesra"))

        val kecilVariants = generateSearchVariants("kecil kecil")
        assertTrue(kecilVariants.contains("kecil kecil"))
        assertTrue(kecilVariants.contains("kecil%kecil"))
        assertTrue(kecilVariants.contains("kecil-kecil"))
    }
}
