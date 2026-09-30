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
}
