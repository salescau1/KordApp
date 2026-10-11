-- ========================================================
-- TABEL app_updates: Kontrol Pembaruan APK & Database KordApp
-- Jalankan query ini di SQL Editor dashboard Supabase Anda.
-- ========================================================

CREATE TABLE IF NOT EXISTS public.app_updates (
    id SERIAL PRIMARY KEY,
    latest_app_version_code INT NOT NULL DEFAULT 1,
    latest_app_version_name TEXT NOT NULL DEFAULT '1.0',
    app_download_url TEXT,
    app_changelog TEXT,
    is_force_app_update BOOLEAN DEFAULT FALSE,
    latest_catalog_version INT DEFAULT 5,
    catalog_download_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Buka izin baca (SELECT) untuk public/anon role agar aplikasi bisa mengecek versi
ALTER TABLE public.app_updates ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read access on app_updates"
ON public.app_updates
FOR SELECT
TO anon, authenticated
USING (true);

-- Masukkan data simulasi versi 1.1 (lebih tinggi dari v1.0 yang terpasang di HP)
INSERT INTO public.app_updates (
    latest_app_version_code,
    latest_app_version_name,
    app_download_url,
    app_changelog,
    is_force_app_update
) VALUES (
    2,
    '1.1',
    'https://raw.githubusercontent.com/yousu/dummy/main/sample.apk',
    '1. Fitur Periksa Pembaruan Otomatis\n2. Peningkatan performa database 157k lagu\n3. Perbaikan minor player chord',
    false
);
