package fr.anekdot

import SettingsManager
import SettingsScreen
import SettingsViewModel
import android.content.Context
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.security.ProviderInstaller
import fr.anekdot.ui.theme.AnekdotTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

val ComfortaaFontFamily = FontFamily(
    Font(R.font.comfortaa_regular, FontWeight.Normal)
)

@Serializable
data class JokeContent(
    val text: String
)

@Serializable
data class JokeResponse(
    val p: JokeContent
)

interface AnekdotApi {
    @GET("?a=")
    suspend fun getRandomJoke(
        @Query("maxlen") maxLength: Int? = null
    ) : JokeResponse
}

// Этот метод понадобился для поддержки Android 6 (SDK 23)
fun getOkHttpClient(context: Context): OkHttpClient {
    // 1. Загружаем сертификат из res/raw
    val cf = CertificateFactory.getInstance("X.509")
    val certInputStream = context.resources.openRawResource(R.raw.isrg_root_x1)
    val certificate = certInputStream.use { cf.generateCertificate(it) }

    // 2. Создаем KeyStore и добавляем туда этот сертификат
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        setCertificateEntry("isrg_root_x1", certificate)
    }

    // 3. Создаем TrustManager, который верит нашему KeyStore
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
        init(keyStore)
    }

    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, tmf.trustManagers, null)
    }

    // 4. Собираем клиент
    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
        .build()
}

object RetrofitInstance {
    private val _jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val _contentType = "application/json".toMediaType()

    val api: AnekdotApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://anekdot.fr/")
            .client(getOkHttpClient(App.getContext()))
            .addConverterFactory(_jsonConfig.asConverterFactory(_contentType))
            .build()
            .create(AnekdotApi::class.java)
    }
}

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

