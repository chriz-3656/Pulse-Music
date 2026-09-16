package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 2,
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

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "pulse_music_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
