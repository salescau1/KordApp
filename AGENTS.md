# ChordApp Agent Constitution

## Misi
Membangun dan memelihara ChordApp sebagai aplikasi Android offline-first yang benar-benar bekerja dari spesifikasi yang disetujui, mockup yang diterima, katalog runtime, dan keputusan produk. UI yang tampak selesai bukan bukti bahwa fitur selesai.

## Sumber Kebenaran
Gunakan urutan berikut sesuai jenis fakta:
1. `Docs/Specs/` untuk requirement dan keputusan produk.
2. Referensi visual/mockup yang telah diterima untuk bentuk UI; jangan mengarang path bila artefaknya tidak ada di checkout.
3. `Docs/CATALOG_ARCHITECTURE.md` untuk kontrak data tunggal; `catalog/` dan `tools/catalog/rebuild_catalog.py` untuk input master dan proses tooling.
4. `app/src/main/assets/catalog.db` untuk fakta katalog runtime yang dikirim bersama aplikasi.
5. Kode dan test untuk status implementasi dan verifikasi aktual.

Requirement, status implementasi, dan hasil verifikasi adalah tiga hal berbeda. Jangan mengubah requirement agar sesuai kode yang ada, dan jangan menaikkan status hanya karena kode tersedia.

## Status yang Diizinkan
Dokumentasi status hanya boleh memakai:
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — implementasi ada dan bukti verifikasi relevan lulus.
- **DIIMPLEMENTASIKAN — QA PENDING** — implementasi ada, tetapi QA yang diwajibkan belum lengkap.
- **PARSIAL** — hanya sebagian requirement yang sudah diimplementasikan.
- **BELUM DIIMPLEMENTASIKAN** — requirement belum tersedia.

Jangan menyatakan semua hijau ketika ada test gagal/timeout atau QA wajib belum dilakukan.

## Aturan Data yang Tidak Dapat Ditawar
- Katalog runtime adalah `app/src/main/assets/catalog.db`, read-only saat runtime.
- Database runtime hanya memiliki tabel `songs` dan indeks `idx_artist_slug`, `idx_title_slug`, serta `idx_source`.
- Pertahankan seluruh 69.847 baris source/version; pengelompokan lagu lintas source bukan deduplikasi destruktif.
- Lagu logis diidentifikasi dengan `(artist_slug, title_slug)`; versi utama direferensikan dengan `source_url`.
- Jangan pernah memakai `songs.id` sebagai identitas userdata yang persisten.
- Data mutable berada di `userdata.db`; jangan menyalin isi chord/lirik ke sana.
- Pertahankan metadata yang kosong sebagai NULL/kosong. Jangan mengarang album, BPM, durasi, genre, rating, review, kategori, Songsterr, atau nilai lain yang tidak didukung.
- Jangan mencetak isi chord/lirik ke log, terminal, laporan, atau chat.
- Jangan memodifikasi database master di `catalog/`.
- `content_hash` mengikuti rebuild script aktual. Jangan mengklaim hash atas konten ternormalisasi kecuali script dan test memang menerapkannya.

## Batas Sistem
Aliran aplikasi adalah `UI → ViewModel → Repository → DB/platform`.
- UI tidak boleh membuka SQLite, filesystem, atau network secara langsung.
- ViewModel mengelola state layar dan memanggil repository.
- Repository menjadi batas akses katalog, userdata, chart/cache, dan platform.
- Katalog chord, pencarian, artist, version picker, Player, Favorites, dan Setlists harus tetap berfungsi offline.
- Network hanya untuk layanan yang memang dinamis, seperti chart dan iklan. Kegagalan network tidak boleh merusak fungsi inti offline.

## Kontrak UI dan Navigasi
- Gunakan istilah produk **Setlists**, bukan menggantinya dengan istilah lama.
- Bottom navigation: `Home | Explore | Setlists | Favorites | Settings`.
- Layar detail menyembunyikan bottom navigation sesuai spec dan menyediakan back behavior yang konsisten.
- Tap lagu dari Home/Search/Artist menuju Song Versions; versi yang dipilih membuka Player. Jalur yang secara eksplisit menyimpan `source_url`, seperti Favorites/Setlist, dapat membuka Player untuk versi tersebut.
- Warna heart aktif adalah `#FF4D6D`.
- Jangan hardcode statistik katalog atau data ilustratif mockup sebagai data produk.

## Metode Kerja
1. Baca spec dan kontrak data yang relevan.
2. Periksa kode, data, dan test aktual.
3. Pisahkan requirement, status implementasi, dan bukti verifikasi.
4. Implementasikan satu irisan yang koheren tanpa rewrite yang tidak perlu.
5. Jalankan test/build paling relevan dan perbaiki kegagalan yang berada dalam scope.
6. Lakukan QA device, visual, offline, dan release jika menjadi gate fitur.
7. Perbarui dokumentasi hanya setelah fakta atau keputusan yang diterima berubah.

## Kondisi Berhenti
Berhenti dan minta keputusan jika requirement saling bertentangan, data wajib tidak tersedia, source/master harus dimutasi, keputusan terbuka menghalangi implementasi, atau behavior memerlukan API/backend yang belum ditentukan.

## Selesai
Sebuah fitur selesai hanya jika acceptance criteria, data nyata, navigasi, behavior, persistence, edge case, dan verifikasi yang sesuai telah lulus. Gunakan `DEFINITION_OF_DONE.md` sebagai checklist reusable. `PROJECT_PLAN.md` adalah satu-satunya ledger hidup untuk status, baseline validasi, backlog aktif, dan keputusan deferred. Seluruh pekerjaan proyek harus tetap berada di `C:\Chord APK`; jangan mencari atau memasang artefak proyek bernama mirip dari lokasi lain.