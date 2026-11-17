package hime.suzuki.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hime.suzuki.R
import hime.suzuki.SuzukiApp
import hime.suzuki.core.AppLocale
import hime.suzuki.core.Fmt
import hime.suzuki.data.AppSettings
import hime.suzuki.data.DownloadItem
import hime.suzuki.download.AudioOptions
import hime.suzuki.download.PlaylistInfo
import hime.suzuki.download.YtdlpEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FetchState(
    val loading: Boolean = false,
    val error: String? = null,
    val preview: PlaylistInfo? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as SuzukiApp).container
    private val engine = container.engine
    private val settingsRepo = container.settings

    val boot = container.boot
    val ytdlp = YtdlpEngine.state
    val paused = engine.paused
    val settings = settingsRepo.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )
    val active = engine.activeItems.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList<DownloadItem>(),
    )
    val finished = engine.finishedItems.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList<DownloadItem>(),
    )
    val recent = engine.recentItems.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList<DownloadItem>(),
    )

    var fetch by mutableStateOf(FetchState())
        private set

    fun fetchUrl(url: String) {
        val clean = url.trim()
        val strings = AppLocale.wrap(getApplication())
        if (clean.isBlank()) {
            fetch = FetchState(error = strings.getString(R.string.error_empty_link))
            return
        }
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            fetch = FetchState(error = strings.getString(R.string.error_invalid_link))
            return
        }
        if (fetch.loading) return
        viewModelScope.launch {
            fetch = FetchState(loading = true)
            val result = runCatching { YtdlpEngine.extract(getApplication(), clean) }
            fetch = result.fold(
                onSuccess = { FetchState(preview = it) },
                onFailure = { FetchState(error = Fmt.errorMessage(it)) },
            )
        }
    }

    fun dismissPreview() {
        fetch = fetch.copy(preview = null)
    }

    fun dismissError() {
        fetch = fetch.copy(error = null)
    }

    fun enqueue(options: AudioOptions) {
        val preview = fetch.preview ?: return
        viewModelScope.launch {
            try {
                engine.enqueue(preview, options, preview.title)
                fetch = FetchState()
            } catch (t: Throwable) {
                fetch = fetch.copy(error = Fmt.errorMessage(t))
            }
        }
    }

    fun retryInit() = container.retryInitialize()

    fun togglePause() = engine.togglePause()
    fun cancel(id: Long) = engine.cancel(id)
    fun cancelAll() = engine.cancelAll()
    fun retry(id: Long) = engine.retry(id)
    fun retryAll() = engine.retryAllFinished()
    fun delete(id: Long) = engine.delete(id)

    fun clearFinished() {
        viewModelScope.launch { engine.clearFinished() }
    }

    fun updateYtdlp() {
        viewModelScope.launch {
            runCatching { YtdlpEngine.update(getApplication()) }
                .onFailure { fetch = fetch.copy(error = Fmt.errorMessage(it)) }
        }
    }

    fun setDownloadDir(value: String) {
        viewModelScope.launch { settingsRepo.setDownloadDir(value) }
    }

    fun setAudioFormat(value: String) {
        viewModelScope.launch { settingsRepo.setAudioFormat(value) }
    }

    fun setBitrate(value: Int) {
        viewModelScope.launch { settingsRepo.setBitrate(value) }
    }

    fun setEmbedThumbnail(value: Boolean) {
        viewModelScope.launch { settingsRepo.setEmbedThumbnail(value) }
    }

    fun setEmbedMetadata(value: Boolean) {
        viewModelScope.launch { settingsRepo.setEmbedMetadata(value) }
    }

    fun setConcurrency(value: Int) {
        viewModelScope.launch { settingsRepo.setConcurrency(value) }
    }

    fun setFilenameTemplate(value: String) {
        viewModelScope.launch { settingsRepo.setFilenameTemplate(value) }
    }

    fun setAutoUpdate(value: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoUpdateYtdlp(value) }
    }

    fun setRestrictFilenames(value: Boolean) {
        viewModelScope.launch { settingsRepo.setRestrictFilenames(value) }
    }

    fun setProxy(value: String) {
        viewModelScope.launch { settingsRepo.setProxy(value) }
    }
}
