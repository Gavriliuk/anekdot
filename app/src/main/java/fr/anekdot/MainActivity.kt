package fr.anekdot

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.anekdot.ui.theme.AnekdotTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

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
    Color(0xFFFF9A9E) to Color(0xFFFAD0C4), // Нежно-розовый
    Color(0xFFA18CD1) to Color(0xFFFBC2EB), // Сиреневый
    Color(0xFF84FAB0) to Color(0xFF8FD3F4), // Мятно-голубой
    Color(0xFFF6D365) to Color(0xFFFDA085), // Солнечно-оранжевый
    Color(0xFFCFD9DF) to Color(0xFFE2E2E2), // Светло-серый жемчуг
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
    @GET("joke?a=")
    suspend fun getRandomJoke() : JokeResponse
}

class JokeViewModel : ViewModel() {
    private val _jokeText = MutableStateFlow("Нажми на кнопку -\nполучишь анекдот")
    val jokeText = _jokeText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Состояние текущего градиента (храним индекс пары)
    private val _gradientIndex = MutableStateFlow(0)
    val gradientIndex = _gradientIndex.asStateFlow()

    private val _jsonConfig = Json {
        ignoreUnknownKeys = true // Игнорировать поля, которых нет в нашем классе
        coerceInputValues = true // На всякий случай, чтобы не падать на null
    }
    private val _contentType = "application/json".toMediaType()
    private val _jsonFactory = _jsonConfig.asConverterFactory(_contentType)
    private val _api = Retrofit.Builder()
        .baseUrl("https://anekdot.fr/")
        .addConverterFactory(_jsonFactory)
        .build()
        .create(AnekdotApi::class.java)

    fun fetchNextJoke() {
        if (_isLoading.value) return
        //Log.d("JokeDebug", "Функция вызвана") // D - Debug
        viewModelScope.launch {
            try {
                _isLoading.value = true
                //Log.d("JokeDebug", "Начинаем запрос...")
                val response = _api.getRandomJoke()
                //Log.d("JokeDebug", "Успех: ${response.p.text}")
                _jokeText.value = response.p.text
                // Выбираем новый случайный индекс градиента
                _gradientIndex.value = (gradientPresets.indices).random()
            } catch (e: Exception) {
                Log.e("JokeDebug", "Ошибка запроса", e)
                _jokeText.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnekdotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    JokeScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun JokeScreen(modifier: Modifier = Modifier) {
    val viewModel: JokeViewModel = viewModel()
    val text by viewModel.jokeText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val gIndex by viewModel.gradientIndex.collectAsState()
    val context = LocalContext.current

    val (startColor, endColor) = gradientPresets[gIndex]

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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                // Большая белая крутилка на цветном фоне
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = Color.White,
                    strokeWidth = 6.dp
                )
            } else {
                Card(
                    //modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        // Эффект матового стекла (90% прозрачности)
                        containerColor = Color.White.copy(alpha = 0.92f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Text(
                        text = text, // Use "$text\n$text" for layout debug
                        fontFamily = ComfortaaFontFamily,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp,
                            lineHeight = 28.sp // Добавим межстрочный интервал для удобства чтения
                        ),
                        modifier = Modifier.padding(28.dp)
                    )
                }
            }
        }

        // Кнопки остаются внизу, они теперь главные акценты
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Кнопка "Следующий"
            FloatingActionButton(
                onClick = {
                    if (!isLoading) {
                        SoundManager.playSound(context, R.raw.button)
                        viewModel.fetchNextJoke()
                    }
                },
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = startColor,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Следующий",
                    modifier = Modifier.size(30.dp)
                )
            }

            // Кнопка "Поделиться"
            FloatingActionButton(
                onClick = {
                    if (!isLoading) {
                        SoundManager.playSound(context, R.raw.bluster)
                        val shareText = "$text\n\nСмейся больше здесь: https://anekdot.fr/"
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
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Поделиться",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

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
        JokeScreen()
    }
}
