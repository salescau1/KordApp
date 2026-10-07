# KordApp Data & Catalog Guide (Context for KordApp-Data)

Dokumen ini adalah pegangan utama untuk AI/Developer di sesi **KordApp-Data**.

## 1. Lokasi & Struktur Data
- **Folder Kerja Utama:** `tools/catalog/` dan `tools/crawler/`
- **Master SQLite Database Asli:** `tools/catalog/full_merged_catalog.db`
- **Output Aset Android:** `app/src/main/assets/catalog.db.xz`

## 2. Status Terakhir Database
- **Total Lagu (Versi):** 157.305 baris
- **Total Judul Unik:** 105.084 lagu
- **Total Artis Unik:** 3.345 artis
- **Riwayat Penambahan Terakhir:**
  - Telah digabung batch scraping terbaru (Batch 16 s/d Batch 21): artis Spotify missing, curated OST & soundtrack franchise, K-Drama, Disney, dan pelengkap Ultimate Guitar/Guitarzone.
  - Script merger: `tools/catalog/merge_batches.py` (sudah otomatis menggunakan `PRAGMA user_version = 5`).
- **Struktur Tabel SQLite (`songs`):**
  - Kolom: `id` (PK), `source`, `source_url`, `title`, `artist`, `title_slug`, `artist_slug`, `version_name`, `rating`, `votes`, `key_base`, `difficulty`, `category`, `region`, `chords`, `chord_count`, `line_count`, `content`, `content_hash`, `scraped_at`.
  - Indexes: `idx_source`, `idx_title_slug`, `idx_artist_slug`.
  - `room_master_table` hash: `f8ce322c55245ffa8c13896137da76ce`.

## 3. ATURAN PENTING: User Version SQLite & Room
- **Room DB Version di App:** 5 (`CatalogDatabase.kt`).
- **PRAGMA SQLite:** Setiap kali mengedit / merge database baru, pastikan user_version bernilai **5**:
  ```python
  cur.execute("PRAGMA user_version = 5")
  con.commit()
  ```
  *(Catatan krusial: Jika user_version < 5, Room Android akan mendeteksi migrasi dan menghapus seluruh tabel menjadi 0 lagu).*

## 4. Alur Memasukkan Lagu / Batch Baru
1. Scraping / merging script dijalankan ke `tools/catalog/full_merged_catalog.db`.
2. Validasi integritas dan count lagu:
   ```bash
   python tools/inspect_catalog.py
   ```
3. Kompresi dan proteksi topeng XOR (`KORD_KEKALCLIP_2`):
   ```bash
   python tools/compress_and_mask_catalog.py
   ```
   Script ini otomatis mengompres ke XZ level 5 dan menulis langsung ke `app/src/main/assets/catalog.db.xz`.

## 5. Hubungan dengan Sesi Lain
- **KordApp-Core:** Mengonsumsi `catalog.db` via Room DAO (`com.chordapp.catalog.*`). Jangan mengubah nama kolom/tabel tanpa koordinasi.
- **KordApp-Ops:** Mengatur lisensi dan packaging build rilis. Sesi Data fokus pada kualitas dan kuantitas chord lagu offline.
