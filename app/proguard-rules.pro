# ProGuard / R8 Obfuscation Rules untuk KordApp

# Room Database
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Entities & DAOs agar tidak error saat serialisasi Room
-keep class com.chordapp.catalog.** { *; }
-keep class com.chordapp.userdata.** { *; }

# Coil image loader
-keep class coil.** { *; }

# XZ decompressor
-keep class org.tukaani.xz.** { *; }
