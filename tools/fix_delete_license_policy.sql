-- ==============================================================================
-- FIX DELETE & RLS PERMISSION PADA TABEL LICENSES
-- Jalankan ini di SQL Editor Supabase kamu (klik Run):
-- ==============================================================================

-- 1. Berikan hak akses DELETE pada tabel licenses untuk role anon & authenticated
GRANT DELETE ON TABLE public.licenses TO anon, authenticated, service_role;

-- 2. Buat policy agar siapapun dengan API Key (anon) bisa menghapus lisensi
DROP POLICY IF EXISTS "Allow anon delete licenses" ON public.licenses;
CREATE POLICY "Allow anon delete licenses" 
ON public.licenses 
FOR DELETE 
TO public 
USING (true);

-- 3. Pastikan policy SELECT, INSERT, UPDATE juga aktif untuk semua
DROP POLICY IF EXISTS "Allow anon all on licenses" ON public.licenses;
CREATE POLICY "Allow anon all on licenses" 
ON public.licenses 
FOR ALL 
TO public 
USING (true) 
WITH CHECK (true);

-- 4. Muat ulang cache REST API Supabase
NOTIFY pgrst, 'reload schema';
