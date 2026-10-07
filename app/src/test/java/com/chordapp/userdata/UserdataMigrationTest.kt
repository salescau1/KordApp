package com.chordapp.userdata

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserdataMigrationTest {
    @Test
    fun migration2To3PreservesExistingRowsAndAddsFavorites() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE `songbooks` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, `note` TEXT, " +
                                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        try {
            val db = helper.writableDatabase
            db.execSQL(
                "INSERT INTO `songbooks` (`name`, `note`, `created_at`, `updated_at`) " +
                    "VALUES ('Existing Book', 'keep me', 10, 20)"
            )

            UserdataDatabase.MIGRATION_2_3.migrate(db)

            db.query("SELECT name, note, created_at, updated_at FROM songbooks").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Existing Book", cursor.getString(0))
                assertEquals("keep me", cursor.getString(1))
                assertEquals(10L, cursor.getLong(2))
                assertEquals(20L, cursor.getLong(3))
            }

            db.execSQL(
                "INSERT INTO `favorite_songs` (" +
                    "`added_at`, `source_url`, `artist_slug`, `title_slug`, `source`, " +
                    "`content_hash`, `transpose`, `capo`) VALUES " +
                    "(30, 'https://example.test/song', 'artist', 'song', 'UG', 'hash', 2, 3)"
            )
            db.query("SELECT source_url, transpose, capo FROM favorite_songs").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("https://example.test/song", cursor.getString(0))
                assertEquals(2, cursor.getInt(1))
                assertEquals(3, cursor.getInt(2))
            }

            db.query("PRAGMA index_list(`favorite_songs`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val uniqueIndex = cursor.getColumnIndexOrThrow("unique")
                var foundUniqueSourceIndex = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "index_favorite_songs_source_url") {
                        foundUniqueSourceIndex = cursor.getInt(uniqueIndex) == 1
                    }
                }
                assertTrue(foundUniqueSourceIndex)
            }
        } finally {
            helper.close()
        }
    }

    @Test
    fun migration3To4ResetsSetlistsButPreservesFavoritesAndHistory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE `songbooks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE `setlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `songbook_id` INTEGER, `name` TEXT NOT NULL, `note` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE `setlist_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `setlist_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `source_url` TEXT NOT NULL, `artist_slug` TEXT NOT NULL, `title_slug` TEXT NOT NULL, `source` TEXT NOT NULL, `content_hash` TEXT NOT NULL, `transpose` INTEGER NOT NULL, `capo` INTEGER NOT NULL, `note` TEXT)")
                        db.execSQL("CREATE TABLE `favorite_songs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `added_at` INTEGER NOT NULL, `source_url` TEXT NOT NULL, `artist_slug` TEXT NOT NULL, `title_slug` TEXT NOT NULL, `source` TEXT NOT NULL, `content_hash` TEXT NOT NULL, `transpose` INTEGER NOT NULL, `capo` INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE `play_history` (`source_url` TEXT NOT NULL PRIMARY KEY, `source` TEXT NOT NULL, `artist` TEXT NOT NULL, `artist_slug` TEXT NOT NULL, `title` TEXT NOT NULL, `title_slug` TEXT NOT NULL, `played_at` INTEGER NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        try {
            val db = helper.writableDatabase
            db.execSQL("INSERT INTO setlists (songbook_id,name,note,created_at,updated_at) VALUES (NULL,'Old',NULL,1,1)")
            db.execSQL("INSERT INTO favorite_songs (added_at,source_url,artist_slug,title_slug,source,content_hash,transpose,capo) VALUES (1,'fav','a','t','UG','h',2,3)")
            db.execSQL("INSERT INTO play_history (source_url,source,artist,artist_slug,title,title_slug,played_at) VALUES ('played','UG','A','a','T','t',2)")
            UserdataDatabase.MIGRATION_3_4.migrate(db)
            db.query("SELECT COUNT(*) FROM setlists").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
            db.query("PRAGMA table_info(setlists)").use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow("name")
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) columns += cursor.getString(nameColumn)
                assertTrue("songbook_id" !in columns)
            }
            db.query("SELECT source_url FROM favorite_songs").use { it.moveToFirst(); assertEquals("fav", it.getString(0)) }
            db.query("SELECT source_url FROM play_history").use { it.moveToFirst(); assertEquals("played", it.getString(0)) }
        } finally {
            helper.close()
        }
    }
}
