# ChordApp Definition of Done

Dokumen ini adalah checklist reusable untuk setiap perubahan/fitur. Requirement berada di `Docs/Specs/`; status dan baseline validasi saat ini hanya berada di `PROJECT_PLAN.md`.

## Status yang Diizinkan
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — implementasi tersedia dan seluruh verifikasi relevan untuk scope tersebut lulus.
- **DIIMPLEMENTASIKAN — QA PENDING** — implementasi tersedia, tetapi satu atau lebih QA wajib belum selesai.
- **PARSIAL** — hanya sebagian requirement tersedia.
- **BELUM DIIMPLEMENTASIKAN** — requirement belum tersedia.

## Checklist Reusable

### Requirement
- [ ] Spec di `Docs/Specs/` dan mockup yang relevan telah dibaca.
- [ ] Acceptance criteria dipenuhi tanpa mengubah requirement agar cocok dengan implementasi.
- [ ] Tidak ada data, metadata, API, atau behavior yang dikarang.
- [ ] Kontradiksi atau keputusan terbuka yang blocking telah diselesaikan secara eksplisit.

### Arsitektur dan Data
- [ ] Aliran `UI → ViewModel → Repository → DB/platform` dipertahankan.
- [ ] Data katalog berasal dari `app/src/main/assets/catalog.db` dan tetap read-only saat runtime.
- [ ] Seluruh source/version dan metadata opsional dipertahankan; missing tetap missing.
- [ ] Userdata mutable hanya berada di `userdata.db` dan tidak menyalin chord/lirik.
- [ ] Persistent catalog reference tidak memakai `songs.id`; gunakan `source_url` dan fallback yang didokumentasikan.
- [ ] Perubahan katalog lolos audit kontrak di `Docs/CATALOG_ARCHITECTURE.md`.
- [ ] Tidak ada destructive catalog migration atau mutasi master database di `catalog/`.

### UI dan Navigasi
- [ ] Mockup dan aturan global yang diterima diikuti.
- [ ] Terminologi produk konsisten, termasuk **Setlists**.
- [ ] Bottom navigation dan detail-screen visibility sesuai kontrak.
- [ ] Back behavior, route parameters, dan pemilihan version/source bekerja.
- [ ] Heart aktif memakai `#FF4D6D`.
- [ ] Loading, empty, unavailable, substituted, offline, dan error state yang relevan ditangani.
- [ ] Teks ilustratif mockup dan statistik katalog tidak di-hardcode sebagai data produk.

### Behavior dan Persistence
- [ ] Interaksi inti bekerja, bukan hanya tampilan UI.
- [ ] State yang wajib tersimpan bertahan setelah recreation/relaunch.
- [ ] Favorites dan Setlists menyimpan referensi katalog yang stabil serta transpose/capo yang relevan.
- [ ] Resolution cascade berjalan dan substitusi lintas source diberitahukan kepada user.
- [ ] Fitur inti katalog/Player tetap bekerja tanpa network.
- [ ] Iklan/chart gagal atau offline tidak merusak fungsi inti.

### Verifikasi
- [ ] Unit test terarah untuk behavior yang berubah lulus.
- [ ] Repository/ViewModel test yang relevan lulus.
- [ ] `testDebugUnitTest` lulus tanpa failure atau timeout.
- [ ] Debug build bersih berhasil dari source saat ini.
- [ ] Install dan smoke test device/emulator selesai untuk flow yang berubah.
- [ ] Visual comparison selesai pada layar terdampak.
- [ ] Offline end-to-end test selesai jika fitur menyentuh fungsi inti.
- [ ] Release build dan release smoke test selesai sebelum release.

### Kualitas dan Dokumentasi
- [ ] Isi chord/lirik tidak muncul di log, terminal, laporan, atau chat.
- [ ] Tidak ada network dependency tak disengaja untuk fitur offline.
- [ ] Tidak ada blocking TODO atau fallback palsu.
- [ ] Dokumentasi diperbarui jika requirement/arsitektur yang diterima berubah.
- [ ] `PROJECT_PLAN.md` diperbarui setelah bukti baru; spec dan checklist tidak dijadikan diary validasi.
- [ ] Klaim status menyebut bukti dan tidak menyatakan semua hijau bila masih ada gap.

## Gate Status
- Gunakan **DIIMPLEMENTASIKAN & TERVERIFIKASI** hanya bila semua item relevan pada checklist telah lulus.
- Gunakan **DIIMPLEMENTASIKAN — QA PENDING** jika implementasi lengkap tetapi device, visual, offline, release, atau test wajib belum ditutup.
- Gunakan **PARSIAL** bila behavior/requirement masih sebagian.
- Gunakan **BELUM DIIMPLEMENTASIKAN** bila implementasi belum ada.

Jangan menyalin hasil run atau snapshot tanggal ke dokumen ini. Catat bukti current, failure aktif, dan backlog hanya di `PROJECT_PLAN.md`.
