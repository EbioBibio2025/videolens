package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_logs")
data class SearchLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val modalityFilter: String,
    val timestamp: Long = System.currentTimeMillis(),
    val matchCount: Int
)
