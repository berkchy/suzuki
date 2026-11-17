package hime.suzuki.download

import android.content.Context
import hime.suzuki.R
import hime.suzuki.core.AppLocale
import hime.suzuki.core.Fmt
import hime.suzuki.core.Storage
import hime.suzuki.data.AppSettings
import hime.suzuki.data.DownloadDao
import hime.suzuki.data.DownloadItem
import hime.suzuki.data.DownloadStatus
import hime.suzuki.data.SettingsRepository
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class AudioOptions(
    val format: String = "mp3",
    val bitrate: Int = 192,
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val range: String = "",
)

data class BatchSummary(val total: Int, val failed: Int, val canceled: Int)

class DownloadEngine(
    private val context: Context,
    private val dao: DownloadDao,
    private val settingsRepo: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pumpMutex = Mutex()
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val canceledIds = Collections.newSetFromMap(ConcurrentHashMap<Long, Boolean>())
    private val batchTotal = AtomicInteger(0)
    private val batchFailed = AtomicInteger(0)
    private val batchCanceled = AtomicInteger(0)

    val paused = MutableStateFlow(false)
    val activeItems: Flow<List<DownloadItem>> = dao.observeActive()
    val finishedItems: Flow<List<DownloadItem>> = dao.observeFinished()
    val recentItems: Flow<List<DownloadItem>> = dao.observeRecent()

    suspend fun enqueue(preview: PlaylistInfo, options: AudioOptions, playlistTitle: String? = null) {
        val selected = applyRange(preview.entries, options.range)
        if (selected.isEmpty()) return

        val now = System.currentTimeMillis()
        val items = selected.mapIndexed { index, entry ->
            DownloadItem(
                videoId = entry.videoId,
                url = entry.url,
                title = entry.title,
                uploader = entry.uploader,
                playlistTitle = playlistTitle ?: preview.title,
                format = options.format,
                bitrate = options.bitrate,
                embedThumbnail = options.embedThumbnail,
                embedMetadata = options.embedMetadata,
                createdAt = now + index,
            )
        }
        dao.insertAll(items)
        batchTotal.addAndGet(items.size)
        DownloadService.start(context)
        pump()
    }

    fun pump() {
        scope.launch {
            pumpMutex.withLock {
                if (paused.value) return@withLock
                val limit = settingsRepo.snapshot().concurrency.coerceIn(1, 4)
                var running = dao.runningCount()
                while (!paused.value && running < limit) {
                    val next = dao.nextQueued() ?: break
                    if (dao.tryStart(next.id) != 1) break
                    launchItem(next)
                    running++
                }
            }
        }
    }

    private fun launchItem(item: DownloadItem) {
        val job = scope.launch {
            try {
                val config: AppSettings = settingsRepo.snapshot()
                val dir = Storage.resolveDir(context, config.downloadDir)
                if (!Storage.ensure(dir)) {
                    throw IOException(
                        AppLocale.wrap(context).getString(R.string.error_folder_not_writable)
                    )
                }

                var lastDbWrite = 0L
                val result = YtdlpEngine.download(
                    context = context,
                    itemId = item.id,
                    request = DownloadRequest(
                        url = item.url,
                        format = item.format,
                        bitrate = item.bitrate,
                        embedThumbnail = item.embedThumbnail,
                        embedMetadata = item.embedMetadata,
                    ),
                    settings = config,
                    dir = dir,
                ) { progress, eta ->
                    val now = System.currentTimeMillis()
                    if (now - lastDbWrite >= 350L) {
                        lastDbWrite = now
                        scope.launch { dao.setProgress(item.id, progress, eta) }
                    }
                }

                val path = result.filePath
                val size = path?.let { File(it).length() } ?: 0L
                dao.setDone(item.id, path, size, System.currentTimeMillis())
                if (path != null) Storage.scan(context, path)
                canceledIds.remove(item.id)
            } catch (t: Throwable) {
                if (canceledIds.remove(item.id)) {
                    batchCanceled.incrementAndGet()
                    dao.setStatus(item.id, DownloadStatus.CANCELED, null, System.currentTimeMillis())
                } else {
                    batchFailed.incrementAndGet()
                    dao.setStatus(
                        item.id,
                        DownloadStatus.FAILED,
                        Fmt.errorMessage(t),
                        System.currentTimeMillis(),
                    )
                }
            } finally {
                jobs.remove(item.id)
                if (!paused.value) pump()
            }
        }
        jobs[item.id] = job
    }

    fun togglePause() {
        paused.value = !paused.value
        if (!paused.value) pump()
    }

    fun cancel(id: Long) {
        val running = jobs.containsKey(id)
        canceledIds.add(id)
        if (running) {
            runCatching { YoutubeDL.getInstance().destroyProcessById("suzuki-$id") }
        } else {
            batchCanceled.incrementAndGet()
        }
        scope.launch {
            dao.setStatus(id, DownloadStatus.CANCELED, null, System.currentTimeMillis())
            if (!running) pump()
        }
    }

    fun cancelAll() {
        scope.launch {
            val now = System.currentTimeMillis()
            jobs.keys.toList().forEach { id ->
                canceledIds.add(id)
                runCatching { YoutubeDL.getInstance().destroyProcessById("suzuki-$id") }
                dao.setStatus(id, DownloadStatus.CANCELED, null, now)
            }
            batchCanceled.addAndGet(dao.cancelQueued(now))
        }
    }

    fun retry(id: Long) {
        scope.launch {
            canceledIds.remove(id)
            dao.requeue(id)
            DownloadService.start(context)
            pump()
        }
    }

    fun retryAllFinished() {
        scope.launch {
            dao.requeueFinished()
            DownloadService.start(context)
            pump()
        }
    }

    fun delete(id: Long) {
        scope.launch {
            if (jobs.containsKey(id)) {
                canceledIds.add(id)
                runCatching { YoutubeDL.getInstance().destroyProcessById("suzuki-$id") }
            }
            dao.delete(id)
        }
    }

    suspend fun clearFinished() = dao.clearFinished()

    suspend fun recoverQueue() {
        dao.resetRunning()
        if (dao.queuedCount() > 0) {
            runCatching {
                DownloadService.start(context)
                pump()
            }
        }
    }

    fun consumeBatch(): BatchSummary? {
        val total = batchTotal.getAndSet(0)
        val failed = batchFailed.getAndSet(0)
        val canceled = batchCanceled.getAndSet(0)
        return if (total > 0) BatchSummary(total, failed, canceled) else null
    }

    private fun applyRange(entries: List<ExtractedEntry>, range: String): List<ExtractedEntry> {
        val raw = range.trim()
        if (raw.isEmpty()) return entries

        val indices = LinkedHashSet<Int>()
        raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
            val bounds = part.split('-', limit = 2)
            if (bounds.size == 2) {
                val start = bounds[0].trim().toIntOrNull()
                val end = bounds[1].trim().toIntOrNull()
                if (start != null && end != null) {
                    for (i in start..end) indices.add(i)
                }
            } else {
                part.toIntOrNull()?.let { indices.add(it) }
            }
        }

        val chosen = indices
            .filter { it in 1..entries.size }
            .sorted()
            .map { entries[it - 1] }

        return chosen.ifEmpty { entries }
    }
}
