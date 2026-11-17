package hime.suzuki.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import hime.suzuki.SuzukiApp
import hime.suzuki.data.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null
    private lateinit var engine: DownloadEngine

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        engine = (application as SuzukiApp).container.engine

        val notification = Notifications.progress(this, 0f, 0, 0, engine.paused.value)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                Notifications.PROGRESS_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(Notifications.PROGRESS_ID, notification)
        }
        observe()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PAUSE -> engine.togglePause()
            ACTION_CANCEL_ALL -> engine.cancelAll()
        }
        if (observer?.isActive != true) observe()
        return START_NOT_STICKY
    }

    private fun observe() {
        observer = scope.launch {
            var lastUpdate = 0L
            engine.activeItems.collect { items ->
                if (items.isEmpty()) {
                    val summary = engine.consumeBatch()
                    if (summary != null) Notifications.batchDone(this@DownloadService, summary)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collect
                }
                val now = SystemClock.elapsedRealtime()
                if (now - lastUpdate >= 700L) {
                    lastUpdate = now
                    val running = items.count { it.status == DownloadStatus.RUNNING }
                    val queued = items.count { it.status == DownloadStatus.QUEUED }
                    val average = items.sumOf { it.progress.toDouble() } / items.size
                    Notifications.update(
                        this@DownloadService,
                        average.toFloat(),
                        running,
                        queued,
                        engine.paused.value
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_TOGGLE_PAUSE = "hime.suzuki.action.TOGGLE_PAUSE"
        const val ACTION_CANCEL_ALL = "hime.suzuki.action.CANCEL_ALL"

        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }
    }
}
