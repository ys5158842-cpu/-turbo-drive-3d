package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "race_records")
data class RaceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val modeId: String,
    val modeName: String,
    val timeSeconds: Float,
    val maxSpeedKmh: Int,
    val stars: Int,
    val coinsEarned: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val isVictory: Boolean = true
)
