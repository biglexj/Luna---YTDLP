# Luna Fetch 🌙 — v1.0.3

Luna Fetch es una aplicación multiplataforma para analizar y descargar videos o audio mediante `yt-dlp` y FFmpeg. Está construida con Kotlin Multiplatform y Compose Multiplatform para compartir interfaz, estados y reglas entre Windows, Linux y Android.

## Funciones

- Análisis de enlaces con título, autor, duración y miniatura.
- Video MP4/WebM hasta la resolución disponible.
- Audio MP3 y M4A con perfiles de calidad.
- Metadatos y portada embebida en audio cuando la fuente los proporciona.
- Descarga completa de playlists y álbumes con numeración de pistas.
- Progreso, velocidad, tamaño, ETA, posprocesamiento y registro técnico.
- Cancelación de descargas y apertura del resultado.
- Preferencia persistente de carpeta de destino.
- Tema automático, claro y oscuro con Material 3.
- Color dinámico del sistema en Android 12 o posterior.
- Servicio en primer plano para descargas Android.

## Plataformas

- **Windows:** aplicación JVM de escritorio distribuida como EXE/MSI desde GitHub y, cuando sea aceptada, mediante Winget.
- **Linux:** aplicación JVM y configuración para paquetes DEB/RPM.
- **Android:** interfaz compartida, almacenamiento mediante selector del sistema y motor local con Python, `yt-dlp` y FFmpeg.

En escritorio, `yt-dlp` y FFmpeg deben estar disponibles en `PATH`. En Android el motor se incluye dentro de la aplicación, busca una actualización estable cada 24 horas y puede obtener los componentes EJS oficiales necesarios para YouTube.

## Desarrollo

Requiere un JDK completo 17 o posterior. Para Android, configura `ANDROID_HOME`.

```powershell
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA "Android\Sdk"
.\gradlew.bat :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid
```

Ejecutar escritorio:

```powershell
.\gradlew.bat :composeApp:run
```

Generar APK de depuración:

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

## Distribución

```powershell
.\build-release.ps1 -LocalOnly -SkipSigning
```

El build local genera EXE, MSI, tres APK de depuración por arquitectura y `SHA256SUMS.txt` dentro de `release/`. Una publicación oficial requiere el certificado de Windows y la firma Android descrita en [Docs/packaging.md](Docs/packaging.md); publica EXE, MSI y APK para ARM64, ARM32 y x86_64. MSIX y Microsoft Store están fuera de la distribución del proyecto.

## Licencias

El código propio de Luna Fetch se publica bajo MIT. La distribución Android incorpora componentes con licencias adicionales, incluido `youtubedl-android` bajo GPL-3.0. Consulta [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) antes de redistribuir un APK.

Biglex J · 2026
