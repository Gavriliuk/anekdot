import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.anekdot.App
import fr.anekdot.Util
import fr.anekdot.toSp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    // ViewModel сама знает, где лежат настройки
    private val settingsManager = App.settingsManager

    private val _gradientColors = MutableStateFlow(Util.GetFirstColorPair())
    val gradientColors = _gradientColors.asStateFlow()

    fun chooseRandomColors() {
        // Выбираем новый случайный индекс градиента
        _gradientColors.value = Util.GetRandomColorPair();
    }

    // Превращаем Flow из SettingsManager в StateFlow для Compose
    val relativeFontSize = settingsManager.relativeFontSize
        .map { it.coerceIn(1, 5) } // Вот она, железная защита
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val isColorStyleEnabled = settingsManager.isColorStyleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isLaughSoundEnabled = settingsManager.isLaughSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isClickSoundEnabled = settingsManager.isClickSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Методы для изменения настроек
    fun updateFontSize(level: Int) {
        viewModelScope.launch { settingsManager.saveFontSize(level) }
    }

    fun setColorStyleEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.saveColorStyleEnabled(enabled) }
    }

    fun setLaughSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.saveLaughSoundEnabled(enabled) }
    }

    fun setClickSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.saveClickSoundEnabled(enabled) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    viewModel: SettingsViewModel,
    baseFontSizeDp: Float
) {
    val isColorStyleEnabled by viewModel.isColorStyleEnabled.collectAsState()
    val gColors by viewModel.gradientColors.collectAsState()
    val sliderColors = if (isColorStyleEnabled) SliderDefaults.colors(
        Color.Black,
        gColors.second,
        Color.Black,
        gColors.first,
        Color.Black
    ) else SliderDefaults.colors()
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.requiredHeight(baseFontSizeDp.dp),
        valueRange = valueRange,
        steps = steps,
        thumb = {
            Box(Modifier.size(baseFontSizeDp.dp).background(sliderColors.thumbColor, CircleShape))
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState,
                Modifier.height((baseFontSizeDp * 0.4).dp),
                colors = sliderColors
            )
        },
        colors = sliderColors
    )
}

@Composable
fun SettingRow(
    label: String,
    checked: Boolean,
    viewModel: SettingsViewModel,
    baseFontSizeDp: Float,
    onCheckedChange: (Boolean) -> Unit
) {
    val isColorStyleEnabled by viewModel.isColorStyleEnabled.collectAsState()
    val textColor = if (isColorStyleEnabled) Color.Black else MaterialTheme.colorScheme.onSurface
    val gColors by viewModel.gradientColors.collectAsState()
    val (startColor, endColor) = gColors
    val switchColors = if (isColorStyleEnabled) SwitchDefaults.colors(
        checkedThumbColor = Color.Black,
        uncheckedThumbColor = Color.Black,
        checkedTrackColor = endColor,
        uncheckedTrackColor = startColor,
        checkedBorderColor = Color.Black,
        uncheckedBorderColor = Color.Black
    ) else SwitchDefaults.colors()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = (baseFontSizeDp * 0.4).dp), // Отступы зависят от размера шрифта
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = baseFontSizeDp.toSp(),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked,
            onCheckedChange,
            Modifier
                .scale(baseFontSizeDp / 32)
                .requiredHeight(baseFontSizeDp.dp)
                .requiredWidth((baseFontSizeDp * 1.6).dp),
            colors = switchColors
        )
    }
}

@Composable
fun SettingsContentGroup(
    viewModel: SettingsViewModel,
    baseFontSizeDp: Float,
    content: @Composable () -> Unit
) {
    val isColorStyleEnabled by viewModel.isColorStyleEnabled.collectAsState()
    if (isColorStyleEnabled) {
        Card(
            shape = RoundedCornerShape((baseFontSizeDp * 1.6).dp),
            colors = CardDefaults.cardColors(
                // Эффект матового стекла (90% прозрачности)
                containerColor = Color.White.copy(alpha = 0.92f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = (baseFontSizeDp * .6).dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = (baseFontSizeDp * .6).dp)) {
                content()
            }
        }
    } else {
        content()
    }
}

