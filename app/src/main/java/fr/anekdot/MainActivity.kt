package fr.anekdot

import SettingsManager
import SettingsScreen
import SettingsViewModel
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.security.ProviderInstaller
import fr.anekdot.ui.theme.AnekdotTheme

class MainActivity : ComponentActivity() {
    private val settingsManager by lazy { SettingsManager(applicationContext) }
    private val settingsViewModel: SettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsManager) as T
            }
        }
    }
    private val jokeViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: Exception) {
            Log.e("JokeDebug", "Не удалось обновить поставщика безопасности", e)
        }
        // Вызываем загрузку только при ПЕРВОМ запуске приложения
        // Если savedInstanceState != null, значит, это поворот экрана,
        // и ViewModel сама сохранит текущий текст.
        if (savedInstanceState == null) {
            jokeViewModel.fetchNextJoke()
        }
        checkIntentForJoke(intent)
        scheduleDailyJoke()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        setContent {
            // Получаем коэффициент размера из настроек для адаптивности
            val smallestScreenWidthDp = LocalConfiguration.current.smallestScreenWidthDp
            val baseFontSizeDp = smallestScreenWidthDp * .05f
            AnekdotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Выбираем, какой экран показать
                    when (App.currentScreen) {
                        "main" -> {
                            MainScreen(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = jokeViewModel,
                                settingsViewModel = settingsViewModel,
                                // callback для перехода в настройки
                                onOpenSettings = {
                                    App.currentScreen = "settings"
                                }
                            )
                        }
                        "settings" -> {
                            SettingsScreen(
                                settingsViewModel,
                                baseFontSizeDp,
                                {
                                    val request = OneTimeWorkRequestBuilder<NotificationWorker>().build()
                                    WorkManager.getInstance(this@MainActivity).enqueue(request)
                                },
                                { App.currentScreen = "main" }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun scheduleDailyJoke() {
        val request = androidx.work.PeriodicWorkRequestBuilder<NotificationWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(
                // Только если есть сеть
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            // Не пересоздавать, если уже запущено
            "daily_joke", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    // Этот метод сработает, если приложение уже открыто и вы нажали на уведомление
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Важно: обновляем intent активности, иначе придет старый
        checkIntentForJoke(intent)
        // Если пришли из уведомления — принудительно на главный экран
        if (intent.hasExtra("joke_from_notification")) {
            App.currentScreen = "main"
        }
    }

    private fun checkIntentForJoke(intent: Intent?) {
        val joke = intent?.getStringExtra("joke_from_notification")
        if (joke != null) {
            jokeViewModel.displayJoke(joke)
        }
    }
}

