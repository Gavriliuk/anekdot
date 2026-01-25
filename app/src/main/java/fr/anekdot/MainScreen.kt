package fr.anekdot

import SettingsViewModel
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.CaptureController
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _jokeText = MutableStateFlow("Нажми на кнопку -\nполучишь анекдот")
    val jokeText = _jokeText.asStateFlow()

    private val _oldJokeText = MutableStateFlow("")
    val oldJokeText = _oldJokeText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    private val _gradientColors = MutableStateFlow(Util.GetFirstColorPair())
    val gradientColors = _gradientColors.asStateFlow()

    var rotationAngle by mutableFloatStateOf(0f)
    var rotationTarget by mutableFloatStateOf(0f)

    fun resetRotation() {
        _oldJokeText.value = _jokeText.value
        rotationAngle = 0f
        rotationTarget = 0f
    }

    fun fetchNextJoke() {
        if (_isLoading.value) return
        //Log.d("Anekdot", "Функция вызвана")

        _oldJokeText.value = _jokeText.value
        viewModelScope.launch {
            try {
                _isLoading.value = true
                //Log.d("Anekdot", "Начинаем запрос...")
                val response = RetrofitInstance.api.getRandomJoke()
                //Log.d("Anekdot", "Успех: ${response.p.text}")
                if (response.p.text.isNotBlank()) {
                    displayJoke(response.p.text)
                } else {
                    _errorEvent.emit("Сервер прислал пустой ответ")
                }
            } catch (e: Exception) {
                Log.e("Anekdot", "Ошибка сети", e)
                _errorEvent.emit( "Ошибка сети: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun displayJoke(text: String) {
        _jokeText.value = text
        // Выбираем новый случайный индекс градиента
        _gradientColors.value = Util.GetRandomColorPair();
        if (App.settingsManager.isAnimationEnabled.value) {
            rotationTarget = -360f // Запускаем вращение
        } else {
            _oldJokeText.value = _jokeText.value
        }
    }

    fun onShare(context: Context, controller: CaptureController, scope: CoroutineScope) {
        if (!_isLoading.value && _jokeText.value.isNotBlank()) {
            // Берем настройки напрямую из менеджера
            if (App.settingsManager.isClickSoundEnabled.value) {
                SoundManager.playSound(R.raw.svist_fit_ha)
            }

            val title = "Вы хочете шуток? Их есть у меня:\n" +
                    "https://play.google.com/store/apps/details?id=fr.anekdot"

            if (App.settingsManager.isColorStyleEnabled.value) {
                scope.launch {
                    withFrameNanos { } // Ожидание завершения фаз Measure/Layout/Draw
                    val bitmap = controller.captureAsync().await()
                    Util.SaveAndShareImage(context, bitmap.asAndroidBitmap(), title)
                }
            } else {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "${_jokeText.value}\n\n$title")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
            }
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
            SoundManager.playSystemClick()
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
    FloatingActionButton(onClick, Modifier.size((iconSize * 1.2).dp), CircleShape, containerColor, contentColor, elevation) {
        Icon(imageVector, contentDescription, Modifier.size(iconSize.dp))
    }
}

@Composable
fun MainTextColored(
    viewModel: MainViewModel,
    dynamicFontSize: Double
) {
    val baseFontSize = App.baseFontSize
    Card(
        Modifier.graphicsLayer {
                // Вращаем по оси Y
                this.rotationY = viewModel.rotationAngle
                // Добавляем перспективу (чтобы один край казался ближе другого)
                cameraDistance = 12f * density
            },
        RoundedCornerShape((baseFontSize * 1.6).dp),
        CardDefaults.cardColors(Color.White.copy(alpha = .9f)),
        CardDefaults.cardElevation((baseFontSize * .6f).dp)
    ) {
        Text(
            text = if (viewModel.rotationAngle > -180f) viewModel.oldJokeText.collectAsState().value else viewModel.jokeText.collectAsState().value,
            fontFamily = ComfortaaFontFamily,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.Black,
                fontSize = dynamicFontSize.toSp(),
                // Добавим межстрочный интервал для удобства чтения
                lineHeight = (dynamicFontSize * 1.4).toSp()
            ),
            modifier = Modifier.padding((baseFontSize * 1.4).dp)
                .graphicsLayer { alpha = if (viewModel.rotationAngle < -90 && viewModel.rotationAngle > -270) 0f else 1f}
        )
    }
}

@Composable
fun MainTextClassic(
    viewModel: MainViewModel,
    dynamicFontSize: Double
) {
    Text(
        text = viewModel.jokeText.collectAsState().value,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = dynamicFontSize.toSp(),
            // Добавим межстрочный интервал для удобства чтения
            lineHeight = (dynamicFontSize * 1.4).toSp()
        )
    )
}

@Composable
fun MainTextCapture(
    viewModel: MainViewModel
) {
    val baseFontSize = App.baseFontSize
    Card(
        Modifier.padding(baseFontSize.dp),
        RoundedCornerShape((baseFontSize * 1.6).dp),
        CardDefaults.cardColors(Color.White.copy(alpha = .9f)),
        CardDefaults.cardElevation((baseFontSize * .6f).dp)
    ) {
        Text(
            viewModel.jokeText.collectAsState().value,
            Modifier.padding((baseFontSize * 1.4).dp),
            fontFamily = ComfortaaFontFamily,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.Black,
                fontSize = baseFontSize.toSp(),
                // Добавим межстрочный интервал для удобства чтения
                lineHeight = (baseFontSize * 1.4).toSp()
            )
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun MainScreen(
    modifier: Modifier,
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel
) {
    val isColored by App.settingsManager.isColorStyleEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val text by viewModel.jokeText.collectAsState()
    val gColors by viewModel.gradientColors.collectAsState()
    val (startColor, endColor) = gColors
    val captureController = rememberCaptureController()
    val captureScope = rememberCoroutineScope()
    val baseFontSize = App.baseFontSize
    val context = LocalContext.current
    val view = LocalView.current

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { message ->
            snackbarHostState.showSnackbar(message, "OK")
        }
    }

    LaunchedEffect(text) { // Если пришел анекдот и повезло (шанс 1 к 7)
        if (settingsViewModel.isLaughSoundEnabled.value && text.length > 50 && (1..3).random() == 1) {
            kotlinx.coroutines.delay((2000..5000).random().toLong())
            if (settingsViewModel.isLaughSoundEnabled.value) {
                SoundManager.playSound(Util.GetRandomLaugh())
            }
        }
    }

    val animatableAngle = remember { Animatable(0f) }

    LaunchedEffect(viewModel.rotationTarget) {
        if (viewModel.rotationTarget != 0f) {
            if (settingsViewModel.isAnimationEnabled.value) { // Запускаем анимацию до -360
                animatableAngle.animateTo(viewModel.rotationTarget, tween(800, 0, FastOutSlowInEasing))
            }
            // Как только докрутили — мгновенно прыгаем в 0 (без анимации!)
            animatableAngle.snapTo(0f)
            // И обнуляем цель во ViewModel, чтобы быть готовыми к следующему разу
            viewModel.resetRotation()
        }
    }

    Box(modifier // --- СКРЫТЫЙ СЛОЙ (для захвата) ---
        .alpha(0f) // Делаем невидимым
        .capturable(captureController)
        .fillMaxWidth()
        .wrapContentHeight()
        .pointerInput(Unit) {}
        .background(Brush.verticalGradient(listOf(startColor, endColor)))
    ) {
        MainTextCapture(viewModel)
    }

    // Обновляем текущий угол во ViewModel, чтобы MainText его видел
    viewModel.rotationAngle = animatableAngle.value

    val boxModifier = if (isColored) modifier.background(Brush.verticalGradient(listOf(startColor, endColor))) else modifier
    Box(boxModifier.fillMaxSize()) {
        // Основной контент теперь занимает всё место, центрируясь
        Column(Modifier
            .fillMaxSize()
            .padding(horizontal = baseFontSize.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = baseFontSize.dp, bottom = (baseFontSize * 6).dp),
            Arrangement.Center,
            Alignment.CenterHorizontally
        ) {
            // Читаем значение (от 1 до 5)
            val relativeFontSize by settingsViewModel.relativeFontSize.collectAsState()
            val dynamicFontSize = baseFontSize * 0.2 * (2 + relativeFontSize) // от 0.6 до 1.4
            if (isColored) {
                MainTextColored(viewModel, dynamicFontSize)
            } else {
                MainTextClassic(viewModel, dynamicFontSize)
            }
        }

        if (isLoading) { // Большая крутилка на фоне текста
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val color = if (isColored) startColor else ProgressIndicatorDefaults.circularColor
                CircularProgressIndicator(Modifier.size((baseFontSize * 12).dp), color, baseFontSize.dp)
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
                MainButton(viewModel, Icons.Default.Share, "Поделиться", baseFontSize * 2) { viewModel.onShare(context, captureController, captureScope) }
                MainButton(viewModel, Icons.Default.Refresh, "Следующий", baseFontSize * 4) { viewModel.onNext(context, view) }
                MainButton(viewModel, Icons.Default.Settings, "Настройки", baseFontSize * 2) { viewModel.onSettings(settingsViewModel) }
            }

            // Если идет загрузка, перекрываем кнопки невидимым кликабельным слоем
            if (isLoading) { // поглощаем клики, ничего не делая
                Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = baseFontSize.dp) // Поднимаем его ЧУТЬ ВЫШЕ кнопок
        )
    }
}
