package hime.suzuki.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import hime.suzuki.MainActivity
import hime.suzuki.R

object Notifications {
    const val CHANNEL_PROGRESS = "downloads"
    const val CHANNEL_EVENTS = "events"
    const val PROGRESS_ID = 1001
    const val EVENTS_ID = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val progress = NotificationChannel(
            CHANNEL_PROGRESS,
            "İndirmeler",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Devam eden müzik indirmeleri"
            setShowBadge(false)
        }
        val events = NotificationChannel(
            CHANNEL_EVENTS,
            "Bildirimler",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Tamamlanan indirmeler ve bilgilendirmeler"
        }
        manager.createNotificationChannels(listOf(progress, events))
    }

    fun progress(
        context: Context,
        progress: Float,
        running: Int,
        queued: Int,
        paused: Boolean,
    ): Notification {
        val text = when {
            paused -> "Duraklatıldı • $queued sırada"
            running > 0 && queued > 0 -> "$running indiriliyor • $queued sırada"
            running > 0 -> "$running müzik indiriliyor"
            else -> "$queued müzik sırada"
        }
        val toggleIntent = PendingIntent.getService(
            context,
            11,
            Intent(context, DownloadService::class.java).setAction(DownloadService.ACTION_TOGGLE_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = PendingIntent.getService(
            context,
            12,
            Intent(context, DownloadService::class.java).setAction(DownloadService.ACTION_CANCEL_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle("Suzuki")
            .setContentText(text)
            .setProgress(100, (progress.coerceIn(0f, 1f) * 100f).toInt(), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent(context))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, if (paused) "Devam" else "Duraklat", toggleIntent)
            .addAction(0, "İptal", cancelIntent)
            .build()
    }

    fun update(
        context: Context,
        progress: Float,
        running: Int,
        queued: Int,
        paused: Boolean,
    ) {
        runCatching {
            NotificationManagerCompat.from(context)
                .notify(PROGRESS_ID, progress(context, progress, running, queued, paused))
        }
    }

    fun batchDone(context: Context, summary: BatchSummary) {
        val done = (summary.total - summary.failed - summary.canceled).coerceAtLeast(0)
        val parts = buildList {
            if (done > 0) add("$done tamamlandı")
            if (summary.failed > 0) add("${summary.failed} başarısız")
            if (summary.canceled > 0) add("${summary.canceled} iptal")
        }
        val text = parts.joinToString(", ").ifEmpty { "${summary.total} işlem bitti" }
        val title = if (summary.failed == 0 && summary.canceled == 0) {
            "İndirme tamamlandı"
        } else {
            "İşlem tamamlandı"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openIntent(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(EVENTS_ID, notification) }
    }

    private fun openIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
