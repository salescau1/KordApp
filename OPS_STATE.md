# KordApp-Ops: State & Summary

Dokumen ringkas untuk menjaga konteks sesi **KordApp-Ops** tetap ringan dan fokus.

---

## 1. Status Terkini (Done 100%)
- **Database Katalog:**
  - 157.305 versi chord, 105.084 lagu unik, 3.345 artis (562.9 MB uncompressed).
  - Terpasang rapi di assets: `catalog.db.xz` (79.8 MB) dengan enkripsi XOR `KORD_KEKALCLIP_2`.
  - Room version: **5** (`PRAGMA user_version = 5`). Berhasil terbaca penuh di Pixel 6 Pro.
- **Sistem Lisensi & Supabase:**
  - URL: `https://rhgcsgrdqbhimpdloxgo.supabase.co/rest/v1`
  - RPC: `activate_license(p_license_key, p_device_id)` & `record_device(...)`.
  - Tamper-proof: Signature SHA-256 (Device ID + Salt).
  - Self-service: Tombol "Pindahkan Lisensi ke HP Lain" aktif di menu Settings.
- **Git Repo:**
  - Inisialisasi selesai di `C:\KordApp-AGY`.
  - Initial commit aman dengan `.gitignore` bersih.

---

## 2. Struktur Pembagian Sesi Kerja
1. **`KordApp-Core`** : UI Compose, Player Chord, Transpose, Explore, Favorit.
2. **`KordApp-Data`** : Crawler/scraping, update chord offline (panduan di `DATA_CATALOG_GUIDE.md`).
3. **`KordApp-Ops`** *(Sesi Ini)* : Lisensi, Supabase, Payment Gateway, In-App Update APK, Build Rilis.

---

## 3. Agenda Berikutnya di KordApp-Ops
Pilih salah satu untuk dikerjakan:
1. **Fitur In-App Update:** Pengecekan versi APK baru via Supabase/GitHub + download & install mandiri.
2. **Otomatisasi Pembayaran:** Webhook Mayar.id / Lynk.id agar lisensi otomatis terbit setelah pembeli bayar.
