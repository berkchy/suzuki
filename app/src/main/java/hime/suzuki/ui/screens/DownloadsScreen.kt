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
import androidx.compose.ui.res.stringResource
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
            title = stringResource(R.string.downloads_title),
            subtitle = when {
                active.isNotEmpty() -> stringResource(
                    R.string.downloads_sub_running, running, queued
                )
                doneCount > 0 -> stringResource(R.string.downloads_sub_done, doneCount)
                else -> stringResource(R.string.downloads_sub_default)
            },
            actions = {
                IconAction(
                    iconRes = if (paused) R.drawable.ic_play else R.drawable.ic_pause,
                    description = if (paused) {
                        stringResource(R.string.downloads_resume)
                    } else {
                        stringResource(R.string.downloads_pause)
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    enabled = active.isNotEmpty(),
                    onClick = vm::togglePause,
                )
            },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionPill(
                stringResource(R.string.downloads_tab_active, active.size),
                selected = tab == 0,
            ) { tab = 0 }
            OptionPill(
                stringResource(R.string.downloads_tab_history, finished.size),
                selected = tab == 1,
            ) { tab = 1 }
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
                        title = if (tab == 0) {
                            stringResource(R.string.downloads_queue_empty)
                        } else {
                            stringResource(R.string.downloads_history_empty)
                        },
                        message = if (tab == 0) {
                            stringResource(R.string.downloads_queue_hint)
                        } else {
                            stringResource(R.string.downloads_history_hint)
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
                            Text(stringResource(R.string.downloads_retry_failed))
                        }
                        TextButton(onClick = vm::clearFinished) {
                            Text(stringResource(R.string.common_clear))
                        }
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
    val context = LocalContext.current
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
                        subtitleFor(context, item),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                when (item.status) {
                    DownloadStatus.RUNNING, DownloadStatus.QUEUED -> IconAction(
                        iconRes = R.drawable.ic_close,
                        description = stringResource(R.string.downloads_cancel_cd),
                        tint = MaterialTheme.colorScheme.error,
                    ) { onCancel(item.id) }

                    DownloadStatus.FAILED, DownloadStatus.CANCELED -> IconAction(
                        iconRes = R.drawable.ic_refresh,
                        description = stringResource(R.string.downloads_retry_cd),
                        tint = MaterialTheme.colorScheme.primary,
                    ) { onRetry(item.id) }

                    DownloadStatus.DONE -> IconAction(
                        iconRes = R.drawable.ic_delete,
                        description = stringResource(R.string.downloads_delete_cd),
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
                            if (item.status == DownloadStatus.QUEUED) {
                                stringResource(R.string.downloads_status_queued)
                            } else {
                                stringResource(R.string.downloads_status_downloading)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        val eta = Fmt.eta(context, item.etaSeconds)
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
                            stringResource(
                                R.string.downloads_meta,
                                Fmt.humanSize(item.sizeBytes),
                                Fmt.date(item.finishedAt ?: 0L),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        IconAction(
                            R.drawable.ic_play,
                            stringResource(R.string.common_open),
                            onClick = onOpen,
                        )
                        IconAction(
                            R.drawable.ic_share,
                            stringResource(R.string.common_share),
                            onClick = onShare,
                        )
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

private fun subtitleFor(context: Context, item: DownloadItem): String {
    val format = Fmt.formatName(context, item.format)
    return when (item.status) {
        DownloadStatus.QUEUED -> context.getString(R.string.downloads_status_queued_fmt, format)
        DownloadStatus.RUNNING -> context.getString(
            R.string.downloads_status_running_fmt, format, Fmt.percent(item.progress)
        )
        DownloadStatus.DONE -> context.getString(
            R.string.downloads_status_done_fmt, format, Fmt.humanSize(item.sizeBytes)
        )
        DownloadStatus.FAILED -> context.getString(R.string.downloads_status_failed_fmt, format)
        DownloadStatus.CANCELED -> context.getString(R.string.downloads_status_canceled)
        else -> format
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
        Toast.makeText(context, context.getString(R.string.toast_file_open_failed), Toast.LENGTH_SHORT).show()
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
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.toast_share_music))
        )
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.toast_share_failed), Toast.LENGTH_SHORT).show()
    }
}
