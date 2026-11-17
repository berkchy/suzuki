package hime.suzuki

import android.app.Application
import androidx.room.Room
import hime.suzuki.data.AppDatabase
import hime.suzuki.data.SettingsRepository
import hime.suzuki.download.DownloadEngine
import hime.suzuki.download.Notifications
import hime.suzuki.download.YtdlpEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BootState(
    val started: Boolean = false,
    val loading: Boolean = false,
    val ready: Boolean = false,
    val error: String? = null,
)

class AppContainer(private val app: Application) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(app, AppDatabase::class.java, "suzuki.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val settings: SettingsRepository by lazy { SettingsRepository(app) }

    val engine: DownloadEngine by lazy {
        DownloadEngine(app, database.downloadDao(), settings)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _boot = MutableStateFlow(BootState())
    val boot: StateFlow<BootState> = _boot

    fun initialize() {
        if (_boot.value.started) return
        _boot.update { it.copy(started = true, loading = true, error = null) }
        scope.launch {
            try {
                YtdlpEngine.init(app)
                _boot.value = BootState(started = true, ready = true)
                runCatching { YtdlpEngine.autoUpdateIfDue(app, settings) }
            } catch (t: Throwable) {
                _boot.value = BootState(error = t.message ?: t.javaClass.simpleName)
            }
        }
    }

    fun retryInitialize() {
        _boot.value = BootState()
        initialize()
    }
}

class SuzukiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        container = AppContainer(this)
        container.initialize()
    }
}
