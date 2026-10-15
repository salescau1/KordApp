package com.chordapp.ui.components

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class LicenseKeyVisualTransformationTest {

    private val transformation = LicenseKeyVisualTransformation()

    @Test
    fun testSanitizerRemovesSpecialCharsAndConvertsUppercase() {
        assertEquals("KORDABCD0001SGHI", LicenseKeyVisualTransformation.sanitize("kord-abcd-0001-sghi"))
        assertEquals("KORDABCD0001SGHI", LicenseKeyVisualTransformation.sanitize("kord abcd 0001 sghi"))
        assertEquals("KORDABCD0001SGHI", LicenseKeyVisualTransformation.sanitize("kord.abcd.0001.sghi"))
        assertEquals("KORD78294410", LicenseKeyVisualTransformation.sanitize("kord-7829-4410"))
        // Max 16 characters truncation
        assertEquals("KORDABCD0001SGHI", LicenseKeyVisualTransformation.sanitize("kordabcd0001sghiextrachars"))
    }

    @Test
    fun testFormatWithDashes() {
        assertEquals("KORD-7829-4410", LicenseKeyVisualTransformation.formatWithDashes("kord78294410"))
        assertEquals("KORD-ABCD-0001-SGHI", LicenseKeyVisualTransformation.formatWithDashes("kordabcd0001sghi"))
    }

    @Test
    fun testVisualTransformationFormatting() {
        // Empty
        assertEquals("", transformation.filter(AnnotatedString("")).text.text)
        
        // 1 to 4 chars (no dash yet)
        assertEquals("K", transformation.filter(AnnotatedString("K")).text.text)
        assertEquals("KORD", transformation.filter(AnnotatedString("KORD")).text.text)

        // 5 chars (dash inserted after 4)
        assertEquals("KORD-A", transformation.filter(AnnotatedString("KORDA")).text.text)

        // 8 chars (dash only between segment 1 and 2, no trailing dash)
        assertEquals("KORD-ABCD", transformation.filter(AnnotatedString("KORDABCD")).text.text)

        // 9 chars
        assertEquals("KORD-ABCD-0", transformation.filter(AnnotatedString("KORDABCD0")).text.text)

        // 12 chars (3-segment license: KORD-7829-4410)
        assertEquals("KORD-7829-4410", transformation.filter(AnnotatedString("KORD78294410")).text.text)

        // 13 chars
        assertEquals("KORD-ABCD-0001-S", transformation.filter(AnnotatedString("KORDABCD0001S")).text.text)

        // 16 chars (4-segment license: KORD-ABCD-0001-SGHI)
        assertEquals("KORD-ABCD-0001-SGHI", transformation.filter(AnnotatedString("KORDABCD0001SGHI")).text.text)
    }

    @Test
    fun testOffsetMappingFourSegments() {
        val raw = "KORDABCD0001SGHI"
        val transformed = transformation.filter(AnnotatedString(raw))
        val mapping = transformed.offsetMapping
        assertEquals("KORD-ABCD-0001-SGHI", transformed.text.text)

        // Original to Transformed mapping
        assertEquals(0, mapping.originalToTransformed(0)) // "|K" -> "|K"
        assertEquals(1, mapping.originalToTransformed(1)) // "K|O" -> "K|O"
        assertEquals(5, mapping.originalToTransformed(4)) // "KORD|" in 16-char string maps to after first dash "KORD-|A"
        assertEquals(6, mapping.originalToTransformed(5)) // "KORDA|" -> "KORD-A|"
        assertEquals(10, mapping.originalToTransformed(8)) // after 'ABCD' maps to after second dash "KORD-ABCD-|0"
        assertEquals(11, mapping.originalToTransformed(9)) // after '0'
        assertEquals(15, mapping.originalToTransformed(12)) // after '0001' maps to after third dash "KORD-ABCD-0001-|S"
        assertEquals(19, mapping.originalToTransformed(16)) // end of string: "KORD-ABCD-0001-SGHI|"

        // Transformed to Original mapping
        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(1, mapping.transformedToOriginal(1))
        assertEquals(4, mapping.transformedToOriginal(4)) // before '-' -> offset 4
        assertEquals(4, mapping.transformedToOriginal(5)) // after '-' -> offset 4
        assertEquals(5, mapping.transformedToOriginal(6)) // after 'A' -> offset 5
        assertEquals(8, mapping.transformedToOriginal(9)) // before second '-' -> offset 8
        assertEquals(8, mapping.transformedToOriginal(10)) // after second '-' -> offset 8
        assertEquals(9, mapping.transformedToOriginal(11))
        assertEquals(12, mapping.transformedToOriginal(14)) // before third '-' -> offset 12
        assertEquals(12, mapping.transformedToOriginal(15)) // after third '-' -> offset 12
        assertEquals(16, mapping.transformedToOriginal(19)) // end
    }

    @Test
    fun testOffsetMappingThreeSegments() {
        val raw = "KORD78294410"
        val transformed = transformation.filter(AnnotatedString(raw))
        val mapping = transformed.offsetMapping
        assertEquals("KORD-7829-4410", transformed.text.text)

        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(3, mapping.originalToTransformed(3))
        assertEquals(5, mapping.originalToTransformed(4))
        assertEquals(14, mapping.originalToTransformed(12))

        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(4, mapping.transformedToOriginal(4))
        assertEquals(4, mapping.transformedToOriginal(5))
        assertEquals(12, mapping.transformedToOriginal(14))
    }
}
