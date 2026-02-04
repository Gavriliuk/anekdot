package fr.anekdot

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class App : Application() {
    enum class Screen {
        MAIN,
        SETTINGS
    }

    companion object {
        private lateinit var instance: App
        // Универсальный способ получить контекст из любой точки приложения
        fun getContext(): Context = instance.applicationContext

        val settingsManager by lazy { SettingsManager() }

        val baseFontSize: Float by lazy {
            val metrics = instance.resources.displayMetrics
            val smallestWidthDp = (metrics.widthPixels.coerceAtMost(metrics.heightPixels)) / metrics.density
            smallestWidthDp * 0.05f
        }

        // Глобальная навигация
        var currentScreen by mutableStateOf(Screen.MAIN)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
