package fr.anekdot

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
    private val _jokeText = MutableStateFlow("Нажми на кнопку - получишь анекдот")
    val jokeText = _jokeText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

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
    val context = LocalContext.current

    // Используем Box для наложения слоев
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Задаем фон всему экрану
    ) {
        // 1. Область с текстом (скролл)
        // Мы кладем её первой, чтобы она была самым нижним слоем
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                // Важно: делаем отступы сверху и снизу, чтобы текст
                // начинался ниже заголовка и заканчивался выше кнопок
                .padding(top = 80.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = text,
                    style = typography.bodyLarge
                )
            }
        }

        // 2. Заголовок с непрозрачным фоном
        Surface(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.background // Закрывает текст под собой
        ) {
            Text(
                text = "Случайный анекдот",
                style = typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    fontSize = (typography.headlineMedium.fontSize.value + 2).sp
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }

        // 3. Плавающие кнопки (Floating Action Buttons)
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isLoading) 0.5f else 1f)
                    .background(
                        // Делаем легкий градиент или просто плашку у кнопок,
                        // чтобы текст под ними не мешал нажимать
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                            startY = 0f,
                            endY = 100f
                        )
                    )
                    .padding(bottom = 32.dp, top = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FloatingActionButton(
                    onClick = { viewModel.fetchNextJoke() },
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Следующий"
                    )
                }

                FloatingActionButton(
                    onClick = {
                        val appUrl = "https://anekdot.fr/joke"
                        val shareText = "$text\n\nИсточник: $appUrl"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    },
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Поделиться"
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

@Preview(showBackground = true)
@Composable
fun JokeScreenPreview() {
    AnekdotTheme {
        JokeScreen()
    }
}