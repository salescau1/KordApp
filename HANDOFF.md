# HANDOFF KORDAPP - ARSITEKTUR KATALOG & STATUS PENGEMBANGAN

## 1. Identitas & Status Proyek
- **Aplikasi:** KordApp (Chord & Lyrics Viewer untuk Musisi Panggung)
- **Workspace:** `C:\KordApp-AGY`
- **Device Target:** Google Pixel 6 Pro (`1A021FDEE001S7`) via USB ADB (Aktif & Terverifikasi).
- **Status Build Terakhir:**
  - `app-debug.apk` (v1.3.0+) berhasil terkompilasi (BUILD SUCCESSFUL).
  - UI Settings disempurnakan:
    - Header: `SETTINGS` + Subtitle `Personal Configuration` di kiri, sejajar dengan kapsul toggle `[ ID | EN ]` di kanan.
    - Baris bahasa lama di section tema/tampilan sudah dibersihkan.
    - KordApp Catalog: Judul/subtitle *"Download Catalog"* dihapus, teks kartu Package A, B, C dirapatkan *(compact)*.
    - User Catalog: Judul/subtitle dihapus, kartu `Creator` berstatus disabled / abu-abu dengan label `(Coming Soon)`.

---

## 2. Blueprint Arsitektur Sistem Katalog (Disetujui)

### A. Pembagian Dua Payung Besar
1. **KORDAPP CATALOG (Database Bawaan / Resmi):**
   - File fisik terpisah: `kordapp_official.db` (atau DB bawaan aplikasi).
   - Berisi paket lagu resmi: **`[Package A]`** (79k), **`[Package B]`** (47k), **`[Package C]`** (32k).
   - Bersifat *Read-Only* dari sudut pandang musisi (tidak akan terhapus saat user membersihkan lagu pribadi).
2. **USER CATALOG (Database Dinamis & Personal):**
   - File fisik: **`user_catalog.db`** (Opsi A - 1 file SQLite dengan **2 tabel terpisah**: `scraped_songs` dan `creator_songs`).
   - Ringan, independen, dan aman:
     - Hapus cache Scraped hanya menyasar tabel `scraped_songs`.
     - Upload ke database online / cloud hanya membaca tabel `creator_songs` (lagu scraped 100% tidak pernah ikut ter-upload).
   - Terbagi menjadi 2 kategori langsung di UI (Dual-Card tanpa sistem tab):
     - **`[Scraped]`**: Wadah *raw cache* lagu hasil pencarian online/crawler web KordApp.
     - **`[Creator]`**: Wadah karya, aransemen pribadi, dan lagu hasil edit (saat ini berstatus *Coming Soon*).

### B. Aturan Lifecycle Lagu (Interaksi Scraped ↔ Creator)
- Lagu hasil pencarian online otomatis masuk ke tabel **`scraped_songs`**.
- Jika lagu dari **`[Scraped]`** atau dari **`[Package A/B/C]`** dibuka lalu **diedit chord/liriknya**, lagu resmi/comotannya tetap utuh, dan hasil editannya otomatis tersimpan sebagai aransemen baru di **`creator_songs`** (*Duplicate & Edit / Fork*).
- Setiap lagu di **`creator_songs`** terikat stempel digital identitas: `creator_email` (terautentikasi via Supabase Auth OTP) dan nama panggung musisi.

### C. Toolbar Aksi (Full Icon Row)
Masing-masing kartu di `SettingsScreen` memiliki bilah aksi:
- **Kartu `[Scraped]` (Aktif):**
  - `[ 🔄 Sync ]`: P2P Hotspot panggung offline ke sesama musisi band.
  - `[ 📤 Export ]`: Simpan ke file lokal HP atau bagikan langsung ke WhatsApp.
  - `[ 📥 Import ]`: Buka file kumpulan lagu scraped dari luar.
  - `[ 🗑️ Hapus ]`: Bersihkan seluruh cache scraped dengan dialog konfirmasi aman.
- **Kartu `[Creator]` (Disabled / Coming Soon):**
  - Ditampilkan redup/grayed-out dengan satu kapsul minimalis bertuliskan **`(Coming Soon)`**.

### D. Rencana Jangka Panjang Explore Screen
- Transisi 3 mode geser dinamis: `EXPLORE` ↔ `EDIT MODE` ↔ `CREATOR MODE`.
- `CREATOR MODE` akan fokus pada penulisan lirik/lagu baru, dengan saklar `Lyrics Edit Mode (0-0)` di masa mendatang.

---

## 3. Status Fitur Terkini & Yang Selesai Dikerjakan
1. **User Catalog Database (`user_catalog.db`):** Selesai & Aktif. Tabel `scraped_songs` dan `creator_songs` terisolasi dari database resmi.
2. **Auto-Routing Scraper:** Selesai & Terverifikasi. Lagu hasil download online langsung masuk ke `scraped_songs` tanpa mencemari Package A/B/C.
3. **Badge Counter:** Selesai. Menampilkan jumlah lagu real-time dari database Room.
4. **Tombol `[ 🗑️ Hapus ]` (Clear Cache):** Selesai. Menampilkan dialog konfirmasi merah dan menghapus isi tabel `scraped_songs`.
5. **Format Custom `.kord` (Serializer & Parser):** Selesai. Dikelola oleh `KordPackageSerializer.kt`.
6. **Tombol `[ 📤 Export ]` & `[ 📥 Import ]`:** Selesai. Ekspor ke `.kord` (Share Sheet / WA / Lokal) dan Impor dari file picker.
7. **Bottom Navigation & Explore:** Utuh dan kembali ke format awal (`[ 🏠 Home ] [ 🧭 Explore ] [ 📋 Setlists ] [ ❤️ Favorites ] [ ⚙️ Settings ]`).

---

## 4. Langkah Kerja Berikutnya
1. Pasang APK terbaru ke Google Pixel 6 Pro via ADB.
2. Uji coba User Catalog Scraped (`Export/Import` file `.kord` dan `Hapus`).
3. Diskusi konsep Explore / Tuner yang sebenarnya diinginkan User.
