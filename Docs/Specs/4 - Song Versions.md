# 4 — Song Versions

**Status:** DIIMPLEMENTASIKAN — QA PENDING  
**Mockup resmi:** `mockups/Accepted/4 - Song Versions.jpg`  
**Source of truth:** `0 - INDEKS & FAKTA BERSAMA.md`

---

## 1. Tujuan dan flow saat ini

Song Versions menampilkan semua transkripsi katalog lokal untuk satu lagu logis sebelum masuk Player.

- Lagu dengan lebih dari satu versi → tampilkan Song Versions.
- Lagu dengan tepat satu versi → redirect langsung ke Player.
- Tap kartu versi → langsung buka Player untuk versi tersebut.
- Tidak ada seleksi sementara dan tidak ada tombol `PLAY NOW`.

Saat berpindah dari Song Versions ke Player, route Song Versions saat ini di-pop dari back stack. Akibatnya, Back dari Player kembali ke layar sebelum Song Versions (misalnya Search, Artist Directory, atau Home), bukan kembali ke pemilih versi.

## 2. Status implementasi

### Header dan metadata — DIIMPLEMENTASIKAN — QA PENDING

Header menampilkan judul, artis, dan jumlah versi nyata. Metadata tambahan bersifat conditional: key hanya bila tersedia; kartu menampilkan difficulty/key bila tersedia serta jumlah chord dan line dari katalog. Album, tempo, durasi, tuning, atau metadata lain tidak dikarang.

### Filter source dan counts — DIIMPLEMENTASIKAN — QA PENDING

Filter yang tersedia:

- `All Sources`
- `Ultimate Guitar`
- `GuitarTuna`
- `ChordTela`

Setiap filter menampilkan count berdasarkan versi yang benar-benar dimuat. Source kosong tetap terlihat dalam keadaan muted dan menghasilkan daftar kosong bila dipilih.

### Version card — DIIMPLEMENTASIKAN — QA PENDING

- Menampilkan source dan `versionName` bila tersedia; fallback ke source bila tidak tersedia.
- Kartu pertama pada urutan aktif diberi badge `BEST`.
- Rating/votes hanya muncul bila `votes > 0`.
- Metadata kartu berasal dari entity katalog dan tidak dibuat-buat.
- Tap kartu langsung membuka Player.

### One-version redirect — DIIMPLEMENTASIKAN — QA PENDING

ViewModel mengirim navigasi Player dan tidak mempertahankan satu versi sebagai daftar yang perlu dirender. Perilaku ini memiliki unit test terfokus, tetapi belum dianggap verifikasi end-to-end penuh.

## 3. Bukti implementasi

- `app/src/main/java/com/chordapp/viewmodels/SongVersionsViewModel.kt`
- `app/src/main/java/com/chordapp/ui/screens/versions/SongVersionsScreen.kt`
- `app/src/main/java/com/chordapp/ui/navigation/AppNavigation.kt`
- `app/src/test/java/com/chordapp/viewmodels/SongVersionsViewModelTest.kt`

## 4. Verifikasi capability

- Unit test terfokus mencakup multi-version/filter dan one-version navigation.
- Belum ada bukti lengkap QA device untuk filter, metadata conditional, direct card-to-Player, one-version redirect, dan perilaku Back setelah route di-pop.
- Baseline project current hanya dicatat di `PROJECT_PLAN.md`.

## 5. Gap terbuka

1. QA end-to-end multi-version dan one-version pada device.
2. Konfirmasi UX bahwa Back dari Player memang harus melewati Song Versions karena route saat ini di-pop.
3. Verifikasi visual state source count 0 dan kartu dengan metadata parsial.
4. Tambahkan/rapikan test navigasi Compose bila diperlukan; unit test ViewModel saja tidak membuktikan back-stack/UI.

## 6. Acceptance checklist

- [x] Semua versi berasal dari katalog lokal.
- [x] Header menampilkan jumlah versi nyata.
- [x] Filter source dan count diterapkan.
- [x] Metadata tambahan ditampilkan secara conditional.
- [x] Rating hanya muncul saat `votes > 0`.
- [x] Satu versi melakukan redirect ke Player.
- [x] Tap kartu langsung membuka Player tanpa `PLAY NOW`.
- [x] Navigasi ke Player mem-pop route Song Versions.
- [ ] Flow, visual, dan Back lulus QA end-to-end di device.
