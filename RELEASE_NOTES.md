# Release Notes — Luna Fetch

## [1.0.1] — Migración Kotlin Multiplatform — 2026-07-16

Luna YT-DLP Downloader adopta el nombre **Luna Fetch** y migra de WPF/.NET a Kotlin Multiplatform con una interfaz Compose compartida para Windows, Linux y Android. La versión conserva análisis, formatos, calidades, miniaturas, progreso y logs, añade cancelación real y permite abrir el archivo descargado pulsando su tarjeta.

Android incorpora Material 3, color dinámico, almacenamiento mediante el selector del sistema y un motor local con Python, `yt-dlp` y FFmpeg. El motor comprueba actualizaciones estables, habilita EJS/QuickJS para YouTube y detecta la resolución máxima desde todos los formatos; los selectores incluyen fallbacks para evitar errores de formato. Se retira FLAC porque la fuente habitual ya es con pérdida.

La distribución de escritorio adopta la cadena verificada de LyraFlow para EXE, MSI, MSIX, DEB/RPM, firma y hashes, incluido el icono nativo de Luna Fetch en la ventana. El flujo también compila APK/AAB y exige credenciales separadas para una publicación Android firmada.

## [1.0.0] — Lollipop — 2026-07-14

Primera versión WPF para Windows con análisis y descarga mediante `yt-dlp`, conversión con FFmpeg, selección de formato/calidad, tema claro/oscuro, progreso y consola técnica.
