# KordApp-Core: State & Summary

Dokumen ringkas untuk menjaga konteks sesi **KordApp-Core** (UI, Player chord, dan fitur aplikasi) tetap ringan, fokus, dan siap lanjut kapan saja.

---

## 1. Fokus & Tanggung Jawab Sesi
- **KordApp-Core** *(Sesi Ini)*: UI Jetpack Compose, Player chord, Transpose, Auto-scroll, Page Flip, Explore, Artist Directory, Favorit/Setlist, Home Top 50 & In-App Scraper.
- **KordApp-Ops**: Lisensi Supabase, Aktivasi Device, Migrasi DB v5, Payment Gateway, In-App Update APK (lihat `OPS_STATE.md`).
- **KordApp-Data**: Katalog database 157k lagu, scraping/crawler, update chord offline (lihat `DATA_CATALOG_GUIDE.md`).

---

## 2. Fitur & Logika Terakhir yang Diselesaikan (100% Done)

### A. Auto-Scroll Hold & Resume Pintar (`PlayerScreen.kt`)
- Memanfaatkan `pointerInput` dengan pass `PointerEventPass.Initial` untuk membaca touch down/up (`isFingerDown`) tanpa menginterupsi gesture native Compose.
- Saat jari menyentuh layar atau scrolling manual, auto-scroll **menahan diri (hold)** dan memperbarui baseline frame (`previousFrame`), mencegah lonjakan posisi. Begitu jari dilepas, auto-scroll langsung lanjut otomatis.

### B. Fitur Sorting di Artist Directory (`ArtistDirectoryViewModel.kt` & `ArtistDirectoryScreen.kt`)
- Enum `ArtistSongSortOrder { ALPHABETICAL, POPULAR }`.
- Menggunakan `OrderedSongMap` agar StateFlow Compose reaktif melakukan recomposition saat urutan berubah.
- Tombol tab filter `ArtistSortButton` ("A-Z" dan "Terpopuler / Popular") terpasang di header katalog artis.
- Unit Test: `ArtistDirectoryViewModelSortingTest.kt` **100% PASS**.

### C. Zero-Latency Top 50 & Background Auto-Scraper (`HomeViewModel.kt`, `HomeScreen.kt`, `UgSongScraper.kt`)
- **Fix Loading Jeda Buka App**:
  - Menghilangkan `ChartState.Loading` saat cold start jika cache disk tersedia.
  - Mematikan call jaringan paksaan saat buka aplikasi. Data cache langsung tampil instan (0 ms) dengan `isRefreshing = false`. Pull-to-refresh hanya aktif saat ditarik manual oleh user.
- **Ultimate Guitar Scraper Engine (`UgSongScraper.kt`)**:
  - Native scraper untuk Ultimate Guitar (UG) mencocokkan format KordApp-Data (JSON `js-store`, strip `[tab]`, convert `[ch]` -> `[...]`, normalisasi spacing, extract unique chords).
  - Unit test `UgSongScraperTest.kt` **100% PASS**.
- **Auto-Scrap Top 50 Spotify**:
  - Saat Top 50 ditampilkan, background coroutine senyap mengecek lagu yang `!row.available`.
  - Secara otomatis menarik lagu yang belum ada dari UG dengan jeda aman 1.5 detik per lagu, memasukkannya ke Room `CatalogDao`, dan memperbarui UI secara real-time.
- **1-Tap Direct Download di Home**:
  - Pada baris lagu yang belum tersedia di Home, tombol kini menampilkan icon download (`Icons.Filled.Download`, `AccentGold`) dan bisa langsung di-tap untuk mengunduh lagu spesifik tersebut saat itu juga.

---

## 3. Status Build & Kode
- **Lokasi Proyek**: `C:\KordApp-AGY`
- **Gradle**: `compileDebugKotlin` **BUILD SUCCESSFUL**.
- **Unit Test Core**:
  - `ArtistDirectoryViewModelSortingTest` **PASS**.
  - `UgSongScraperTest` **PASS**.

---

### D. Page Flip Dense Packing Fix (`SheetPaginator.kt` & `PlayerScreen.kt`)
- **Masalah Sebelumnya**: Halaman di mode Page Flip terpotong prematur hanya 2 baris teks/pasang chord-lirik, meninggalkan 60-70% ruang kosong melompong di bagian bawah halaman (`isi-isi-<kosong>-<kosong>`).
- **Penyebab**:
  1. `lineWrapCount` menghitung panjang teks dari `chordLine.originalText` yang memiliki 30–40 karakter spasi indentasi guitar tab. Ini menyebabkan hampir semua baris chord dianggap wrap dan memakan slot 2x lipat (4 slots = ~90dp per baris). Padahal di `ChordLyricFlow`, teks chord dipasangkan per-kata (FlowRow) tanpa spasi kosong panjang tersebut.
  2. Section header membundel Section + Blank + Chord + Lyric menjadi satu blok besar yang tidak bisa dipecah, sehingga begitu section header muncul di tengah halaman, halaman langsung di-flush prematurely.
  3. Di `PlayerScreen.kt`, kalkulasi `capacity` mengurangi padding berlebih (40dp) dan headroom (-1 slot) di atas Box yang sudah memiliki padding top & bottom.
