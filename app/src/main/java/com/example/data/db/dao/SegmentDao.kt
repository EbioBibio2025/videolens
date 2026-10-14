package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.MediaSegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SegmentDao {
    @Query("SELECT * FROM media_segments WHERE mediaId = :mediaId ORDER BY segmentIndex ASC")
    fun getSegmentsForMedia(mediaId: Long): Flow<List<MediaSegmentEntity>>

    @Query("SELECT * FROM media_segments WHERE mediaId = :mediaId ORDER BY segmentIndex ASC")
    suspend fun getSegmentsForMediaSync(mediaId: Long): List<MediaSegmentEntity>

    @Query("SELECT * FROM media_segments")
    fun getAllSegments(): Flow<List<MediaSegmentEntity>>

    @Query("SELECT * FROM media_segments")
    suspend fun getAllSegmentsSync(): List<MediaSegmentEntity>

    @Query("SELECT COUNT(*) FROM media_segments")
    fun getTotalSegmentCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: MediaSegmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<MediaSegmentEntity>)

    @Query("DELETE FROM media_segments WHERE mediaId = :mediaId")
    suspend fun deleteSegmentsForMedia(mediaId: Long)

    @Query("DELETE FROM media_segments")
    suspend fun clearAllSegments()
}
