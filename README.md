# Suzuki

A Material 3 music downloader for Android. It downloads YouTube / YouTube Music
links and playlists with yt-dlp + ffmpeg, and includes a dark blue theme,
animated transitions, a download queue, notifications, and detailed settings.

## Download

- Latest automatic build: [continuous release](https://github.com/berkchy/suzuki/releases/tag/continuous)
  - `suzuki-latest-arm64-v8a.apk` (~63 MB) — most phones from 2019 onward
  - `suzuki-latest-armeabi-v7a.apk` (~56 MB) — older / 32-bit devices
- Releases: [v1.2.0](https://github.com/berkchy/suzuki/releases/tag/v1.2.0)

If you are unsure about your architecture, try the `arm64-v8a` APK first; if it
will not install, use the `armeabi-v7a` build. Android may ask you to allow
"install unknown apps".

## APK size and components

The APK bundles a Python runtime and ffmpeg. On Android 10 and later, executing
binaries from the app data directory is blocked (W^X), so these cannot be
downloaded and run later. The yt-dlp script starts with the bundled version and
updates itself from GitHub once a day (Settings → update yt-dlp). Separate
per-ABI APKs mean each device downloads only its own architecture.

## Features

- Video and playlist link support (YouTube / YouTube Music)
- Format selection: Original, MP3, M4A, OPUS, OGG, FLAC, WAV
- Bitrate selection (128 / 192 / 256 / 320 kbps)
- Cover art and metadata embedding
- Playlist track ranges (e.g. `1-10`, `2,5,8`)
- Concurrent downloads (1-4), pause, cancel, retry
- Foreground service + progress and completion notifications
- Download folder selection, with support for all-files access permission
- Update yt-dlp from inside the app (automatic daily check)
- Queue/history with Room, settings with DataStore
- 25-language interface with in-app language selection (offline)
- First-launch setup for language, storage access, and download folder
- Swipe between Home, Downloads, and Settings with smooth animated transitions
- Material 3 dark blue theme, Compose navigation transition animations

## Architecture

- Kotlin + Jetpack Compose + Material 3
- yt-dlp and ffmpeg: `io.github.junkfood02.youtubedl-android` (bundled in the
  APK and extracted to the app's private directory on first launch; no
  `Android/data` needed)
- Queue: Room + coroutine-based engine, foreground `dataSync` service
- Downloaded files: defaults to `/storage/emulated/0/Music/Suzuki`

## Build

```bash
./gradlew :app:assembleRelease
```

Requirements: JDK 17, Android SDK 35.

## GitHub Actions

- Every push to `main`: a signed APK is built and the `continuous` release is updated
- `v*` tag: a versioned release is created and the APKs are uploaded
- The Gradle dependency cache is maintained with `gradle/actions/setup-gradle`

Repository secrets for signing: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`. Without secrets, the build is signed with a debug key.

## Note

This app is intended for personal archiving. Please respect the usage rights of
downloaded content and the terms of the platforms involved.
