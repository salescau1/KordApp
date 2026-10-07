package com.chordapp.catalog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.tukaani.xz.XZInputStream

@Database(
    entities = [CatalogSongEntity::class],
    version = 5,
    exportSchema = false
)
abstract class CatalogDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao

    companion object {
        @Volatile
        private var INSTANCE: CatalogDatabase? = null

        fun getDatabase(context: Context): CatalogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CatalogDatabase::class.java,
                    "catalog.db"
                )
                    // Katalog dikirim sebagai assets/catalog.db.xz (terkompres XZ).
                    // Saat pertama buka atau database version berubah, Room memakai stream
                    // ter-decompress ini untuk mengisi DB path (menjadi catalog.db penuh di HP).
                    .createFromInputStream {
                        XZInputStream(EncryptedAssetInputStream(context.assets.open("catalog.db.xz")))
                    }
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
