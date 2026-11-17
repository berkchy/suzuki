package hime.suzuki.download

import android.content.Context
import android.util.Log
import hime.suzuki.data.AppSettings
import hime.suzuki.data.SettingsRepository
import hime.suzuki.core.Fmt
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.Locale

data class ExtractedEntry(
    val videoId: String,
    val url: String,
    val title: String,
    val uploader: String?,
)

data class PlaylistInfo(
    val title: String,
    val entries: List<ExtractedEntry>,
    val isPlaylist: Boolean,
)

data class YtdlpState(
    val ready: Boolean = false,
    val version: String? = null,
    val updating: Boolean = false,
    val lastError: String? = null,
)

data class DownloadRequest(
    val url: String,
    val format: String,
    val bitrate: Int,
    val embedThumbnail: Boolean,
    val embedMetadata: Boolean,
)

data class DownloadResult(val filePath: String?)

object YtdlpEngine {
    private const val TAG = "YtdlpEngine"
    private const val FORMAT_ORIGINAL = "original"
    private val BITRATE_FORMATS = setOf("mp3", "m4a", "opus", "ogg")
    private val AUDIO_EXT = setOf("mp3", "m4a", "opus", "ogg", "flac", "wav", "webm", "aac")

    private val initMutex = Mutex()

    @Volatile
    private var initialized = false

    private val _state = MutableStateFlow(YtdlpState())
    val state: StateFlow<YtdlpState> = _state.asStateFlow()

