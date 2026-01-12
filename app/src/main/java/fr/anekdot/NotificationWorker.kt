package fr.anekdot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
            // 1. Идем за анекдотом (используем твой RetrofitInstance)
            val response = RetrofitInstance.api.getRandomJoke(500)
            val jokeText = response.p.text

            // 2. Показываем уведомление
            showNotification(jokeText)

            Result.success()
        } catch (e: Exception) {
            Result.retry() // Если нет интернета, попробуем позже
        }
    }

    private fun showNotification(text: String) {
        val channelId = "anekdot_daily"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Создаем канал (нужно для Android 8.0+)
        val channel = NotificationChannel(
            channelId, "Анекдот дня",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

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

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_send) // Потом заменишь на свою иконку
            .setContentTitle("Анекдот дня приехал!")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)) // Чтобы текст не обрезался
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
