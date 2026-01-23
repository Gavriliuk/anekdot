package fr.anekdot

import android.app.Application
import android.content.Context

class App : Application() {
    companion object {
        private var instance: App? = null

        // Универсальный способ получить контекст из любой точки приложения
        fun getContext(): Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
