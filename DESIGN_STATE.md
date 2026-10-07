# KordApp-Design: State & Handoff Summary

Dokumen ringkas untuk mencatat pencapaian sesi **KordApp-Design** dan handoff ke sesi **KordApp-Ops**.

---

## 1. Status Terkini (Done 100%)

### A. Redesign UI/UX Player Screen
- **Top Bar Melayang & Terpisah**:
  - Tombol aksi berbentuk lingkaran bulat (42dp): `Back`, `Favorite (Love)`, dan `Add to Setlist`.
  - Kapsul tengah mandiri: Menampilkan judul lagu, tag versi (misal: `UG v1 ▾`), nama artis, dan base key.
  - Tapping pada kapsul judul langsung memunculkan menu dropdown pemilihan versi lagu.
- **Floating Symbolic Bottom Dock**:
  - Dock kapsul melayang (tinggi ~54dp, radius 32dp) dengan frosted background.
  - Simbol penuh yang proporsional:
    - `[ ▶ / ⏸ ]` Auto Scroll (highlight emas saat aktif).
    - `[ 📖 ]` Page Flip.
    - `[ − | 0 | + ]` Transpose: angka tengah menunjukkan delta dan **bila di-tap langsung reset ke 0**.
    - `[ a | 0 | A ]` Font Size: `a` kecil untuk perkecil, `A` besar untuk perbesar, dan **bila angka 0 di-tap langsung reset ke default (14sp)**.
    - `[ 💡 ]` Keep Screen Awake (highlight emas saat aktif).
- **Aksen Fadeout Scrim (Lirik Memudar)**:
  - Scrim gradien atas (tinggi 115dp) & bawah (tinggi 125dp).
  - Lirik bergerak mulus di balik floating bar dan memudar ke warna latar belakang (hitam pada Dark Mode, putih pada Light Mode).
- **Peningkatan Kontras Chord**:
  - Warna chord menggunakan `ChordText` (Warm Amber `#FFB74D` di Dark Mode, Ochre `#B45309` di Light Mode).
  - Fitur Capo dibersihkan sepenuhnya dari Player Screen sesuai preferensi user.

### B. Global Dark Mode & Light Mode
- **Menu Pengaturan / Settings**:
  - Section baru: **TAMPILAN / APPEARANCE** dengan pemilih tema:
    1. `Mode Gelap (Dark Mode)` — default khas panggung.
    2. `Mode Terang (Light Mode)` — latar kertas putih bersih, lirik hitam, chord kontras.
    3. `Ikuti Sistem (System Default)` — otomatis sinkron dengan OS Android.
- **Sistem Tema Dinamis**:
  - Dikelola oleh `AppThemeManager` & tersimpan persisten di `PlayerPreferences`.
  - Terintegrasi global via `ChordAppTheme` dan `LocalChordAppColors`.
  - Status bar & Navigation bar OS ikut berubah otomatis.

---

## 2. File yang Dimodifikasi
- `app/src/main/java/com/chordapp/ui/screens/player/PlayerScreen.kt`
- `app/src/main/java/com/chordapp/ui/screens/player/ChordLyricFlow.kt`
- `app/src/main/java/com/chordapp/ui/screens/player/ChordDiagramDialog.kt`
- `app/src/main/java/com/chordapp/ui/theme/Color.kt`
- `app/src/main/java/com/chordapp/ui/theme/Theme.kt`
- `app/src/main/java/com/chordapp/ui/theme/AppThemeManager.kt` (file baru)
- `app/src/main/java/com/chordapp/settings/PlayerPreferences.kt`
- `app/src/main/java/com/chordapp/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/java/com/chordapp/viewmodels/SettingsViewModel.kt`
- `app/src/main/java/com/chordapp/viewmodels/PlayerViewModel.kt`
- `app/src/main/java/com/chordapp/MainActivity.kt`
- `app/src/main/java/com/chordapp/localization/AppStrings.kt`, `EnglishStrings.kt`, `IndonesianStrings.kt`

---

## 3. Status Kompilasi
- Verifikasi kompilasi Gradle: `.\gradlew.bat compileDebugKotlin` **BUILD SUCCESSFUL (0 error)**.

---

## 4. Handoff ke Sesi KordApp-Ops
Tugas untuk sesi **KordApp-Ops**:
1. Jalankan build APK: `.\gradlew.bat assembleDebug` (atau `assembleRelease`).
2. Instalasi dan uji coba di perangkat riil Android via ADB (`adb install -r ...`).
3. Pengujian integrasi lisensi Supabase & verifikasi update offline.
