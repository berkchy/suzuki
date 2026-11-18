package hime.suzuki.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hime.suzuki.R
import hime.suzuki.core.Storage
import hime.suzuki.ui.components.AudioOptionsSheet
import hime.suzuki.ui.components.LoadingDialog
import hime.suzuki.ui.screens.DownloadsScreen
import hime.suzuki.ui.screens.HomeScreen
import hime.suzuki.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun SuzukiRoot(vm: MainViewModel) {
    val context = LocalContext.current
    val boot by vm.boot.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    var selectedTab by remember { mutableIntStateOf(pagerState.currentPage) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    var storageOk by remember { mutableStateOf(Storage.hasWriteAccess(context)) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { selectedTab = it }
    }

    val goToPage: (Int) -> Unit = { page ->
        selectedTab = page
        scope.launch {
            pagerState.animateScrollToPage(
                page,
                animationSpec = tween(420, easing = FastOutSlowInEasing),
            )
        }
    }

    BackHandler(enabled = pagerState.currentPage != 0) { goToPage(0) }

    LaunchedEffect(vm.queueSignal) {
        if (vm.queueSignal == 0) return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = context.getString(R.string.downloads_status_queued),
            actionLabel = context.getString(R.string.common_open),
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) goToPage(1)
    }

    LaunchedEffect(boot.error) {
        val error = boot.error ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = context.getString(R.string.boot_failed, error),
            actionLabel = context.getString(R.string.common_retry),
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) vm.retryInit()
    }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        storageOk = Storage.hasWriteAccess(context)
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

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

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(vm.fetch.error) {
        val error = vm.fetch.error
        if (error != null) {
            snackbar.showSnackbar(error)
            vm.dismissError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { SuzukiBottomBar(selectedTab, goToPage) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 2,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding),
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        vm = vm,
                        storageOk = storageOk,
                        onGrantStorage = grantStorage,
                        onOpenDownloads = { goToPage(1) },
                    )
                    1 -> DownloadsScreen(vm)
                    else -> SettingsScreen(vm, storageOk, grantStorage)
                }
            }
            if (boot.loading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }
        }
    }

    if (vm.fetch.loading) {
        LoadingDialog(
            context.getString(R.string.loading_title),
            context.getString(R.string.loading_body),
        )
    }

    val preview = vm.fetch.preview
    if (preview != null) {
        AudioOptionsSheet(
            preview = preview,
            defaults = settings,
            onDismiss = vm::dismissPreview,
            onConfirm = vm::enqueue,
        )
    }
}

@Composable
private fun SuzukiBottomBar(currentPage: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        Triple(0, R.drawable.ic_home, stringResource(R.string.nav_home)),
        Triple(1, R.drawable.ic_download, stringResource(R.string.nav_downloads)),
        Triple(2, R.drawable.ic_settings, stringResource(R.string.nav_settings)),
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { (page, icon, label) ->
            NavigationBarItem(
                selected = currentPage == page,
                onClick = { onSelect(page) },
                icon = {
                    Icon(
                        painterResource(icon),
                        label,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}
