package fr.anekdot

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
data class JokeResponse(
    val text: String
)

interface AnekdotApi {
    @GET("joke?a=")
    suspend fun getRandomJoke() : JokeResponse
}

class JokeViewModel : ViewModel() {
    private val _jokeText = MutableStateFlow("Нажми на кнопку, чтобы получить анекдот")
    val jokeText = _jokeText.asStateFlow()

    private val _api = Retrofit.Builder()
        .baseUrl("https://anekdot.fr/")
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AnekdotApi::class.java)

    fun fetchNextJoke() {
        viewModelScope.launch {
            try {
                val response = _api.getRandomJoke()
                _jokeText.value = response.text
            } catch (e: Exception) {
                _jokeText.value = "Ошибка: ${e.message}"
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
    val viewModel = JokeViewModel()
    val text by viewModel.jokeText.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Случайный анекдот",
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { viewModel.fetchNextJoke() }) {
                Text("Следующий")
            }
            Button(onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
            }) {
                Text("Поделиться")
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