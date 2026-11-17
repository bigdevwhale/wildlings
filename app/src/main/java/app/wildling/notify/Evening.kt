package app.wildling.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.wildling.MainActivity
import app.wildling.R
import app.wildling.container
import app.wildling.domain.Lang
import app.wildling.domain.today
import app.wildling.domain.tr
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * The evening nudge: once a day at the chosen hour, if the journal is still empty today, a creature asks
 * how the day went. Runs hourly and speaks only in its hour.
 */
class Evening(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.settings.now()
        if (!s.onboarded || s.evening < 0 || LocalDateTime.now().hour != s.evening) return Result.success()
        val t = today()
        if (c.dao.noteList().any { it.day == t }) return Result.success()
        Lang.ru = applicationContext.resources.configuration.locales[0].language == "ru"
        val h = c.dao.habitList().firstOrNull { !it.archived }
        show(applicationContext, tr("${h?.name ?: "Зверь"} заглядывает в окно", "${h?.name ?: "The creature"} looks in at the window"),
            tr("Как прошёл день? Пара слов в дневник – и зверь успокоится.", "How did the day go? A few words in the journal and the creature settles down."))
        return Result.success()
    }

    companion object {
        private const val CH = "evening"
        fun schedule(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("evening", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<Evening>(1, TimeUnit.HOURS).build())
        }
        fun show(ctx: Context, title: String, text: String) {
            if (android.os.Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH, tr("Вечерний вопрос", "Evening question"), NotificationManager.IMPORTANCE_DEFAULT))
            val pi = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
            val n = NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(pi).setAutoCancel(true).build()
            NotificationManagerCompat.from(ctx).notify(1, n)
        }
    }
}
