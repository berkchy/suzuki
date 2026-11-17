package hime.suzuki.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hime.suzuki.R
import hime.suzuki.core.Fmt
import hime.suzuki.data.DownloadItem
import hime.suzuki.data.DownloadStatus
import hime.suzuki.ui.MainViewModel
import hime.suzuki.ui.components.Cover
import hime.suzuki.ui.components.EmptyState
import hime.suzuki.ui.components.IconAction
import hime.suzuki.ui.components.OptionPill
import hime.suzuki.ui.components.ScreenHeader
import java.io.File

@Composable
fun DownloadsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val active by vm.active.collectAsStateWithLifecycle()
    val finished by vm.finished.collectAsStateWithLifecycle()
    val paused by vm.paused.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp),
    ) {
        val running = active.count { it.status == DownloadStatus.RUNNING }
        val queued = active.count { it.status == DownloadStatus.QUEUED }
        val doneCount = finished.count { it.status == DownloadStatus.DONE }

        ScreenHeader(
            title = "İndirmeler",
            subtitle = when {
                active.isNotEmpty() -> "$running indiriliyor • $queued sırada"
                doneCount > 0 -> "$doneCount şarkı tamamlandı"
                else -> "Kuyruk ve indirme geçmişi"
            },
            actions = {
                IconAction(
                    iconRes = if (paused) R.drawable.ic_play else R.drawable.ic_pause,
                    description = if (paused) "Devam et" else "Duraklat",
                    tint = MaterialTheme.colorScheme.primary,
                    enabled = active.isNotEmpty(),
                    onClick = vm::togglePause,
                )
            },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionPill("Aktif (${active.size})", selected = tab == 0) { tab = 0 }
            OptionPill("Geçmiş (${finished.size})", selected = tab == 1) { tab = 1 }
        }

        Spacer(Modifier.height(14.dp))

        val list = if (tab == 0) active else finished
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        iconRes = if (tab == 0) R.drawable.ic_download else R.drawable.ic_music,
                        title = if (tab == 0) "Kuyruk boş" else "Geçmiş boş",
                        message = if (tab == 0) {
                            "Ana sayfadan bir playlist veya şarkı ekle."
                        } else {
                            "Tamamlanan indirmeler burada görünecek."
                        },
                    )
                }
            }

            items(list, key = { it.id }) { item ->
                DownloadCard(
                    item = item,
                    onCancel = vm::cancel,
                    onRetry = vm::retry,
                    onDelete = vm::delete,
                    onOpen = { openAudio(context, item) },
                    onShare = { shareAudio(context, item) },
                )
            }

            if (tab == 1 && list.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = vm::retryAll,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Başarısızları tekrar dene")
                        }
                        TextButton(onClick = vm::clearFinished) { Text("Temizle") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadItem,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(item.thumbnailUrl, size = 54.dp, corner = 14.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitleFor(item),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                when (item.status) {
                    DownloadStatus.RUNNING, DownloadStatus.QUEUED -> IconAction(
                        iconRes = R.drawable.ic_close,
                        description = "İptal et",
                        tint = MaterialTheme.colorScheme.error,
                    ) { onCancel(item.id) }

                    DownloadStatus.FAILED, DownloadStatus.CANCELED -> IconAction(
                        iconRes = R.drawable.ic_refresh,
                        description = "Tekrar dene",
                        tint = MaterialTheme.colorScheme.primary,
                    ) { onRetry(item.id) }

                    DownloadStatus.DONE -> IconAction(
                        iconRes = R.drawable.ic_delete,
                        description = "Sil",
                    ) { onDelete(item.id) }
                }
            }

            when (item.status) {
                DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { if (item.status == DownloadStatus.QUEUED) 0f else item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (item.status == DownloadStatus.QUEUED) "Sırada" else "İndiriliyor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        val eta = Fmt.eta(item.etaSeconds)
                        Text(
                            buildString {
                                append(Fmt.percent(item.progress))
                                if (eta.isNotEmpty() && item.status == DownloadStatus.RUNNING) {
                                    append(" • ")
                                    append(eta)
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                DownloadStatus.DONE -> {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_check),
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${Fmt.humanSize(item.sizeBytes)} • ${Fmt.date(item.finishedAt ?: 0L)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        IconAction(R.drawable.ic_play, "Aç", onClick = onOpen)
                        IconAction(R.drawable.ic_share, "Paylaş", onClick = onShare)
                    }
                }

                DownloadStatus.FAILED -> {
                    val error = item.errorMessage
                    if (!error.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                else -> Unit
            }
        }
    }
}

private fun subtitleFor(item: DownloadItem): String {
    return when (item.status) {
        DownloadStatus.QUEUED -> "Sırada • ${Fmt.formatName(item.format)}"
        DownloadStatus.RUNNING -> "${Fmt.formatName(item.format)} • ${Fmt.percent(item.progress)}"
        DownloadStatus.DONE -> "${Fmt.formatName(item.format)} • ${Fmt.humanSize(item.sizeBytes)}"
        DownloadStatus.FAILED -> "Başarısız • ${Fmt.formatName(item.format)}"
        DownloadStatus.CANCELED -> "İptal edildi"
        else -> Fmt.formatName(item.format)
    }
}

private fun openAudio(context: Context, item: DownloadItem) {
    val path = item.filePath ?: return
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(path),
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, Fmt.mimeFor(path))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Dosya açılamadı", Toast.LENGTH_SHORT).show()
    }
}

private fun shareAudio(context: Context, item: DownloadItem) {
    val path = item.filePath ?: return
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(path),
        )
        val intent = Intent(Intent.ACTION_SEND)
            .setType(Fmt.mimeFor(path))
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Müziği paylaş"))
    }.onFailure {
        Toast.makeText(context, "Paylaşım açılamadı", Toast.LENGTH_SHORT).show()
    }
}
