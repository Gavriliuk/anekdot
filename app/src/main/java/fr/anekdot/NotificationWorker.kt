package fr.anekdot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationWorker(
    val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.api.getRandomJoke()
            val jokeText = response.p.text

            showNotification(jokeText)

            Result.success()
        } catch (e: Exception) {
            Result.retry() // Если нет интернета, попробуем позже
        }
    }

    private fun showNotification(text: String) {
        val channelId = "anekdot_daily"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Проверяем версию перед созданием канала
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Создаем канал (нужно для Android 8.0+)
            val channel = NotificationChannel(
                channelId, "Анекдот дня",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Что будет при клике на уведомление (открываем MainActivity)
        val intent = Intent(context, MainActivity::class.java).apply {
            // Кладём текст под ключом "joke_from_notification"
            putExtra("joke_from_notification", text)
            // Эти флаги важны, чтобы приложение не открывало новую копию, а использовало текущую
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        var limitedText = text.lines().take(2).joinToString("\n") // Берем первые 5 строк
        if (limitedText != text) {
            limitedText += "\n... (читать полностью)" // Добавляем многоточие, если текст был обрезан
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_logo) // Используем новый векторный силуэт
            .setLargeIcon(BitmapFactory.decodeResource(applicationContext.resources, R.mipmap.ic_launcher)) // Цветная иконка приложения
            .setContentTitle("Анекдот дня приехал!")
            // Текст в свернутом виде (всегда 1-2 строки)
            .setContentText(limitedText)
            // Текст в развернутом виде (ограничен 2 строками исходного текста)
            .setStyle(NotificationCompat.BigTextStyle().bigText(limitedText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
