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
import hime.suzuki.core.AppLocale

object Notifications {
    const val CHANNEL_PROGRESS = "downloads"
    const val CHANNEL_EVENTS = "events"
    const val PROGRESS_ID = 1001
    const val EVENTS_ID = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val ctx = AppLocale.wrap(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val progress = NotificationChannel(
            CHANNEL_PROGRESS,
            ctx.getString(R.string.notif_channel_downloads),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = ctx.getString(R.string.notif_channel_downloads_desc)
            setShowBadge(false)
        }
        val events = NotificationChannel(
            CHANNEL_EVENTS,
            ctx.getString(R.string.notif_channel_events),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = ctx.getString(R.string.notif_channel_events_desc)
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
        val ctx = AppLocale.wrap(context)
        val text = when {
            paused -> ctx.getString(R.string.notif_paused, queued)
            running > 0 && queued > 0 -> ctx.getString(R.string.notif_running_queue, running, queued)
            running > 0 -> ctx.getString(R.string.notif_running, running)
            else -> ctx.getString(R.string.notif_queued, queued)
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
            .addAction(
                0,
                if (paused) ctx.getString(R.string.notif_resume) else ctx.getString(R.string.notif_pause),
                toggleIntent,
            )
            .addAction(0, ctx.getString(R.string.notif_cancel), cancelIntent)
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
        val ctx = AppLocale.wrap(context)
        val done = (summary.total - summary.failed - summary.canceled).coerceAtLeast(0)
        val parts = buildList {
            if (done > 0) add(ctx.getString(R.string.notif_batch_done, done))
            if (summary.failed > 0) add(ctx.getString(R.string.notif_batch_failed, summary.failed))
            if (summary.canceled > 0) {
                add(ctx.getString(R.string.notif_batch_canceled, summary.canceled))
            }
        }
        val text = parts.joinToString(", ")
            .ifEmpty { ctx.getString(R.string.notif_batch_finished, summary.total) }
        val title = if (summary.failed == 0 && summary.canceled == 0) {
            ctx.getString(R.string.notif_complete_title)
        } else {
            ctx.getString(R.string.notif_finished_title)
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
