package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.db.dao.MediaDao
import com.example.data.db.dao.ModelConfigDao
import com.example.data.db.dao.SearchLogDao
import com.example.data.db.dao.SegmentDao
import com.example.data.db.entity.MediaItemEntity
import com.example.data.db.entity.MediaSegmentEntity
import com.example.data.db.entity.ModelConfigEntity
import com.example.data.db.entity.SearchLogEntity

@Database(
    entities = [
        MediaItemEntity::class,
        MediaSegmentEntity::class,
        SearchLogEntity::class,
        ModelConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun segmentDao(): SegmentDao
    abstract fun searchLogDao(): SearchLogDao
    abstract fun modelConfigDao(): ModelConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "videolens_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
