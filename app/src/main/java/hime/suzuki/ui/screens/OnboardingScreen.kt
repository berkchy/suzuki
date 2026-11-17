package hime.suzuki.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hime.suzuki.R
import hime.suzuki.core.AppLocale
import hime.suzuki.core.Storage
import hime.suzuki.ui.MainViewModel
import hime.suzuki.ui.components.FolderPickerDialog
import hime.suzuki.ui.components.SelectDialog
import hime.suzuki.ui.components.SuzukiCard

@Composable
fun OnboardingScreen(vm: MainViewModel, onFinish: () -> Unit) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    var showFolder by rememberSaveable { mutableStateOf(false) }
    var storageOk by remember { mutableStateOf(Storage.hasWriteAccess(context)) }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        storageOk = Storage.hasWriteAccess(context)
    }

    val grantStorage: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 30) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        } else {
            storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                storageOk = Storage.hasWriteAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentTag = AppLocale.tag(context)
    val systemLabel = stringResource(R.string.language_system)
    val languageOptions = listOf("" to systemLabel) +
        AppLocale.languages.map { it.tag to it.nativeName }
    val languageLabel = if (currentTag.isBlank()) {
        systemLabel
    } else {
        AppLocale.displayName(context, currentTag)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 36.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = colorResource(R.color.launcher_bg),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                ),
                shadowElevation = 10.dp,
                modifier = Modifier.size(104.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 6.dp),
            )

            Spacer(Modifier.height(28.dp))

            SuzukiCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_language),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            languageLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showLanguage = true }) {
                        Text(stringResource(R.string.common_change))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SuzukiCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_storage),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            stringResource(
                                if (storageOk) {
                                    R.string.onboarding_granted
                                } else {
                                    R.string.onboarding_not_granted
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (storageOk) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                    if (!storageOk) {
                        TextButton(onClick = grantStorage) {
                            Text(stringResource(R.string.home_grant))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.home_storage_banner),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            SuzukiCard {
                Text(
                    stringResource(R.string.settings_download_folder),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    settings.downloadDir,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showFolder = true },
                    enabled = storageOk,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_folder),
                        null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_choose_folder))
                }
                if (!storageOk) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.onboarding_folder_locked),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text(stringResource(R.string.onboarding_start))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.onboarding_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showLanguage) {
        SelectDialog(
            title = stringResource(R.string.settings_language),
            options = languageOptions,
            selected = currentTag,
            onSelect = { tag ->
                AppLocale.set(context, tag)
                showLanguage = false
                (context as? android.app.Activity)?.recreate()
            },
            onDismiss = { showLanguage = false },
        )
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
}
