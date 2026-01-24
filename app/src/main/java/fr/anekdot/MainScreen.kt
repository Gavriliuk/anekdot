package fr.anekdot

import SettingsViewModel
import android.content.Intent
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _jokeText = MutableStateFlow("Нажми на кнопку -\nполучишь анекдот")
    val jokeText = _jokeText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _gradientColors = MutableStateFlow(Util.GetFirstColorPair())
    val gradientColors = _gradientColors.asStateFlow()

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
        _gradientColors.value = Util.GetRandomColorPair();
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onOpenSettings: () -> Unit
) {
    val text by viewModel.jokeText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val gColors by viewModel.gradientColors.collectAsState()
    val context = LocalContext.current

    val view = androidx.compose.ui.platform.LocalView.current
    val (startColor, endColor) = gColors

    // Читаем значение (от 1 до 5)
    val relativeFontSize by settingsViewModel.relativeFontSize.collectAsState()
    val smallestWidth = LocalConfiguration.current.smallestScreenWidthDp // Это ВСЕГДА меньшая сторона
    val dynamicFontSize = smallestWidth * 0.01f * (2 + relativeFontSize)

    LaunchedEffect(text) {
        if (settingsViewModel.isLaughSoundEnabled.value && text.length > 50 && (1..3).random() == 1) { // Если пришел анекдот и повезло (шанс 1 к 7)
            kotlinx.coroutines.delay((2000..5000).random().toLong())
            if (settingsViewModel.isLaughSoundEnabled.value) SoundManager.playSound(context, Util.GetRandomLaugh())
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
                    targetState = text to gColors,
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
                                fontSize = dynamicFontSize.toSp(),
                                // Добавим межстрочный интервал для удобства чтения
                                lineHeight = (dynamicFontSize * 1.4).toSp()
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
                    .padding(bottom = (dynamicFontSize * 1.2).dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка "Поделиться"
                FloatingActionButton(
                    onClick = {
                        if (!isLoading) {
                            if (settingsViewModel.isClickSoundEnabled.value) SoundManager.playSound(context, R.raw.svist_fit_ha)
                            val shareText = "$text\n\nВы хочете шуток? Их есть у меня:\n"+
                                    "https://play.google.com/store/apps/details?id=fr.anekdot"
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
                    contentColor = startColor,
                    elevation = FloatingActionButtonDefaults.elevation((dynamicFontSize * .4).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Поделиться",
                        modifier = Modifier.size((dynamicFontSize * 2).dp)
                    )
                }

                // Кнопка "Следующий"
                FloatingActionButton(
                    onClick = {
                        if (!isLoading) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            if (settingsViewModel.isClickSoundEnabled.value) SoundManager.playSound(context, R.raw.bluster)
                            viewModel.fetchNextJoke()
                        }
                    },
                    shape = CircleShape,
                    containerColor = Color.White,
                    contentColor = startColor,
                    elevation = FloatingActionButtonDefaults.elevation((dynamicFontSize * .8).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Следующий",
                        modifier = Modifier.size((dynamicFontSize * 4).dp)
                    )
                }

                // Кнопка "Настройки"
                FloatingActionButton(
                    onClick = {
                        if (settingsViewModel.isClickSoundEnabled.value) SoundManager.playSound(context, R.raw.button)
                        if (settingsViewModel.isColorStyleEnabled.value) settingsViewModel.chooseRandomColors()
                        onOpenSettings()
                    },
                    shape = CircleShape,
                    containerColor = Color.White,
                    contentColor = startColor,
                    elevation = FloatingActionButtonDefaults.elevation((dynamicFontSize * .4).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        modifier = Modifier.size((dynamicFontSize * 2).dp)
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