@Composable
fun SettingsContent(
    modifier: Modifier, // Сюда придет Modifier.padding(innerPadding)
    viewModel: SettingsViewModel,
    baseFontSizeDp: Float,
    onSendNotification: () -> Unit
) {
    val isColorStyleEnabled by viewModel.isColorStyleEnabled.collectAsState()
    val textColor = if (isColorStyleEnabled) Color.Black else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = (baseFontSizeDp * .4).dp)
            .padding(horizontal = (baseFontSizeDp * (if (isColorStyleEnabled) .9 else 1.5)).dp),
        verticalArrangement = Arrangement.spacedBy((baseFontSizeDp * 1.5).dp)
    ) {
        // Собираем значения из ViewModel
        val relativeFontSize by viewModel.relativeFontSize.collectAsState()
        val isColorStyleEnabled by viewModel.isColorStyleEnabled.collectAsState()
        val isLaughSoundEnabled by viewModel.isLaughSoundEnabled.collectAsState()
        val isClickSoundEnabled by viewModel.isClickSoundEnabled.collectAsState()
        val gColors by viewModel.gradientColors.collectAsState()
        val (startColor, endColor) = gColors
        val buttonColors = if (isColorStyleEnabled) ButtonDefaults.buttonColors(
            containerColor = startColor,
            contentColor = Color.Black
        ) else ButtonDefaults.buttonColors()

        // Секция размера шрифта
        SettingsContentGroup(viewModel, baseFontSizeDp) {
            Column(modifier = Modifier.padding(vertical = (baseFontSizeDp * 0.4).dp), ) {
                Text(
                    "Размер шрифта: $relativeFontSize",
                    color = textColor,
                    fontSize = baseFontSizeDp.toSp(),
                    modifier = Modifier.padding(bottom = (baseFontSizeDp * 0.2).dp)
                )
                SettingSlider(
                    relativeFontSize.toFloat(),
                    { viewModel.updateFontSize(it.toInt()) },
                    1f..5f,
                    3,
                    viewModel,
                    baseFontSizeDp
                )
            }
        }

        // Блок разметки
        SettingsContentGroup(viewModel, baseFontSizeDp) {
            SettingRow("Цветной фон", isColorStyleEnabled, viewModel, baseFontSizeDp) {
                if (it) viewModel.chooseRandomColors()
                viewModel.setColorStyleEnabled(it)
            }
        }

        // Блок звуков
        SettingsContentGroup(viewModel, baseFontSizeDp) {
            SettingRow("Звук хохота", isLaughSoundEnabled, viewModel, baseFontSizeDp) {
                viewModel.setLaughSoundEnabled(it)
            }
        }

        SettingsContentGroup(viewModel, baseFontSizeDp) {
            SettingRow("Звук кнопок", isClickSoundEnabled, viewModel, baseFontSizeDp) {
                viewModel.setClickSoundEnabled(it)
            }
        }

        // Кнопка отправки уведомления
        SettingsContentGroup(viewModel, baseFontSizeDp) {
            Button(
                onClick = onSendNotification,
                colors = buttonColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = (baseFontSizeDp * .6).dp)
                    .requiredHeight((baseFontSizeDp * 2).dp)
            ) {
                Text("Анекдот дня", fontSize = baseFontSizeDp.toSp())
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsTopAppBar(
    viewModel: SettingsViewModel,
    baseFontSizeDp: Float,
    onBack: () -> Unit
) {
    val isColorStyleEnabled by viewModel.isColorStyleEnabled.collectAsState()
    val textColor = if (isColorStyleEnabled) Color.Black else MaterialTheme.colorScheme.onSurface
    TopAppBar(
        title = { Text("Настройки", color = textColor, fontSize = (baseFontSizeDp * 1.2).toSp()) },
        modifier = Modifier.requiredHeight((baseFontSizeDp * 3).dp),
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.size((baseFontSizeDp * 2.4).dp)) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    "Назад",
                    Modifier.size((baseFontSizeDp * 1.8).dp),
                    textColor)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent // Оставляем фон чистым
        )
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreenClassic(
    viewModel: SettingsViewModel,
    baseFontSizeDp: Float,
    onSendNotification: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { SettingsTopAppBar(viewModel, baseFontSizeDp, onBack) }
    ) { innerPadding ->
        SettingsContent(Modifier.padding(innerPadding), viewModel, baseFontSizeDp, onSendNotification)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreenColored(
    viewModel: SettingsViewModel,
    baseFontSizeDp: Float,
    onSendNotification: () -> Unit,
    onBack: () -> Unit
) {
    val gColors by viewModel.gradientColors.collectAsState()
    val (startColor, endColor) = gColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(startColor, endColor)))
    ) {
        Scaffold(
            topBar = { SettingsTopAppBar(viewModel, baseFontSizeDp, onBack) },
            containerColor = Color.Transparent // Прозрачный фон, чтобы видеть градиент
        ) { innerPadding ->
            SettingsContent(Modifier.padding(innerPadding), viewModel, baseFontSizeDp, onSendNotification)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    viewModel: SettingsViewModel,
    baseFontSizeDp: Float, // Коэффициент из MainActivity
    onSendNotification: () -> Unit,
    onBack: () -> Unit      // Действие при выходе из настроек
) {
    // Подписываемся на настройку стиля
    val isColorStyleEnabled by viewModel.isColorStyleEnabled.collectAsState()

    // Мгновенный выбор разметки при изменении чекбокса
    if (isColorStyleEnabled) {
        SettingsScreenColored(viewModel, baseFontSizeDp, onSendNotification, onBack)
    } else {
        SettingsScreenClassic(viewModel, baseFontSizeDp, onSendNotification, onBack)
    }
}
