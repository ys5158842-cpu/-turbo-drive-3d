package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

class GameViewModel(
    application: Application,
    private val repository: GameRepository
) : AndroidViewModel(application) {

    private val soundManager = GameSoundManager(application, repository.preferences)

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _currentTrack = MutableStateFlow(Road3DTrack(GameModes.HIGHWAY))
    val currentTrack: StateFlow<Road3DTrack> = _currentTrack.asStateFlow()

    private val _currentCar = MutableStateFlow(CarCatalog.cars.first())
    val currentCar: StateFlow<CarModel> = _currentCar.asStateFlow()

    val allRecords: StateFlow<List<RaceRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHighScores: StateFlow<List<HighScore>> = repository.allHighScores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topHighScores: StateFlow<List<HighScore>> = repository.topHighScores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val highestScore: StateFlow<Int> = repository.highestScore
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val playerName: StateFlow<String> = repository.playerName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getPlayerName())

    val coins: StateFlow<Int> = repository.coins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val selectedCarId: StateFlow<String> = repository.selectedCarId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "speed_demon")

    val particles = ArrayList<RaceParticle>()

    // Inputs
    var isSteeringLeft = false
    var isSteeringRight = false
    var isAccelerating = false
    var isBraking = false
    var isNitroPressed = false

    private var gameLoopJob: Job? = null
    private var lastTimeMillis: Long = System.currentTimeMillis()

    init {
        RaceNotificationHelper.createNotificationChannel(application)
        viewModelScope.launch {
            selectedCarId.collect { id ->
                _currentCar.value = CarCatalog.getCarById(id)
            }
        }
    }

    fun startRace(mode: GameMode) {
        gameLoopJob?.cancel()
        particles.clear()

        val track = Road3DTrack(mode)
        _currentTrack.value = track

        val car = CarCatalog.getCarById(repository.preferences.getSavedCarId())
        _currentCar.value = car

        _gameState.value = GameState(
            mode = mode,
            status = RaceStatus.COUNTDOWN,
            countdownTimer = 3.5f,
            playerX = 0f,
            playerZ = 0f,
            speedKmh = 0f,
            maxSpeedKmh = car.topSpeedKmh.toFloat(),
            carHealth = 100f,
            nitroPercent = 100f,
            isNitroActive = false,
            coinsCollected = 0,
            elapsedTimeSeconds = 0f,
            timeRemainingSeconds = if (mode.hasTimeLimit) mode.timeLimitSeconds else 0f,
            distanceToFinish = mode.trackLengthMeters,
            steerRoll = 0f,
            cameraShake = 0f,
            notificationSent = false
        )

        lastTimeMillis = System.currentTimeMillis()
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            var lastBeepFloor = 4
            while (isActive) {
                val now = System.currentTimeMillis()
                val dt = min(0.05f, max(0.005f, (now - lastTimeMillis) / 1000f))
                lastTimeMillis = now

                val current = _gameState.value

                when (current.status) {
                    RaceStatus.COUNTDOWN -> {
                        val newTimer = current.countdownTimer - dt
                        val floor = newTimer.toInt()
                        if (floor < lastBeepFloor && floor >= 0) {
                            lastBeepFloor = floor
                            soundManager.playCountdownBeep(isFinalGo = floor == 0)
                        }

                        if (newTimer <= 0f) {
                            _gameState.value = current.copy(
                                status = RaceStatus.RACING,
                                countdownTimer = 0f
                            )
                        } else {
                            _gameState.value = current.copy(countdownTimer = newTimer)
                        }
                    }
                    RaceStatus.RACING -> {
                        updateRacingPhysics(dt)
                    }
                    RaceStatus.VICTORY_FINISHED -> {
                        // Slow down after crossing finish line
                        val newSpeed = max(0f, current.speedKmh - 120f * dt)
                        val newZ = current.playerZ + (newSpeed * (1000f / 3600f) * dt)
                        updateParticles(dt)
                        _gameState.value = current.copy(
                            speedKmh = newSpeed,
                            playerZ = newZ
                        )
                    }
                    RaceStatus.CRASHED_GAME_OVER, RaceStatus.PAUSED -> {
                        updateParticles(dt)
                    }
                }

                delay(16) // ~60 FPS
            }
        }
    }

    private fun updateRacingPhysics(dt: Float) {
        val current = _gameState.value
        val car = _currentCar.value
        val track = _currentTrack.value

        var speed = current.speedKmh
        var playerX = current.playerX
        var health = current.carHealth
        var nitro = current.nitroPercent
        var isNitroActive = current.isNitroActive
        var coins = current.coinsCollected
        var elapsed = current.elapsedTimeSeconds + dt
        var timeRemaining = current.timeRemainingSeconds - (if (current.mode.hasTimeLimit) dt else 0f)
        var cameraShake = max(0f, current.cameraShake - dt * 3f)
        var bannerText = current.bannerText
        var bannerDuration = max(0f, current.bannerDuration - dt)
        if (bannerDuration <= 0f) bannerText = null

        // 1. Acceleration & Braking
        val topSpeed = if (isNitroActive) car.topSpeedKmh * 1.32f else car.topSpeedKmh.toFloat()
        val accelRate = (if (isNitroActive) 160f else 95f) * car.acceleration

        if (isNitroPressed && nitro > 2f) {
            if (!isNitroActive) {
                soundManager.playNitroSound()
            }
            isNitroActive = true
            nitro = max(0f, nitro - dt * 25f)
        } else {
            isNitroActive = false
            // Slow passive regen
            nitro = min(100f, nitro + dt * 4f)
        }

        if (isAccelerating) {
            speed = min(topSpeed, speed + accelRate * dt)
        } else if (isBraking) {
            speed = max(0f, speed - 220f * dt)
        } else {
            // Natural drag / friction
            speed = max(0f, speed - 45f * dt)
        }

        // Road & Sidewalk handling
        val absX = abs(playerX)
        if (absX in 0.95f..1.35f) {
            // Driving on Sidewalk & Curb: rumble strip camera shake and light friction
            cameraShake = 0.3f
            speed = max(40f, speed - 40f * dt)
            if (speed > 50f) {
                spawnTireSmoke(playerX, 0.25f)
            }
        } else if (absX > 1.35f) {
            // Off sidewalk into terrain
            speed = max(30f, speed - 160f * dt)
            cameraShake = 0.6f
            if (speed > 40f) {
                spawnTireSmoke(playerX, 0.45f)
            }
        }

        // 2. Steering & Roll
        val steerSpeed = 2.4f * car.handling
        var targetRoll = 0f
        if (isSteeringLeft) {
            playerX -= steerSpeed * dt * (speed / max(1f, topSpeed))
            targetRoll = -1.0f
        }
        if (isSteeringRight) {
            playerX += steerSpeed * dt * (speed / max(1f, topSpeed))
            targetRoll = 1.0f
        }
        val steerRoll = current.steerRoll + (targetRoll - current.steerRoll) * min(1f, dt * 10f)

        // Keep within reasonable world bounds including sidewalk
        playerX = playerX.coerceIn(-1.55f, 1.55f)

        // 3. Movement along track
        val metersPerSec = speed * (1000f / 3600f)
        val playerZ = current.playerZ + (metersPerSec * dt)
        val distanceToFinish = max(0f, current.mode.trackLengthMeters - playerZ)

        // 4. Track Segments & Collision detection
        val currentSeg = track.findSegment(playerZ)

        // Traffic Obstacles update & collision
        for (obs in currentSeg.obstacles) {
            if (!obs.hit) {
                val dx = abs(playerX - obs.offsetPercent)
                if (dx < 0.42f) {
                    obs.hit = true
                    when (obs.type) {
                        ObstacleType.TRAFFIC_CAR, ObstacleType.POLICE_CAR -> {
                            speed = max(20f, speed * 0.4f)
                            health = max(0f, health - 22f)
                            cameraShake = 1.5f
                            soundManager.playCrashSound()
                            spawnCrashSparks(playerX)
                            bannerText = if (obs.type == ObstacleType.POLICE_CAR) "🚨 اصطدام بدورية الشرطة! -20% صحة" else "💥 اصطدام مروري!"
                            bannerDuration = 1.8f
                        }
                        ObstacleType.ROAD_BARRIER, ObstacleType.DESERT_BOULDER -> {
                            speed = max(10f, speed * 0.3f)
                            health = max(0f, health - 30f)
                            cameraShake = 2.0f
                            soundManager.playCrashSound()
                            spawnCrashSparks(playerX)
                            bannerText = "⚠️ اصطدام بحاجز صخري!"
                            bannerDuration = 1.8f
                        }
                        ObstacleType.OIL_SLICK -> {
                            speed = max(40f, speed * 0.75f)
                            cameraShake = 0.8f
                            // Force brief skid
                            playerX += (if (playerX >= 0) 0.35f else -0.35f)
                            soundManager.playCrashSound()
                            bannerText = "🛢️ بقعة زيت! انزلاق عجلات!"
                            bannerDuration = 1.8f
                        }
                    }
                }
            }
        }

        // Pickups collection
        for (pickup in currentSeg.pickups) {
            if (!pickup.collected) {
                val dx = abs(playerX - pickup.offsetPercent)
                if (dx < 0.45f) {
                    pickup.collected = true
                    when (pickup.type) {
                        PickupType.COIN -> {
                            val gain = (10 * current.mode.coinBonusMultiplier).toInt()
                            coins += gain
                            soundManager.playCoinSound()
                            bannerText = "+$gain عملة ذهبية! 🪙"
                            bannerDuration = 1.0f
                        }
                        PickupType.NITRO -> {
                            nitro = 100f
                            soundManager.playNitroSound()
                            bannerText = "⚡ شحن نيترو كامل! NOS READY"
                            bannerDuration = 1.5f
                        }
                        PickupType.REPAIR_WRENCH -> {
                            health = min(100f, health + 35f)
                            soundManager.playCheckpointSound()
                            bannerText = "🔧 صيانة سريعة! +35% صحة"
                            bannerDuration = 1.5f
                        }
                        PickupType.TIME_BONUS -> {
                            timeRemaining += 12f
                            soundManager.playCheckpointSound()
                            bannerText = "⏱️ نقطة تفتيش! +12 ثانية"
                            bannerDuration = 1.5f
                        }
                    }
                }
            }
        }

        // 5. Particles Update
        updateParticles(dt)

        // 6. Check Win (Reached Finish Line!)
        val hasCrossedFinish = playerZ >= current.mode.trackLengthMeters

        if (hasCrossedFinish && current.status == RaceStatus.RACING) {
            handleVictory(coins, elapsed, health, current.mode)
            return
        }

        // 7. Check Defeat
        val isTimeOut = current.mode.hasTimeLimit && timeRemaining <= 0f
        val isWrecked = health <= 0f

        if (isTimeOut || isWrecked) {
            handleDefeat(isTimeOut)
            return
        }

        _gameState.value = current.copy(
            speedKmh = speed,
            playerX = playerX,
            playerZ = playerZ,
            carHealth = health,
            nitroPercent = nitro,
            isNitroActive = isNitroActive,
            coinsCollected = coins,
            elapsedTimeSeconds = elapsed,
            timeRemainingSeconds = max(0f, timeRemaining),
            distanceToFinish = distanceToFinish,
            steerRoll = steerRoll,
            cameraShake = cameraShake,
            bannerText = bannerText,
            bannerDuration = bannerDuration
        )
    }

    private fun handleVictory(coins: Int, timeElapsed: Float, health: Float, mode: GameMode) {
        val stars = when {
            health >= 70f && timeElapsed < (mode.trackLengthMeters / 40f) -> 3
            health >= 40f -> 2
            else -> 1
        }
        val bonusCoins = (50 * mode.coinBonusMultiplier).toInt()
        val totalCoinsEarned = coins + bonusCoins
        val car = _currentCar.value

        val sessionScore = HighScore.calculateScore(
            isVictory = true,
            distanceCoveredMeters = mode.trackLengthMeters,
            trackLengthMeters = mode.trackLengthMeters,
            timeSeconds = timeElapsed,
            maxSpeedKmh = car.topSpeedKmh,
            coinsCollected = totalCoinsEarned,
            stars = stars
        )

        soundManager.playVictoryFanfare()
        spawnVictoryConfetti()

        // Format time
        val minutes = (timeElapsed / 60).toInt()
        val seconds = (timeElapsed % 60).toInt()
        val millis = ((timeElapsed * 10) % 10).toInt()
        val timeFormatted = String.format("%02d:%02d.%d", minutes, seconds, millis)

        viewModelScope.launch {
            val previousBest = repository.getHighestScoreDirect()
            val isNewRecord = sessionScore > previousBest

            // 1. Save RaceRecord to Room DB
            repository.recordRaceResult(
                RaceRecord(
                    modeId = mode.id,
                    modeName = mode.nameAr,
                    timeSeconds = timeElapsed,
                    maxSpeedKmh = car.topSpeedKmh,
                    stars = stars,
                    coinsEarned = totalCoinsEarned,
                    isVictory = true
                )
            )

            // 2. Save HighScore to Room DB
            repository.saveHighScore(
                HighScore(
                    playerName = repository.getPlayerName(),
                    score = sessionScore,
                    modeId = mode.id,
                    modeName = mode.nameAr,
                    timeSeconds = timeElapsed,
                    maxSpeedKmh = car.topSpeedKmh,
                    stars = stars,
                    coinsEarned = totalCoinsEarned,
                    distanceCoveredMeters = mode.trackLengthMeters,
                    isCompleted = true,
                    carName = car.nameAr
                )
            )

            _gameState.value = _gameState.value.copy(
                status = RaceStatus.VICTORY_FINISHED,
                coinsCollected = totalCoinsEarned,
                starsEarned = stars,
                sessionScore = sessionScore,
                isNewHighScore = isNewRecord,
                previousHighScore = previousBest,
                distanceToFinish = 0f,
                bannerText = if (isNewRecord) "🏆 سكور قياسي جديد: $sessionScore نقطة!" else "🏁 خط النهاية! فوز ساحق! 🏁",
                bannerDuration = 5f
            )
        }

        // CRITICAL REQUIREMENT: Trigger Finish Notification
        RaceNotificationHelper.sendFinishLineNotification(
            context = getApplication(),
            modeName = mode.nameAr,
            timeFormatted = timeFormatted,
            coinsEarned = totalCoinsEarned,
            stars = stars
        )
    }

    private fun handleDefeat(isTimeOut: Boolean) {
        val current = _gameState.value
        val car = _currentCar.value
        val distanceTraveled = current.playerZ
        val sessionScore = HighScore.calculateScore(
            isVictory = false,
            distanceCoveredMeters = distanceTraveled,
            trackLengthMeters = current.mode.trackLengthMeters,
            timeSeconds = current.elapsedTimeSeconds,
            maxSpeedKmh = car.topSpeedKmh,
            coinsCollected = current.coinsCollected,
            stars = 0
        )

        soundManager.playCrashSound()

        viewModelScope.launch {
            val previousBest = repository.getHighestScoreDirect()
            val isNewRecord = sessionScore > previousBest && previousBest > 0

            // Save HighScore after race session even on crash/timeout
            repository.saveHighScore(
                HighScore(
                    playerName = repository.getPlayerName(),
                    score = sessionScore,
                    modeId = current.mode.id,
                    modeName = current.mode.nameAr,
                    timeSeconds = current.elapsedTimeSeconds,
                    maxSpeedKmh = car.topSpeedKmh,
                    stars = 0,
                    coinsEarned = current.coinsCollected,
                    distanceCoveredMeters = distanceTraveled,
                    isCompleted = false,
                    carName = car.nameAr
                )
            )

            _gameState.value = current.copy(
                status = RaceStatus.CRASHED_GAME_OVER,
                carHealth = 0f,
                speedKmh = 0f,
                sessionScore = sessionScore,
                isNewHighScore = isNewRecord,
                previousHighScore = previousBest,
                bannerText = if (isTimeOut) "⏳ انتهى الوقت المحدد!" else "💥 تحطمت السيارة بالكامل!"
            )
        }
    }

    fun updatePlayerName(name: String) {
        repository.setPlayerName(name)
    }

    fun clearAllScores() {
        viewModelScope.launch {
            repository.clearHighScoresOnly()
        }
    }

    private fun updateParticles(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) {
                it.remove()
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
            }
        }
    }

    private fun spawnCrashSparks(playerX: Float) {
        val random = java.util.Random()
        for (i in 0..25) {
            val vx = (random.nextFloat() - 0.5f) * 600f
            val vy = -random.nextFloat() * 450f
            particles.add(
                RaceParticle(
                    x = 400f + (playerX * 180f),
                    y = 700f,
                    vx = vx,
                    vy = vy,
                    size = 4f + random.nextFloat() * 6f,
                    color = if (random.nextBoolean()) 0xFFFFD700 else 0xFFFF3D00,
                    alpha = 1.0f,
                    life = 0.5f + random.nextFloat() * 0.4f,
                    maxLife = 0.9f
                )
            )
        }
    }

    private fun spawnTireSmoke(playerX: Float, intensity: Float) {
        val random = java.util.Random()
        for (i in 0..3) {
            particles.add(
                RaceParticle(
                    x = 400f + (playerX * 180f) + (random.nextFloat() - 0.5f) * 40f,
                    y = 750f,
                    vx = (random.nextFloat() - 0.5f) * 80f,
                    vy = -20f - random.nextFloat() * 40f,
                    size = 8f + random.nextFloat() * 12f,
                    color = 0xFFCCCCCC,
                    alpha = 0.4f * intensity,
                    life = 0.4f,
                    maxLife = 0.4f
                )
            )
        }
    }

    private fun spawnVictoryConfetti() {
        val random = java.util.Random()
        val colors = listOf(0xFFFF1744, 0xFF00E5FF, 0xFFFFD700, 0xFF00E676, 0xFF7C4DFF, 0xFFFF9100)
        for (i in 0..120) {
            particles.add(
                RaceParticle(
                    x = random.nextFloat() * 800f,
                    y = random.nextFloat() * 300f,
                    vx = (random.nextFloat() - 0.5f) * 350f,
                    vy = 100f + random.nextFloat() * 350f,
                    size = 6f + random.nextFloat() * 8f,
                    color = colors[random.nextInt(colors.size)],
                    alpha = 1.0f,
                    life = 2.5f + random.nextFloat() * 1.5f,
                    maxLife = 4.0f
                )
            )
        }
    }

    fun selectCar(carId: String) {
        repository.selectCar(carId)
        _currentCar.value = CarCatalog.getCarById(carId)
    }

    fun buyCar(car: CarModel): Boolean {
        return repository.buyCar(car)
    }

    fun isCarUnlocked(carId: String): Boolean {
        return repository.preferences.isCarUnlocked(carId)
    }

    fun pauseGame() {
        if (_gameState.value.status == RaceStatus.RACING) {
            _gameState.value = _gameState.value.copy(status = RaceStatus.PAUSED)
        }
    }

    fun resumeGame() {
        if (_gameState.value.status == RaceStatus.PAUSED) {
            _gameState.value = _gameState.value.copy(status = RaceStatus.RACING)
            lastTimeMillis = System.currentTimeMillis()
        }
    }

    fun sendTestNotification() {
        RaceNotificationHelper.sendTestNotification(getApplication())
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
        gameLoopJob?.cancel()
    }
}
