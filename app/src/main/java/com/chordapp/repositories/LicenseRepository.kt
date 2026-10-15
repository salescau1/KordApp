package com.chordapp.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Pengelola lisensi KordApp & pencatat perangkat (Supabase).
 */
object LicenseRepository {

    private const val PREFS_NAME = "kkc_kordapp_license_prefs"
    private const val KEY_IS_LICENSED = "is_licensed"
    private const val KEY_LICENSE_KEY = "license_key"

    private const val SUPABASE_URL = "https://rhgcsgrdqbhimpdloxgo.supabase.co/rest/v1"
    private const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJoZ2NzZ3JkcWJoaW1wZGxveGdvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEzMDAzNjksImV4cCI6MjEwNjg3NjM2OX0.JKlFUjdom0M0WV62xYvCn9vCq23O9UPisXdPdCCHGtk"

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
    }

    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty()
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }

    fun getOsVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    fun getAppVersion(context: Context): String {
        return runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        }.getOrDefault("1.0")
    }

    private const val KEY_SIGNATURE = "license_signature"
    private const val SECRET_SALT = "KKC_KORDAPP_SECURE_TOKEN_v1_#9824_X!"

    private fun computeSignature(deviceId: String, key: String): String {
        val raw = "$deviceId|$key|$SECRET_SALT"
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(raw.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isLicensed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFlagTrue = prefs.getBoolean(KEY_IS_LICENSED, false)
        if (!isFlagTrue) return false

        val savedKey = prefs.getString(KEY_LICENSE_KEY, null) ?: return false
        val savedSig = prefs.getString(KEY_SIGNATURE, null) ?: return false
        val deviceId = getDeviceId(context)

        // Verifikasi keaslian tanda tangan digital dengan Device ID HP ini
        val expectedSig = computeSignature(deviceId, savedKey)
        return savedSig == expectedSig
    }

    fun getSavedLicenseKey(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LICENSE_KEY, null)
    }

    private fun saveLicenseSuccess(context: Context, key: String) {
        val cleanKey = key.trim().uppercase()
        val deviceId = getDeviceId(context)
        val signature = computeSignature(deviceId, cleanKey)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_LICENSED, true)
            .putString(KEY_LICENSE_KEY, cleanKey)
            .putString(KEY_SIGNATURE, signature)
            .apply()
    }

    fun revokeLicense(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Cek validasi lisensi secara diam-diam jika HP sedang terhubung ke internet.
     * Jika di Supabase lisensi di-reset atau dipindah ke HP lain, cabut akses lokal di HP ini.
     * Jika offline, fungsi ini tidak mengunci aplikasi (tetap offline-ready).
     */
    suspend fun syncLicenseStatusIfOnline(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!isLicensed(context)) return@withContext false
        val savedKey = getSavedLicenseKey(context) ?: return@withContext false
        val deviceId = getDeviceId(context)

        runCatching {
            val encodedKey = java.net.URLEncoder.encode(savedKey, "UTF-8")
            val url = URL("$SUPABASE_URL/licenses?license_key=eq.$encodedKey&select=is_used,device_id")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4_000
                readTimeout = 4_000
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }

            val status = conn.responseCode
            if (status in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val arr = org.json.JSONArray(responseText)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val isUsed = obj.optBoolean("is_used", false)
                    val boundDevice = obj.optString("device_id", "")

                    // Jika lisensi di-reset oleh admin atau berpindah ke HP lain
                    if (!isUsed || boundDevice != deviceId) {
                        revokeLicense(context)
                        return@withContext false
                    }
                } else {
                    // Lisensi dihapus di server
                    revokeLicense(context)
                    return@withContext false
                }
            } else {
                conn.disconnect()
            }
            true
        }.getOrElse {
            // Jika sedang offline atau timeout, TETAP IZINKAN OFFLINE
            true
        }
    }

    /**
     * Lepas lisensi dari perangkat ini secara mandiri agar bisa dipakai di HP lain.
     */
    suspend fun transferLicenseSelfService(context: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val savedKey = getSavedLicenseKey(context) ?: return@withContext Pair(false, "Tidak ada lisensi tersimpan.")
        val deviceId = getDeviceId(context)

        runCatching {
            val encodedKey = java.net.URLEncoder.encode(savedKey, "UTF-8")
            val url = URL("$SUPABASE_URL/licenses?license_key=eq.$encodedKey&device_id=eq.$deviceId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }

            val body = JSONObject().apply {
                put("is_used", false)
                put("device_id", JSONObject.NULL)
                put("activated_at", JSONObject.NULL)
            }

            conn.outputStream.use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { it.write(body.toString()) }
            }

            val status = conn.responseCode
            conn.disconnect()

            if (status in 200..299) {
                revokeLicense(context)
                Pair(true, "Lisensi berhasil dilepas. Kode Anda: $savedKey (Simpan untuk HP baru).")
            } else {
                Pair(false, "Gagal melepas lisensi dari server (Error $status). Periksa koneksi internet.")
            }
        }.getOrElse { error ->
            Pair(false, "Koneksi gagal: ${error.localizedMessage ?: "Periksa internet Anda."}")
        }
    }

    /**
     * Catat perangkat ke Supabase devices table via RPC.
     * Dijalankan di background thread (Dispatchers.IO).
     */
    suspend fun recordDevice(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$SUPABASE_URL/rpc/record_device")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }

            val body = JSONObject().apply {
                put("p_device_id", getDeviceId(context))
                put("p_device_model", getDeviceModel())
                put("p_os_version", getOsVersion())
                put("p_app_version", getAppVersion(context))
            }

            conn.outputStream.use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { it.write(body.toString()) }
            }

            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        }.getOrDefault(false)
    }

    private const val KEY_RECOVERY_EMAIL = "recovery_email"
    private const val KEY_EMAIL_VERIFIED = "email_verified"
    private const val KEY_DISMISSED_BIND_EMAIL_PROMPT = "dismissed_bind_email_prompt"

    fun getSavedRecoveryEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_RECOVERY_EMAIL, null)
    }

    fun isEmailVerified(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_EMAIL_VERIFIED, false)
    }

    fun saveRecoveryEmailLocally(context: Context, email: String, verified: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_RECOVERY_EMAIL, email.trim().lowercase())
            .putBoolean(KEY_EMAIL_VERIFIED, verified)
            .apply()
    }

    fun isBindEmailPromptDismissed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DISMISSED_BIND_EMAIL_PROMPT, false)
    }

    fun setBindEmailPromptDismissed(context: Context, dismissed: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DISMISSED_BIND_EMAIL_PROMPT, dismissed).apply()
    }

    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank() || !email.contains("@")) return "-"
        val namePart = email.substringBefore("@")
        val domainPart = email.substringAfter("@")
        val visibleCount = if (namePart.length <= 3) 1 else 3
        return namePart.take(visibleCount) + "***@" + domainPart
    }

    /**
     * Aktivasi kode lisensi ke Supabase.
     * Mengembalikan Result: Pair(isSuccess, message).
     * Otomatis menormalisasi format 3-segmen atau 4-segmen (menambahkan dash jika input mentah).
     */
    suspend fun activateLicense(context: Context, rawKey: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val cleanKey = rawKey.trim().uppercase()
            if (cleanKey.isEmpty()) {
                return@withContext Pair(false, "Kode lisensi tidak boleh kosong!")
            }

            // Normalisasi otomatis jika input alfanumerik tanpa dash (12 atau 16 karakter)
            val normalizedKey = if (!cleanKey.contains("-") && (cleanKey.length == 12 || cleanKey.length == 16)) {
                cleanKey.chunked(4).joinToString("-")
            } else {
                cleanKey
            }

            val deviceId = getDeviceId(context)

            runCatching {
                val url = URL("$SUPABASE_URL/rpc/activate_license")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                }

                val body = JSONObject().apply {
                    put("p_license_key", normalizedKey)
                    put("p_device_id", deviceId)
                }

                conn.outputStream.use { os ->
                    OutputStreamWriter(os, StandardCharsets.UTF_8).use { it.write(body.toString()) }
                }

                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect()

                if (status in 200..299 && responseText.isNotEmpty()) {
                    val json = JSONObject(responseText)
                    val success = json.optBoolean("success", false)
                    val message = json.optString("message", "Respon server tidak valid.")
                    if (success) {
                        saveLicenseSuccess(context, normalizedKey)
                        recordDevice(context)
                    }
                    Pair(success, message)
                } else {
                    Pair(false, "Gagal menghubungi server (Error $status). Periksa koneksi internet.")
                }
            }.getOrElse { error ->
                Pair(false, "Gagal koneksi: ${error.localizedMessage ?: "Periksa internet Anda."}")
            }
        }

    /**
     * Mengajukan penautan email pemulihan ke server Supabase via RPC request_license_otp.
     * Mengembalikan Pair(isSuccess, message).
     */
    suspend fun requestEmailBindingOtp(
        context: Context,
        email: String,
        generatedOtp: String = (100000..999999).random().toString()
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val savedKey = getSavedLicenseKey(context) ?: return@withContext Pair(false, "Lisensi belum aktif.")
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return@withContext Pair(false, "Format email tidak valid.")
        }

        runCatching {
            val url = URL("$SUPABASE_URL/rpc/request_license_otp")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }

            val body = JSONObject().apply {
                put("p_license_key", savedKey)
                put("p_email", cleanEmail)
                put("p_otp", generatedOtp)
                put("p_expires_minutes", 15)
            }

            conn.outputStream.use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { it.write(body.toString()) }
            }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()

            if (status in 200..299 && responseText.isNotEmpty()) {
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false)
                val message = json.optString("message", "Respon server tidak valid.")
                if (success) {
                    saveRecoveryEmailLocally(context, cleanEmail, false)
                }
                Pair(success, message)
            } else {
                Pair(false, "Gagal request OTP (Error $status). Periksa koneksi internet.")
            }
        }.getOrElse { error ->
            Pair(false, "Koneksi gagal: ${error.localizedMessage ?: "Periksa internet Anda."}")
        }
    }

    /**
     * Memverifikasi kode OTP 6-digit untuk email pemulihan via RPC verify_license_otp.
     * Mengembalikan Pair(isSuccess, message).
     */
    suspend fun verifyEmailOtp(
        context: Context,
        otp: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val savedKey = getSavedLicenseKey(context) ?: return@withContext Pair(false, "Lisensi belum aktif.")
        val cleanOtp = otp.trim()
        if (cleanOtp.length != 6) {
            return@withContext Pair(false, "Kode OTP harus berupa 6 digit angka.")
        }

        runCatching {
            val url = URL("$SUPABASE_URL/rpc/verify_license_otp")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }

            val body = JSONObject().apply {
                put("p_license_key", savedKey)
                put("p_otp", cleanOtp)
            }

            conn.outputStream.use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { it.write(body.toString()) }
            }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()

            if (status in 200..299 && responseText.isNotEmpty()) {
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false)
                val message = json.optString("message", "Respon server tidak valid.")
                if (success) {
                    val verifiedEmail = json.optString("email", getSavedRecoveryEmail(context) ?: "")
                    if (verifiedEmail.isNotEmpty()) {
                        saveRecoveryEmailLocally(context, verifiedEmail, true)
                    }
                }
                Pair(success, message)
            } else {
                Pair(false, "Gagal verifikasi OTP (Error $status). Periksa koneksi internet.")
            }
        }.getOrElse { error ->
            Pair(false, "Koneksi gagal: ${error.localizedMessage ?: "Periksa internet Anda."}")
        }
    }

    /**
     * Permintaan reset lisensi mandiri berbasis email pemulihan terverifikasi via RPC request_license_reset_by_email.
     */
    suspend fun requestLicenseResetByEmail(
        email: String,
        resetToken: String = java.util.UUID.randomUUID().toString()
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return@withContext Pair(false, "Format email tidak valid.")
        }

        runCatching {
            val url = URL("$SUPABASE_URL/rpc/request_license_reset_by_email")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }

            val body = JSONObject().apply {
                put("p_email", cleanEmail)
                put("p_reset_token", resetToken)
                put("p_expires_minutes", 30)
            }

            conn.outputStream.use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { it.write(body.toString()) }
            }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()

            if (status in 200..299 && responseText.isNotEmpty()) {
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false)
                val message = json.optString("message", "Respon server tidak valid.")
                Pair(success, message)
            } else {
                Pair(false, "Gagal permintaan reset (Error $status). Periksa koneksi internet.")
            }
        }.getOrElse { error ->
            Pair(false, "Koneksi gagal: ${error.localizedMessage ?: "Periksa internet Anda."}")
        }
    }

    /**
     * Konfirmasi eksekusi reset lisensi via token unik reset (bisa dipanggil dari web link atau deep link).
     */
    suspend fun confirmLicenseReset(resetToken: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanToken = resetToken.trim()
        if (cleanToken.isEmpty()) {
            return@withContext Pair(false, "Token reset tidak boleh kosong.")
        }

        runCatching {
            val url = URL("$SUPABASE_URL/rpc/confirm_license_reset")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }

            val body = JSONObject().apply {
                put("p_reset_token", cleanToken)
            }

            conn.outputStream.use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { it.write(body.toString()) }
            }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()

            if (status in 200..299 && responseText.isNotEmpty()) {
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false)
                val message = json.optString("message", "Respon server tidak valid.")
                Pair(success, message)
            } else {
                Pair(false, "Gagal konfirmasi reset (Error $status).")
            }
        }.getOrElse { error ->
            Pair(false, "Koneksi gagal: ${error.localizedMessage ?: "Periksa internet Anda."}")
        }
    }

    /**
     * Konfirmasi reset lisensi dari HP lama via 6-digit OTP email pemulihan langsung di dalam aplikasi (All-in-One).
     * Mengembalikan Triple(isSuccess, licenseKey, message).
     */
    suspend fun confirmLicenseResetByEmailOtp(
        email: String,
        otp: String
    ): Triple<Boolean, String, String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanOtp = otp.trim()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return@withContext Triple(false, "", "Format email tidak valid.")
        }
        if (cleanOtp.isEmpty()) {
            return@withContext Triple(false, "", "Kode OTP tidak boleh kosong.")
        }

        runCatching {
            val url = URL("$SUPABASE_URL/rpc/confirm_license_reset_by_email_otp")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }

            val body = JSONObject().apply {
                put("p_email", cleanEmail)
                put("p_otp", cleanOtp)
            }

            conn.outputStream.use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { it.write(body.toString()) }
            }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()

            if (status in 200..299 && responseText.isNotEmpty()) {
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false)
                val licenseKey = json.optString("license_key", "")
                val message = json.optString("message", "Respon server tidak valid.")
                Triple(success, licenseKey, message)
            } else {
                Triple(false, "", "Gagal konfirmasi reset (Error $status). Periksa koneksi internet.")
            }
        }.getOrElse { error ->
            Triple(false, "", "Koneksi gagal: ${error.localizedMessage ?: "Periksa internet Anda."}")
        }
    }
}
