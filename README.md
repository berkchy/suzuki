# Suzuki

Android için Material 3 müzik indirici. YouTube / YouTube Music bağlantılarını ve
çalma listelerini yt-dlp + ffmpeg ile indirir; koyu mavi tema, animasyonlu geçişler,
indirme kuyruğu, bildirimler ve ayrıntılı ayarlar içerir.

## Özellikler

- Video ve çalma listesi bağlantı desteği (YouTube / YouTube Music)
- Format seçimi: Orijinal, MP3, M4A, OPUS, OGG, FLAC, WAV
- Bit hızı seçimi (128 / 192 / 256 / 320 kbps)
- Kapak resmi ve etiket (metadata) gömme
- Playlist parça aralığı (örn. `1-10`, `2,5,8`)
- Eşzamanlı indirme sayısı (1-4), duraklatma, iptal, tekrar deneme
- Foreground servis + ilerleme ve tamamlanma bildirimleri
- İndirme klasörü seçimi, tüm dosyalara erişim izni desteği
- yt-dlp'yi uygulama içinden güncelleme (günde bir otomatik kontrol)
- Room ile kuyruk/geçmiş, DataStore ile ayarlar
- Material 3 koyu mavi tema, Compose navigasyon geçiş animasyonları

## Mimari

- Kotlin + Jetpack Compose + Material 3
- yt-dlp ve ffmpeg: `io.github.junkfood02.youtubedl-android` (APK içinde gelir,
  ilk açılışta uygulama özel dizinine çıkarılır; `Android/data` gerekmez)
- Kuyruk: Room + coroutine tabanlı motor, foreground `dataSync` servisi
- İndirilen dosyalar: varsayılan `/storage/emulated/0/Music/Suzuki`

## Derleme

```bash
./gradlew :app:assembleRelease
```

Gereksinimler: JDK 17, Android SDK 35.

## GitHub Actions

- Her `main` push: imzalı APK derlenir, `continuous` release'i güncellenir
- `v*` etiketi: sürüm release'i ve APK yüklenir
- Gradle bağımlılık cache'i `gradle/actions/setup-gradle` ile korunur

İmzalama için repo secret'ları: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`. Secret yoksa debug anahtarıyla imzalanır.

## Not

Bu uygulama kişisel arşivleme amacıyla geliştirilmiştir. İndirilen içeriklerin
kullanım haklarına ve platform koşullarına saygı gösterin.
