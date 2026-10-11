-- Jalankan di SQL Editor Supabase untuk melengkapi metadata lisensi (Opsional namun sangat disarankan)
ALTER TABLE public.licenses 
ADD COLUMN IF NOT EXISTS license_type text DEFAULT 'Jualan',
ADD COLUMN IF NOT EXISTS notes text DEFAULT '';

-- Refresh schema cache
NOTIFY pgrst, 'reload schema';
