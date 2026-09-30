package com.example.game

enum class RaceStatus {
    COUNTDOWN,
    RACING,
    VICTORY_FINISHED,
    CRASHED_GAME_OVER,
    PAUSED
}

data class RaceParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var color: Long,
    var alpha: Float,
    var life: Float, // 1.0 down to 0.0
    val maxLife: Float
)

data class GameState(
    val mode: GameMode = GameModes.HIGHWAY,
    val status: RaceStatus = RaceStatus.COUNTDOWN,
    val countdownTimer: Float = 3f, // 3, 2, 1, GO!
    val playerX: Float = 0f, // -1.0 to 1.0 (road bounds)
    val playerZ: Float = 0f, // Distance traveled in meters
    val speedKmh: Float = 0f,
    val maxSpeedKmh: Float = 240f,
    val carHealth: Float = 100f,
    val nitroPercent: Float = 100f,
    val isNitroActive: Boolean = false,
    val coinsCollected: Int = 0,
    val elapsedTimeSeconds: Float = 0f,
    val timeRemainingSeconds: Float = 45f,
    val distanceToFinish: Float = 2500f,
    val steerRoll: Float = 0f, // Tilt left/right
    val cameraShake: Float = 0f,
    val notificationSent: Boolean = false,
    val bannerText: String? = null,
    val bannerDuration: Float = 0f,
    val starsEarned: Int = 3,
    val sessionScore: Int = 0,
    val isNewHighScore: Boolean = false,
    val previousHighScore: Int = 0
)
