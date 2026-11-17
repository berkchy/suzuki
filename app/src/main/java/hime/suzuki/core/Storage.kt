package hime.suzuki.core

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale

object Storage {
    const val DEFAULT_DIR = "/storage/emulated/0/Music/Suzuki"

    fun hasWriteAccess(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= 30 -> Environment.isExternalStorageManager()
            else -> ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun appFallbackDir(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        return File(base, "Suzuki")
    }

    fun resolveDir(context: Context, preferred: String): File {
        val candidate = File(preferred.trim().ifBlank { DEFAULT_DIR })
        if (hasWriteAccess(context) && ensure(candidate)) return candidate
        val fallback = appFallbackDir(context)
        ensure(fallback)
        return fallback
    }

    fun ensure(dir: File): Boolean {
        if (!dir.exists() && !dir.mkdirs()) return false
        return dir.isDirectory && dir.canWrite()
    }

    fun scan(context: Context, path: String) {
        runCatching {
            MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
        }
    }

    fun humanSize(bytes: Long): String {
        if (bytes <= 0L) return "-"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024.0 && unit < units.lastIndex) {
            value /= 1024.0
            unit++
        }
        return String.format(Locale.US, "%.1f %s", value, units[unit])
    }
}
