package hime.suzuki.core

import android.content.Context
import hime.suzuki.R
import java.util.Locale

object Fmt {

    fun percent(progress: Float): String {
        val value = (progress.coerceIn(0f, 1f) * 100f).toInt()
        return "$value%"
    }

    fun eta(context: Context, seconds: Long): String {
        if (seconds <= 0L) return ""
        val minutes = seconds / 60
        val secs = seconds % 60
        return if (minutes > 0L) {
            context.getString(R.string.duration_min_sec, minutes, secs)
        } else {
            context.getString(R.string.duration_sec, secs)
        }
    }

    fun humanSize(bytes: Long): String = Storage.humanSize(bytes)

    fun mimeFor(path: String?): String {
        val ext = path?.substringAfterLast('.', "")?.lowercase(Locale.US) ?: ""
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "opus", "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "webm" -> "audio/webm"
            else -> "audio/*"
        }
    }

    fun formatName(context: Context, format: String): String = when (format) {
        "original" -> context.getString(R.string.format_original)
        "mp3" -> "MP3"
        "m4a" -> "M4A"
        "opus" -> "OPUS"
        "ogg" -> "OGG"
        "flac" -> "FLAC"
        "wav" -> "WAV"
        else -> format.uppercase(Locale.US)
    }

    fun date(timestamp: Long): String {
        if (timestamp <= 0L) return "-"
        val fmt = java.text.SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
        return fmt.format(java.util.Date(timestamp))
    }

    fun errorMessage(t: Throwable): String {
        val raw = t.message ?: t.javaClass.simpleName
        val line = raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .lastOrNull()
            ?: "Unknown error"
        return line.replace("ERROR:", "").trim().take(300)
    }
}
