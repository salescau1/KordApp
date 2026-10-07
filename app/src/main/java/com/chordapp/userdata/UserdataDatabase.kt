package com.chordapp.userdata

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SetlistEntity::class,
        SetlistItemEntity::class,
        PlayHistoryEntity::class,
        FavoriteSongEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class UserdataDatabase : RoomDatabase() {
    abstract fun userdataDao(): UserdataDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `play_history` (" +
                        "`source_url` TEXT NOT NULL, " +
                        "`source` TEXT NOT NULL, " +
                        "`artist` TEXT NOT NULL, " +
                        "`artist_slug` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`title_slug` TEXT NOT NULL, " +
                        "`played_at` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`source_url`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorite_songs` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`added_at` INTEGER NOT NULL, " +
                        "`source_url` TEXT NOT NULL, " +
                        "`artist_slug` TEXT NOT NULL, " +
                        "`title_slug` TEXT NOT NULL, " +
                        "`source` TEXT NOT NULL, " +
                        "`content_hash` TEXT NOT NULL, " +
                        "`transpose` INTEGER NOT NULL, " +
                        "`capo` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_favorite_songs_source_url` " +
                        "ON `favorite_songs` (`source_url`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Development Setlists may be reset; Favorites and play history stay intact.
                db.execSQL("DROP TABLE IF EXISTS `setlist_items`")
                db.execSQL("DROP TABLE IF EXISTS `setlists`")
                db.execSQL("DROP TABLE IF EXISTS `songbooks`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `setlists` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `note` TEXT, " +
                        "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `setlist_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`setlist_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
                        "`source_url` TEXT NOT NULL, `artist_slug` TEXT NOT NULL, " +
                        "`title_slug` TEXT NOT NULL, `source` TEXT NOT NULL, " +
                        "`content_hash` TEXT NOT NULL, `transpose` INTEGER NOT NULL, " +
                        "`capo` INTEGER NOT NULL, `note` TEXT, " +
                        "FOREIGN KEY(`setlist_id`) REFERENCES `setlists`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_setlist_items_setlist_id` " +
                        "ON `setlist_items` (`setlist_id`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: UserdataDatabase? = null

        fun getDatabase(context: Context): UserdataDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserdataDatabase::class.java,
                    "userdata.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
