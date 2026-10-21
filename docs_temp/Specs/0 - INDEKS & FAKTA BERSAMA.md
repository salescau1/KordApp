# ChordApp — Indeks & Fakta Bersama

**Peran:** kontrak global untuk Spec 1–10.  
**Baca bersama:** `Docs/CATALOG_ARCHITECTURE.md` untuk detail data tunggal; `PROJECT_PLAN.md` untuk status implementasi; `DEFINITION_OF_DONE.md` untuk gate verifikasi.

## 1. Pemisahan Kontrak
- Spec menentukan requirement dan acceptance criteria.
- Mockup menentukan arah visual yang telah diterima.
- Database menentukan data katalog yang nyata.
- Kode menunjukkan implementasi saat ini.
- Test/build/QA menunjukkan bukti verifikasi.

Jangan menyamakan requirement, status implementasi, dan verifikasi. Status hanya boleh memakai **DIIMPLEMENTASIKAN & TERVERIFIKASI**, **DIIMPLEMENTASIKAN — QA PENDING**, **PARSIAL**, atau **BELUM DIIMPLEMENTASIKAN**.

## 2. Indeks Layar
1. Home Screen
2. Search Results
3. Artist Directory
4. Song Versions
5. Chord Player
6. Setlists
7. Setlist Tracklist
8. Explore Screen
9. Favorites Screen
10. Settings Screen

Nama produk di UI dan navigasi utama adalah **Setlists**. Setlists merupakan koleksi datar; konsep Songbook dan pemisahan Standalone tidak lagi menjadi requirement produk.

## 3. Online dan Offline
Fungsi inti offline memakai katalog lokal: search, artist browsing, Song Versions, Player, transpose/capo, diagram, auto-scroll/page flip, Favorites, Setlists, dan settings lokal.

Internet hanya untuk layanan dinamis yang memang diintegrasikan, seperti chart dan iklan. Chart online bukan katalog chord. Ketika offline atau ad gagal, slot iklan collapse ke tinggi 0; jangan tampilkan kotak kosong/error dan jangan ganggu Player.

## 4. Fakta Katalog Runtime
Aset authoritative: `app/src/main/assets/catalog.db`.

- Ukuran: 297.267.200 bytes.
- SHA-256: `e5bf721425f3695deb21d374545f9b20379af8e66582687b25140a3581d12cc4`.
- Schema: tabel `songs` saja.
- Indeks: `idx_artist_slug`, `idx_source`, `idx_title_slug`.
- Total: 69.847 row/version.
- Lagu logis `(artist_slug, title_slug)`: 65.538.
- Artist logis `artist_slug`: 20.778.
- Source: ChordTela 41.973; GT 19.753; UG 8.121.
- Region: `id` 37.051; `en` 32.793; `jp` 3.
- Lagu multi-source: 2.323.
- Lagu multi-row: 3.065.
- Maksimum: 5 row per lagu logis.

Angka UI harus dihitung dari database aktual, bukan di-hardcode. Detail schema, master/tooling/runtime, hash, dan integrity gate hanya didefinisikan di `Docs/CATALOG_ARCHITECTURE.md`.

## 5. Identitas dan Batas Data
- Lagu logis: `(artist_slug, title_slug)`.
- Artist logis: `artist_slug`.
- Versi/source row: referensi utama `source_url`.
- `songs.id` adalah surrogate lokal dan tidak boleh menjadi persistent userdata key.
- Seluruh source/version dipertahankan; grouping bukan deduplikasi destruktif.
- Katalog read-only saat runtime.
- Mutable user state berada di `userdata.db`; jangan salin isi chord/lirik ke userdata.
- Resolution reference mengikuti exact `source_url`, `(source, content_hash)`, versi source sama, lalu source lain dari lagu logis yang sama. Substitusi lintas source harus diberitahukan; unresolved data tidak dihapus diam-diam.

Jangan mengarang album, durasi, BPM/tempo, genre, kategori, review count, rating/votes/key/difficulty yang hilang, Songsterr, label trending, atau statistik mockup.

## 6. Arsitektur Layer

```text
UI → ViewModel → Repository → DB/platform
```

UI hanya merender state dan mengirim intent. ViewModel mengelola state layar. Repository menjadi batas katalog, userdata, chart/cache, dan platform. DAO katalog hanya read-only; DAO userdata menangani mutasi user. Jangan mengakses SQLite, filesystem, atau network langsung dari composable.

## 7. Navigasi Global
Bottom navigation:

```text
Home | Explore | Setlists | Favorites | Settings
```

Detail layar menyembunyikan navigasi utama sesuai route/spec dan memakai back behavior konsisten.

Kontrak adaptif navigasi:
- Window compact tetap memakai bottom navigation saat ini.
- Pada layout landscape/tablet yang memakai navigation rail, rail berada di sisi kanan.
- Urutan tetap `Home | Explore | Setlists | Favorites | Settings`.
- Setiap tab rail menampilkan ikon di atas dan label di bawah; seluruh tile menjadi target tap.
- Warna/indikator aktif mengikuti gaya yang sudah ada tanpa blur, transparansi, border, shadow, atau gradient baru.
- Player dan layar detail tetap menyembunyikan navigasi utama.
- Pemilihan layout mengikuti lebar window, bukan orientasi saja, agar ponsel landscape dan tablet tidak dipaksa memakai susunan identik.

- Home/Search/Artist song tap → Song Versions.
- Pilih source/version → Player.
- Favorites/Setlist item yang sudah menyimpan `source_url` → Player versi tersimpan.
- Artist title tap selalu membuka version picker; source chip di Artist adalah label deduped non-interaktif.

## 8. Kontrak Tampilan
- Background `#0E1117`; surface `#161B22`; raised surface `#1A202C`; border `#242C3D`.
- Accent gold `#E5A93C`; muted gold `#2A210C`.
- Primary text `#F5F8FF`; muted text `#8E9AA8`.
- Heart aktif `#FF4D6D`.
- Source: UG `#F59E0B`/`#2A210C`; GT `#22C55E`/`#0D281E`; ChordTela `#38BDF8`/`#0C2340`.
- Komponen teratas rapat di bawah status bar, target sekitar 6–8dp; hindari padding ganda.
- Metadata NULL/kosong tidak ditampilkan sebagai `-` atau `null`.
- Sort alfabet menggunakan ordering case-insensitive.
- Mockup menentukan layout/hierarki visual; data ilustratif yang tidak ada di database harus dihilangkan, bukan dikarang.

## 9. Konten dan Logging
Player merender konten source-faithful: bracket yang tersimpan dipertahankan, baris chord polos tetap polos. Jangan mencetak isi lengkap chord/lirik ke log, terminal, laporan, atau chat. Debug hanya dengan metadata aman seperti jumlah row/chord, source, coverage, atau referensi.

## 10. Fakta Teknis
- Katalog Room dibuka dari `createFromAsset("catalog.db")`; schema entity dan tiga indeks harus cocok.
- DAO katalog tidak memiliki mutasi runtime.
- Userdata memakai migration eksplisit; jangan gunakan destructive fallback.
- Test katalog besar memerlukan heap yang memadai.
- Query SQLite harus memakai fitur yang tersedia di environment test; fallback harus tetap menghasilkan semantics yang benar.

## 11. Status Proyek
Status implementasi, baseline test/build, backlog aktif, dan keputusan deferred hanya dicatat di `PROJECT_PLAN.md`. Dokumen ini tetap menjadi kontrak global dan tidak boleh dipakai sebagai diary validasi atau dasar untuk mengklaim seluruh aplikasi selesai.