package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.ModelConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelConfigDao {
    @Query("SELECT * FROM model_config WHERE id = 1")
    fun getConfig(): Flow<ModelConfigEntity?>

    @Query("SELECT * FROM model_config WHERE id = 1")
    suspend fun getConfigSync(): ModelConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: ModelConfigEntity)

    @Update
    suspend fun updateConfig(config: ModelConfigEntity)
}
