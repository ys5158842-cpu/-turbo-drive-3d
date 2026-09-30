package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("turbo_drive_prefs", Context.MODE_PRIVATE)

    private val _coins = MutableStateFlow(getSavedCoins())
    val coins: Flow<Int> = _coins.asStateFlow()

    private val _selectedCarId = MutableStateFlow(getSavedCarId())
    val selectedCarId: Flow<String> = _selectedCarId.asStateFlow()

    private val _playerName = MutableStateFlow(getSavedPlayerName())
    val playerName: Flow<String> = _playerName.asStateFlow()

    fun getPlayerName(): String = getSavedPlayerName()

    private fun getSavedPlayerName(): String =
        prefs.getString(KEY_PLAYER_NAME, "كابتن السرعة") ?: "كابتن السرعة"

    fun setPlayerName(name: String) {
        val trimmed = name.trim().ifEmpty { "كابتن السرعة" }
        prefs.edit().putString(KEY_PLAYER_NAME, trimmed).apply()
        _playerName.value = trimmed
    }

    fun getCoins(): Int = prefs.getInt(KEY_COINS, 100)

    private fun getSavedCoins(): Int = prefs.getInt(KEY_COINS, 100)

    fun addCoins(amount: Int) {
        val current = getSavedCoins()
        val updated = current + amount
        prefs.edit().putInt(KEY_COINS, updated).apply()
        _coins.value = updated
    }

    fun spendCoins(amount: Int): Boolean {
        val current = getSavedCoins()
        if (current >= amount) {
            val updated = current - amount
            prefs.edit().putInt(KEY_COINS, updated).apply()
            _coins.value = updated
            return true
        }
        return false
    }

    fun getSavedCarId(): String = prefs.getString(KEY_SELECTED_CAR, "speed_demon") ?: "speed_demon"

    fun setSelectedCarId(carId: String) {
        prefs.edit().putString(KEY_SELECTED_CAR, carId).apply()
        _selectedCarId.value = carId
    }

    fun isCarUnlocked(carId: String): Boolean {
        if (carId == "speed_demon") return true
        return prefs.getBoolean("car_unlocked_$carId", false)
    }

    fun unlockCar(carId: String) {
        prefs.edit().putBoolean("car_unlocked_$carId", true).apply()
    }

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION, value).apply()

    var isNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    companion object {
        private const val KEY_COINS = "player_coins"
        private const val KEY_SELECTED_CAR = "selected_car_id"
        private const val KEY_PLAYER_NAME = "pref_player_name"
        private const val KEY_SOUND = "pref_sound"
        private const val KEY_VIBRATION = "pref_vibration"
        private const val KEY_NOTIFICATIONS = "pref_notifications"
    }
}
