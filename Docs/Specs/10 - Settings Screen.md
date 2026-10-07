# 10 — Settings Screen

**Status:** PARSIAL  
**Source of truth:** implementasi aplikasi, preference lokal, catalog offline, dan konfigurasi build

---

## 1. Requirement

Settings hanya boleh menampilkan kontrol dan informasi yang benar-benar berfungsi:

- jumlah artis, jumlah lagu unik, dan ukuran catalog dari data/file aktual;
- version/build dari konfigurasi aplikasi dan ukuran APK terpasang;
- About Application yang menampilkan cache/fallback segera dan menyegarkan plain text publik di background;
- theme jika theme alternatif benar-benar tersedia;
- default Player mode jika mode tersebut benar-benar diterapkan;
- export referensi dan userdata tanpa isi chord/lyrics.

Bluetooth pedal berada di luar scope saat ini. Jangan tampilkan baris/status koneksi palsu. Fretboard left-handed juga dihapus dari scope saat ini.

## 2. Implementasi Aktual

### Player preferences

- Keep Screen dipindahkan ke Player dan tidak lagi diedit dari Settings.
- Default auto-scroll speed tetap preference internal Player dan tidak lagi ditampilkan sebagai setting global.

### Informasi aktual

- Jumlah artis dan lagu unik berasal dari query repository/catalog.
- Ukuran catalog berasal dari panjang file database `catalog.db` aktual.
- Version name berasal dari `PackageManager`, yaitu konfigurasi aplikasi, bukan string UI hardcode.
- Ukuran APK berasal dari panjang file `applicationInfo.sourceDir` aktual.

### About Application

- Settings membuka detail About dan bottom navigation disembunyikan pada route detail.
- Nama aplikasi, version name, dan version code berasal dari runtime metadata.
- Konten plain text menampilkan cache atau fallback bundled segera lalu refresh di background dari Google Docs export HTTPS.
- Remote source membatasi redirect, timeout, content type, UTF-8, dan ukuran respons 256 KiB; cache diganti melalui temporary file dan atomic move bila didukung.
- Kegagalan refresh mempertahankan konten lama dan isi dokumen tidak dicatat ke log.

### Fitur yang tidak ditampilkan

- Tidak ada status/baris Bluetooth pedal palsu.
- Tidak ada kontrol left-handed yang sudah dihapus dari scope.
- Theme alternatif, default Player mode, clear cache, dan export belum tersedia dan tidak ditampilkan.

## 3. Verifikasi yang Ada

- `PlayerPreferencesTest` memverifikasi default keep-awake/default speed, persistence lintas instance, dan clamp speed.
- `PlayerViewModelTest` memverifikasi Player membaca keep-awake/default speed dan perubahan speed tersimpan.
- Inspeksi Player UI mengonfirmasi preference keep-awake diterapkan ke Android View.
- Inspeksi `SettingsViewModel` mengonfirmasi count, ukuran catalog, version, dan ukuran APK berasal dari runtime/configuration aktual.
- `AboutRepositoryTest`, `HttpAboutRemoteSourceTest`, dan `AboutViewModelTest` memverifikasi cache/fallback, refresh preservation, plain-text/HTTPS/size/redirect guards, serta stale-while-refresh.
- Belum ada dedicated test untuk `SettingsViewModel`, query count, ukuran file, atau visual/device QA Settings/About.

## 4. Gap

- Theme selection belum diimplementasikan.
- Default Player mode belum diimplementasikan.
- Clear cache belum diimplementasikan; karena itu acceptance keselamatan clear belum dapat diverifikasi.
- Export Setlist belum diimplementasikan; aturan tidak menyertakan chord/lyrics belum dapat diverifikasi end-to-end.
- Tidak ada QA perangkat khusus untuk memastikan Keep Screen, About online/cache/offline, dan metadata runtime bekerja pada app terpasang.

## 5. Acceptance

- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — keep screen awake disimpan dan digunakan Player.
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — default scroll speed disimpan, dibatasi, dan digunakan Player.
- **DIIMPLEMENTASIKAN — QA PENDING** — jumlah catalog berasal dari query aktual.
- **DIIMPLEMENTASIKAN — QA PENDING** — ukuran catalog, version, dan ukuran APK berasal dari runtime/configuration aktual.
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — tidak ada status Bluetooth palsu; pedal tetap di luar scope.
- **DIIMPLEMENTASIKAN & TERVERIFIKASI** — About cache/fallback, refresh preservation, HTTPS/plain text, bounded redirect/timeout/size, dan atomic cache replacement.
- **DIIMPLEMENTASIKAN — QA PENDING** — About navigation, visual, metadata runtime, serta online/offline behavior pada perangkat.
- **BELUM DIIMPLEMENTASIKAN** — theme selection.
- **BELUM DIIMPLEMENTASIKAN** — default Player mode.
- **BELUM DIIMPLEMENTASIKAN** — clear cache.
- **BELUM DIIMPLEMENTASIKAN** — export Setlist tanpa chord/lyrics.
- **BELUM DIIMPLEMENTASIKAN** — dedicated Settings ViewModel/device QA.