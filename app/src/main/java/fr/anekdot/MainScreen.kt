package fr.anekdot

import SettingsViewModel
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
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
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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

    fun onShare(context: Context, text: String) {
        if (!_isLoading.value && text.isNotBlank()) {
            // Берем настройки напрямую из менеджера
            if (App.settingsManager.isClickSoundEnabled.value) {
                SoundManager.playSound(R.raw.svist_fit_ha)
            }

            val shareText = "${text}\n\nВы хочете шуток? Их есть у меня:\n" +
                    "https://play.google.com/store/apps/details?id=fr.anekdot"

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, null))
        }
    }

    fun onNext(context: Context, view: View) {
        if (!isLoading.value) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (App.settingsManager.isClickSoundEnabled.value) {
                SoundManager.playSound(R.raw.bluster)
            }
            fetchNextJoke()
        }
    }

    fun onSettings(settingsViewModel: SettingsViewModel) {
        if (App.settingsManager.isClickSoundEnabled.value) {
            SoundManager.playSound(R.raw.button)
        }
        if (App.settingsManager.isColorStyleEnabled.value) {
            settingsViewModel.chooseRandomColors()
        }
        App.currentScreen = App.Screen.SETTINGS
    }
}

@Composable
fun MainButton(
    viewModel: MainViewModel,
    imageVector: ImageVector,
    contentDescription: String,
    iconSize: Float,
    onClick: () -> Unit
) {
    val isColorStyle by App.settingsManager.isColorStyleEnabled.collectAsState()
    val containerColor = if (isColorStyle) Color.White else MaterialTheme.colorScheme.surface
    val contentColor = if (isColorStyle) viewModel.gradientColors.collectAsState().value.first else MaterialTheme.colorScheme.onSurface
    val elevation = if (isColorStyle) FloatingActionButtonDefaults.elevation((iconSize * .2).dp) else FloatingActionButtonDefaults.elevation()
    FloatingActionButton(onClick, Modifier, CircleShape, containerColor, contentColor, elevation) {
        Icon(imageVector, contentDescription, Modifier.size(iconSize.dp))
    }
}

@Composable
fun MainText(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    text: String
) {
    val baseFontSize = App.baseFontSize
    // Читаем значение (от 1 до 5)
    val relativeFontSize by settingsViewModel.relativeFontSize.collectAsState()
    val dynamicFontSize = baseFontSize * 0.2 * (2 + relativeFontSize) // от 0.6 до 1.4
    if (App.settingsManager.isColorStyleEnabled.collectAsState().value) {
        val gColors by viewModel.gradientColors.collectAsState()
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
                shape = RoundedCornerShape((baseFontSize * 1.6).dp),
                colors = CardDefaults.cardColors(
                    // Эффект матового стекла (90% прозрачности)
                    containerColor = Color.White.copy(alpha = 0.92f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = (baseFontSize * .6).dp)
            ) {
                Text(
                    text = text,
                    fontFamily = ComfortaaFontFamily,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Black,
                        fontSize = dynamicFontSize.toSp(),
                        // Добавим межстрочный интервал для удобства чтения
                        lineHeight = (dynamicFontSize * 1.4).toSp()
                    ),
                    modifier = Modifier.padding((baseFontSize * 1.4).dp)
                )
            }
        }
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = dynamicFontSize.toSp(),
                // Добавим межстрочный интервал для удобства чтения
                lineHeight = (dynamicFontSize * 1.4).toSp()
            ),
            modifier = Modifier//.padding((baseFontSize * 1.4).dp)
        )
    }
}

@Composable
fun MainScreen(
    modifier: Modifier,
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel
) {
    val isColored by App.settingsManager.isColorStyleEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val text by viewModel.jokeText.collectAsState()

    val baseFontSize = App.baseFontSize
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(text) { // Если пришел анекдот и повезло (шанс 1 к 7)
        if (settingsViewModel.isLaughSoundEnabled.value && text.length > 50 && (1..3).random() == 1) {
            kotlinx.coroutines.delay((2000..5000).random().toLong())
            if (settingsViewModel.isLaughSoundEnabled.value) {
                SoundManager.playSound(Util.GetRandomLaugh())
            }
        }
    }

    val boxModifier = if (isColored) {
        val gColors by viewModel.gradientColors.collectAsState()
        val (startColor, endColor) = gColors
        modifier.background(Brush.verticalGradient(listOf(startColor, endColor)))
    } else {
        modifier
    }
    Box(boxModifier.fillMaxSize()) {
        // Основной контент теперь занимает всё место, центрируясь
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = baseFontSize.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = baseFontSize.dp, bottom = (baseFontSize * 6).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) { // Большая белая крутилка на цветном фоне
                val color = if (isColored) Color.White else ProgressIndicatorDefaults.circularColor
                CircularProgressIndicator(Modifier.size((baseFontSize * 12).dp), color, (baseFontSize * .8).dp)
            } else {
                MainText(viewModel, settingsViewModel, text)
            }
        }

        // Кнопки остаются внизу, они теперь - главные акценты
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (baseFontSize * 1.2).dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainButton(viewModel, Icons.Default.Share, "Поделиться", baseFontSize * 2) { viewModel.onShare(context, text) }
                MainButton(viewModel, Icons.Default.Refresh, "Следующий", baseFontSize * 4) { viewModel.onNext(context, view) }
                MainButton(viewModel, Icons.Default.Settings, "Настройки", baseFontSize * 2) { viewModel.onSettings(settingsViewModel) }
            }

            // Если идет загрузка, перекрываем кнопки невидимым кликабельным слоем
            if (isLoading) { // поглощаем клики, ничего не делая
                Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
            }
        }
    }
}
