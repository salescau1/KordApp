# ChordApp Project Plan — Status Ledger

**Tanggal snapshot:** 10 September 2026  
**Validasi terakhir:** 10 September 2026  
**Peran:** satu-satunya ledger status implementasi, baseline validasi, active backlog, dan keputusan deferred. Requirement tetap berada di `Docs/Specs/`; kontrak katalog berada di `Docs/CATALOG_ARCHITECTURE.md`; gate reusable berada di `DEFINITION_OF_DONE.md`.

## Arti Status
- **DIIMPLEMENTASIKAN & TERVERIFIKASI:** implementasi ada dan verifikasi relevan untuk capability tersebut lulus.
- **DIIMPLEMENTASIKAN — QA PENDING:** implementasi ada, tetapi QA wajib belum lengkap.
- **PARSIAL:** hanya sebagian requirement tersedia.
- **BELUM DIIMPLEMENTASIKAN:** requirement belum tersedia.

## Ledger Capability

| Area / capability | Status | Bukti saat ini | Gap nyata |
|---|---|---|---|
| Foundation Android, Compose, Room, navigation shell | **DIIMPLEMENTASIKAN — QA PENDING** | Proyek Android, lima tab, detail routes, repository/ViewModel, katalog dan userdata terpisah tersedia; shell memakai bottom bar pada lebar compact dan right rail pada lebar >= `840.dp`; debug/release assembly berhasil. | Release/device acceptance menyeluruh dan visual adaptive navigation belum ditutup. |
| Katalog runtime | **DIIMPLEMENTASIKAN & TERVERIFIKASI** | `app/src/main/assets/catalog.db` read-only; 69.847 row, 65.538 lagu logis, 20.778 artis; seluruh source/version dipertahankan. | Tidak ada gap implementasi aktif; integrity gate tetap wajib untuk perubahan katalog berikutnya. |
| Home: adaptive layout, Top 20, cache, history, database, ad collapse | **DIIMPLEMENTASIKAN — QA PENDING** | Compact mempertahankan one-recent/bottom bar; expanded >= `840.dp` memakai panel kiri app/Search/recent-five/statistik katalog aktual, Top 20 di tengah, dan right rail. Selector Spotify/YouTube, cache persisten per platform, pilihan terakhir, cache-first background refresh, stale-response guard, whole-row navigation, unavailable state, dan ad collapse tersedia; focused/full tests lulus. | Visual adaptive-window dan device/offline QA belum lengkap. |
| Search: title/artist, exact artist hero, navigation | **PARSIAL** | Autofocus, pencarian title/artist, exact artist hero, clear-X yang mempertahankan fokus, bottom nav, dan navigasi hasil tersedia. | Lyric search, explicit sort, artist count hero, dan device QA belum tersedia. |
| Artist Directory | **DIIMPLEMENTASIKAN — QA PENDING** | Lagu logis A–Z, filter judul lokal, kartu menuju Song Versions, dan badge source non-interaktif tersedia. | Device QA, category-leak sampling, cross-source slug sampling, dan ikon X pada filter belum lengkap. |
| Song Versions | **DIIMPLEMENTASIKAN — QA PENDING** | Filter source, metadata kondisional, one-version redirect, dan direct card-to-Player tersedia. | Device/visual edge-case QA dan konfirmasi UX Back masih terbuka. |
| Chord Player core | **DIIMPLEMENTASIKAN — QA PENDING** | Rendering source-faithful, diagram nyata dengan seleksi posisi paling nyaman (open shape > sedikit jari > span pendek > tanpa barre > fret rendah; `findPositions` membuka alternatif, card menampilkan bentuk terbaik + 1 alternatif), transpose/capo internal, frame-based auto-scroll, swipe/tap Page Flip tanpa wrap, font, version switching, Favorite, Add to Setlist, tile Keep Screen persisted, dan shared Back tersedia; focused tests lulus. | Gesture precedence, smoothness, Keep Screen lifecycle, dan visual diagram perlu QA perangkat. |
| Setlists repository dan Player add flow | **DIIMPLEMENTASIKAN — QA PENDING** | Schema/repository datar, explicit 3→4 domain-only reset, Favorites/history preservation, duplicate handling, stable references, dan flat Player picker tersedia; focused tests lulus. | Device QA Add to Setlist dan update-survival belum lengkap. |
| Setlists root UI | **DIIMPLEMENTASIKAN — QA PENDING** | `SETLISTS`, satu daftar datar, direct FAB create, direct Tracklist navigation, global search/clear, dan sort tersedia. | CRUD/backstack/visual QA perangkat belum lengkap. |
| Setlist Tracklist core | **PARSIAL** | Load item, resolution/substitution/unavailable, block playback unavailable, saved transpose/capo, delete+undo, rename/delete, scroll state, dan persistence reorder tersedia. | Drag baru berpindah satu posisi per gesture; unrestricted reorder dan device QA update-catalog/scroll restoration belum lengkap. |
| Explore functional flow | **DIIMPLEMENTASIKAN — QA PENDING** | Tepat A–Z, grid lima kolom, sel 1:1, disabled empty letters, search/category reset, clear-X, real artist counts, dan navigation tersedia; full unit suite terbaru lulus. | Visual/device QA belum lengkap. |
| Favorites core | **PARSIAL** | Persistence, catalog resolution, local search/clear-X, sort, remove+undo, dan saved transpose/capo menuju Player tersedia. | Swipe interaction, dedicated ViewModel/device QA, dan catalog-update survival belum lengkap. |
| Settings / About | **DIIMPLEMENTASIKAN — QA PENDING** | Runtime catalog/app stats tersedia; Keep Screen dan speed editor sudah dikeluarkan dari Settings. About detail memakai bundled fallback/cache-first refresh, HTTPS-only bounded remote, UTF-8/plain text/size guards, atomic cache, runtime app/version metadata, dan focused tests. | About online/cache/offline dan visual/device QA belum selesai; theme, default Player mode, safe cache clear, dan export tetap belum tersedia. |
| Resolusi userdata setelah catalog update | **PARSIAL** | Cascade exact `source_url` → `(source, content_hash)` → versi source sama → source lain pada lagu logis tersedia. | Notice substitusi/unavailable dan survival Favorites/Setlists belum dibuktikan end-to-end. |
| QA integrasi | **PARSIAL** | Full unit suite terbaru **108/108 lulus**; `lintDebug`, `assembleDebug`, dan `assembleRelease` berhasil pada 10 September 2026. | Visual adaptive Home, offline end-to-end, persistence/update-survival, dan performance/memory QA belum lengkap. |
| Release | **PARSIAL** | `assembleRelease` berhasil pada baseline 10 September 2026. | Release smoke/acceptance dan packaging/persistence gates belum selesai. |