    suspend fun init(context: Context) {
        if (initialized) return
        initMutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                val app = context.applicationContext
                YoutubeDL.getInstance().init(app)
                runCatching { FFmpeg.init(app) }.onFailure { Log.w(TAG, "ffmpeg init", it) }
                initialized = true
                val version = runCatching { YoutubeDL.getInstance().versionName(app) }.getOrNull()
                    ?: runCatching { YoutubeDL.getInstance().version(app) }.getOrNull()
                _state.update { it.copy(ready = true, version = version) }
            }
        }
    }

    suspend fun update(context: Context, nightly: Boolean = true): String? = withContext(Dispatchers.IO) {
        check(initialized) { "yt-dlp hazır değil" }
        _state.update { it.copy(updating = true, lastError = null) }
        try {
            val channel = if (nightly) {
                YoutubeDL.UpdateChannel.NIGHTLY
            } else {
                YoutubeDL.UpdateChannel.STABLE
            }
            YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext, channel)
            val version = runCatching { YoutubeDL.getInstance().versionName(context) }.getOrNull()
            _state.update { it.copy(updating = false, version = version) }
            version
        } catch (t: Throwable) {
            _state.update { it.copy(updating = false, lastError = Fmt.errorMessage(t)) }
            throw t
        }
    }

    suspend fun autoUpdateIfDue(context: Context, settingsRepo: SettingsRepository) {
        val settings = settingsRepo.snapshot()
        if (!settings.autoUpdateYtdlp) return
        val day = 24L * 60L * 60L * 1000L
        if (System.currentTimeMillis() - settings.lastYtdlpUpdate < day) return
        val result = runCatching { update(context) }
        if (result.isSuccess) {
            settingsRepo.setLastYtdlpUpdate(System.currentTimeMillis())
        }
    }

    suspend fun extract(url: String): PlaylistInfo = withContext(Dispatchers.IO) {
        check(initialized) { "yt-dlp hazır değil" }
        val request = YoutubeDLRequest(url)
        request.addOption("--flat-playlist")
        request.addOption("--skip-download")
        request.addOption("--no-warnings")
        request.addOption("--ignore-errors")
        request.addOption("--dump-single-json")

        val response = YoutubeDL.getInstance().execute(request, null, false)
        val raw = response.out.trim()
        if (raw.isBlank()) {
            throw IllegalStateException(Fmt.errorMessage(Throwable(response.err.ifBlank { "Bağlantı okunamadı" })))
        }

        val json = JSONObject(raw)
        val entriesJson = json.optJSONArray("entries")
        if (entriesJson == null) {
            val entry = parseEntry(json) ?: throw IllegalStateException("Video bilgisi alınamadı")
            PlaylistInfo(
                title = stringOf(json, "title") ?: "Tek parça",
                entries = listOf(entry),
                isPlaylist = false,
            )
        } else {
            val entries = ArrayList<ExtractedEntry>(entriesJson.length())
            for (i in 0 until entriesJson.length()) {
                val item = entriesJson.optJSONObject(i) ?: continue
                parseEntry(item)?.let { entries.add(it) }
            }
            if (entries.isEmpty()) throw IllegalStateException("Çalma listesinde indirilebilir parça bulunamadı")
            PlaylistInfo(
                title = stringOf(json, "title") ?: "Çalma listesi",
                entries = entries,
                isPlaylist = true,
            )
        }
    }

    suspend fun download(
        context: Context,
        itemId: Long,
        request: DownloadRequest,
        settings: AppSettings,
        dir: File,
        onProgress: (Float, Long) -> Unit,
    ): DownloadResult = withContext(Dispatchers.IO) {
        check(initialized) { "yt-dlp hazır değil" }

        val ytdlp = YoutubeDLRequest(request.url)
        ytdlp.addOption("--no-playlist")
        ytdlp.addOption("--newline")
        ytdlp.addOption("--no-warnings")
        ytdlp.addOption("--ignore-errors")
        ytdlp.addOption("--retries", "5")
        ytdlp.addOption("--fragment-retries", "5")
        ytdlp.addOption("--no-mtime")
        ytdlp.addOption("-f", "bestaudio/best")

        if (settings.proxy.isNotBlank()) {
            ytdlp.addOption("--proxy", settings.proxy.trim())
        }
        if (settings.restrictFilenames) {
            ytdlp.addOption("--restrict-filenames")
        }

        val template = settings.filenameTemplate.ifBlank { "%(title)s.%(ext)s" }
        ytdlp.addOption("-o", File(dir, template).absolutePath)
        ytdlp.addOption("--print", "after_move:filepath")

        if (request.format != FORMAT_ORIGINAL) {
            ytdlp.addOption("--extract-audio")
            ytdlp.addOption("--audio-format", request.format)
            if (request.format in BITRATE_FORMATS) {
                ytdlp.addOption("--audio-quality", "${request.bitrate}K")
            }
        }

        if (request.embedThumbnail && request.format != "wav") {
            ytdlp.addOption("--embed-thumbnail")
            if (request.format == "mp3" || request.format == "m4a") {
                ytdlp.addOption("--convert-thumbnails", "jpg")
            }
        }
        if (request.embedMetadata && request.format != "wav") {
            ytdlp.addOption("--embed-metadata")
        }

        val startedAt = System.currentTimeMillis()
        val processId = "suzuki-$itemId"
        val response = YoutubeDL.getInstance().execute(ytdlp, processId, false) { progress, eta, _ ->
            onProgress((progress / 100f).coerceIn(0f, 1f), eta)
        }

        var path = response.out.lineSequence()
            .map { it.trim() }
            .lastOrNull { it.startsWith("/") && it.substringAfterLast('.', "").lowercase(Locale.US) in AUDIO_EXT }

        if (path == null) {
            path = findFreshFile(dir, startedAt)
        }
        DownloadResult(path)
    }

    private fun findFreshFile(dir: File, since: Long): String? {
        return dir.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.lastModified() >= since - 15_000L }
            ?.filter { it.extension.lowercase(Locale.US) in AUDIO_EXT }
            ?.maxByOrNull { it.lastModified() }
            ?.absolutePath
    }

    private fun parseEntry(json: JSONObject): ExtractedEntry? {
        val id = stringOf(json, "id")
        val rawUrl = stringOf(json, "url")
        val url = when {
            rawUrl != null && rawUrl.startsWith("http") -> rawUrl
            id != null -> "https://www.youtube.com/watch?v=$id"
            rawUrl != null -> rawUrl
            else -> return null
        }
        return ExtractedEntry(
            videoId = id.orEmpty(),
            url = url,
            title = stringOf(json, "title") ?: "Bilinmeyen parça",
            uploader = stringOf(json, "uploader")
                ?: stringOf(json, "channel")
                ?: stringOf(json, "artists"),
        )
    }

    private fun stringOf(json: JSONObject, key: String): String? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optString(key, "").trim().ifBlank { null }
    }
}
