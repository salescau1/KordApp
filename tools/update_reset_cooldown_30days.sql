-- ==============================================================================
-- UPDATE: COOLDOWN RESET 1 BULAN SEKALI (30 HARI)
-- ==============================================================================
-- 1. Tambah kolom last_reset_at untuk mencatat tanggal terakhir reset
-- 2. request_license_reset_by_email menolak jika belum lewat 30 hari
-- 3. confirm_license_reset_by_email_otp mencatat last_reset_at = NOW()
-- ==============================================================================

-- 1. Tambah kolom ke tabel licenses
ALTER TABLE public.licenses
ADD COLUMN IF NOT EXISTS last_reset_at timestamptz DEFAULT NULL;

-- 2. Update fungsi request_license_reset_by_email (Cek Batas 30 Hari)
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
    v_row record;
    v_days_left int;
    v_next_date text;
BEGIN
    v_clean_email := LOWER(TRIM(p_email));

    SELECT id, license_key, recovery_email, email_verified, last_reset_at
    INTO v_row
    FROM public.licenses
    WHERE recovery_email = v_clean_email
      AND email_verified = true;

    IF NOT FOUND THEN
        RETURN json_build_object(
            'success', false, 
            'message', 'Tidak ditemukan lisensi aktif yang terikat dengan email tersebut.'
        );
    END IF;

    -- ATURAN 1 BULAN SEKALI: Cek apakah masih dalam masa cooldown 30 hari
    IF v_row.last_reset_at IS NOT NULL AND v_row.last_reset_at > (NOW() - INTERVAL '30 days') THEN
        v_days_left := CEIL(EXTRACT(EPOCH FROM ((v_row.last_reset_at + INTERVAL '30 days') - NOW())) / 86400.0);
        v_next_date := TO_CHAR(v_row.last_reset_at + INTERVAL '30 days', 'DD Mon YYYY');
        
        RETURN json_build_object(
            'success', false,
            'message', 'Reset lisensi dibatasi 1x sebulan. Anda baru dapat mereset kembali dalam ' || v_days_left || ' hari lagi (tanggal ' || v_next_date || ').'
        );
    END IF;

    -- Pasang token OTP reset sementara
    UPDATE public.licenses
    SET reset_token = p_reset_token,
        reset_token_expires_at = NOW() + (p_expires_minutes || ' minutes')::interval
    WHERE id = v_row.id;

    RETURN json_build_object(
        'success', true, 
        'message', 'Kode verifikasi reset berhasil dibuat. Silakan periksa email Anda.'
    );
END;
$$;

-- 3. Update fungsi confirm_license_reset_by_email_otp (Catat last_reset_at = NOW())
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
        RETURN json_build_object('success', false, 'message', 'Kode verifikasi reset sudah kadaluarsa (melewati batas waktu 30 menit). Silakan minta kode baru.');
    END IF;

    -- Reset ikatan hardware HP lama & CATAT last_reset_at = NOW()
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

GRANT EXECUTE ON FUNCTION public.request_license_reset_by_email(text, text, int) TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.confirm_license_reset_by_email_otp(text, text) TO anon, authenticated, service_role;
NOTIFY pgrst, 'reload schema';
