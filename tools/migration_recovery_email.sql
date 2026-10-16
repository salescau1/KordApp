-- ==============================================================================
-- MIGRATION: RECOVERY EMAIL & SELF-SERVICE LICENSE RESET
-- KordApp v1.0+ (Supabase Database Migration)
-- ==============================================================================
-- Script ini menambahkan kolom dan fungsi RPC untuk mendukung:
-- 1. Penautan email pemulihan (recovery email) anti-typo dengan verifikasi OTP 6-digit.
-- 2. Fitur reset lisensi mandiri via tautan email jika HP lama rusak/hilang/ganti.
--
-- CARA PENGGUNAAN:
-- Buka Dashboard Supabase -> Masuk ke "SQL Editor" -> Buka query baru ->
-- Tempel (paste) seluruh isi file ini -> Klik "Run".
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. TAMBAH KOLOM KE TABEL public.licenses
-- ------------------------------------------------------------------------------
ALTER TABLE public.licenses
ADD COLUMN IF NOT EXISTS recovery_email text DEFAULT NULL,
ADD COLUMN IF NOT EXISTS email_verified boolean DEFAULT false,
ADD COLUMN IF NOT EXISTS verification_otp text DEFAULT NULL,
ADD COLUMN IF NOT EXISTS otp_expires_at timestamptz DEFAULT NULL,
ADD COLUMN IF NOT EXISTS reset_token text DEFAULT NULL,
ADD COLUMN IF NOT EXISTS reset_token_expires_at timestamptz DEFAULT NULL,
ADD COLUMN IF NOT EXISTS last_reset_at timestamptz DEFAULT NULL;

-- ------------------------------------------------------------------------------
-- 2. INDEXING UNTUK QUERY PERFORMA TINGGI
-- ------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_licenses_recovery_email 
ON public.licenses (recovery_email);

CREATE INDEX IF NOT EXISTS idx_licenses_reset_token 
ON public.licenses (reset_token);

CREATE INDEX IF NOT EXISTS idx_licenses_otp 
ON public.licenses (license_key, verification_otp);

