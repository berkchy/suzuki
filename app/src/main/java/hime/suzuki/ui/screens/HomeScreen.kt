package hime.suzuki.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hime.suzuki.R
import hime.suzuki.core.Fmt
import hime.suzuki.data.DownloadItem
import hime.suzuki.ui.MainViewModel
import hime.suzuki.ui.components.Cover
import hime.suzuki.ui.components.HeroCard
import hime.suzuki.ui.components.IconAction
import hime.suzuki.ui.components.InfoBanner
import hime.suzuki.ui.components.ScreenHeader
import hime.suzuki.ui.components.SectionTitle
import hime.suzuki.ui.components.StatChip
import hime.suzuki.ui.components.SuzukiCard

@Composable
fun HomeScreen(
    vm: MainViewModel,
    storageOk: Boolean,
    onGrantStorage: () -> Unit,
    onOpenDownloads: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var url by rememberSaveable { mutableStateOf("") }
    val fetching = vm.fetch.loading
    val recent by vm.recent.collectAsStateWithLifecycle()
    val ytdlp by vm.ytdlp.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 24.dp),
    ) {
        ScreenHeader(
            title = "Suzuki",
            subtitle = stringResource(R.string.home_subtitle),
        )

        if (!storageOk) {
            InfoBanner(
                text = stringResource(R.string.home_storage_banner),
                actionLabel = stringResource(R.string.home_grant),
                onAction = onGrantStorage,
            )
            Spacer(Modifier.height(14.dp))
        }

        HeroCard {
            Text(stringResource(R.string.home_paste_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_link_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://music.youtube.com/playlist?list=…") },
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_link),
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    if (url.isNotEmpty()) {
                        IconAction(
                            R.drawable.ic_close,
                            stringResource(R.string.common_clear),
                        ) { url = "" }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { readClipboard(context)?.let { url = it } },
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_paste),
                        null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_paste))
                }
                Button(
                    onClick = { if (url.isNotBlank()) vm.fetchUrl(url) },
                    enabled = url.isNotBlank() && !fetching && ytdlp.ready,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (fetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            painterResource(R.drawable.ic_download),
                            null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (ytdlp.ready) {
                                stringResource(R.string.home_list)
                            } else {
                                stringResource(R.string.home_preparing)
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatChip(R.drawable.ic_music, stringResource(R.string.home_chip_playlist))
            StatChip(R.drawable.ic_check, stringResource(R.string.home_chip_cover))
            StatChip(R.drawable.ic_check, stringResource(R.string.home_chip_tags))
            StatChip(R.drawable.ic_folder, stringResource(R.string.home_chip_folder))
        }

        Spacer(Modifier.height(22.dp))
        SectionTitle(
            text = stringResource(R.string.home_recent),
            trailing = {
                if (recent.isNotEmpty()) {
                    TextButton(onClick = onOpenDownloads) {
                        Text(stringResource(R.string.common_all))
                    }
                }
            },
        )
        Spacer(Modifier.height(8.dp))

        if (recent.isEmpty()) {
            SuzukiCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_music),
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            stringResource(R.string.home_no_downloads),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.home_add_first),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            recent.forEach { item ->
                RecentRow(item, onOpenDownloads)
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        val version = ytdlp.version ?: stringResource(R.string.home_ytdlp_loading)
        val status = stringResource(R.string.home_ytdlp_status, version)
        Text(
            if (ytdlp.updating) stringResource(R.string.home_ytdlp_updating, status) else status,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecentRow(item: DownloadItem, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(item.thumbnailUrl, size = 46.dp, corner = 12.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(
                        R.string.home_item_meta,
                        Fmt.formatName(context, item.format),
                        Fmt.humanSize(item.sizeBytes),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painterResource(R.drawable.ic_check),
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun readClipboard(context: Context): String? {
    val manager = context.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = manager.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context).toString().trim().takeIf { it.isNotBlank() }
}
