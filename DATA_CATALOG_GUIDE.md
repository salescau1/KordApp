# KordApp Data & Catalog Guide (Context for KordApp-Data)

Dokumen ini adalah pegangan utama untuk AI/Developer di sesi **KordApp-Data**.

## 1. Lokasi & Struktur Data
- **Folder Kerja Utama:** `tools/catalog/` dan `tools/crawler/`
- **Master SQLite Database Asli:** `tools/catalog/full_merged_catalog.db`
- **Output Aset Android:** `app/src/main/assets/catalog.db.xz`

## 2. Status Terakhir Database
- **Total Lagu (Versi):** 157.425 baris
- **Total Judul Unik:** 105.182 lagu
- **Total Artis Unik:** 3.362 artis
- **Riwayat Penambahan & Normalisasi Terakhir:**
  - **Batch 23 ("Mega Expansion: Ghibli, Anime Anthems, Disney Renaissance, K-Drama Hits & Viral Tracks"):** Menambahkan 84 lagu premium (Studio Ghibli: Spirited Away, Howl's Moving Castle, Totoro, Kiki, Mononoke; Shonen & Modern Hits: Oshi no Ko Idol, Jujutsu Kaisen Ao no Sumika, Hunter x Hunter, Your Lie in April, Bocchi the Rock!, Tokyo Revengers, Bleach, Fairy Tail, Death Note; Disney Classics: The Little Mermaid, Cars, Brave, Pinocchio; K-Drama: Hospital Playlist, Our Beloved Summer, Moon Lovers, Queen of Tears; Indo Hits Viral: Bernadya, Sal Priadi, Rio Clappy, Denny Caknan, Gildcoustic, Wahyu F Giri, Juicy Luicy, Mahalini, Guyon Waton, Happy Asmara, Ndarboy Genk, Feby Putri).
  - **Batch 22 ("Soundtrack Complete Overhaul"):** 49 track (Naruto 24 lagu, Frozen 8 lagu, Moana, Encanto, Coco, Tangled, Shrek, LOTR, Top Gun, Barbie, CLOY).
  - Sebanyak **261 lagu OST** di menu Jelajah Franchise KordApp (`OstCatalogData.kt`) terverifikasi **100% cocok (0 broken link)** ke database katalog.
  - Pemisahan resmi band Seventeen: `Seventeen` Indonesia (`seventeen-id`, `INDO`, 49 lagu) vs Boyband K-Pop `SEVENTEEN` (`seventeen-kr`, `KOREA`, 80 lagu).
  - Normalisasi nilai region database: Seluruh baris `INDONESIA` diseragamkan ke `INDO`.
  - Sistem Plat Wilayah (Region Badge): `[ID]` (Indonesia), `[EN]` (Barat), `[KR]` (Korea), `[JP]` (Jepang) aktif di UI (`RegionBadge.kt`).
  - Script merger: `tools/catalog/merge_batches.py` (sudah otomatis menggabungkan ke-25 batch, membagi Seventeen ID/KR, menyetel `PRAGMA user_version = 5`, dan mengompresi dengan XOR-masking `KORD_KEKALCLIP_2`).
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
