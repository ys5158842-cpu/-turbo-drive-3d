package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CarCatalog
import com.example.data.CarModel
import com.example.game.GameViewModel
import com.example.ui.theme.*
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val coins by viewModel.coins.collectAsState()
    val selectedCarId by viewModel.selectedCarId.collectAsState()

    var activeCar by remember(selectedCarId) {
        mutableStateOf(CarCatalog.getCarById(selectedCarId))
    }

    val isUnlocked = remember(activeCar.id, coins) {
        viewModel.isCarUnlocked(activeCar.id)
    }
    val isSelected = activeCar.id == selectedCarId

    val infiniteTransition = rememberInfiniteTransition(label = "turntable")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotate"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مرآب السيارات الخارقة 🚗",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TurboGold.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = TurboGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$coins",
                                color = TurboGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 1. Interactive 3D Turntable Display
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, activeCar.primaryColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Turntable Canvas
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cx = w / 2f
                            val cy = h * 0.65f

                            // Podium Disc
                            drawOval(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(cx - 160f, cy - 20f),
                                size = Size(320f, 60f)
                            )
                            drawOval(
                                color = activeCar.primaryColor.copy(alpha = 0.35f),
                                topLeft = Offset(cx - 150f, cy - 16f),
                                size = Size(300f, 52f)
                            )

                            // Render stylized car on podium
                            rotate(degrees = rotationAngle, pivot = Offset(cx, cy)) {
                                drawCarVisual(
                                    cx = cx,
                                    cy = cy - 20f,
                                    car = activeCar
                                )
                            }
                        }

                        // Car Name Badge Overlay
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = activeCar.nameAr,
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = activeCar.nameEn,
                                color = TurboCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeCar.descriptionAr,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            )
                        }
                    }
                }
            }

            // 2. Car Selection Carousel
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "أسطول المركبات",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(CarCatalog.cars) { car ->
                        val isCurrent = car.id == activeCar.id
                        val unlocked = viewModel.isCarUnlocked(car.id)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCurrent) DarkSurfaceVariant else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isCurrent) 2.dp else 1.dp,
                                if (isCurrent) TurboCyan else Color(0x33FFFFFF)
                            ),
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { activeCar = car }
                                .testTag("car_item_${car.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(car.primaryColor, CircleShape)
                                        .border(2.dp, car.secondaryColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = car.nameAr,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (unlocked) {
                                    Text(
                                        text = if (car.id == selectedCarId) "مُحدد ✓" else "متاح",
                                        color = if (car.id == selectedCarId) TurboGreen else TurboCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "${car.priceCoins} 🪙",
                                        color = TurboGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Vehicle Specifications & Stats Bars
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "المواصفات الفنية",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        CarStatProgress(
                            label = "السرعة القصوى",
                            valueText = "${activeCar.topSpeedKmh} كم/س",
                            progress = (activeCar.topSpeedKmh - 180f) / 160f,
                            color = TurboOrange
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        CarStatProgress(
                            label = "معدل التسارع",
                            valueText = "${(activeCar.acceleration * 100).toInt()}%",
                            progress = activeCar.acceleration,
                            color = TurboCyan
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        CarStatProgress(
                            label = "التحكم والمناورة",
                            valueText = "${(activeCar.handling * 100).toInt()}%",
                            progress = activeCar.handling,
                            color = TurboGreen
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        CarStatProgress(
                            label = "قوة النيترو (NOS)",
                            valueText = "${(activeCar.nitroPower * 100).toInt()}%",
                            progress = activeCar.nitroPower,
                            color = TurboGold
                        )
                    }
                }
            }

            // 4. Action Button (Select or Buy)
            item {
                Spacer(modifier = Modifier.height(20.dp))

                if (isSelected) {
                    Surface(
                        color = TurboGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, TurboGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TurboGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "السيارة النشطة حالياً في السباق",
                                color = TurboGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (isUnlocked) {
                    Button(
                        onClick = { viewModel.selectCar(activeCar.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = TurboCyan, contentColor = DarkBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_select_car")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار هذه السيارة للسباق", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                } else {
                    val canAfford = coins >= activeCar.priceCoins
                    Button(
                        onClick = { viewModel.buyCar(activeCar) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurboGold,
                            contentColor = DarkBg,
                            disabledContainerColor = DarkSurfaceVariant,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_buy_car")
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (canAfford) "فتح وشراء السيارة (${activeCar.priceCoins} 🪙)" else "عملات غير كافية (${activeCar.priceCoins} 🪙)",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCarVisual(
    cx: Float,
    cy: Float,
    car: CarModel
) {
    val carW = 160f
    val carH = 90f

    // Shadow
    drawOval(
        color = Color(0x66000000),
        topLeft = Offset(cx - carW * 0.5f, cy - carH * 0.1f),
        size = Size(carW, carH * 0.4f)
    )

    // Body
    val bodyTop = cy - carH
    val path = Path().apply {
        moveTo(cx - carW * 0.45f, cy - carH * 0.2f)
        lineTo(cx - carW * 0.38f, bodyTop + carH * 0.3f)
        lineTo(cx - carW * 0.28f, bodyTop)
        lineTo(cx + carW * 0.28f, bodyTop)
        lineTo(cx + carW * 0.38f, bodyTop + carH * 0.3f)
        lineTo(cx + carW * 0.45f, cy - carH * 0.2f)
        close()
    }
    drawPath(path, color = car.primaryColor)

    // Canopy
    val canopy = Path().apply {
        moveTo(cx - carW * 0.24f, bodyTop + carH * 0.28f)
        lineTo(cx - carW * 0.18f, bodyTop + carH * 0.08f)
        lineTo(cx + carW * 0.18f, bodyTop + carH * 0.08f)
        lineTo(cx + carW * 0.24f, bodyTop + carH * 0.28f)
        close()
    }
    drawPath(canopy, color = Color(0xFF0F172A))

    // Spoiler
    drawRoundRect(
        color = car.secondaryColor,
        topLeft = Offset(cx - carW * 0.42f, bodyTop - carH * 0.08f),
        size = Size(carW * 0.84f, carH * 0.12f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
    )

    // Tail Lights
    drawRect(
        color = Color(0xFFFF1744),
        topLeft = Offset(cx - carW * 0.38f, bodyTop + carH * 0.45f),
        size = Size(carW * 0.16f, carH * 0.14f)
    )
    drawRect(
        color = Color(0xFFFF1744),
        topLeft = Offset(cx + carW * 0.22f, bodyTop + carH * 0.45f),
        size = Size(carW * 0.16f, carH * 0.14f)
    )
}

@Composable
fun CarStatProgress(
    label: String,
    valueText: String,
    progress: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextSecondary, fontSize = 12.sp)
            Text(text = valueText, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0.1f, 1.0f) },
            color = color,
            trackColor = DarkSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}
