package hime.suzuki.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hime.suzuki.R
import hime.suzuki.data.AppSettings
import hime.suzuki.download.AudioOptions
import hime.suzuki.download.PlaylistInfo

private val FORMATS = listOf(
    "original" to "Orijinal",
    "mp3" to "MP3",
    "m4a" to "M4A",
    "opus" to "OPUS",
    "ogg" to "OGG",
    "flac" to "FLAC",
    "wav" to "WAV",
)

private val BITRATES = listOf(128, 192, 256, 320)
private val BITRATE_FORMATS = setOf("mp3", "m4a", "opus", "ogg")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOptionsSheet(
    preview: PlaylistInfo,
    defaults: AppSettings,
    onDismiss: () -> Unit,
    onConfirm: (AudioOptions) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var format by remember { mutableStateOf(defaults.audioFormat) }
    var bitrate by remember { mutableIntStateOf(defaults.bitrate) }
    var embedThumbnail by remember { mutableStateOf(defaults.embedThumbnail) }
    var embedMetadata by remember { mutableStateOf(defaults.embedMetadata) }
    var range by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                if (preview.isPlaylist) "ÇALMA LİSTESİ" else "TEK PARÇA",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                preview.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (preview.isPlaylist) {
                    "${preview.entries.size} parça • ${preview.entries.firstOrNull()?.uploader ?: "YouTube Music"}"
                } else {
                    preview.entries.firstOrNull()?.uploader ?: "Tek şarkı"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (preview.isPlaylist && preview.entries.size > 1) {
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = range,
                    onValueChange = { range = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Parça aralığı") },
                    placeholder = { Text("Boş = tümü, örn: 1-10 veya 2,5,8") },
                    singleLine = true,
                )
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Ses formatı")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FORMATS.forEach { (value, label) ->
                    OptionPill(text = label, selected = format == value) { format = value }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("Bit hızı")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BITRATES.forEach { value ->
                    OptionPill(
                        text = "${value}k",
                        selected = bitrate == value,
                        enabled = format in BITRATE_FORMATS,
                    ) { bitrate = value }
                }
            }
            if (format !in BITRATE_FORMATS) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (format == "original") {
                        "Orijinal ses akışı indirilir, yeniden kodlanmaz."
                    } else {
                        "Bu format kayıpsız olduğu için bit hızı uygulanmaz."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            SwitchRow(
                title = "Kapak resmi ekle",
                subtitle = "Şarkı kapağını dosyaya göm",
                checked = embedThumbnail,
                onCheckedChange = { embedThumbnail = it },
            )
            SwitchRow(
                title = "Etiketleri ekle",
                subtitle = "Başlık, sanatçı ve albüm bilgisi",
                checked = embedMetadata,
                onCheckedChange = { embedMetadata = it },
            )

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    onConfirm(
                        AudioOptions(
                            format = format,
                            bitrate = bitrate,
                            embedThumbnail = embedThumbnail,
                            embedMetadata = embedMetadata,
                            range = range,
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_download),
                    null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Kuyruğa ekle (${preview.entries.size})")
            }
        }
    }
}
