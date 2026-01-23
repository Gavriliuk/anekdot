import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.anekdot.ui.theme.AnekdotTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {
    // Превращаем Flow из SettingsManager в StateFlow для Compose
    val relativeFontSize = settingsManager.relativeFontSize
        .map { it.coerceIn(1, 5) } // Вот она, железная защита
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val isLaughSoundEnabled = settingsManager.isLaughSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isClickSoundEnabled = settingsManager.isClickSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Методы для изменения настроек
    fun updateFontSize(level: Int) {
        viewModelScope.launch { settingsManager.saveFontSize(level) }
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
    steps: Int = 0,
    fontSize: Float,
    colorActive: Color,
    colorInactive: Color
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        thumb = {
            val thumbSize = (fontSize * 1.5).dp // Делаем его в 1.5 раза больше шрифта
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .background(
                        color = colorActive,
                        shape = CircleShape
                    )
            )
        },
        // Делаем хендл выразительным
        colors = SliderDefaults.colors(
            activeTrackColor = colorActive, // Яркая линия слева
            inactiveTrackColor = colorInactive // Бледная справа
        )
    )
}

@Composable
fun SettingRow(
    label: String,
    checked: Boolean,
    fontSize: Float,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = (fontSize * 0.4).dp), // Отступы зависят от размера шрифта
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = fontSize.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    viewModel: SettingsViewModel,
    dynamicFontSize: Float, // Коэффициент из MainActivity
    onSendNotification: () -> Unit,
    onBack: () -> Unit      // Действие при выходе из настроек
) {
    // Собираем значения из ViewModel
    val relativeFontSize by viewModel.relativeFontSize.collectAsState()
    val isLaughSoundEnabled by viewModel.isLaughSoundEnabled.collectAsState()
    val isClickSoundEnabled by viewModel.isClickSoundEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontSize = (dynamicFontSize * 1.2).sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size((dynamicFontSize * 1.5).dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent // Оставляем фон чистым
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = (dynamicFontSize * 1.5).dp)
                .padding(horizontal = (dynamicFontSize * 1.5).dp),
            verticalArrangement = Arrangement.spacedBy((dynamicFontSize * 1.5).dp)
        ) {
            // Секция размера шрифта
            Column {
                Text(
                    "Размер шрифта: $relativeFontSize",
                    fontSize = dynamicFontSize.sp,
                    modifier = Modifier.padding(bottom = (dynamicFontSize * 0.2).dp)
                )
                SettingSlider(
                    value = relativeFontSize.toFloat(),
                    onValueChange = { viewModel.updateFontSize(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    fontSize = dynamicFontSize,
                    colorActive = MaterialTheme.colorScheme.primary,
                    colorInactive = MaterialTheme.colorScheme.primaryContainer
                )
            }

            // Блок звуков
            SettingRow("Звук хохота", isLaughSoundEnabled, dynamicFontSize) {
                viewModel.setLaughSoundEnabled(it)
            }

            SettingRow("Звук кнопок", isClickSoundEnabled, dynamicFontSize) {
                viewModel.setClickSoundEnabled(it)
            }

            // Кнопка отправки уведомления
            Button(
                onClick = onSendNotification,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Анекдот дня", fontSize = dynamicFontSize.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    // 1. Создаем временный менеджер настроек (используем LocalContext)
    val context = androidx.compose.ui.platform.LocalContext.current
    // 2. Создаем вью-модель специально для превью
    // Используем remember, чтобы модель не пересоздавалась постоянно
    val viewModel = remember { SettingsViewModel(SettingsManager(context)) }

    AnekdotTheme {
        SettingsScreen(
            viewModel = viewModel,
            dynamicFontSize = 20f,   // Просто число для примера, как на планшете
            onSendNotification = {}, // Пустая заглушка
            onBack = {}              // Пустая заглушка
        )
    }
}
