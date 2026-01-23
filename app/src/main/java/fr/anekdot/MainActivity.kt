package fr.anekdot

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.security.ProviderInstaller
import fr.anekdot.ui.theme.AnekdotTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null

    fun playSound(context: Context, resId: Int) {
        mediaPlayer?.release() // Освобождаем ресурсы предыдущего звука
        mediaPlayer = MediaPlayer.create(context, resId)
        mediaPlayer?.start()
    }
}

val ComfortaaFontFamily = FontFamily(
    Font(R.font.comfortaa_regular, FontWeight.Normal)
)

// Пары цветов для градиента (Start Color, End Color)
val gradientPresets = listOf(
    Color(0xFFCFD9DF) to Color(0xFFE2E2E2), // Светло-серый жемчуг
    Color(0xFFFF9A9E) to Color(0xFFFAD0C4), // Нежно-розовый
    Color(0xFFA18CD1) to Color(0xFFFBC2EB), // Сиреневый
    Color(0xFF84FAB0) to Color(0xFF8FD3F4), // Мятно-голубой
    Color(0xFFF6D365) to Color(0xFFFDA085), // Солнечно-оранжевый
    Color(0xFFA8E063) to Color(0xFF56AB2F)  // Сочное яблоко
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

class JokeViewModel : ViewModel() {
    private val _jokeText = MutableStateFlow("Нажми на кнопку -\nполучишь анекдот")
    val jokeText = _jokeText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _gradientIndex = MutableStateFlow(0)
    val gradientIndex = _gradientIndex.asStateFlow()

    fun fetchNextJoke() {
        if (_isLoading.value) return
        //Log.d("JokeDebug", "Функция вызвана") // D - Debug
        viewModelScope.launch {
            try {
                _isLoading.value = true
                //Log.d("JokeDebug", "Начинаем запрос...")
                val response = RetrofitInstance.api.getRandomJoke()
                //Log.d("JokeDebug", "Успех: ${response.p.text}")
                displayJoke(response.p.text)
            } catch (e: Exception) {
                Log.e("JokeDebug", "Ошибка запроса", e)
                _jokeText.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun displayJoke(text: String) {
        _jokeText.value = text
        // Выбираем новый случайный индекс градиента
        _gradientIndex.value = (gradientPresets.indices).random()
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var jokeViewModel: JokeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: Exception) {
            Log.e("JokeDebug", "Не удалось обновить поставщика безопасности", e)
        }
        jokeViewModel = ViewModelProvider(this)[JokeViewModel::class.java]
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
            AnekdotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    JokeScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = jokeViewModel
                    )
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
    }

    private fun checkIntentForJoke(intent: Intent?) {
        val joke = intent?.getStringExtra("joke_from_notification")
        if (joke != null) {
            jokeViewModel.displayJoke(joke)
        }
    }
}

@Composable
fun JokeScreen(
    modifier: Modifier = Modifier,
    viewModel: JokeViewModel
) {
    val text by viewModel.jokeText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val gIndex by viewModel.gradientIndex.collectAsState()
    val context = LocalContext.current

    val view = androidx.compose.ui.platform.LocalView.current
    val (startColor, endColor) = gradientPresets[gIndex]

    val smallestWidth = LocalConfiguration.current.smallestScreenWidthDp // Это ВСЕГДА меньшая сторона
    val dynamicFontSize = smallestWidth * 0.05f

    LaunchedEffect(text) {
        if (text.length > 50 && (1..3).random() == 1) { // Если пришел анекдот и повезло (шанс 1 к 7)
            kotlinx.coroutines.delay((2000..5000).random().toLong())
            SoundManager.playSound(context, laughterResources.random())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(startColor, endColor)))
    ) {
        // Основной контент теперь занимает всё место, центрируясь
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dynamicFontSize.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = dynamicFontSize.dp, bottom = (dynamicFontSize * 6).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                // Большая белая крутилка на цветном фоне
                CircularProgressIndicator(
                    modifier = Modifier.size((dynamicFontSize * 12).dp),
                    color = Color.White,
                    strokeWidth = (dynamicFontSize * .8).dp
                )
            } else {
                AnimatedContent(
                    targetState = text to gIndex,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(600)) +
                                slideInVertically(animationSpec = tween(600), initialOffsetY = { it / 2 }))
                            .togetherWith(fadeOut(animationSpec = tween(300)) +
                                    slideOutVertically(animationSpec = tween(300), targetOffsetY = { -it / 2 }))
                    },
                    label = "JokeAnimation"
                ) { (targetText, _) ->
                    Card(
                        shape = RoundedCornerShape((dynamicFontSize * 1.6).dp),
                        colors = CardDefaults.cardColors(
                            // Эффект матового стекла (90% прозрачности)
                            containerColor = Color.White.copy(alpha = 0.92f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = (dynamicFontSize * .6).dp)
                    ) {
                        Text(
                            text = targetText,
                            fontFamily = ComfortaaFontFamily,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.Black,
                                fontSize = dynamicFontSize.sp,
                                // Добавим межстрочный интервал для удобства чтения
                                lineHeight = (dynamicFontSize * 1.4).sp
                            ),
                            modifier = Modifier.padding((dynamicFontSize * 1.4).dp)
                        )
                    }
                }
            }
        }

        // Кнопки остаются внизу, они теперь главные акценты
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (dynamicFontSize * 2.4).dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Кнопка "Следующий"
                FloatingActionButton(
                    onClick = {
                        if (!isLoading) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            SoundManager.playSound(context, R.raw.button)
                            viewModel.fetchNextJoke()
                        }
                    },
                    shape = CircleShape,
                    containerColor = Color.White,
                    contentColor = startColor,
                    elevation = FloatingActionButtonDefaults.elevation((dynamicFontSize * .4).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Следующий",
                        modifier = Modifier.size((dynamicFontSize * 1.5).dp)
                    )
                }

                // Условие (smallestWidth < 100) не выполняется никогда
                if (BuildConfig.DEBUG && smallestWidth < 100) {
                    // Вставьте это между двумя FloatingActionButton в JokeScreen
                    FloatingActionButton(
                        onClick = {
                            val test = OneTimeWorkRequestBuilder<NotificationWorker>().build()
                            WorkManager.getInstance(context).enqueue(test)
                        },
                        shape = CircleShape,
                        containerColor = Color.White,
                        contentColor = endColor,
                        elevation = FloatingActionButtonDefaults.elevation((dynamicFontSize * .4).dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Notifications,
                            contentDescription = "Тест уведомления",
                            modifier = Modifier.size((dynamicFontSize * 1.5).dp)
                        )
                    }
                }

                // Кнопка "Поделиться"
                FloatingActionButton(
                    onClick = {
                        if (!isLoading) {
                            SoundManager.playSound(context, R.raw.bluster)
                            val shareText = "$text\n\nЧитай тут: https://anekdot.fr"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                        }
                    },
                    shape = CircleShape,
                    containerColor = Color.White,
                    contentColor = endColor,
                    elevation = FloatingActionButtonDefaults.elevation((dynamicFontSize * .4).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Поделиться",
                        modifier = Modifier.size((dynamicFontSize * 1.5).dp)
                    )
                }
            }

            // Если идет загрузка, перекрываем кнопки невидимым кликабельным слоем
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures { } // поглощаем клики, ничего не делая
                        }

                )
            }
        }
    }
}

