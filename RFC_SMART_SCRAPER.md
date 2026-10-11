# RFC: In-App Smart Scraper (Fitur Eksklusif PRO)

Dokumen rancangan fitur untuk sesi **KordApp-Core**.  
**STATUS: PERENCANAAN (DILARANG LANGSUNG CODING SEBELUM ALUR DISETUJUI USER).**

---

## 1. Latar Belakang & Konsep
Fitur eksklusif bagi pengguna berlisensi PRO untuk melengkapi koleksi chord offline secara mandiri langsung dari aplikasi tanpa perlu menunggu update database dari developer.

Tersedia **2 Tipe Scraping**:
1. **Mode Artis (Batch Artist Importer):**
   - User memasukkan daftar nama artis (1 nama per baris).
   - Aplikasi mencari seluruh daftar lagu milik artis tersebut dan menyedot chord-nya ke database lokal.
2. **Mode Judul (Single/Batch Title Importer):**
   - User memasukkan format: `Artis - Judul Lagu` (misal: `Bernadya - Satu Bulan`).
   - Aplikasi langsung mencari dan mengambil spesifik lagu tersebut.

---

## 2. Struktur Tampilan & Lokasi Menu
- **Akses:** Menu `Settings` -> Bagian Eksklusif PRO: **"Studio Impor Chord"**.
- **Komponen UI:**
  - Tab Switcher: `[ Berdasarkan Artis ]` | `[ Berdasarkan Judul ]`.
  - Multi-line TextField (Text Editor): Tempat input list nama artis/judul.
  - Source Selector: Default `ChordTela` (Indo/Pop), opsi alternatif `Ultimate Guitar`.
  - Action Button: `[ Mulai Tarik Chord ]` (Warna Accent Gold).
  - Live Console / Progress Card: Menampilkan progres scraping (Nama lagu saat ini, total terunduh, error handling).

---

## 3. Arsitektur Teknis Android
1. **Parser Engine:**
   - Porting logic scraper HTML dari Python (`tools/crawler/parsers/chordtela.py`) ke Kotlin menggunakan library parser HTML Android (seperti Jsoup atau Regex parser ringan bawaan).
2. **Penyimpanan Lokal:**
   - Menggunakan Room DAO `CatalogDao` untuk eksekusi `INSERT OR IGNORE INTO songs`.
   - Update otomatis cache Explore/Search agar lagu baru langsung terbaca tanpa restart app.
3. **Resilience & Background Process:**
   - Menggunakan Kotlin Coroutine dengan delay aman (1–2 detik per lagu) untuk menghindari limit IP/anti-bot.
   - Penanganan timeout/koneksi terputus dengan logging yang jelas pada UI.

---

## 4. Instruksi untuk KordApp-Core
- **JANGAN LANGSUNG MENULIS KODE PRODUKSI.**
- Diskusikan layout UI dan konfirmasi dependencies (misal: penambahan library Jsoup di `build.gradle.kts`) dengan user terlebih dahulu.
