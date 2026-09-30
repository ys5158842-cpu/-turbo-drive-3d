package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class TrackTheme {
    HIGHWAY,
    DESERT,
    CYBERPUNK,
    MOUNTAIN_NIGHT,
    POLICE_ESCAPE
}

data class GameMode(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val descriptionAr: String,
    val trackLengthMeters: Float,
    val theme: TrackTheme,
    val trafficDensity: Float, // 0.0 to 1.0
    val obstacleDensity: Float, // 0.0 to 1.0
    val hasTimeLimit: Boolean = false,
    val timeLimitSeconds: Float = 0f,
    val hasPolice: Boolean = false,
    val skyColorTop: Color,
    val skyColorBottom: Color,
    val roadColorLight: Color,
    val roadColorDark: Color,
    val curbColorA: Color,
    val curbColorB: Color,
    val terrainColorLight: Color,
    val terrainColorDark: Color,
    val fogColor: Color,
    val coinBonusMultiplier: Float = 1.0f
)

object GameModes {
    val HIGHWAY = GameMode(
        id = "highway_sprint",
        nameEn = "Highway Sprint",
        nameAr = "طريق السريع - النهار",
        descriptionAr = "سباق سريع على طريق معبد متعرج بين التلال الخضراء مع حركة مرور لتفاديها حتى خط النهاية.",
        trackLengthMeters = 2500f,
        theme = TrackTheme.HIGHWAY,
        trafficDensity = 0.5f,
        obstacleDensity = 0.3f,
        skyColorTop = Color(0xFF1E88E5),
        skyColorBottom = Color(0xFF90CAF9),
        roadColorLight = Color(0xFF2C3240),
        roadColorDark = Color(0xFF252A36),
        curbColorA = Color(0xFFE53935),
        curbColorB = Color(0xFFFFFFFF),
        terrainColorLight = Color(0xFF2E7D32),
        terrainColorDark = Color(0xFF1B5E20),
        fogColor = Color(0x6690CAF9),
        coinBonusMultiplier = 1.0f
    )

    val DESERT = GameMode(
        id = "desert_storm",
        nameEn = "Desert Storm",
        nameAr = "عاصفة الرمال الذهبية",
        descriptionAr = "تحدي الرمال الشرس بين الكثبان والأهرامات والصخور المتساقطة مع رياح قوية حتى خط النهاية.",
        trackLengthMeters = 3000f,
        theme = TrackTheme.DESERT,
        trafficDensity = 0.4f,
        obstacleDensity = 0.7f,
        skyColorTop = Color(0xFFE65100),
        skyColorBottom = Color(0xFFFFB74D),
        roadColorLight = Color(0xFF423B32),
        roadColorDark = Color(0xFF383229),
        curbColorA = Color(0xFFFF6F00),
        curbColorB = Color(0xFFFFE082),
        terrainColorLight = Color(0xFFD7A755),
        terrainColorDark = Color(0xFFC7923E),
        fogColor = Color(0x88FFB74D),
        coinBonusMultiplier = 1.3f
    )

    val CYBER_NEON = GameMode(
        id = "cyber_neon",
        nameEn = "Cyber Neon 2099",
        nameAr = "مدينة النيون 2099",
        descriptionAr = "طريق ليلي مشع بأضواء النيون وناطحات السحاب المستقبلية مع منصات سرعة تيربو وحواجز إلكترونية.",
        trackLengthMeters = 3500f,
        theme = TrackTheme.CYBERPUNK,
        trafficDensity = 0.65f,
        obstacleDensity = 0.55f,
        skyColorTop = Color(0xFF0A0017),
        skyColorBottom = Color(0xFF311B92),
        roadColorLight = Color(0xFF16192B),
        roadColorDark = Color(0xFF0F111E),
        curbColorA = Color(0xFF00E5FF),
        curbColorB = Color(0xFFFF007F),
        terrainColorLight = Color(0xFF0D0A24),
        terrainColorDark = Color(0xFF070514),
        fogColor = Color(0x77311B92),
        coinBonusMultiplier = 1.6f
    )

    val TIME_ATTACK = GameMode(
        id = "time_attack",
        nameEn = "Time Attack",
        nameAr = "تحدي سباق ضد الساعة",
        descriptionAr = "العداد يتناقص بسرعة! اجمع معززات الوقت وتجاوز الحواجز قبل نفاد الوقت للوصول لخط النهاية.",
        trackLengthMeters = 2200f,
        theme = TrackTheme.MOUNTAIN_NIGHT,
        trafficDensity = 0.45f,
        obstacleDensity = 0.8f,
        hasTimeLimit = true,
        timeLimitSeconds = 45f,
        skyColorTop = Color(0xFF021B2B),
        skyColorBottom = Color(0xFF084B6F),
        roadColorLight = Color(0xFF1F2937),
        roadColorDark = Color(0xFF111827),
        curbColorA = Color(0xFFFFD600),
        curbColorB = Color(0xFF000000),
        terrainColorLight = Color(0xFF0F382E),
        terrainColorDark = Color(0xFF06231D),
        fogColor = Color(0x66084B6F),
        coinBonusMultiplier = 1.8f
    )

    val POLICE_PURSUIT = GameMode(
        id = "police_pursuit",
        nameEn = "Police Pursuit",
        nameAr = "مطاردة الشرطة المثيرة",
        descriptionAr = "دوريات الشرطة تحاصر الطريق بصفارات الإنذار والحواجز! عليك الإفلات والوصول إلى خط النهاية الآمن!",
        trackLengthMeters = 3200f,
        theme = TrackTheme.POLICE_ESCAPE,
        trafficDensity = 0.7f,
        obstacleDensity = 0.6f,
        hasPolice = true,
        skyColorTop = Color(0xFF0B101E),
        skyColorBottom = Color(0xFF1A237E),
        roadColorLight = Color(0xFF262C38),
        roadColorDark = Color(0xFF1D222B),
        curbColorA = Color(0xFF2979FF),
        curbColorB = Color(0xFFFF1744),
        terrainColorLight = Color(0xFF1A1F2C),
        terrainColorDark = Color(0xFF10141D),
        fogColor = Color(0x771A237E),
        coinBonusMultiplier = 2.0f
    )

    val allModes = listOf(HIGHWAY, DESERT, CYBER_NEON, TIME_ATTACK, POLICE_PURSUIT)

    fun getModeById(id: String): GameMode {
        return allModes.find { it.id == id } ?: HIGHWAY
    }
}
