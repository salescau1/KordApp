package com.chordapp.repositories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LicenseRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        LicenseRepository.revokeLicense(context)
    }

    @Test
    fun testInitialLicenseStateIsUnlicensed() {
        assertFalse("New install should not be licensed", LicenseRepository.isLicensed(context))
        assertNull("Saved key should initially be null", LicenseRepository.getSavedLicenseKey(context))
    }

    @Test
    fun testRecoveryEmailLocalPersistence() {
        assertNull("Initially recovery email should be null", LicenseRepository.getSavedRecoveryEmail(context))
        assertFalse("Initially email should not be verified", LicenseRepository.isEmailVerified(context))

        LicenseRepository.saveRecoveryEmailLocally(context, "Musician@KordApp.com", false)
        assertEquals("musician@kordapp.com", LicenseRepository.getSavedRecoveryEmail(context))
        assertFalse(LicenseRepository.isEmailVerified(context))

        LicenseRepository.saveRecoveryEmailLocally(context, "musician@kordapp.com", true)
        assertTrue(LicenseRepository.isEmailVerified(context))
    }

    @Test
    fun testDismissedBindEmailPrompt() {
        assertFalse(LicenseRepository.isBindEmailPromptDismissed(context))
        LicenseRepository.setBindEmailPromptDismissed(context, true)
        assertTrue(LicenseRepository.isBindEmailPromptDismissed(context))
    }

    @Test
    fun testSendOtpEmailViaResendWithInvalidEmailReturnsFalse() = runBlocking {
        val resultEmpty = LicenseRepository.sendOtpEmailViaResend("", "123456", "Test Subject")
        assertFalse("Empty email should fail validation immediately", resultEmpty)

        val resultNoAt = LicenseRepository.sendOtpEmailViaResend("invalid-email.com", "123456", "Test Subject")
        assertFalse("Email without @ should fail validation immediately", resultNoAt)
    }

    @Test
    fun testRequestEmailBindingOtpWithInvalidEmailReturnsError() = runBlocking {
        // Without active license
        val (okNoLicense, msgNoLicense) = LicenseRepository.requestEmailBindingOtp(context, "test@example.com")
        assertFalse(okNoLicense)
        assertEquals("Lisensi belum aktif.", msgNoLicense)
    }

    @Test
    fun testRequestLicenseResetByEmailWithInvalidEmailReturnsError() = runBlocking {
        val (okEmpty, msgEmpty) = LicenseRepository.requestLicenseResetByEmail("")
        assertFalse(okEmpty)
        assertEquals("Format email tidak valid.", msgEmpty)

        val (okInvalid, msgInvalid) = LicenseRepository.requestLicenseResetByEmail("notanemail")
        assertFalse(okInvalid)
        assertEquals("Format email tidak valid.", msgInvalid)
    }

    @Test
    fun testConfirmLicenseResetByEmailOtpWithBlankInputsReturnsError() = runBlocking {
        val (okBlankEmail, _, msgBlankEmail) = LicenseRepository.confirmLicenseResetByEmailOtp("", "123456")
        assertFalse(okBlankEmail)
        assertEquals("Format email tidak valid.", msgBlankEmail)

        val (okBlankOtp, _, msgBlankOtp) = LicenseRepository.confirmLicenseResetByEmailOtp("user@example.com", "")
        assertFalse(okBlankOtp)
        assertEquals("Kode OTP tidak boleh kosong.", msgBlankOtp)
    }

    @Test
    fun testMaskEmail() {
        assertEquals("-", LicenseRepository.maskEmail(null))
        assertEquals("-", LicenseRepository.maskEmail(""))
        assertEquals("-", LicenseRepository.maskEmail("invalid-email"))
        assertEquals("k***@gmail.com", LicenseRepository.maskEmail("kek@gmail.com"))
        assertEquals("yau***@domain.com", LicenseRepository.maskEmail("yaumil@domain.com"))
    }

    @Test
    fun testVerifiedEmailStateOnNewDevice() {
        // Simulasi HP baru setelah aktivasi lisensi yang sudah memiliki email_verified di server
        LicenseRepository.saveRecoveryEmailLocally(context, "owner@guitar.com", true)
        LicenseRepository.setBindEmailPromptDismissed(context, true)

        assertTrue("Recovery email harus terverifikasi", LicenseRepository.isEmailVerified(context))
        assertTrue("Prompt bind email harus dismissed", LicenseRepository.isBindEmailPromptDismissed(context))
        assertEquals("owner@guitar.com", LicenseRepository.getSavedRecoveryEmail(context))
    }
}
