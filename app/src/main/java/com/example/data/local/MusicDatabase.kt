package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AlbumDao
import com.example.data.local.dao.ArtistDao
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.RecentSearchDao
import com.example.data.local.dao.SongDao
import com.example.data.local.entity.AlbumEntity
import com.example.data.local.entity.AlbumSongCrossRef
import com.example.data.local.entity.ArtistEntity
import com.example.data.local.entity.ArtistSongCrossRef
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistSongCrossRef
import com.example.data.local.entity.RecentSearchEntity
import com.example.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        AlbumEntity::class,
        AlbumSongCrossRef::class,
        ArtistEntity::class,
        ArtistSongCrossRef::class,
        RecentSearchEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

                val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_songs_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration` INTEGER NOT NULL, `artworkUrl` TEXT NOT NULL, `stream160Url` TEXT NOT NULL, `stream320Url` TEXT NOT NULL, `lyrics` TEXT, `isDownloaded` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `localFilePath` TEXT, `year` TEXT NOT NULL, `artistId` TEXT NOT NULL, `albumId` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                database.execSQL(
                    "INSERT INTO `cached_songs_new` (`id`, `title`, `artist`, `album`, `duration`, `artworkUrl`, `stream160Url`, `stream320Url`, `lyrics`, `isDownloaded`, `isFavorite`, `localFilePath`, `year`, `artistId`, `albumId`, `cachedAt`) SELECT `id`, `title`, `artist`, `album`, `durationSec` * 1000, `artworkUrl`, `stream160Url`, `stream320Url`, `lyrics`, `isDownloaded`, `isFavorite`, `localFilePath`, `year`, `artistId`, `albumId`, `cachedAt` FROM `cached_songs`"
                )
                database.execSQL("DROP TABLE `cached_songs`")
                database.execSQL("ALTER TABLE `cached_songs_new` RENAME TO `cached_songs`")
            }
        }

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "pulse_music_database"
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .addMigrations(MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
