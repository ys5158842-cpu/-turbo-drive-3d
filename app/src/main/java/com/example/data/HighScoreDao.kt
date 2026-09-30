package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores ORDER BY score DESC, timeSeconds ASC, timestamp DESC")
    fun getAllHighScores(): Flow<List<HighScore>>

    @Query("SELECT * FROM high_scores ORDER BY score DESC, timeSeconds ASC, timestamp DESC LIMIT :limit")
    fun getTopHighScores(limit: Int = 10): Flow<List<HighScore>>

    @Query("SELECT * FROM high_scores WHERE modeId = :modeId ORDER BY score DESC, timeSeconds ASC, timestamp DESC")
    fun getHighScoresByMode(modeId: String): Flow<List<HighScore>>

    @Query("SELECT * FROM high_scores WHERE modeId = :modeId ORDER BY score DESC, timeSeconds ASC, timestamp DESC LIMIT 1")
    fun getTopHighScoreForMode(modeId: String): Flow<HighScore?>

    @Query("SELECT MAX(score) FROM high_scores")
    fun getHighestScore(): Flow<Int?>

    @Query("SELECT MAX(score) FROM high_scores")
    suspend fun getHighestScoreDirect(): Int?

    @Query("SELECT COUNT(*) FROM high_scores")
    fun getScoresCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighScore(highScore: HighScore): Long

    @Query("DELETE FROM high_scores WHERE id = :id")
    suspend fun deleteHighScore(id: Long)

    @Query("DELETE FROM high_scores")
    suspend fun clearAllHighScores()
}
