package com.chordapp.usercatalog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScrapedSongEntity::class, CreatorSongEntity::class],
    version = 1,
    exportSchema = false
)
abstract class UserCatalogDatabase : RoomDatabase() {
    abstract fun scrapedSongDao(): ScrapedSongDao
    abstract fun creatorSongDao(): CreatorSongDao

    companion object {
        @Volatile
        private var INSTANCE: UserCatalogDatabase? = null

        fun getDatabase(context: Context): UserCatalogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserCatalogDatabase::class.java,
                    "user_catalog.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
