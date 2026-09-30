package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(
    private val raceDao: RaceDao,
    val preferences: GamePreferences
) {
    val allRecords: Flow<List<RaceRecord>> = raceDao.getAllRecords()
    val totalStars: Flow<Int> = raceDao.getTotalStars()
    val totalVictories: Flow<Int> = raceDao.getVictoryCount()
    val coins: Flow<Int> = preferences.coins
    val selectedCarId: Flow<String> = preferences.selectedCarId

    suspend fun recordRaceResult(record: RaceRecord): Long {
        if (record.coinsEarned > 0) {
            preferences.addCoins(record.coinsEarned)
        }
        return raceDao.insertRecord(record)
    }

    fun getBestRecord(modeId: String): Flow<RaceRecord?> {
        return raceDao.getBestRecordForMode(modeId)
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
    }
}
