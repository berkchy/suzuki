package hime.suzuki.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object DownloadStatus {
    const val QUEUED = "QUEUED"
    const val RUNNING = "RUNNING"
    const val DONE = "DONE"
    const val FAILED = "FAILED"
    const val CANCELED = "CANCELED"
}

@Entity(
    tableName = "downloads",
    indices = [Index("status"), Index("createdAt"), Index("finishedAt")]
)
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val videoId: String,
    val url: String,
    val title: String,
    val uploader: String? = null,
    val playlistTitle: String? = null,
    val format: String = "mp3",
    val bitrate: Int = 192,
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val status: String = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val etaSeconds: Long = 0L,
    val filePath: String? = null,
    val sizeBytes: Long = 0L,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
) {
    val thumbnailUrl: String?
        get() = if (videoId.isBlank()) null else "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
}