## Baseline Validasi Aktif
- `testDebugUnitTest --rerun-tasks`: **108 test; 108 pass; 0 failure** pada 10 September 2026. Coverage baru mencakup breakpoint adaptive/navigation visibility, recent-five newest-first, runtime catalog stats, dan stale-platform Home.
- `lintDebug`: **berhasil** pada 10 September 2026; laporan masih memuat warning non-blocking.
- `assembleDebug`: **berhasil** pada 10 September 2026.
- `assembleRelease`: **berhasil** pada 10 September 2026.
- Build gabungan awal kehabisan heap ketika mengompresi aset release; heap Gradle dinaikkan dari 2 GiB ke 4 GiB dan validasi diulang dengan isolated no-daemon build hingga lulus.
- Latest debug APK berhasil di-install melalui ADB dan diluncurkan pada Pixel 6 Pro tanggal 10 September 2026; `com.chordapp/.MainActivity` terkonfirmasi sebagai `topResumedActivity` dan proses aplikasi aktif.
- Install/launch tersebut hanya membuktikan startup build terbaru; interaction, visual, offline, persistence/update-survival, performance/memory, dan release acceptance QA belum lengkap.
- Visual QA, offline end-to-end QA, persistence/update-survival QA, performance/memory QA, dan release smoke/acceptance QA belum lengkap.

