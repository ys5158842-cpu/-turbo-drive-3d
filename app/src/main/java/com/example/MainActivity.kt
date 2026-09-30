package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.GamePreferences
import com.example.data.GameRepository
import com.example.game.GameModes
import com.example.game.GameViewModel
import com.example.game.RaceNotificationHelper
import com.example.ui.*
import com.example.ui.theme.DarkBg
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    HOME,
    GAME,
    GARAGE,
    RECORDS,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val preferences = GamePreferences(this)
        repository = GameRepository(database.raceDao(), database.highScoreDao(), preferences)

        RaceNotificationHelper.createNotificationChannel(this)

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(Screen.HOME) }

                // Factory or direct view model
                val viewModel = remember {
                    GameViewModel(application, repository)
                }

                // Notification Permission Launcher (Android 13+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        Toast.makeText(this, "تم تفعيل إشعارات خط النهاية بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                }

                // Request on initial launch if on API 33+
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (!RaceNotificationHelper.hasNotificationPermission(this@MainActivity)) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    when (currentScreen) {
                        Screen.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onStartRace = { mode ->
                                    viewModel.startRace(mode)
                                    currentScreen = Screen.GAME
                                },
                                onNavigateToGarage = { currentScreen = Screen.GARAGE },
                                onNavigateToRecords = { currentScreen = Screen.RECORDS },
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }
                        Screen.GAME -> {
                            GameScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = Screen.HOME },
                                onNavigateToGarage = { currentScreen = Screen.GARAGE }
                            )
                        }
                        Screen.GARAGE -> {
                            GarageScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = Screen.HOME }
                            )
                        }
                        Screen.RECORDS -> {
                            RecordsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = Screen.HOME }
                            )
                        }
                        Screen.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                repository = repository,
                                onNavigateBack = { currentScreen = Screen.HOME },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
