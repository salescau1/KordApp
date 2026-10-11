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
                val hasBundledAsset = runCatching {
                    context.assets.open("catalog.db.xz").close()
                    true
                }.getOrDefault(false)

                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    CatalogDatabase::class.java,
                    "catalog.db"
                )

                if (hasBundledAsset) {
                    builder.createFromInputStream {
                        XZInputStream(EncryptedAssetInputStream(context.assets.open("catalog.db.xz")))
                    }
                }

                val instance = builder
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("CREATE INDEX IF NOT EXISTS idx_source_url ON songs (source_url)")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        /**
         * Mengimpor dan mengekstrak file catalog.db.xz yang diunduh dari internet ke database aktif.
         */
        fun importDatabaseFromXz(context: Context, xzFile: java.io.File): Boolean {
            closeDatabase()
            val dbFile = context.getDatabasePath("catalog.db")
            val tempDb = java.io.File(context.cacheDir, "temp_imported_catalog.db")
            return try {
                if (tempDb.exists()) tempDb.delete()
                java.io.FileInputStream(xzFile).use { fis ->
                    EncryptedAssetInputStream(fis).use { eis ->
                        XZInputStream(eis).use { xzis ->
                            tempDb.outputStream().use { fos ->
                                xzis.copyTo(fos)
                            }
                        }
                    }
                }

                if (!tempDb.exists() || tempDb.length() < 100_000) {
                    if (tempDb.exists()) tempDb.delete()
                    return false
                }

                // Hapus file sqlite lama & wal/shm
                val walFile = java.io.File(dbFile.path + "-wal")
                val shmFile = java.io.File(dbFile.path + "-shm")
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()
                if (dbFile.exists()) dbFile.delete()

                dbFile.parentFile?.mkdirs()
                val success = tempDb.renameTo(dbFile)
                if (!success) {
                    // Fallback copy if renameTo fails across mounts
                    tempDb.copyTo(dbFile, overwrite = true)
                    tempDb.delete()
                }

                // Inisialisasi ulang database dan pastikan index siap
                val reloaded = getDatabase(context)
                reloaded.openHelper.writableDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_source_url ON songs (source_url)")
                true
            } catch (e: Exception) {
                android.util.Log.e("CatalogDatabase", "Gagal mengimpor catalog.db.xz", e)
                if (tempDb.exists()) tempDb.delete()
                false
            }
        }
    }
}
