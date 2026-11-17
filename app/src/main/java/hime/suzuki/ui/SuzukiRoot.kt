package hime.suzuki.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hime.suzuki.R
import hime.suzuki.core.Storage
import hime.suzuki.ui.components.AudioOptionsSheet
import hime.suzuki.ui.components.LoadingDialog
import hime.suzuki.ui.screens.DownloadsScreen
import hime.suzuki.ui.screens.HomeScreen
import hime.suzuki.ui.screens.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
}

@Composable
fun SuzukiRoot(vm: MainViewModel) {
    val context = LocalContext.current
    val boot by vm.boot.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val nav = rememberNavController()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var storageOk by remember { mutableStateOf(Storage.hasWriteAccess(context)) }

    LaunchedEffect(boot.error) {
        val error = boot.error ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = "Hazırlanamadı: $error",
            actionLabel = "Tekrar dene",
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
        bottomBar = { SuzukiBottomBar(nav) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding),
                enterTransition = {
                    fadeIn(animationSpec = tween(220)) +
                        slideInHorizontally(animationSpec = tween(260), initialOffsetX = { it / 14 })
                },
                exitTransition = { fadeOut(animationSpec = tween(140)) },
                popEnterTransition = { fadeIn(animationSpec = tween(200)) },
                popExitTransition = {
                    fadeOut(animationSpec = tween(160)) +
                        slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it / 14 })
                },
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        vm = vm,
                        storageOk = storageOk,
                        onGrantStorage = grantStorage,
                        onOpenDownloads = {
                            nav.navigate(Routes.DOWNLOADS) { launchSingleTop = true }
                        },
                    )
                }
                composable(Routes.DOWNLOADS) { DownloadsScreen(vm) }
                composable(Routes.SETTINGS) { SettingsScreen(vm, storageOk, grantStorage) }
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
        LoadingDialog("Bağlantı okunuyor", "Çalma listesi bilgileri getiriliyor…")
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
private fun SuzukiBottomBar(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val tabs = listOf(
        Triple(Routes.HOME, R.drawable.ic_home, "Ana sayfa"),
        Triple(Routes.DOWNLOADS, R.drawable.ic_download, "İndirmeler"),
        Triple(Routes.SETTINGS, R.drawable.ic_settings, "Ayarlar"),
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { (route, icon, label) ->
            NavigationBarItem(
                selected = current == route,
                onClick = {
                    nav.navigate(route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
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
