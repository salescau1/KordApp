package com.chordapp.catalog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.tukaani.xz.XZInputStream

@Database(
    entities = [CatalogSongEntity::class],
    version = 6,
    exportSchema = false
)
abstract class CatalogDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao

    companion object {
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN tier TEXT NOT NULL DEFAULT 'A'")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_tier ON songs (tier)")
            }
        }

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
                    .addMigrations(MIGRATION_5_6)
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

        fun notifyDatabaseReloaded(context: Context) {
            getDatabase(context)
            com.chordapp.repositories.CatalogRepository.notifyCatalogUpdated()
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
                notifyDatabaseReloaded(context)
                true
            } catch (e: Exception) {
                android.util.Log.e("CatalogDatabase", "Gagal mengimpor catalog.db.xz", e)
                if (tempDb.exists()) tempDb.delete()
                false
            }
        }

        /**
         * Mengimpor satu paket database (A, B, atau C) ke dalam catalog aktif secara modular.
         */
        fun importTierDatabaseFromXz(context: Context, xzFile: java.io.File, tier: String): Boolean {
            val dbFile = context.getDatabasePath("catalog.db")
            val currentSongs = runCatching {
                getDatabase(context).openHelper.readableDatabase.query("SELECT COUNT(*) FROM songs").use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else 0
                }
            }.getOrDefault(0)
            if (!dbFile.exists() || currentSongs == 0) {
                return importDatabaseFromXz(context, xzFile)
            }

            val tempDb = java.io.File(context.cacheDir, "temp_tier_$tier.db")
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

                val db = getDatabase(context).openHelper.writableDatabase
                db.execSQL("ATTACH DATABASE '${tempDb.absolutePath.replace("'", "''")}' AS temp_tier_db")
                try {
                    db.beginTransaction()
                    try {
                        db.execSQL("INSERT OR REPLACE INTO songs SELECT * FROM temp_tier_db.songs")
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                } finally {
                    db.execSQL("DETACH DATABASE temp_tier_db")
                }

                tempDb.delete()
                notifyDatabaseReloaded(context)
                true
            } catch (e: Exception) {
                android.util.Log.e("CatalogDatabase", "Gagal mengimpor paket $tier", e)
                if (tempDb.exists()) tempDb.delete()
                false
            }
        }

        /**
         * Menghapus paket lagu tertentu (A, B, atau C) dari database lokal dan memadatkan penyimpanan.
         */
        suspend fun deleteTier(context: Context, tier: String): Boolean {
            return try {
                val dao = getDatabase(context).catalogDao()
                dao.deleteSongsByTier(tier)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        getDatabase(context).openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
                    } catch (_: Exception) {}
                }
                notifyDatabaseReloaded(context)
                true
            } catch (e: Exception) {
                android.util.Log.e("CatalogDatabase", "Gagal menghapus paket $tier", e)
                false
            }
        }
    }
}
