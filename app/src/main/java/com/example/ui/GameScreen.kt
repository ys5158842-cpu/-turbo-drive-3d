package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CarModel
import com.example.game.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGarage: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val currentCar by viewModel.currentCar.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "game_anim")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // 1. The 3D Racing Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("racing_canvas")
        ) {
            Road3DRenderer.renderScene(
                scope = this,
                track = currentTrack,
                car = currentCar,
                gameState = gameState,
                particles = viewModel.particles,
                animationTime = animTime
            )
        }

        // 2. Top HUD Bar
        GameTopHud(
            gameState = gameState,
            onPauseClick = { viewModel.pauseGame() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 3. Status/Warning Banner
        gameState.bannerText?.let { banner ->
            Surface(
                color = DarkSurface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, TurboCyan),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = banner,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        // 4. Countdown 3-2-1-GO Display
        if (gameState.status == RaceStatus.COUNTDOWN) {
            val count = gameState.countdownTimer.toInt()
            val text = when (count) {
                3 -> "3"
                2 -> "2"
                1 -> "1"
                else -> "انطلق! 🏎️💨"
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = text,
                    color = if (count <= 0) TurboGreen else TurboGold,
                    fontSize = if (count <= 0) 54.sp else 76.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .background(
                            color = Color(0xCC000000),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 36.dp, vertical = 20.dp)
                )
            }
        }

        // 5. Bottom On-Screen Controls
        if (gameState.status == RaceStatus.RACING) {
            GameControlsOverlay(
                viewModel = viewModel,
                gameState = gameState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            )
        }

        // 6. Victory Dialog / Finish Line Celebration
        if (gameState.status == RaceStatus.VICTORY_FINISHED) {
            VictoryOverlay(
                gameState = gameState,
                car = currentCar,
                onPlayAgain = { viewModel.startRace(gameState.mode) },
                onGoToGarage = onNavigateToGarage,
                onGoToMenu = onNavigateBack,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 7. Defeat / Crashed Dialog
        if (gameState.status == RaceStatus.CRASHED_GAME_OVER) {
            DefeatOverlay(
                gameState = gameState,
                onRetry = { viewModel.startRace(gameState.mode) },
                onGoToMenu = onNavigateBack,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 8. Pause Dialog
        if (gameState.status == RaceStatus.PAUSED) {
            PauseOverlay(
                onResume = { viewModel.resumeGame() },
                onRestart = { viewModel.startRace(gameState.mode) },
                onQuit = onNavigateBack,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun GameTopHud(
    gameState: GameState,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Track Progress to Finish Line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xBB10141E), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "🏁 النهاية:",
                color = TurboGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))

            val progress = (1f - (gameState.distanceToFinish / gameState.mode.trackLengthMeters)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                color = TurboCyan,
                trackColor = DarkSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            )

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${gameState.distanceToFinish.roundToInt()} م",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("pause_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Speed, Health, Nitro & Coins Status Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speedometer & Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xDD0D111A), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${gameState.speedKmh.roundToInt()}",
                    color = if (gameState.isNitroActive) TurboCyan else TurboOrange,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = " كم/س",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(10.dp))
                VerticalDivider(color = Color(0x44FFFFFF), modifier = Modifier.height(18.dp))
                Spacer(modifier = Modifier.width(10.dp))

                val time = if (gameState.mode.hasTimeLimit) gameState.timeRemainingSeconds else gameState.elapsedTimeSeconds
                val minutes = (time / 60).toInt()
                val seconds = (time % 60).toInt()
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    color = if (gameState.mode.hasTimeLimit && gameState.timeRemainingSeconds < 10f) TurboRed else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Health & Coins
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xDD0D111A), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                // Health Indicator
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Health",
                    tint = if (gameState.carHealth > 30f) TurboGreen else TurboRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${gameState.carHealth.roundToInt()}%",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(10.dp))
                VerticalDivider(color = Color(0x44FFFFFF), modifier = Modifier.height(18.dp))
                Spacer(modifier = Modifier.width(10.dp))

                // Coins
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Coins",
                    tint = TurboGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${gameState.coinsCollected}",
                    color = TurboGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GameControlsOverlay(
    viewModel: GameViewModel,
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Steering Left & Right
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Left Button
            Surface(
                shape = CircleShape,
                color = DarkSurface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(2.dp, TurboCyan.copy(alpha = 0.6f)),
                modifier = Modifier
                    .size(72.dp)
                    .testTag("btn_steer_left")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.isSteeringLeft = true
                                tryAwaitRelease()
                                viewModel.isSteeringLeft = false
                            }
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Steer Left",
                        tint = TurboCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Right Button
            Surface(
                shape = CircleShape,
                color = DarkSurface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(2.dp, TurboCyan.copy(alpha = 0.6f)),
                modifier = Modifier
                    .size(72.dp)
                    .testTag("btn_steer_right")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.isSteeringRight = true
                                tryAwaitRelease()
                                viewModel.isSteeringRight = false
                            }
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Steer Right",
                        tint = TurboCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Center Nitro NOS Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            val nitroReady = gameState.nitroPercent > 10f
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (nitroReady) TurboCyanDark.copy(alpha = 0.9f) else DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    if (nitroReady) TurboCyan else Color.Gray
                ),
                modifier = Modifier
                    .size(68.dp)
                    .testTag("btn_nitro")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.isNitroPressed = true
                                tryAwaitRelease()
                                viewModel.isNitroPressed = false
                            }
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Nitro Boost",
                            tint = if (nitroReady) TurboGold else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "NOS",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = if (nitroReady) TextPrimary else TextMuted
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Nitro Bar
            LinearProgressIndicator(
                progress = { gameState.nitroPercent / 100f },
                color = TurboCyan,
                trackColor = Color(0x55000000),
                modifier = Modifier
                    .width(68.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }

        // Brake & Gas (Pedals)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Brake
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = TurboRed.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(width = 62.dp, height = 76.dp)
                    .testTag("btn_brake")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.isBraking = true
                                tryAwaitRelease()
                                viewModel.isBraking = false
                            }
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "فرامل\nSTOP",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Gas (Accelerator)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = TurboGreen.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(width = 72.dp, height = 86.dp)
                    .testTag("btn_accelerate")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.isAccelerating = true
                                tryAwaitRelease()
                                viewModel.isAccelerating = false
                            }
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "دواسة\nGAS",
                        color = Color(0xFF003311),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun VictoryOverlay(
    gameState: GameState,
    car: CarModel,
    onPlayAgain: () -> Unit,
    onGoToGarage: () -> Unit,
    onGoToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(2.dp, TurboGold),
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(0.92f)
            .shadow(24.dp, RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏆 وصلت إلى خط النهاية! 🏁",
                color = TurboGold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "مبروك! لقد أتممت سباق ${gameState.mode.nameAr}",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stars
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (s in 1..3) {
                    val earned = s <= gameState.starsEarned
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = if (earned) TurboGold else Color(0xFF475569),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification confirmation badge
            Surface(
                color = TurboCyanDark.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TurboCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notification sent",
                        tint = TurboCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تم إرسال إشعار الفوز إلى جهازك بنجاح! 🔔",
                        color = TurboCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val mins = (gameState.elapsedTimeSeconds / 60).toInt()
                val secs = (gameState.elapsedTimeSeconds % 60).toInt()
                ResultStatBadge(title = "الوقت", value = String.format("%02d:%02d", mins, secs))
                ResultStatBadge(title = "العملات", value = "+${gameState.coinsCollected} 🪙")
                ResultStatBadge(title = "السرعة", value = "${car.topSpeedKmh} كم/س")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = TurboCyan, contentColor = DarkBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_victory_play_again")
            ) {
                Icon(Icons.Default.Replay, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعادة السباق", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onGoToGarage,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("المرآب 🚗", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onGoToMenu,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("القائمة 🏠", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ResultStatBadge(title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = title, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun DefeatOverlay(
    gameState: GameState,
    onRetry: () -> Unit,
    onGoToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(2.dp, TurboRed),
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(0.88f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = TurboRed,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "لم تصل إلى خط النهاية!",
                color = TurboRed,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (gameState.carHealth <= 0f) "تحطمت السيارة بسبب الاصطدام بالحواجز!" else "نفد الوقت المحدد قبل الوصول لخط النهاية!",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = TurboOrange, contentColor = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_defeat_retry")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = onGoToMenu) {
                Text("العودة للقائمة الرئيسية", color = TextSecondary)
            }
        }
    }
}

@Composable
fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, TurboCyan),
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(0.82f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "اللعبة متوقفة مؤقتاً",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = TurboCyan, contentColor = DarkBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("متابعة السباق", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onRestart,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعادة تشغيل السباق")
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = onQuit) {
                Text("الخروج للقائمة", color = TurboRed)
            }
        }
    }
}
