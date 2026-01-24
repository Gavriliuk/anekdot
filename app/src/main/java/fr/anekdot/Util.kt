package fr.anekdot

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

class Util {
    companion object {
        // Пары цветов для градиента (Start Color, End Color)
        private val gradientPresets = listOf(
            Color(0xFFCFD9DF) to Color(0xFFE2E2E2), // Светло-серый жемчуг
            Color(0xFFFF9A9E) to Color(0xFFFAD0C4), // Нежно-розовый
            Color(0xFFA18CD1) to Color(0xFFFBC2EB), // Сиреневый
            Color(0xFF84FAB0) to Color(0xFF8FD3F4), // Мятно-голубой
            Color(0xFFF6D365) to Color(0xFFFDA085), // Солнечно-оранжевый
            Color(0xFFA8E063) to Color(0xFF56AB2F)  // Сочное яблоко
        )

        fun GetFirstColorPair(): Pair<Color, Color> = gradientPresets[0]

        fun GetRandomColorPair(): Pair<Color, Color> = gradientPresets[(gradientPresets.indices).random()]
    }
}

@Composable
fun Float.toSp(): androidx.compose.ui.unit.TextUnit {
    // Получаем текущий масштаб шрифта пользователя (например, 1.0, 1.1, 1.3)
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale

    // Мы делим на масштаб. Если система умножает шрифт на 1.5,
    // мы делим его на 1.5 перед подачей. В итоге получаем исходный размер.
    return (this / fontScale).sp
}

@Composable
fun Double.toSp(): androidx.compose.ui.unit.TextUnit {
    // Получаем текущий масштаб шрифта пользователя (например, 1.0, 1.1, 1.3)
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale

    // Мы делим на масштаб. Если система умножает шрифт на 1.5,
    // мы делим его на 1.5 перед подачей. В итоге получаем исходный размер.
    return (this / fontScale).sp
}
