package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.SearchLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchLogDao {
    @Query("SELECT * FROM search_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 20): Flow<List<SearchLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SearchLogEntity)

    @Query("DELETE FROM search_logs")
    suspend fun clearLogs()
}
