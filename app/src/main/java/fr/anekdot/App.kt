package fr.anekdot

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class App : Application() {
    companion object {
        private lateinit var instance: App
        // Универсальный способ получить контекст из любой точки приложения
        fun getContext(): Context = instance.applicationContext

        // Глобальная навигация
        var currentScreen by mutableStateOf("main")
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
