# 5 — Chord Player

**Status:** DIIMPLEMENTASIKAN — QA PENDING  
**Prioritas:** CORE FEATURE  
**Mockup resmi:** `mockups/Accepted/5 - Chord Player.jpg`  
**Source of truth:** `0 - INDEKS & FAKTA BERSAMA.md`

---

## 1. Kontrak Player saat ini

Player menampilkan sheet katalog lokal dan menyediakan:

- Auto Scroll,
- Page Flip,
- transpose + reset,
- capo user `0..12`,
- ukuran font,
- version switching,
- Favorite,
- Add to Setlist,
- diagram chord gitar nyata,
- keep-awake mengikuti preferensi Settings,
- Back bersama yang mengikuti konteks navigasi.

Bottom navigation disembunyikan pada Player. Tidak ada status pedal palsu atau iklan di area sheet/kontrol.

## 2. Status implementasi

### Sheet dan rendering — DIIMPLEMENTASIKAN — QA PENDING

- Konten berasal dari `catalog.db` dan dirender monospace.
- Parser mempertahankan section, chord, lyric, blank, dan posisi kolom.
- Rendering source-faithful: baris sumber ber-bracket tetap ber-bracket; baris chord polos tidak diberi bracket baru.
- Saat transpose memperlebar simbol, renderer memberi minimal satu spasi agar chord tidak menyatu.

### Transpose, capo, dan font — DIIMPLEMENTASIKAN — QA PENDING

- Transpose in-memory berada pada rentang `-11..+11` dan memiliki reset `+0`.
- Chord serta effective key mengikuti transpose; suffix dipertahankan oleh transposer.
- Capo adalah nilai user `0..12`, bukan metadata capo katalog. Kontrol berputar kembali ke 0 setelah 12.
- Ukuran font dapat dinaikkan/diturunkan dalam batas implementasi.
- Transpose/capo tersimpan bersama Favorite atau item Setlist dan dipulihkan ketika Player dibuka dari entry tersebut; ini bukan penyimpanan global per lagu untuk semua jalur masuk.

### Version switching — DIIMPLEMENTASIKAN — QA PENDING

Chip `vX/Y` membuka versi lintas source. Pergantian versi mempertahankan transpose dan mode. Pada mode scroll normal, posisi di-re-anchor ke section ekuivalen; Page Flip memulai versi baru dari halaman pertama.

### Auto Scroll dan Page Flip — DIIMPLEMENTASIKAN — QA PENDING

- Auto Scroll memiliki play/pause, kecepatan `0.5..4` baris/detik, rail vertikal saat berjalan, dan berhenti di akhir.
- Page Flip memakai halaman diskret `HorizontalPager` dengan page dots; halaman tidak scroll vertikal.
- Kedua mode saling eksklusif.
- Kecepatan default Auto Scroll berasal dari Settings dan perubahan rail disimpan kembali sebagai preferensi.

### Diagram chord — DIIMPLEMENTASIKAN — QA PENDING

- Data fingering nyata berasal dari `app/src/main/assets/chords_guitar.json` dan dimuat lazy melalui cache.
- Tap chord menampilkan kartu fretboard kecil di kanan atas.
- Lookup menangani mapping root aset, enharmonic, slash chord sebagai root chord, dan alias suffix yang didukung.
- Chord tanpa data menampilkan keadaan unavailable; diagram tidak dikarang.

### Keep-awake dan Back — DIIMPLEMENTASIKAN — QA PENDING

- Player mengaktifkan `keepScreenOn` hanya selama screen terlihat bila tile Keep Screen aktif, lalu mematikannya saat keluar.
- Default preferensi aktif dan dapat diubah langsung dari tile dua baris Keep Screen ON/OFF pada Player.
- Back memakai `AppBackButton` dan `popBackStack`; tujuan aktual bergantung pada jalur masuk. Dari Song Versions, route pemilih saat ini sudah di-pop ketika masuk Player.

### Favorite dan Add to Setlist — DIIMPLEMENTASIKAN — QA PENDING

- Header menampilkan Favorite dan Add to Setlist.
- Favorite menyimpan transpose/capo saat ini ke userdata.
- Add to Setlist mendukung daftar datar yang ada atau membuat setlist baru, menyimpan transpose/capo, dan menolak duplikat persis.
- **QA-1 tetap terbuka**: perilaku dan tampilan Favorite/Add to Setlist belum ditutup melalui QA device terbaru.

## 3. Bukti implementasi

- `app/src/main/java/com/chordapp/ui/screens/player/PlayerScreen.kt`
- `app/src/main/java/com/chordapp/ui/screens/player/ChordDiagramDialog.kt`
- `app/src/main/java/com/chordapp/viewmodels/PlayerViewModel.kt`
- `app/src/main/java/com/chordapp/player/`
- `app/src/main/java/com/chordapp/settings/PlayerPreferences.kt`
- `app/src/main/assets/chords_guitar.json`
- `app/src/test/java/com/chordapp/player/`
- `app/src/test/java/com/chordapp/ui/screens/player/PlayerChordLineTest.kt`
- `app/src/test/java/com/chordapp/viewmodels/PlayerViewModelTest.kt`

## 4. Verifikasi capability

- Gelombang Player sebelumnya mencatat **7/7 focused tests lulus** serta QA device untuk diagram dan source-faithful rendering.
- QA device belum menutup keseluruhan kontrol Player, version switching, keep-awake, saved transpose/capo, Favorite, dan Add to Setlist.
- Baseline project current hanya dicatat di `PROJECT_PLAN.md`; bukti focused Player tidak boleh dipakai untuk mengklaim seluruh aplikasi hijau.

## 5. Gap terbuka

1. Tutup QA-1 Favorite/Add to Setlist di device.
2. Regression QA seluruh kontrol: Auto Scroll, Page Flip, transpose/reset, capo 0..12, font size, dan version switching.
3. Verifikasi jalur Favorite/Setlist memulihkan saved transpose/capo.
4. Verifikasi keep-awake aktif/nonaktif mengikuti Settings dan selalu dibersihkan saat keluar.
5. Konfirmasi perilaku Back setelah Song Versions di-pop sesuai UX yang diinginkan.
6. Penuhi full-suite validation gate yang dicatat di `PROJECT_PLAN.md` sebelum status `DIIMPLEMENTASIKAN & TERVERIFIKASI`.

## 6. Acceptance checklist

- [x] Konten katalog dirender monospace dan source-faithful.
- [x] Transpose/reset dan effective key diterapkan.
- [x] Capo user 0..12 diterapkan tanpa mengubah katalog.
- [x] Font size dapat diubah.
- [x] Auto Scroll dan Page Flip diterapkan serta saling eksklusif.
- [x] Version switching mempertahankan transpose/mode.
- [x] Diagram memakai `chords_guitar.json`, bukan fingering buatan.
- [x] Keep-awake mengikuti preferensi Settings.
- [x] Favorite dan Add to Setlist terhubung ke userdata.
- [x] Back bersama dan bottom-nav tersembunyi diterapkan.
- [ ] QA-1 Favorite/Add to Setlist ditutup di device.
- [ ] Semua jalur Player lulus regression QA device.
- [ ] Full-suite validation gate di `PROJECT_PLAN.md` terpenuhi.