// Source: https://zvukogram.com/
val laughterResources = listOf(
    R.raw.carefree_cheerful_laughter_of_a_young_man, R.raw.chilling_laughter_of_baba_yaga,
    R.raw.creepy_laugh_single_long_male, R.raw.creepy_laugh_single_long_male_jellied,
    R.raw.creepy_laugh_single_male_close, R.raw.grandpa_laughs, R.raw.infectious_laughter,
    R.raw.jellied_male_laughter, R.raw.lady_laughs, R.raw.lady_laughs_cheeky,
    R.raw.laughter_1, R.raw.laughter_2, R.raw.laughter_5, R.raw.laughter_13,
    R.raw.laughter_22, R.raw.laughter_24, R.raw.laughter_26, R.raw.laughter_36,
    R.raw.laughter_37, R.raw.laughter_38, R.raw.laughter_43, R.raw.laughter_45,
    R.raw.laughter_sinister_long_low, R.raw.low_verbal_laughter, R.raw.male_short_laugh,
    R.raw.malevolent_laughter_of_a_man, R.raw.the_man_is_very_funny,
    R.raw.the_sound_of_male_laughter_man_laughing, R.raw.the_teasing_laughter_of_the_seductress,
    R.raw.the_woman_laughs_women39s_laughter, R.raw.uncontrollable_hysterical_laughter_of_a_man,
    R.raw.vulgar_female_laughter, R.raw.witch_single_long_rhythmic, R.raw.woman_laughing
)

@Preview(showBackground = true)
@Composable
fun JokeScreenPreview() {
    AnekdotTheme {
        JokeScreen(viewModel = viewModel())
    }
}
