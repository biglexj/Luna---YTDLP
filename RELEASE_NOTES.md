# Release Notes — Luna Fetch

## [1.0.3] — Audio con contexto — 2026-07-18

MP3 y M4A conservan ahora todos los metadatos y portada que entregue la fuente, sin asumir que cada audio es una canción. Las playlists y álbumes se detectan como colecciones, pueden descargarse completas y numeran sus pistas; cuando existen, su título e índice se incorporan como álbum y pista.

## [1.0.2] — APK por arquitectura — 2026-07-18

Android se distribuye ahora en APK firmados y separados para ARM64, ARM32 y x86_64: se eliminan el APK universal, x86 y AAB para reducir drásticamente las descargas. El selector de tema se simplifica a un único icono que rota entre Sistema, Claro y Oscuro.

## [1.0.1] — Migración Kotlin Multiplatform — 2026-07-16

Luna YT-DLP Downloader adopta el nombre **Luna Fetch** y migra de WPF/.NET a Kotlin Multiplatform con una interfaz Compose compartida para Windows, Linux y Android. La versión conserva análisis, formatos, calidades, miniaturas, progreso y logs, añade cancelación real y permite abrir el archivo descargado pulsando su tarjeta.

Android incorpora Material 3, color dinámico, almacenamiento mediante el selector del sistema y un motor local con Python, `yt-dlp` y FFmpeg. El motor comprueba actualizaciones estables, habilita EJS/QuickJS para YouTube y detecta la resolución máxima desde todos los formatos; los selectores incluyen fallbacks para evitar errores de formato. Se retira FLAC porque la fuente habitual ya es con pérdida.

La distribución de escritorio adopta una cadena reproducible para EXE, MSI, DEB/RPM, firma y hashes, incluido el icono nativo de Luna Fetch en la ventana. GitHub publica EXE/MSI y el manifiesto Winget apunta al MSI; MSIX y Microsoft Store quedan descartados. Android se firma de manera permanente.

## [1.0.0] — Lollipop — 2026-07-14

Primera versión WPF para Windows con análisis y descarga mediante `yt-dlp`, conversión con FFmpeg, selección de formato/calidad, tema claro/oscuro, progreso y consola técnica.
