package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CarCatalog
import com.example.game.GameModes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Turbo Drive 3D", appName)
  }

  @Test
  fun `verify game modes exist`() {
    assertTrue(GameModes.allModes.size >= 5)
    val highway = GameModes.getModeById("highway_sprint")
    assertNotNull(highway)
    assertTrue(highway.trackLengthMeters > 0)
  }

  @Test
  fun `verify cars catalog has unlocked default car`() {
    val defaultCar = CarCatalog.getCarById("speed_demon")
    assertNotNull(defaultCar)
    assertTrue(defaultCar.isDefaultUnlocked)
  }

  @Test
  fun `verify high score calculation formula`() {
    val score = com.example.data.HighScore.calculateScore(
      isVictory = true,
      distanceCoveredMeters = 2500f,
      trackLengthMeters = 2500f,
      timeSeconds = 55f,
      maxSpeedKmh = 240,
      coinsCollected = 15,
      stars = 3
    )
    assertTrue(score > 10000)
  }

  @Test
  fun `verify Room database save and retrieve high scores`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.highScoreDao()

    val highScore = com.example.data.HighScore(
      playerName = "كابتن السرعة",
      score = 14500,
      modeId = "highway_sprint",
      modeName = "طريق المطار السريع",
      timeSeconds = 52.4f,
      maxSpeedKmh = 250,
      stars = 3,
      coinsEarned = 150,
      distanceCoveredMeters = 2500f,
      isCompleted = true,
      carName = "الوحش النفاث"
    )

    val id = dao.insertHighScore(highScore)
    assertTrue(id > 0)

    val highest = dao.getHighestScoreDirect()
    assertEquals(14500, highest)

    db.close()
  }
}
