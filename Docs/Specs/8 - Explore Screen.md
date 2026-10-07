# 8 — Explore Screen

**Status:** PARSIAL  
**Source of truth:** katalog offline, implementasi aplikasi, baseline validasi fresh, dan `0 - INDEKS & FAKTA BERSAMA.md`

---

## 1. Requirement

Explore membantu pengguna menemukan lagu melalui katalog artis nyata secara offline:

`Explore → kategori A–Z → daftar artis → Artist Directory → Song Versions → Player`

- Default menampilkan tepat 26 kategori `A`–`Z`.
- Grid memakai lima kolom; 26 huruf menghasilkan enam baris.
- Sel berbentuk 1:1, memakai stable key, dan huruf kosong tetap terlihat tetapi disabled/muted.
- Tap huruf menampilkan artis yang nama tampilannya diawali huruf tersebut secara case-insensitive.
- Setiap artis menampilkan nama, slug navigasi, dan jumlah lagu faktual.
- Search lokal memfilter nama artis case-insensitive, membatalkan pilihan huruf, dan tetap bekerja offline.
- `Semua huruf` mengembalikan tampilan ke grid.
- Loading dan no-match state wajib tersedia.
- Tidak ada genre, mood, popularitas, foto, atau kategori rekaan.

## 2. Implementasi Aktual

- Data artis dan jumlah lagu dimuat dari katalog offline.
- UI membentuk `('A'..'Z')`, memakai `LazyVerticalGrid(GridCells.Fixed(5))`, stable key huruf, dan `aspectRatio(1f)`.
- Huruf tanpa artis dibuat muted dan `clickable(enabled = false)`.
- Filter kategori dan search menggunakan pencocokan case-insensitive.
- Memasukkan query menghapus kategori terpilih; memilih kategori mengosongkan query.
- Daftar artis memakai stable key `artistSlug`, nama maksimum dua baris dengan ellipsis, jumlah lagu katalog, dan navigasi ke Artist Directory.
- Loading, empty/no match, dan kontrol `Semua huruf` tersedia.
- Seluruh alur data ini tidak memerlukan jaringan.

## 3. Verifikasi Capability

- Test Explore memeriksa pemuatan catalog, pembentukan tepat 26 kategori tanpa entri rekaan, normalisasi huruf case-insensitive, serta aturan interaksi search/kategori.
- Inspeksi implementasi mengonfirmasi lima kolom, rasio 1:1, stable key, disabled empty letters, ellipsis, loading, dan no-match state.
- Reliability full-suite untuk setup/loading Explore masih menjadi gap validasi dan dicatat hanya di `PROJECT_PLAN.md`.
- Belum ada visual/device test yang memverifikasi hasil render sebenarnya.

## 4. Gap QA

Implementasi fungsional selesai, tetapi visual QA perangkat masih wajib untuk:

- grid lima kolom dan baris terakhir pada beberapa ukuran layar;
- nama artis panjang dan ellipsis;
- font scale/accessibility;
- scrolling dan touch target;
- huruf disabled/muted;
- navigasi Explore → Artist Directory → Song Versions → Player;
- perilaku saat catalog nyata menghasilkan daftar sangat panjang.

## 5. Acceptance

- **DIIMPLEMENTASIKAN — QA PENDING** — data berasal dari catalog offline nyata; reliability full-suite dan device QA belum ditutup.
- **DIIMPLEMENTASIKAN — QA PENDING** — implementasi membentuk tepat 26 kategori A–Z, lima kolom, enam baris, dan sel 1:1; test terfokus tersedia tetapi gate full-suite current tetap mengacu `PROJECT_PLAN.md`.
- **DIIMPLEMENTASIKAN — QA PENDING** — huruf kosong muted/disabled tanpa kategori rekaan.
- **DIIMPLEMENTASIKAN — QA PENDING** — filter huruf dan search case-insensitive beserta aturan reset state.
- **DIIMPLEMENTASIKAN — QA PENDING** — nama, slug, jumlah lagu, loading, no-match, dan navigasi artis tersedia.
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — `lintDebug`, `assembleDebug`, dan `assembleRelease` sukses.
- **DIIMPLEMENTASIKAN — QA PENDING** — visual, font scale, scrolling, touch target, dan alur navigasi pada perangkat.
- **PARSIAL** — full-suite reliability gate belum terpenuhi; hasil current hanya dicatat di `PROJECT_PLAN.md`.