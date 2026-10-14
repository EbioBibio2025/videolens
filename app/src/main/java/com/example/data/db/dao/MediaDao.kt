package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY createdAt DESC")
    fun getAllMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    fun getMediaById(id: Long): Flow<MediaItemEntity?>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaByIdSync(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE mediaType = :mediaType ORDER BY createdAt DESC")
    fun getMediaByType(mediaType: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE topicTag = :topic ORDER BY createdAt DESC")
    fun getMediaByTopic(topic: String): Flow<List<MediaItemEntity>>

    @Query("SELECT DISTINCT topicTag FROM media_items ORDER BY topicTag ASC")
    fun getDistinctTopics(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM media_items")
    fun getMediaCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE mediaType = :type")
    fun getMediaCountByType(type: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(items: List<MediaItemEntity>): List<Long>

    @Update
    suspend fun updateMedia(item: MediaItemEntity)

    @Delete
    suspend fun deleteMedia(item: MediaItemEntity)

    @Query("UPDATE media_items SET indexingStatus = :status, errorMessage = :error, indexedAt = :indexedAt WHERE id = :id")
    suspend fun updateIndexingStatus(id: Long, status: String, error: String? = null, indexedAt: Long? = null)

    @Query("UPDATE media_items SET topicTag = :topic WHERE id = :id")
    suspend fun updateTopic(id: Long, topic: String)

    @Query("DELETE FROM media_items")
    suspend fun clearAllMedia()
}
