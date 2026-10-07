# 7 — Setlist Tracklist

**Status:** PARSIAL  
**Source of truth:** implementasi aplikasi, userdata lokal, dan `0 - INDEKS & FAKTA BERSAMA.md`

---

## 1. Requirement

Tracklist menampilkan isi satu Setlist sebagai daftar berurutan:

- nomor, judul, artis, source/status, dan aksi play;
- drag handle untuk reorder dan persist `setlist_items.position`;
- delete item dengan undo tanpa mengubah catalog;
- tap lagu membuka Player dengan transpose/capo yang tersimpan;
- posisi list dipertahankan ketika kembali dari Player;
- rename/delete Setlist tersedia dengan konfirmasi delete;
- tidak ada BPM/durasi rekaan dan tidak ada `Add Song` pada layar ini.

Gig Mode tetap keputusan terbuka dan tidak boleh diimplementasikan sebelum keputusan produk final.

## 2. Resolution Requirement

Saat memuat item, referensi catalog diselesaikan berurutan:

1. exact `source_url`;
2. exact `(source, content_hash)` pada lagu logis yang sama;
3. fallback logis `(artist_slug, title_slug)`, dengan preferensi source yang sama;
4. source lain untuk lagu yang sama sebagai substitusi;
5. jika tidak ada versi, item tetap terlihat sebagai tidak tersedia dan tidak dapat dibuka.

Catalog tetap read-only.

## 3. Implementasi Aktual

- `SetlistTracklistViewModel` mengamati item tersimpan, menjalankan resolution cascade, dan membentuk state available/substituted.
- UI menandai `Versi pengganti` untuk substitusi source lain dan `Tidak tersedia` jika resolution gagal.
- Tap item available mengirim resolved `source_url`, saved transpose, dan saved capo ke Player.
- Delete tersedia sebagai tombol ikon; Snackbar `Urungkan` mengembalikan item dan menormalkan ulang posisi.
- Rename dan delete Setlist tersedia pada menu kanan atas; delete memakai dialog konfirmasi dan kembali sesudah selesai.
- `LazyListState` memakai `rememberSaveable(setlistId, Saver)` untuk mempertahankan state list selama state navigasi Compose tersimpan.
- Tidak ada aksi Add Song, BPM, atau durasi.
- Reorder saat ini baru memindahkan satu posisi bersebelahan ke atas/bawah setelah satu drag selesai. Jarak drag tidak menghasilkan perpindahan multi-posisi dan item tidak bergerak secara kontinu selama drag.

## 4. Verifikasi yang Ada

- `UserdataRepositoryTest.testTracklistAndResolution` memverifikasi penyimpanan item, transpose/capo, reorder repository, direct resolution, dan delete.
- `UserdataRepositoryTest.createSetlistReturnsIdAndExactDuplicatesAreRejected` memverifikasi posisi berurutan dan aturan duplicate.
- Wiring navigasi Tracklist → Player membawa transpose/capo; back menggunakan navigation back stack.
- Belum ada test UI khusus untuk unavailable/substitution label, undo, rename/delete dialog, `rememberSaveable`, atau gesture drag.
- Belum ada QA perangkat untuk kembali dari Player dan restorasi posisi scroll.

## 5. Gap

- Gesture reorder belum memenuhi reorder bebas; hanya satu adjacent step per completed drag.
- Delete fungsional memakai ikon, bukan swipe-to-delete.
- Resolution fallback dan unavailable sudah diimplementasikan, tetapi skenario catalog berubah belum diuji secara khusus.
- State list memakai `rememberSaveable`, tetapi survival aktual pada back navigation/configuration change belum diverifikasi di perangkat.
- Gig Mode masih deferred/open dan sengaja belum diimplementasikan.

## 6. Acceptance

- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — item tersimpan dimuat berurutan dan direct resolution bekerja.
- **DIIMPLEMENTASIKAN — QA PENDING** — substitution/unavailable state dan fallback resolution penuh.
- **DIIMPLEMENTASIKAN — QA PENDING** — tap item membawa saved transpose/capo ke Player dan back memakai konteks sebelumnya.
- **DIIMPLEMENTASIKAN — QA PENDING** — delete item + undo tanpa menyentuh catalog.
- **DIIMPLEMENTASIKAN — QA PENDING** — rename/delete Setlist.
- **DIIMPLEMENTASIKAN — QA PENDING** — `rememberSaveable` mempertahankan list state secara desain.
- **PARSIAL** — reorder tersimpan, tetapi gesture hanya satu langkah bersebelahan per drag selesai.
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — tidak ada Add Song, BPM, atau durasi rekaan pada Tracklist.
- **BELUM DIIMPLEMENTASIKAN** — swipe delete.
- **BELUM DIIMPLEMENTASIKAN** — Gig Mode; tetap keputusan terbuka.