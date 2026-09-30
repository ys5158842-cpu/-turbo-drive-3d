package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(
    private val raceDao: RaceDao,
    private val highScoreDao: HighScoreDao,
    val preferences: GamePreferences
) {
    // Race Records
    val allRecords: Flow<List<RaceRecord>> = raceDao.getAllRecords()
    val totalStars: Flow<Int> = raceDao.getTotalStars()
    val totalVictories: Flow<Int> = raceDao.getVictoryCount()
    val coins: Flow<Int> = preferences.coins
    val selectedCarId: Flow<String> = preferences.selectedCarId
    val playerName: Flow<String> = preferences.playerName

    // High Scores Room Operations
    val allHighScores: Flow<List<HighScore>> = highScoreDao.getAllHighScores()
    val topHighScores: Flow<List<HighScore>> = highScoreDao.getTopHighScores(30)
    val highestScore: Flow<Int?> = highScoreDao.getHighestScore()

    suspend fun recordRaceResult(record: RaceRecord): Long {
        if (record.coinsEarned > 0) {
            preferences.addCoins(record.coinsEarned)
        }
        return raceDao.insertRecord(record)
    }

    suspend fun saveHighScore(highScore: HighScore): Long {
        return highScoreDao.insertHighScore(highScore)
    }

    suspend fun getHighestScoreDirect(): Int {
        return highScoreDao.getHighestScoreDirect() ?: 0
    }

    fun getBestRecord(modeId: String): Flow<RaceRecord?> {
        return raceDao.getBestRecordForMode(modeId)
    }

    fun getHighScoresByMode(modeId: String): Flow<List<HighScore>> {
        return highScoreDao.getHighScoresByMode(modeId)
    }

    fun getTopHighScoreForMode(modeId: String): Flow<HighScore?> {
        return highScoreDao.getTopHighScoreForMode(modeId)
    }

    fun getPlayerName(): String = preferences.getPlayerName()

    fun setPlayerName(name: String) {
        preferences.setPlayerName(name)
    }

    fun selectCar(carId: String) {
        preferences.setSelectedCarId(carId)
    }

    fun buyCar(car: CarModel): Boolean {
        if (preferences.isCarUnlocked(car.id)) {
            selectCar(car.id)
            return true
        }
        val bought = preferences.spendCoins(car.priceCoins)
        if (bought) {
            preferences.unlockCar(car.id)
            selectCar(car.id)
            return true
        }
        return false
    }

    suspend fun clearHistory() {
        raceDao.clearAll()
        highScoreDao.clearAllHighScores()
    }

    suspend fun clearHighScoresOnly() {
        highScoreDao.clearAllHighScores()
    }
}
