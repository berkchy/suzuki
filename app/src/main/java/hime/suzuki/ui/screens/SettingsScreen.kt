package hime.suzuki.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hime.suzuki.BuildConfig
import hime.suzuki.R
import hime.suzuki.core.Fmt
import hime.suzuki.core.Storage
import hime.suzuki.ui.MainViewModel
import hime.suzuki.ui.components.DividerLine
import hime.suzuki.ui.components.FolderPickerDialog
import hime.suzuki.ui.components.InfoBanner
import hime.suzuki.ui.components.IconAction
import hime.suzuki.ui.components.LabeledValue
import hime.suzuki.ui.components.OptionPill
import hime.suzuki.ui.components.ScreenHeader
import hime.suzuki.ui.components.SectionTitle
import hime.suzuki.ui.components.SelectDialog
import hime.suzuki.ui.components.StepButton
import hime.suzuki.ui.components.SuzukiCard
import hime.suzuki.ui.components.SwitchRow

private val FORMAT_OPTIONS = listOf(
    "mp3" to "MP3 — genel uyumluluk",
    "m4a" to "M4A — AAC",
    "opus" to "OPUS — verimli",
    "ogg" to "OGG — Vorbis",
    "flac" to "FLAC — kayıpsız",
    "wav" to "WAV — sıkıştırmasız",
    "original" to "Orijinal — yeniden kodlama yok",
)

private val BITRATE_OPTIONS = listOf(128, 192, 256, 320)
private val BITRATE_FORMATS = setOf("mp3", "m4a", "opus", "ogg")

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    storageOk: Boolean,
    onGrantStorage: () -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val ytdlp by vm.ytdlp.collectAsStateWithLifecycle()
    var showFolder by rememberSaveable { mutableStateOf(false) }
    var showFormat by rememberSaveable { mutableStateOf(false) }
    var showClear by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 24.dp),
    ) {
        ScreenHeader("Ayarlar", "Depolama, format ve motor tercihleri")

        SectionTitle("Depolama")
        Spacer(Modifier.height(8.dp))
        SuzukiCard {
            LabeledValue("İndirme klasörü", settings.downloadDir)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showFolder = true },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_folder),
                        null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Klasör seç")
                }
                OutlinedButton(
                    onClick = { vm.setDownloadDir(Storage.DEFAULT_DIR) },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Varsayılan")
                }
            }
            if (!storageOk) {
                Spacer(Modifier.height(12.dp))
                InfoBanner(
                    text = "Tüm dosyalara erişim izni yok. İndirmeler uygulamanın kendi klasörüne kaydedilir.",
                    actionLabel = "İzin ver",
                    onAction = onGrantStorage,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("İndirme")
        Spacer(Modifier.height(8.dp))
        SuzukiCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Varsayılan format", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        Fmt.formatName(settings.audioFormat),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showFormat = true }) { Text("Değiştir") }
            }
            DividerLine()
            Text("Bit hızı", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BITRATE_OPTIONS.forEach { value ->
                    OptionPill(
                        text = "${value}k",
                        selected = settings.bitrate == value,
                        enabled = settings.audioFormat in BITRATE_FORMATS,
                    ) { vm.setBitrate(value) }
                }
            }
            if (settings.audioFormat !in BITRATE_FORMATS) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Seçili format kayıpsız veya orijinal; bit hızı uygulanmaz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DividerLine()
            SwitchRow(
                title = "Kapak resmi göm",
                subtitle = "İndirilen dosyaya şarkı kapağı eklenir",
                checked = settings.embedThumbnail,
                onCheckedChange = { vm.setEmbedThumbnail(it) },
            )
            SwitchRow(
                title = "Etiketleri göm",
                subtitle = "Başlık ve sanatçı bilgileri yazılır",
                checked = settings.embedMetadata,
                onCheckedChange = { vm.setEmbedMetadata(it) },
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Performans")
        Spacer(Modifier.height(8.dp))
        SuzukiCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Eşzamanlı indirme", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Aynı anda işlenecek parça sayısı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StepButton("−", enabled = settings.concurrency > 1) {
                    vm.setConcurrency(settings.concurrency - 1)
                }
                Text(
                    "${settings.concurrency}",
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                StepButton("+", enabled = settings.concurrency < 4) {
                    vm.setConcurrency(settings.concurrency + 1)
                }
            }
            DividerLine()
            SwitchRow(
                title = "yt-dlp'yi otomatik güncelle",
                subtitle = "Günde bir kez kontrol edilir",
                checked = settings.autoUpdateYtdlp,
                onCheckedChange = { vm.setAutoUpdate(it) },
            )
            DividerLine()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("yt-dlp sürümü", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        ytdlp.version ?: "Bilinmiyor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (ytdlp.updating) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = vm::updateYtdlp,
                    enabled = !ytdlp.updating,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (ytdlp.updating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painterResource(R.drawable.ic_refresh),
                            null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Güncelle")
                    }
                }
            }
            ytdlp.lastError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Gelişmiş")
        Spacer(Modifier.height(8.dp))
        SuzukiCard {
            EditableSetting(
                label = "Dosya adı şablonu",
                value = settings.filenameTemplate,
                placeholder = "%(title)s.%(ext)s",
                onSave = vm::setFilenameTemplate,
            )
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                title = "Dosya adlarını sadeleştir",
                subtitle = "Türkçe karakterleri ASCII'ye çevirir",
                checked = settings.restrictFilenames,
                onCheckedChange = { vm.setRestrictFilenames(it) },
            )
            DividerLine()
            EditableSetting(
                label = "Proxy (isteğe bağlı)",
                value = settings.proxy,
                placeholder = "http://127.0.0.1:8080",
                onSave = vm::setProxy,
            )
            DividerLine()
            Button(
                onClick = { showClear = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text("İndirme geçmişini temizle")
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Hakkında")
        Spacer(Modifier.height(8.dp))
        SuzukiCard {
            LabeledValue("Sürüm", BuildConfig.VERSION_NAME)
            DividerLine()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("GitHub deposu", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "berkchy/suzuki",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { openUrl(context, "https://github.com/berkchy/suzuki") }) {
                    Text("Aç")
                }
            }
            DividerLine()
            Text(
                "Bu uygulama kişisel arşivleme amacıyla geliştirildi. İndirdiğin içeriklerin kullanım haklarına ve platform koşullarına saygı göster.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showFolder) {
        FolderPickerDialog(
            start = settings.downloadDir,
            onDismiss = { showFolder = false },
            onPick = {
                vm.setDownloadDir(it)
                showFolder = false
            },
        )
    }

    if (showFormat) {
        SelectDialog(
            title = "Varsayılan format",
            options = FORMAT_OPTIONS,
            selected = settings.audioFormat,
            onSelect = vm::setAudioFormat,
            onDismiss = { showFormat = false },
        )
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Geçmişi temizle") },
            text = {
                Text("Tamamlanan, başarısız ve iptal edilen kayıtlar listeden silinir. İndirilen dosyalar cihazdan silinmez.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearFinished()
                        showClear = false
                    }
                ) { Text("Temizle") }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text("Vazgeç") }
            },
        )
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun EditableSetting(
    label: String,
    value: String,
    placeholder: String,
    onSave: (String) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        trailingIcon = {
            if (text.trim() != value) {
                IconAction(
                    iconRes = R.drawable.ic_check,
                    description = "Kaydet",
                    tint = MaterialTheme.colorScheme.primary,
                ) { onSave(text.trim()) }
            }
        },
    )
}
