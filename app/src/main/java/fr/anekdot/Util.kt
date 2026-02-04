package fr.anekdot

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        fun getFirstColorPair(): Pair<Color, Color> = gradientPresets[0]

        fun getRandomColorPair(): Pair<Color, Color> = gradientPresets[(gradientPresets.indices).random()]

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

        fun getRandomLaugh() = laughterResources.random()

        suspend fun saveAndShareImage(context: Context, bitmap: Bitmap, title: String, errorEvent: MutableSharedFlow<String>
        ) {
            try {
                // 1. Создаем папку в кэше
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()

                // 2. Создаем сам файл
                val ts = SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File(cachePath, "anekdot_$ts.png")
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                // 3. Получаем безопасный URI через FileProvider
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

                // 4. Создаем Intent для шаринга
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Даем права на чтение
                }

                context.startActivity(Intent.createChooser(intent, "Поделиться анекдотом"))
            } catch (e: IOException) {
                if (BuildConfig.DEBUG) {
                    Log.e("SaveAndShareImage", "Ошибка при сохранении картинки", e)
                }
                // Вызываем напрямую, так как мы уже в suspend функции
                errorEvent.emit("Ошибка при сохранении картинки")
            }
        }
    }
}

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null

    fun playSystemClick() {
        val audioManager = App.getContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)
    }

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
