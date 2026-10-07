# 9 — Favorites Screen

**Status:** PARSIAL  
**Source of truth:** implementasi aplikasi, userdata lokal, dan `0 - INDEKS & FAKTA BERSAMA.md`

---

## 1. Requirement

Favorites adalah daftar lagu yang disimpan pengguna dan dapat digunakan offline.

- Header dan jumlah lagu tersimpan.
- Search judul/artis secara case-insensitive.
- Sort berdasarkan Date, Title, atau Artist.
- Baris menampilkan nomor, judul, artis, status/source, hati aktif untuk delete, dan play.
- Favorite bertahan di userdata lokal dan tidak dihapus otomatis jika versi catalog hilang.
- Tap lagu membuka Player dengan transpose/capo tersimpan.
- Delete harus memiliki undo.
- Swipe-to-delete adalah interaksi yang ditargetkan.
- Metadata yang tidak tersedia, seperti BPM/durasi/tag rekaan, tidak boleh ditampilkan.

## 2. Persistence dan Resolution Requirement

Favorite menyimpan `source_url`, `artist_slug`, `title_slug`, `source`, `content_hash`, `transpose`, `capo`, serta timestamp; bukan `songs.id`.

Urutan resolution:

1. exact `source_url`;
2. exact `(source, content_hash)` pada lagu logis yang sama;
3. fallback ke versi source yang sama;
4. fallback ke source lain untuk lagu logis yang sama dan tandai substitusi;
5. jika tidak ada versi, pertahankan row sebagai tidak tersedia.

## 3. Implementasi Aktual

- Room menyimpan favorite secara lokal dengan unique `source_url`.
- Repository mendukung add/toggle/remove/restore serta resolution cascade yang sama dengan Setlist.
- Favorite yang tidak resolve tetap tampil sebagai `Tidak tersedia`; versi dari source lain ditandai `Versi pengganti`.
- Favorites UI memiliki counter, search judul/artis, sort `Terbaru`/`Judul`/`Artis`, empty/no-match/loading state, dan daftar lagu.
- Tap row available membuka Player memakai resolved URL serta saved transpose/capo.
- Hati aktif menghapus favorite dan Snackbar menyediakan `Urungkan` untuk restore.
- Swipe-to-delete belum ada.
- Carousel artis tidak diimplementasikan dan bukan requirement final.

## 4. Verifikasi yang Ada

- `UserdataRepositoryTest.favoriteTogglePersistsPlayerStateAndRejectsDuplicates` memverifikasi penyimpanan transpose/capo, unique favorite, direct resolution, dan toggle delete.
- `UserdataMigrationTest` memverifikasi schema/migration favorite dan preservasi row pada migration yang diuji.
- `PlayerViewModelTest` memverifikasi toggle favorite dari Player.
- Search/sort, remove+undo UI, substitution/unavailable UI, dan pengiriman saved state ke Player terlihat pada wiring implementasi tetapi belum memiliki test ViewModel/UI khusus.
- Belum ada QA restart aplikasi/perangkat atau simulasi catalog-update untuk membuktikan survival end-to-end.

## 5. Gap

- Swipe-to-delete belum diimplementasikan; delete saat ini melalui ikon hati.
- Belum ada dedicated `FavoritesViewModel` test untuk search, sort, fallback, unavailable, dan state loading.
- Belum ada QA perangkat untuk delete+undo dan saved transpose/capo saat Player terbuka.
- Belum ada pengujian survival setelah update/replacement catalog nyata.

## 6. Acceptance

- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — persistence schema, duplicate protection, toggle, dan saved transpose/capo pada repository.
- **DIIMPLEMENTASIKAN — QA PENDING** — favorite bertahan setelah restart aplikasi.
- **DIIMPLEMENTASIKAN — QA PENDING** — fallback resolution, substitution, unavailable state, dan survival sesudah catalog diperbarui.
- **DIIMPLEMENTASIKAN — QA PENDING** — search judul/artis dan sort Date/Title/Artist.
- **DIIMPLEMENTASIKAN — QA PENDING** — delete melalui hati + undo.
- **DIIMPLEMENTASIKAN — QA PENDING** — saved transpose/capo diteruskan ke Player.
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — UI tidak menampilkan BPM, durasi, atau tag rekaan.
- **BELUM DIIMPLEMENTASIKAN** — swipe-to-delete.
- **BELUM DIIMPLEMENTASIKAN** — dedicated ViewModel/device/catalog-update survival QA.