-- ------------------------------------------------------------------------------
-- 3. STORED PROCEDURE / RPC: REQUEST EMAIL BINDING & GENERATE OTP
-- ------------------------------------------------------------------------------
-- Dipanggil saat pengguna memasukkan email pemulihan di aplikasi Android / Edge Function.
-- Menyimpan email (lowercased) dan kode OTP sementara dengan masa berlaku (default 15 menit).
CREATE OR REPLACE FUNCTION public.request_license_otp(
    p_license_key text,
    p_email text,
    p_otp text,
    p_expires_minutes int DEFAULT 15
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_clean_key text;
    v_clean_email text;
    v_row record;
BEGIN
    v_clean_key := UPPER(TRIM(p_license_key));
    v_clean_email := LOWER(TRIM(p_email));

    IF v_clean_key IS NULL OR v_clean_key = '' THEN
        RETURN json_build_object('success', false, 'message', 'Kode lisensi tidak valid.');
    END IF;

    IF v_clean_email IS NULL OR v_clean_email NOT LIKE '%_@__%.__%' THEN
        RETURN json_build_object('success', false, 'message', 'Format email tidak valid.');
    END IF;

    SELECT id, is_used, email_verified, recovery_email
    INTO v_row
    FROM public.licenses
    WHERE license_key = v_clean_key;

    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Kode lisensi tidak ditemukan di server.');
    END IF;

    -- Cegah penimpaan email jika sudah terikat & terverifikasi (Immutable Owner Identity)
    IF v_row.email_verified = true THEN
        RETURN json_build_object(
            'success', false, 
            'message', 'Lisensi ini sudah terikat secara permanen dengan email (' || v_row.recovery_email || '). Email pemilik lisensi bersifat permanen dan tidak dapat diubah.'
        );
    END IF;

    -- Update tabel licenses dengan OTP baru & email yang diajukan
    UPDATE public.licenses
    SET recovery_email = v_clean_email,
        email_verified = false,
        verification_otp = p_otp,
        otp_expires_at = NOW() + (p_expires_minutes || ' minutes')::interval
    WHERE license_key = v_clean_key;

    RETURN json_build_object(
        'success', true, 
        'message', 'Kode OTP berhasil dibuat. Segera masukkan kode 6-digit yang dikirim ke email.',
        'email', v_clean_email,
        'expires_in_minutes', p_expires_minutes
    );
END;
$$;

-- ------------------------------------------------------------------------------
-- 4. STORED PROCEDURE / RPC: VERIFY EMAIL OTP
-- ------------------------------------------------------------------------------
-- Dipanggil saat pengguna memasukkan 6-digit OTP dari email ke aplikasi Android.
-- Memastikan kode benar dan belum kadaluarsa, lalu menandai email_verified = true.
CREATE OR REPLACE FUNCTION public.verify_license_otp(
    p_license_key text,
    p_otp text
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_clean_key text;
    v_clean_otp text;
    v_row record;
BEGIN
    v_clean_key := UPPER(TRIM(p_license_key));
    v_clean_otp := TRIM(p_otp);

    SELECT id, recovery_email, verification_otp, otp_expires_at, email_verified
    INTO v_row
    FROM public.licenses
    WHERE license_key = v_clean_key;

    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Kode lisensi tidak ditemukan.');
    END IF;

    IF v_row.verification_otp IS NULL OR v_row.otp_expires_at IS NULL THEN
        RETURN json_build_object('success', false, 'message', 'Tidak ada permintaan verifikasi OTP yang aktif.');
    END IF;

    IF NOW() > v_row.otp_expires_at THEN
        RETURN json_build_object('success', false, 'message', 'Kode OTP sudah kadaluarsa (melewati batas waktu). Silakan minta kode baru.');
    END IF;

    IF v_row.verification_otp <> v_clean_otp THEN
        RETURN json_build_object('success', false, 'message', 'Kode OTP salah. Pastikan Anda memasukkan 6 digit angka yang benar.');
    END IF;

    -- Verifikasi sukses: hapus OTP dan aktifkan status terverifikasi
    UPDATE public.licenses
    SET email_verified = true,
        verification_otp = NULL,
        otp_expires_at = NULL
    WHERE id = v_row.id;

    RETURN json_build_object(
        'success', true, 
        'message', 'Email pemulihan (' || v_row.recovery_email || ') berhasil diverifikasi dan terikat ke lisensi Anda.',
        'email', v_row.recovery_email
    );
END;
$$;

-- ------------------------------------------------------------------------------
-- 5. STORED PROCEDURE / RPC: REQUEST LICENSE RESET BY VERIFIED EMAIL
-- ------------------------------------------------------------------------------
-- Dipanggil saat pengguna ingin memindahkan lisensi tetapi HP lama sudah tidak ada (rusak/hilang).
-- Mencari lisensi yang terikat dengan email yang sudah terverifikasi, lalu membuat reset token.
CREATE OR REPLACE FUNCTION public.request_license_reset_by_email(
    p_email text,
    p_reset_token text,
    p_expires_minutes int DEFAULT 30
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_clean_email text;
    v_count int;
BEGIN
    v_clean_email := LOWER(TRIM(p_email));

    SELECT id, last_reset_at
    INTO v_row
    FROM public.licenses
    WHERE recovery_email = v_clean_email
      AND email_verified = true
    LIMIT 1;

    IF v_row.id IS NULL THEN
        RETURN json_build_object(
            'success', false, 
            'message', 'Tidak ditemukan lisensi aktif yang tertaut dan terverifikasi dengan email tersebut.'
        );
    END IF;

    -- Aturan Cooldown 1x Sebulan (30 Hari)
    IF v_row.last_reset_at IS NOT NULL AND v_row.last_reset_at > NOW() - INTERVAL '30 days' THEN
        RETURN json_build_object(
            'success', false, 
            'message', 'Reset lisensi hanya dapat dilakukan 1x setiap 30 hari (1 bulan). Silakan coba lagi nanti.'
        );
    END IF;

    -- Pasang token reset ke lisensi terkait
    UPDATE public.licenses
    SET reset_token = p_reset_token,
        reset_token_expires_at = NOW() + (p_expires_minutes || ' minutes')::interval
    WHERE id = v_row.id;

    RETURN json_build_object(
        'success', true, 
        'message', 'Tautan reset lisensi berhasil disiapkan.'
    );
END;
$$;

-- ------------------------------------------------------------------------------
-- 6. STORED PROCEDURE / RPC: CONFIRM LICENSE RESET (VIA RESET TOKEN)
-- ------------------------------------------------------------------------------
-- Dipanggil saat pengguna mengklik link konfirmasi dari email mereka.
-- Melepaskan lisensi (is_used = false, device_id = NULL) sehingga bisa diaktifkan di HP baru.
CREATE OR REPLACE FUNCTION public.confirm_license_reset(
    p_reset_token text
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_clean_token text;
    v_row record;
BEGIN
    v_clean_token := TRIM(p_reset_token);

    IF v_clean_token IS NULL OR v_clean_token = '' THEN
        RETURN json_build_object('success', false, 'message', 'Token reset tidak valid.');
    END IF;

    SELECT id, license_key, recovery_email, reset_token_expires_at
    INTO v_row
    FROM public.licenses
    WHERE reset_token = v_clean_token;

    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Tautan reset tidak valid atau sudah pernah digunakan.');
    END IF;

    IF NOW() > v_row.reset_token_expires_at THEN
        RETURN json_build_object('success', false, 'message', 'Tautan reset sudah kadaluarsa (melewati batas waktu 30 menit). Silakan ajukan ulang.');
    END IF;

    -- Reset ikatan perangkat: lepaskan lisensi
    UPDATE public.licenses
    SET is_used = false,
        device_id = NULL,
        activated_at = NULL,
        reset_token = NULL,
        reset_token_expires_at = NULL,
        last_reset_at = NOW()
    WHERE id = v_row.id;

    RETURN json_build_object(
        'success', true, 
        'message', 'Lisensi berhasil dilepas dari perangkat lama! Anda sekarang dapat mengaktifkannya di perangkat baru.',
        'license_key', v_row.license_key,
        'email', v_row.recovery_email
    );
END;
$$;

-- ------------------------------------------------------------------------------
-- 7. STORED PROCEDURE / RPC: CONFIRM LICENSE RESET VIA IN-APP EMAIL OTP (ALL-IN-ONE)
-- ------------------------------------------------------------------------------
-- Dipanggil langsung dari modal di dalam aplikasi KordApp ketika pengguna memasukkan 6-digit OTP reset dari email.
CREATE OR REPLACE FUNCTION public.confirm_license_reset_by_email_otp(
    p_email text,
    p_otp text
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_clean_email text;
    v_clean_otp text;
    v_row record;
BEGIN
    v_clean_email := LOWER(TRIM(p_email));
    v_clean_otp := TRIM(p_otp);

    IF v_clean_email IS NULL OR v_clean_email = '' THEN
        RETURN json_build_object('success', false, 'message', 'Email tidak boleh kosong.');
    END IF;

    IF v_clean_otp IS NULL OR v_clean_otp = '' THEN
        RETURN json_build_object('success', false, 'message', 'Kode verifikasi tidak boleh kosong.');
    END IF;

    SELECT id, license_key, recovery_email, reset_token, reset_token_expires_at
    INTO v_row
    FROM public.licenses
    WHERE recovery_email = v_clean_email
      AND email_verified = true
      AND reset_token = v_clean_otp;

    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Kode verifikasi reset salah atau tidak sesuai.');
    END IF;

    IF NOW() > v_row.reset_token_expires_at THEN
        RETURN json_build_object('success', false, 'message', 'Kode verifikasi reset telah kadaluarsa (melewati batas waktu 30 menit). Silakan minta kode baru.');
    END IF;

    -- Reset ikatan perangkat: lepaskan lisensi dari HP lama
    UPDATE public.licenses
    SET is_used = false,
        device_id = NULL,
        activated_at = NULL,
        reset_token = NULL,
        reset_token_expires_at = NULL,
        last_reset_at = NOW()
    WHERE id = v_row.id;

    RETURN json_build_object(
        'success', true, 
        'message', 'Perangkat lama berhasil dilepas! Lisensi siap diaktifkan.',
        'license_key', v_row.license_key,
        'email', v_row.recovery_email
    );
END;
$$;

-- ------------------------------------------------------------------------------
-- 8. STORED PROCEDURE / RPC: ACTIVATE LICENSE (RETURNING RECOVERY EMAIL INFO)
-- ------------------------------------------------------------------------------
-- Mengaktifkan lisensi pada perangkat baru dan mengembalikan info email pemilik permanen
CREATE OR REPLACE FUNCTION public.activate_license(
    p_license_key text,
    p_device_id text
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_clean_key text;
    v_clean_device text;
    v_row record;
BEGIN
    v_clean_key := UPPER(TRIM(p_license_key));
    v_clean_device := TRIM(p_device_id);

    IF v_clean_key IS NULL OR v_clean_key = '' THEN
        RETURN json_build_object('success', false, 'message', 'Kode lisensi tidak valid.');
    END IF;

    IF v_clean_device IS NULL OR v_clean_device = '' THEN
        RETURN json_build_object('success', false, 'message', 'Device ID tidak valid.');
    END IF;

    SELECT id, is_used, device_id, recovery_email, email_verified
    INTO v_row
    FROM public.licenses
    WHERE license_key = v_clean_key;

    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Kode lisensi tidak valid atau tidak ditemukan.');
    END IF;

    IF v_row.is_used AND v_row.device_id <> v_clean_device THEN
        RETURN json_build_object(
            'success', false, 
            'message', 'Lisensi ini sedang aktif di perangkat lain. Silakan reset lisensi via menu "Reset Lisensi Mandiri" di aplikasi jika Anda berganti perangkat.'
        );
    END IF;

    -- Aktivasi berhasil atau re-aktivasi perangkat yang sama
    UPDATE public.licenses
    SET is_used = true,
        device_id = v_clean_device,
        activated_at = COALESCE(activated_at, NOW())
    WHERE id = v_row.id;

    RETURN json_build_object(
        'success', true,
        'message', 'Lisensi berhasil diaktifkan!',
        'license_key', v_clean_key,
        'recovery_email', COALESCE(v_row.recovery_email, ''),
        'email_verified', COALESCE(v_row.email_verified, false)
    );
END;
$$;

-- ------------------------------------------------------------------------------
-- 9. GRANT PERMISSION KE ANON & AUTHENTICATED
-- ------------------------------------------------------------------------------
GRANT EXECUTE ON FUNCTION public.activate_license(text, text) TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.request_license_otp(text, text, text, int) TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.verify_license_otp(text, text) TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.request_license_reset_by_email(text, text, int) TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.confirm_license_reset(text) TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.confirm_license_reset_by_email_otp(text, text) TO anon, authenticated, service_role;

-- ------------------------------------------------------------------------------
-- 10. RELOAD SCHEMA CACHE SUPABASE
-- ------------------------------------------------------------------------------
NOTIFY pgrst, 'reload schema';
