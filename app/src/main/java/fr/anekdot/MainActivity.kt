package fr.anekdot

import SettingsScreen
import SettingsViewModel
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import com.google.android.gms.security.ProviderInstaller
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import fr.anekdot.ui.theme.AnekdotTheme

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var appUpdateManager: AppUpdateManager

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.e("Update", "Пользователь отклонил обновление или произошла ошибка")
            // Здесь можно что-то сделать, если обновление критично
        }
    }

    private fun checkForUpdates() { // Проверка наличия обновления и запуск установки
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) { // Если обновление есть И оно поддерживает принудительный режим
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
        }
    }

    // Проверка, если юзер свернул приложение во время обновления
    private fun checkForContinueUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdates()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: Exception) {
            Log.e("Anekdot", "Не удалось обновить поставщика безопасности", e)
        }

        // Вызываем загрузку только при ПЕРВОМ запуске приложения
        // Если savedInstanceState != null, значит, это поворот экрана,
        // и ViewModel сама сохранит текущий текст.
        if (savedInstanceState == null) {
            mainViewModel.fetchNextJoke()
        }
        checkIntentForJoke(intent)
        scheduleDailyJoke()

        enableEdgeToEdge()
        setContent {
            AnekdotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Выбираем, какой экран показать
                    when (App.currentScreen) {
                        App.Screen.MAIN -> {
                            MainScreen(
                                Modifier.padding(innerPadding),
                                mainViewModel,
                                settingsViewModel
                            )
                        }
                        App.Screen.SETTINGS -> {
                            SettingsScreen(
                                Modifier.padding(innerPadding),
                                settingsViewModel,
                                App.Screen.MAIN
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkForContinueUpdate()
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
            App.currentScreen = App.Screen.MAIN
        }
    }

    private fun checkIntentForJoke(intent: Intent?) {
        val joke = intent?.getStringExtra("joke_from_notification")
        if (joke != null) {
            mainViewModel.displayJoke(joke)
        }
    }
}

