-- ==============================================================================
-- UPDATE: PERMANENT EMAIL BINDING & SEAMLESS DEVICE MIGRATION
-- ==============================================================================
-- 1. Lisensi terkunci permanen ke 1 email (sekali verified tidak bisa diubah).
-- 2. Reset Key HANYA mencabut kuncian hardware HP lama (device_id = NULL).
-- 3. Saat HP baru mengaktifkan lisensi, recovery_email & email_verified langsung dikembalikan
--    sehingga HP baru langsung mengenali email pemilik tanpa pop-up bind lagi.
-- ==============================================================================

-- 1. UPDATE FUNCTION activate_license (Return recovery_email & email_verified)
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
        RETURN json_build_object('success', false, 'message', 'Kode lisensi tidak boleh kosong.');
    END IF;

    SELECT id, license_key, is_used, device_id, recovery_email, email_verified
    INTO v_row
    FROM public.licenses
    WHERE license_key = v_clean_key;

    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Kode lisensi tidak ditemukan di server.');
    END IF;

    -- Jika lisensi sedang aktif di perangkat lain
    IF v_row.is_used = true AND v_row.device_id IS NOT NULL AND v_row.device_id <> v_clean_device THEN
        RETURN json_build_object(
            'success', false, 
            'message', 'Lisensi ini sedang aktif di perangkat lain. Silakan gunakan fitur Reset Key jika HP lama hilang/rusak.'
        );
    END IF;

    -- Aktifkan / perbarui status lisensi di perangkat ini
    UPDATE public.licenses
    SET is_used = true,
        device_id = v_clean_device,
        activated_at = COALESCE(activated_at, NOW())
    WHERE id = v_row.id;

    RETURN json_build_object(
        'success', true,
        'message', 'Lisensi aktif di perangkat ini.',
        'recovery_email', v_row.recovery_email,
        'email_verified', COALESCE(v_row.email_verified, false)
    );
END;
$$;

-- 2. UPDATE FUNCTION request_license_otp (Kunci Permanen Jika Sudah Verified)
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

    -- ANTI-JAUL BELI AKUN: Jika sudah terverifikasi, tolak perubahan email
    IF v_row.email_verified = true THEN
        RETURN json_build_object(
            'success', false, 
            'message', 'Lisensi ini sudah terikat secara permanen dengan email pemilik resminya dan tidak dapat diubah.'
        );
    END IF;

    UPDATE public.licenses
    SET recovery_email = v_clean_email,
        email_verified = false,
        verification_otp = p_otp,
        otp_expires_at = NOW() + (p_expires_minutes || ' minutes')::interval
    WHERE license_key = v_clean_key;

    RETURN json_build_object(
        'success', true, 
        'message', 'Kode OTP berhasil dibuat. Silakan periksa email Anda.',
        'email', v_clean_email,
        'expires_in_minutes', p_expires_minutes
    );
END;
$$;

-- 3. PERMISSION & SCHEMA CACHE RELOAD
GRANT EXECUTE ON FUNCTION public.activate_license(text, text) TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.request_license_otp(text, text, text, int) TO anon, authenticated, service_role;
NOTIFY pgrst, 'reload schema';