## Active Backlog — Urutan Prioritas
1. Sambungkan kembali USB ADB Pixel 6 Pro, install latest debug APK, lalu lakukan device QA untuk Home di bawah/atas `840.dp` (compact bottom bar vs expanded tiga area/right rail), switching platform/cache/offline, Player Keep Screen/auto-scroll/Page Flip/diagram, flat Setlists, clear-X focus, serta About online/cache/offline.
2. Lengkapi Search capability: lyric search, explicit sort, dan artist count hero.
3. Implementasikan unrestricted Tracklist reorder; buktikan scroll restoration dan update-catalog behavior.
4. Lengkapi Favorites swipe interaction serta update-survival/device QA.
5. Lengkapi Settings lain yang masih diterima: theme, default Player mode, safe cache clear, dan export.
6. Tutup integration QA Favorites/Setlists dan cascade substitution/unavailable.
7. Jalankan focused visual, offline, persistence, performance/memory, dan release acceptance QA.
8. Konfirmasi UX Back Song Versions → Player sebagai keputusan produk final.

Catatan keputusan UI: bottom navigation `Home | Explore | Setlists | Favorites | Settings` tetap memakai tampilan saat ini; tidak ditambahkan blur, transparansi, border, shadow, atau gradient baru. Full unit suite saat ini hijau, sehingga stabilisasi `ExploreViewModelTest` tidak lagi menjadi backlog aktif; tetap jalankan regression test bila kode Explore atau dependency terkait berubah.

## Deferred / Open — Bukan Active Backlog
- Gig Mode.
- Bluetooth pedal dan left-handed fretboard.
- Real ad-provider integration selama behavior accepted tetap collapse saat tidak ada iklan.

### PR GT (dicatat 2026-09-21, tidak di-re-scrape sekarang)
**Keputusan user:** data GT apa adanya — tidak di-re-scrape. UG (`C:\ChordApp\ug_crawler\data\ug\ug_catalog.db`, 154.437 row) dianggap sumber utama chord karena lebih komplit dan rapih. GT (48.918 row) jadi suplemen.

**Bug yang tercatat di GT (756 lagu terdampak, 3.588 baris merged = 0.149% dari total 2.406.927 baris lirik GT):** crawler GT (`C:\ChordApp\guitartuna_crawler\crawler.py`, `build_sheet_from_json`/`build_sheet_from_dom`) kadang menggabungkan baris chord overlay dan lirik ke satu baris fisik, contoh: `/E /Eb Didn't mean to make you cry` (Queen — Bohemian Rhapsody, GT original).

**Mengapa tidak di-re-scrape sekarang:** tidak ada cache HTML mentah per-lagu di lokal — `index_cache` dan `artist_cache` hanya menyimpan halaman artist (index), bukan konten lagu; `batch_NN.db`/`merged.db` hanya menyimpan `content` yang sudah ter-parse (sudah digabung). Re-scrape 756 URL diperkirakan ±1–2 jam di 1 mesin atau ±15–30 menit di 6 mesin paralel.

**Workaround yang sudah diterapkan di app:** `ChordParser.isChordSymbol` kini mengenali chord slash polos (`/E`, `/Eb`), dan `isInlinePlainOverlay` memisahkan baris GT yang digabung jadi overlay chord di atas + lirik di bawah, sehingga tampilan di app sudah benar meskipun sumber data masih "salah".

**PR yang masih terbuka (belum dikerjakan):**
1. Perbaiki logika `crawler.py` GT (`build_sheet_from_json`/`build_sheet_from_dom`) supaya chord dan lirik di hasil parse selalu jadi 2 baris terpisah, bukan 1 baris fisik.
2. Setelah crawler diperbaiki, re-scrape 756 URL GT yang terdampak (bukan 48.918 lagi).
3. Pertimbangkan guard di merge pipeline GT/UG untuk prioritaskan UG pada lagu yang sama di kedua sumber.

Item yang sudah selesai tidak boleh dimasukkan kembali ke backlog tanpa regresi atau requirement baru yang diterima. Status hanya boleh dinaikkan dengan bukti baru; jangan mengubah requirement agar ledger tampak selesai.
