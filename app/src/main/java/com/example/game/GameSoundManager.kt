package com.example.game

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.GamePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameSoundManager(
    private val context: Context,
    private val preferences: GamePreferences
) {
    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val audioScope = CoroutineScope(Dispatchers.Default)

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    fun playCoinSound() {
        if (!preferences.isSoundEnabled) return
        audioScope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 60)
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
        vibrateLight()
    }

    fun playNitroSound() {
        if (!preferences.isSoundEnabled) return
        audioScope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 180)
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
        vibrateMedium()
    }

    fun playCrashSound() {
        if (!preferences.isSoundEnabled) return
        audioScope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
        vibrateHeavy()
    }

    fun playVictoryFanfare() {
        if (!preferences.isSoundEnabled) return
        audioScope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
                delay(160)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 200)
                delay(220)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 350)
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
        vibrateVictory()
    }

    fun playCheckpointSound() {
        if (!preferences.isSoundEnabled) return
        audioScope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 120)
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
        vibrateLight()
    }

    fun playCountdownBeep(isFinalGo: Boolean) {
        if (!preferences.isSoundEnabled) return
        audioScope.launch {
            try {
                if (isFinalGo) {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 250)
                } else {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                }
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
    }

    private fun vibrateLight() {
        if (!preferences.isVibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun vibrateMedium() {
        if (!preferences.isVibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun vibrateHeavy() {
        if (!preferences.isVibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, 255))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun vibrateVictory() {
        if (!preferences.isVibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 100, 80, 100, 80, 250)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 100, 80, 100, 80, 250), -1)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            // Ignore
        }
        toneGenerator = null
    }
}
