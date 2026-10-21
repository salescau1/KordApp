# 2 — Search Results

**Status:** PARSIAL  
**Mockup resmi:** `mockups/Accepted/2 - Search Results.jpg`  
**Source of truth:** `0 - INDEKS & FAKTA BERSAMA.md`

---

## 1. Tujuan dan kontrak saat ini

Search adalah layar pencarian katalog lokal. Implementasi saat ini mencari berdasarkan **judul lagu dan nama artis**. Pencarian isi lirik belum tersedia.

Flow:

- Home Search → Search Results dengan field otomatis fokus dan keyboard dibuka.
- Tap hasil lagu → Song Versions.
- Tap Artist Hero exact-match → Artist Directory.
- Bottom navigation tetap terlihat; memilih tab berpindah ke destination tab, bukan mengaktifkan Explore secara otomatis.

## 2. Status implementasi

### Input dan navigasi — DIIMPLEMENTASIKAN — QA PENDING

- Search dibuka dari Home.
- `FocusRequester` meminta fokus dan keyboard saat layar masuk.
- Tombol Back memakai kontrol Back bersama.
- Bottom nav tetap terlihat pada route Search.
- Hasil lagu membuka Song Versions; source/version tidak ditampilkan pada hasil Search.

### Pencarian judul + artis — DIIMPLEMENTASIKAN — QA PENDING

Query lokal mencocokkan `title` atau `artist`, lalu mengelompokkan hasil sebagai lagu logis berdasarkan artist/title slug. Hasil menampilkan judul dan artis saja.

### Artist Hero exact-match — DIIMPLEMENTASIKAN — QA PENDING

Artist Hero muncul hanya bila nama artis pada hasil sama dengan query secara case-insensitive, lalu membuka Artist Directory. Kartu saat ini belum menampilkan jumlah lagu artis.

### Pencarian lirik — BELUM DIIMPLEMENTASIKAN

Placeholder Home menyebut lyrics, tetapi query DAO saat ini tidak mencari kolom `content`. Dokumentasi tidak boleh mengklaim pencarian lirik tersedia.

### Sorting eksplisit — BELUM DIIMPLEMENTASIKAN

Tidak ada kontrol sort `Popular` atau `A–Z` di Search. Urutan berasal dari query katalog (`title ASC`); itu bukan mode sort eksplisit. `Popular` tidak boleh ditambahkan tanpa data popularitas nyata.

## 3. Bukti implementasi

- `app/src/main/java/com/chordapp/ui/screens/search/SearchScreen.kt`
- `app/src/main/java/com/chordapp/viewmodels/SearchViewModel.kt`
- `app/src/main/java/com/chordapp/catalog/CatalogDao.kt`
- `app/src/main/java/com/chordapp/ui/navigation/AppNavigation.kt`

## 4. Verifikasi capability

- Implementasi input, query, exact artist hero, bottom navigation, dan routing telah diperiksa pada kode.
- Belum ada test Search terfokus atau QA keyboard/bottom-nav di device yang cukup untuk status terverifikasi.
- Baseline project current hanya dicatat di `PROJECT_PLAN.md`.

## 5. Gap terbuka

1. Tambahkan pencarian isi lirik bila kontrak produk tetap mengharuskannya.
2. Tentukan sort yang jujur; A–Z dapat diterapkan, sedangkan Popular memerlukan sumber data nyata.
3. Tambahkan jumlah lagu nyata pada Artist Hero bila requirement visual itu dipertahankan.
4. QA di device untuk autofocus/keyboard, Back, bottom-nav, exact artist hero, dan navigasi hasil lagu.

## 6. Acceptance checklist

- [x] Search dibuka dari Home.
- [x] Field meminta fokus dan keyboard saat masuk.
- [x] Bottom navigation tetap terlihat dan Search tidak mengaktifkan Explore otomatis.
- [x] Hasil lagu hanya menampilkan judul + artis.
- [x] Tap lagu membuka Song Versions.
- [x] Exact artist match menampilkan hero dan membuka Artist Directory.
- [ ] Artist Hero menampilkan jumlah lagu nyata.
- [ ] Isi lirik ikut dicari.
- [ ] Kontrol sorting eksplisit tersedia.
- [ ] Interaksi Search lulus QA di device.
