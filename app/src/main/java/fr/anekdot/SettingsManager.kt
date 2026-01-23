import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Создаем расширение для контекста, чтобы DataStore был доступен везде
val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    // Ключи, по которым будем сохранять данные
    companion object {
        val RELATIVE_FONT_SIZE = intPreferencesKey("relative_font_size")
        val IS_LAUGH_SOUND_ENABLED = booleanPreferencesKey("is_laugh_sound_enabled")
        val IS_CLICK_SOUND_ENABLED = booleanPreferencesKey("is_click_sound_enabled")
    }

    // Читаем настройки (Flow позволяет интерфейсу обновляться мгновенно)
    val relativeFontSize: Flow<Int> = context.dataStore.data.map { pref ->
        pref[RELATIVE_FONT_SIZE] ?: 3
    }

    val isLaughSoundEnabled: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[IS_LAUGH_SOUND_ENABLED] ?: true
    }

    val isClickSoundEnabled: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[IS_CLICK_SOUND_ENABLED] ?: true
    }

    // Методы для сохранения (записи)
    suspend fun saveFontSize(size: Int) {
        context.dataStore.edit { pref -> pref[RELATIVE_FONT_SIZE] = size }
    }

    suspend fun saveLaughSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { pref -> pref[IS_LAUGH_SOUND_ENABLED] = enabled }
    }

    suspend fun saveClickSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { pref -> pref[IS_CLICK_SOUND_ENABLED] = enabled }
    }
}