- **Solusi yang Diterapkan**:
  1. `visualLineLength`: Mengukur lebar teks visual sebenarnya berdasarkan panjang lirik dan posisi chord token efektif, mengeliminasi false wrap pada chord lines.
  2. Dense Packing Algorithm: Menghilangkan monster block section header. Cukup cegah orphan section header di baris paling bawah halaman (`sectionBudget = Section + Blank + FirstContentLine`). Jika muat, section masuk dan baris-baris setelahnya dipack satu per satu secara bebas hingga kapasitas layar penuh.
  3. Koreksi `slotDp` & `usableHeightPx`: Menyelaraskan ukuran 1 slot dengan tinggi visual 1 baris monospace di Compose (~1.35x fontSize, ~18dp) dan padding realistis (20dp).
- **Hasil**:
  - Halaman terisi padat dan proporsional dari atas sampai bawah ("isi-isi-isi-isi").
  - Unit tests `SheetPaginatorTest` (termasuk tes dense packing dan pencegahan orphan bridge) **100% PASS**.

### E. UG Scraper Candidate Matching & Normalization Fix (`UgSongScraper.kt`)
- **Masalah Sebelumnya**: Lagu seperti "What If I Call" oleh Alex Crichton (Spotify Top 50 #47) tidak berhasil ter-scrap meski chord-nya ada di Ultimate Guitar.
- **Akar Masalah**:
  1. `findBestChordTabUrl` sebelumnya hanya menghitung skor `rating * (votes + 1)` di seluruh array hasil pencarian tanpa mengecek apakah judul atau artisnya cocok.
  2. Saat mencari "Alex Crichton What If I Call", UG mengembalikan 55 hasil pencarian fuzzy. Lagu populer lain (misalnya "If I Killed Someone For You" oleh Alec Benjamin) memiliki 1.245 votes, jauh melampaui lagu asli yang baru memiliki 6 votes.
  3. Akibatnya, paginator memilih URL Alec Benjamin, mengunduhnya, dan menyimpannya. Saat aplikasi mencocokkan dengan "What If I Call - Alex Crichton", lagu tidak ditemukan dan baris tetap abu-abu (`available = false`).
- **Solusi yang Diterapkan**:
  1. **Tiered Title & Artist Matching**: Kandidat diwajibkan lolos verifikasi kecocokan judul (`cleanTitle`) dan artis (`primaryArtist`). Judul yang tidak cocok langsung dibuang (`tier = 0`).
     - Tier 3: Judul exact match + Artis match.
     - Tier 2: Judul exact match atau Judul partial + Artis match.
     - Tier 1: Judul partial match.
     Kandidat dengan tier tertinggi yang dipilih, baru kemudian diurutkan berdasarkan skor rating & votes.
  2. **Query Fallback**: Jika query gabungan artis + judul tidak membuahkan hasil, scraper otomatis mencoba fallback query dengan judul bersih.
  3. **HTML Entity Unescaping**: Meng-unescape entitas HTML numerik dan tipografi (`&#039;`, `&#39;`, `&#x27;`, dll) di teks lirik `rawContent`, sehingga teks seperti `you&#039;re` bersih menjadi `you're`.
  4. **OnConflictStrategy.REPLACE**: Memastikan re-scrape lagu yang diperbarui otomatis menimpa entri lama di database Room.
- **Hasil**:
  - Lagu "What If I Call" oleh Alex Crichton berhasil di-scrape seketika, masuk ke Room database, dan 100% playable di aplikasi.
  - Unit tests `UgSongScraperTest` **100% PASS**.

---

### F. Multi-Source Search & Smart Scraper Engine (Module 1 - 100% Selesai)
- **Source Selector Pills (`SearchScreen.kt` & `SearchViewModel.kt`)**:
  - Tiga tab sumber di bawah kolom input pencarian: `(•) Local (Offline)`, `UG (Online)`, dan `GT (GuitarTuna)`.
  - Default: `(Local)` untuk menjaga performa cepat dan pengalaman offline-first.
- **Scraping Online Artis (`UgSongScraper.kt`)**:
  - Sesuai aturan dual-version: jika scraping berdasarkan Artist, mengambil **semua lagu** artis dari UG dengan **2 versi per lagu**:
    1. Versi Top Votes (paling banyak divalidasi komunitas).
    2. Versi Best Rating (rating bintang tertinggi dengan minimal 3 votes).
  - Dilengkapi dialog progress bar real-time saat pengunduhan batch berlangsung.
- **Scraping Online Judul Lagu (Song Mode)**:
  - Jika pencarian judul lagu di tab `UG`, menampilkan daftar versi lengkap dengan metadata: Key, Rating bintang, dan Jumlah Votes.
  - Tombol **[ 👁️ Preview ]**: Membuka dialog lembar chord monospace dengan informasi kunci nada & rating, sehingga user dapat membaca isi lagu sebelum mengunduh.
  - Tombol **[ 📥 Unduh ]**: Mengunduh dan menyimpan versi terpilih langsung ke database lokal.
- **GuitarTuna Tab (`GT`)**:
  - Filter instan untuk 48.918 lagu GuitarTuna yang telah tersimpan di database lokal (Easy & Original versions).
- **Pembersihan HTML Entities Global (`ChordParser.kt`)**:
  - Menambahkan `unescapeHtml` langsung di dalam `ChordParser.parse(content)` dan `UgSongScraper.kt` untuk menangani entity desimal (`&#039;`, `&#39;`, `&#x27;`, dll).
- **Unit Tests & Build**:
  - `UgSongScraperTest` **100% PASS**.
  - `CatalogDaoTest` **100% PASS**.
  - `ArtistDirectoryViewModelSortingTest` **100% PASS**.
  - `compileDebugKotlin` **BUILD SUCCESSFUL**.
  - APK debug siap di: `app/build/outputs/apk/debug/app-debug.apk` (96.8 MB).

---

### G. Penyeragaman Header Global & Edit Mode Database Explore (Module 2 - 100% Selesai)
- **Penyeragaman Header Seluruh Tab**:
  - Semua screen (`Explore`, `Setlists`, `Favorites`, `Settings`) kini menggunakan layout, margin (`16.dp, 16.dp, top 16.dp, bottom 12.dp`), font style (`titleLarge`, `FontWeight.Bold`), dan subtitle (`bodySmall`, `MutedText`) yang 100% sejajar dan seragam:
    - **EXPLORE**: `Explore` / `Artists & Music Genres` + Tombol Pensil ✏️
    - **EDIT MODE** (Explore): `Edit Mode` / `Customize Your Database` + Tombol ✕
    - **SETLISTS**: `Setlists` / `Add To Your Set` + Tombol `+` & Sort
    - **FAVORITES**: `Favorites` / `X saved songs` (dinamis)
    - **SETTINGS**: `Settings` / `Preferences & Customization`
- **Pembersihan Favorites Screen (`FavoritesScreen.kt`)**:
  - Menghapus icon play button panah (`PlayArrow`) redundant di ujung kanan baris favorit, membuat tampilan lebih bersih dan lega.
- **Database Edit Mode di Explore (`ExploreViewModel.kt`, `ExploreScreen.kt`, `CatalogDao.kt`, `CatalogRepository.kt`)**:
  - **Pemicu**: Tap tombol pensil ✏️ di pojok kanan atas Explore untuk masuk ke Edit Mode.
  - **Level 1 (List Artis)**:
    - Tiap artis dilengkapi `ColorCircleSwitch` (slider bulat warna merah-hijau).
    - 🟢 **Hijau (Keep)**: Default. Artis aman, bisa di-tap untuk masuk ke daftar lagunya.
    - 🔴 **Merah (Hapus)**: Artis ditandai hapus total (seluruh lagunya ikut terhapus), baris meredup dan tap terkunci.
  - **Level 2 (List Lagu Artis - Pruning Detail)**:
    - Terurut Alphabetical (A–Z).
    - Tap lagu tidak membuka player/lirik chord.
    - Tiap lagu bisa ditandai hapus utuh, atau di-expand (`[X Versi ▾]`) untuk menghapus versi tertentu saja (misal versi yang jelek) dengan switch Merah-Hijau per versi.
    - Tombol `← Kembali ke Artis` untuk kembali ke daftar artis tanpa menghilangkan pilihan.
  - **Bottom Action Bar**:
    - Menggantikan navigasi utama saat Edit Mode aktif:
      `🗑️ X Artists • Y Songs • Z Versions      [ Apply ]`
  - **Dialog Konfirmasi**:
    - Tombol `[ Apply ]`: Dialog konfirmasi hapus permanen dari Room SQLite lokal.
    - Tombol `✕` di header: Safeguard konfirmasi jika ada item yang telah ditandai agar tidak salah keluar.
- **Unit Tests & Build**:
  - `UgSongScraperTest` **100% PASS**.
  - `CatalogDaoTest` **100% PASS**.
  - `ArtistDirectoryViewModelSortingTest` **100% PASS**.
  - `compileDebugKotlin` **BUILD SUCCESSFUL**.
  - APK debug siap di: `app/build/outputs/apk/debug/app-debug.apk` (96.8 MB).

---

## 4. Status Kode
- Seluruh kode Modul 1 dan Modul 2 telah 100% diimplementasikan, diverifikasi lulus unit test, dan siap diuji langsung di perangkat.


