package com.example.data

import androidx.compose.ui.graphics.Color

data class CarModel(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val descriptionAr: String,
    val topSpeedKmh: Int,
    val acceleration: Float, // 0.0 to 1.0
    val handling: Float,     // 0.0 to 1.0
    val nitroPower: Float,   // 0.0 to 1.0
    val primaryColor: Color,
    val secondaryColor: Color,
    val priceCoins: Int,
    val isDefaultUnlocked: Boolean = false,
    val bodyType: CarBodyType = CarBodyType.SUPER_CAR
)

enum class CarBodyType {
    SUPER_CAR,
    CYBER_RUNNER,
    DUNE_BUGGY,
    FORMULA_ONE,
    HYPER_GT
}

object CarCatalog {
    val cars = listOf(
        CarModel(
            id = "speed_demon",
            nameEn = "Apex GT",
            nameAr = "أبيكس جي تي",
            descriptionAr = "سيارة رياضية متوازنة ومثالية للمبتدئين في السباق السريع",
            topSpeedKmh = 230,
            acceleration = 0.65f,
            handling = 0.70f,
            nitroPower = 0.60f,
            primaryColor = Color(0xFFFF1744), // Crimson Red
            secondaryColor = Color(0xFF1E293B),
            priceCoins = 0,
            isDefaultUnlocked = true,
            bodyType = CarBodyType.SUPER_CAR
        ),
        CarModel(
            id = "cyber_phantom",
            nameEn = "Cyber Phantom",
            nameAr = "فانتوم السايبر",
            descriptionAr = "سيارة مستقبلية فائقة التكنولوجيا مع شعلة نيترو نيون مذهلة",
            topSpeedKmh = 270,
            acceleration = 0.85f,
            handling = 0.80f,
            nitroPower = 0.90f,
            primaryColor = Color(0xFF00E5FF), // Cyan Neon
            secondaryColor = Color(0xFF7C4DFF),
            priceCoins = 450,
            isDefaultUnlocked = false,
            bodyType = CarBodyType.CYBER_RUNNER
        ),
        CarModel(
            id = "dune_striker",
            nameEn = "Dune Striker",
            nameAr = "كاسر الرمال",
            descriptionAr = "وحش الطرق الوعرة مع تحكم هائل واجتياز ممتاز للرمال والعوائق",
            topSpeedKmh = 245,
            acceleration = 0.75f,
            handling = 0.95f,
            nitroPower = 0.70f,
            primaryColor = Color(0xFFFF9100), // Desert Amber
            secondaryColor = Color(0xFF263238),
            priceCoins = 300,
            isDefaultUnlocked = false,
            bodyType = CarBodyType.DUNE_BUGGY
        ),
        CarModel(
            id = "formula_apex",
            nameEn = "Formula Vortex",
            nameAr = "فورتكس فورمولا",
            descriptionAr = "سرعة قصوى مرعبة وتسارع فوري لكسر جميع الأرقام القياسية",
            topSpeedKmh = 320,
            acceleration = 0.95f,
            handling = 0.85f,
            nitroPower = 0.95f,
            primaryColor = Color(0xFFFFD700), // Racing Gold
            secondaryColor = Color(0xFF000000),
            priceCoins = 800,
            isDefaultUnlocked = false,
            bodyType = CarBodyType.FORMULA_ONE
        )
    )

    fun getCarById(id: String): CarModel {
        return cars.find { it.id == id } ?: cars.first()
    }
}
