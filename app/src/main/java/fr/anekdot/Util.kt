package fr.anekdot

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

        // Source: https://zvukogram.com/
        private val laughterResources = listOf(
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

        fun GetRandomLaugh() = laughterResources.random()
    }
}

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null

    fun playSound(resId: Int) {
        mediaPlayer?.release() // Освобождаем ресурсы предыдущего звука
        mediaPlayer = MediaPlayer.create(App.getContext(), resId)
        mediaPlayer?.start()
    }
}

val ComfortaaFontFamily = FontFamily(
    Font(R.font.comfortaa_regular, FontWeight.Normal)
)

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
