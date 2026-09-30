package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HighScore
import com.example.data.RaceRecord
import com.example.game.GameModes
import com.example.game.GameViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class RecordsTab {
    HIGH_SCORES,
    RACE_HISTORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val records by viewModel.allRecords.collectAsState()
    val highScores by viewModel.topHighScores.collectAsState()
    val highestScore by viewModel.highestScore.collectAsState()
    val playerName by viewModel.playerName.collectAsState()

    var selectedTab by remember { mutableStateOf(RecordsTab.HIGH_SCORES) }
    var selectedFilterModeId by remember { mutableStateOf<String?>(null) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newPlayerNameInput by remember { mutableStateOf(playerName) }

    val filteredHighScores = remember(highScores, selectedFilterModeId) {
        if (selectedFilterModeId == null) {
            highScores
        } else {
            highScores.filter { it.modeId == selectedFilterModeId }
        }
    }

    val totalVictories = records.count { it.isVictory }
    val totalCoins = records.sumOf { it.coinsEarned }
    val totalStars = records.sumOf { it.stars }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text("تعديل اسم المتسابق", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        "سيتم حفظ هذا الاسم مع نقاط السكور العالي للسباقات القادمة:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPlayerNameInput,
                        onValueChange = { newPlayerNameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurboCyan,
                            unfocusedBorderColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePlayerName(newPlayerNameInput)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurboCyan, contentColor = DarkBg)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الأرقام القياسية والسكور 🏆",
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
            // Player Profile & Highest Score Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, TurboGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(TurboGold.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = TurboGold,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = playerName,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            newPlayerNameInput = playerName
                                            showEditNameDialog = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Name",
                                            tint = TurboCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "أعلى سكور مسجل: $highestScore نقطة",
                                    color = TurboCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            color = TurboGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TurboGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Room DB",
                                color = TurboGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Tabs Selector: High Scores vs Race History
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                ) {
                    TabButton(
                        title = "لوحة السكور العالي 🏆",
                        isSelected = selectedTab == RecordsTab.HIGH_SCORES,
                        onClick = { selectedTab = RecordsTab.HIGH_SCORES },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        title = "سجل السباقات 📜",
                        isSelected = selectedTab == RecordsTab.RACE_HISTORY,
                        onClick = { selectedTab = RecordsTab.RACE_HISTORY },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (selectedTab == RecordsTab.HIGH_SCORES) {
                // Filter by Game Mode
                item {
                    Text(
                        text = "تصفية حسب الحلبة:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilterModeId == null,
                                onClick = { selectedFilterModeId = null },
                                label = { Text("جميع الحلبات") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TurboCyan,
                                    selectedLabelColor = DarkBg,
                                    containerColor = DarkSurface,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                        items(GameModes.allModes) { mode ->
                            FilterChip(
                                selected = selectedFilterModeId == mode.id,
                                onClick = { selectedFilterModeId = mode.id },
                                label = { Text(mode.nameAr) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TurboCyan,
                                    selectedLabelColor = DarkBg,
                                    containerColor = DarkSurface,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (filteredHighScores.isEmpty()) {
                    item {
                        EmptyScoresCard()
                    }
                } else {
                    itemsIndexed(filteredHighScores) { index, highScore ->
                        HighScoreItemCard(
                            rank = index + 1,
                            highScore = highScore
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            } else {
                // Race History Tab
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatSummaryCard(
                            title = "سباقات ناجحة",
                            value = "$totalVictories",
                            icon = Icons.Default.EmojiEvents,
                            tint = TurboGold,
                            modifier = Modifier.weight(1f)
                        )
                        StatSummaryCard(
                            title = "مجموع النجوم",
                            value = "$totalStars ⭐",
                            icon = Icons.Default.Star,
                            tint = TurboCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatSummaryCard(
                            title = "عملات تم حصدها",
                            value = "+$totalCoins 🪙",
                            icon = Icons.Default.MonetizationOn,
                            tint = TurboOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (records.isEmpty()) {
                    item {
                        EmptyHistoryCard()
                    }
                } else {
                    items(records) { record ->
                        RecordItemCard(record = record)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) TurboCyan else Color.Transparent,
            contentColor = if (isSelected) DarkBg else TextSecondary
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = null,
        modifier = modifier.height(38.dp)
    ) {
        Text(
            text = title,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

@Composable
fun HighScoreItemCard(
    rank: Int,
    highScore: HighScore
) {
    val dateStr = remember(highScore.timestamp) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(highScore.timestamp))
    }
    val mins = (highScore.timeSeconds / 60).toInt()
    val secs = (highScore.timeSeconds % 60).toInt()

    val rankColor = when (rank) {
        1 -> TurboGold
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> TextSecondary
    }

    val rankBadge = when (rank) {
        1 -> "🥇 #1"
        2 -> "🥈 #2"
        3 -> "🥉 #3"
        else -> "#$rank"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (rank == 1) TurboGold.copy(alpha = 0.8f) else Color(0x22FFFFFF)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank Badge
                Surface(
                    color = rankColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, rankColor.copy(alpha = 0.5f)),
                    modifier = Modifier.size(width = 46.dp, height = 40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = rankBadge,
                            color = rankColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = highScore.playerName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (highScore.isCompleted) {
                            Text(text = "⭐".repeat(highScore.stars), fontSize = 10.sp)
                        } else {
                            Surface(
                                color = TurboRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "اصطدام",
                                    color = TurboRed,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${highScore.modeName} • ${highScore.carName}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "الزمن: ${String.format("%02d:%02d", mins, secs)} • السرعة: ${highScore.maxSpeedKmh} كم/س • $dateStr",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // Score Value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${highScore.score}",
                    color = TurboGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Text(
                    text = "نقطة سكور",
                    color = TurboCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyScoresCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SportsScore,
                contentDescription = null,
                tint = TurboGold,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "لا توجد أرقام قياسية مسجلة بعد!",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "العب أي سباق وسيتم احتساب سكور الجولة وحفظه تلقائياً في قاعدة بيانات Room المحلية!",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyHistoryCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "لم تكتمل أي سباقات بعد!",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "انطلق في أحد الأوضاع وعبر خط النهاية لتسجيل أرقامك القياسية!",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StatSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(text = title, color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun RecordItemCard(record: RaceRecord) {
    val dateStr = remember(record.completedAt) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(record.completedAt))
    }
    val mins = (record.timeSeconds / 60).toInt()
    val secs = (record.timeSeconds % 60).toInt()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.modeName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⭐".repeat(record.stars),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "الزمن: ${String.format("%02d:%02d", mins, secs)} • السرعة: ${record.maxSpeedKmh} كم/س",
                    color = TurboCyan,
                    fontSize = 11.sp
                )
                Text(
                    text = dateStr,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "+${record.coinsEarned} 🪙",
                    color = TurboGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
