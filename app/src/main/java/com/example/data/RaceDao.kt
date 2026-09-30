package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceDao {
    @Query("SELECT * FROM race_records ORDER BY completedAt DESC")
    fun getAllRecords(): Flow<List<RaceRecord>>

    @Query("SELECT * FROM race_records WHERE modeId = :modeId ORDER BY timeSeconds ASC LIMIT 1")
    fun getBestRecordForMode(modeId: String): Flow<RaceRecord?>

    @Query("SELECT COUNT(*) FROM race_records WHERE isVictory = 1")
    fun getVictoryCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(stars), 0) FROM race_records")
    fun getTotalStars(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RaceRecord): Long

    @Query("DELETE FROM race_records")
    suspend fun clearAll()
}
