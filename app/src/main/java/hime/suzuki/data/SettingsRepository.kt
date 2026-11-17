package hime.suzuki.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import hime.suzuki.core.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("suzuki_settings")

data class AppSettings(
    val downloadDir: String = Storage.DEFAULT_DIR,
    val audioFormat: String = "mp3",
    val bitrate: Int = 192,
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val concurrency: Int = 2,
    val filenameTemplate: String = "%(title)s.%(ext)s",
    val autoUpdateYtdlp: Boolean = true,
    val restrictFilenames: Boolean = false,
    val proxy: String = "",
    val lastYtdlpUpdate: Long = 0L,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val downloadDir = stringPreferencesKey("download_dir")
        val audioFormat = stringPreferencesKey("audio_format")
        val bitrate = intPreferencesKey("bitrate")
        val embedThumbnail = booleanPreferencesKey("embed_thumbnail")
        val embedMetadata = booleanPreferencesKey("embed_metadata")
        val concurrency = intPreferencesKey("concurrency")
        val filenameTemplate = stringPreferencesKey("filename_template")
        val autoUpdateYtdlp = booleanPreferencesKey("auto_update_ytdlp")
        val restrictFilenames = booleanPreferencesKey("restrict_filenames")
        val proxy = stringPreferencesKey("proxy")
        val lastYtdlpUpdate = longPreferencesKey("last_ytdlp_update")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            downloadDir = prefs[Keys.downloadDir] ?: Storage.DEFAULT_DIR,
            audioFormat = prefs[Keys.audioFormat] ?: "mp3",
            bitrate = prefs[Keys.bitrate] ?: 192,
            embedThumbnail = prefs[Keys.embedThumbnail] ?: true,
            embedMetadata = prefs[Keys.embedMetadata] ?: true,
            concurrency = (prefs[Keys.concurrency] ?: 2).coerceIn(1, 4),
            filenameTemplate = prefs[Keys.filenameTemplate] ?: "%(title)s.%(ext)s",
            autoUpdateYtdlp = prefs[Keys.autoUpdateYtdlp] ?: true,
            restrictFilenames = prefs[Keys.restrictFilenames] ?: false,
            proxy = prefs[Keys.proxy] ?: "",
            lastYtdlpUpdate = prefs[Keys.lastYtdlpUpdate] ?: 0L,
        )
    }

    suspend fun snapshot(): AppSettings = settings.first()

    suspend fun setDownloadDir(value: String) = edit { it[Keys.downloadDir] = value }
    suspend fun setAudioFormat(value: String) = edit { it[Keys.audioFormat] = value }
    suspend fun setBitrate(value: Int) = edit { it[Keys.bitrate] = value }
    suspend fun setEmbedThumbnail(value: Boolean) = edit { it[Keys.embedThumbnail] = value }
    suspend fun setEmbedMetadata(value: Boolean) = edit { it[Keys.embedMetadata] = value }
    suspend fun setConcurrency(value: Int) = edit { it[Keys.concurrency] = value.coerceIn(1, 4) }
    suspend fun setFilenameTemplate(value: String) = edit { it[Keys.filenameTemplate] = value }
    suspend fun setAutoUpdateYtdlp(value: Boolean) = edit { it[Keys.autoUpdateYtdlp] = value }
    suspend fun setRestrictFilenames(value: Boolean) = edit { it[Keys.restrictFilenames] = value }
    suspend fun setProxy(value: String) = edit { it[Keys.proxy] = value }
    suspend fun setLastYtdlpUpdate(value: Long) = edit { it[Keys.lastYtdlpUpdate] = value }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.settingsStore.edit { block(it) }
    }
}
