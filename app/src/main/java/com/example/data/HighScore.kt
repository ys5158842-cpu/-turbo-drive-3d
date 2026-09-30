package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "high_scores")
data class HighScore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerName: String,
    val score: Int,
    val modeId: String,
    val modeName: String,
    val timeSeconds: Float,
    val maxSpeedKmh: Int,
    val stars: Int,
    val coinsEarned: Int,
    val distanceCoveredMeters: Float,
    val isCompleted: Boolean = true,
    val carName: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Calculates arcade score based on race performance parameters.
         */
        fun calculateScore(
            isVictory: Boolean,
            distanceCoveredMeters: Float,
            trackLengthMeters: Float,
            timeSeconds: Float,
            maxSpeedKmh: Int,
            coinsCollected: Int,
            stars: Int
        ): Int {
            var points = 0

            // Distance points (2 points per meter traveled)
            points += (distanceCoveredMeters * 2f).toInt()

            // Coins collected (50 points per coin)
            points += coinsCollected * 50

            // Top speed factor (15 points per km/h)
            points += maxSpeedKmh * 15

            // Finish Line bonuses
            if (isVictory) {
                // Major finish line bonus
                points += 3500

                // Star rating bonus (1,000 pts per star)
                points += stars * 1000

                // Time bonus: fast finishers get extra points
                val targetTime = trackLengthMeters / 38f // e.g. ~65s for 2500m
                if (timeSeconds < targetTime && timeSeconds > 0) {
                    val timeBonus = ((targetTime - timeSeconds) * 150f).toInt()
                    points += timeBonus.coerceAtLeast(0)
                }
            }

            return points.coerceAtLeast(100)
        }
    }
}
