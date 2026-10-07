# 6 — Setlists

**Status:** DIIMPLEMENTASIKAN — QA PENDING  
**Istilah final user-facing:** `Setlists`  
**Source of truth:** implementasi aplikasi, userdata lokal, dan `0 - INDEKS & FAKTA BERSAMA.md`

---

## 1. Requirement

Layar ini mengelola satu koleksi datar Setlists:

- Tidak ada Songbook, folder, atau pemisahan Standalone.
- Istilah utama yang terlihat pengguna harus `Setlists`, bukan `Playlists`.
- Pengguna dapat membuat Setlist langsung dari tombol `+`.
- Lagu ditambahkan dari Player, bukan melalui aksi `Add Song` pada layar koleksi.
- Exact duplicate harus ditolak secara eksplisit.
- Rename/delete harus tersedia.
- Daftar utama memerlukan search dan sort.
- Catalog tetap read-only dan userdata tidak bergantung pada `songs.id`.

## 2. Model Data

### `setlists`

- `id`, `name`, `note`, `created_at`, `updated_at`.

### `setlist_items`

- `id`, `setlist_id`, `position`, `source_url`, `artist_slug`, `title_slug`, `source`, `content_hash`, `transpose`, `capo`, `note`.

Referensi lagu disimpan sebagai identitas logis/source; catalog tidak dimutasi.

## 3. Implementasi Aktual

- `SetlistEntity` dan `SetlistItemEntity` membentuk koleksi datar; model aktif tidak memiliki Songbook atau `songbookId`.
- Root berjudul `SETLISTS`, menampilkan satu daftar, dan FAB `+` langsung membuka dialog pembuatan Setlist.
- Search global, clear-X, dan sort tersedia; tap item langsung membuka Tracklist.
- Player memakai aksi Add to Setlist untuk memilih Setlist yang ada atau membuat Setlist baru, lalu menyimpan lagu aktif beserta transpose/capo.
- Exact duplicate didefinisikan sebagai kombinasi `source_url + transpose + capo` yang sama di Setlist yang sama. Repository mengembalikan `AlreadyPresent`; lagu/source yang sama dengan transpose atau capo berbeda tetap boleh ditambahkan.
- Rename/delete Setlist tersedia dan penghapusan Setlist menghapus item melalui cascade.
- Migration eksplisit `3 → 4` mereset hanya domain Setlists development dan mempertahankan Favorites serta play history.
- Route Songbook dan layar nested sudah dikeluarkan dari navigasi aktif.

## 4. Verifikasi yang Ada

- `UserdataRepositoryTest` memverifikasi flat CRUD, create Setlist, penolakan exact duplicate, dan penerimaan transpose berbeda.
- `UserdataMigrationTest` memverifikasi migration `3 → 4` menghapus data Setlists lama tanpa menghapus Favorites dan play history.
- `PlayerViewModelTest` memverifikasi Add to Setlist membawa transpose/capo, menolak exact duplicate, dan create-and-add memakai Setlist datar.
- Full unit suite terbaru lulus; baseline keseluruhan hanya dicatat di `PROJECT_PLAN.md`.
- Verifikasi tersebut adalah unit/Robolectric; QA perangkat untuk seluruh alur koleksi belum selesai.

## 5. Gap

- Device QA flat CRUD, Add to Setlist, duplicate feedback, rename/delete, search/sort, dan backstack belum lengkap.
- Update-survival dan state substitution/unavailable untuk item Setlist belum dibuktikan end-to-end.
- Tracklist masih memiliki gap unrestricted reorder yang dicatat terpisah pada Spec 7 dan `PROJECT_PLAN.md`.

## 6. Acceptance

- **DIIMPLEMENTASIKAN — QA PENDING** — istilah user-facing dan root final memakai `SETLISTS`.
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — backend Setlists datar tanpa Songbook/nested/Standalone.
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — create Setlist dan create-and-add dari Player.
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — exact duplicate ditolak tanpa menduplikasi row.
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — userdata memakai referensi source/logis dan catalog tetap read-only.
- **DIIMPLEMENTASIKAN — QA PENDING** — rename/delete, FAB, search, sort, navigasi, dan backstack tersedia tetapi belum ditutup lewat QA perangkat.
- **PARSIAL** — update-survival serta substitution/unavailable item Setlist belum dibuktikan end-to-end.