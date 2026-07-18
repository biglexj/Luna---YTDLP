package com.biglexj.lunafetch.platform

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.biglexj.lunafetch.domain.DownloadEngine
import com.biglexj.lunafetch.domain.DownloadException
import com.biglexj.lunafetch.domain.DownloadProgress
import com.biglexj.lunafetch.domain.DownloadRequest
import com.biglexj.lunafetch.domain.DownloadResult
import com.biglexj.lunafetch.domain.VideoInfo
import com.biglexj.lunafetch.domain.YtdlpProtocol
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidDownloadEngine(private val context: Context) : DownloadEngine {
    @Volatile
    private var processId: String? = null
    @Volatile
    private var initialized = false

    override suspend fun analyze(url: String): VideoInfo = withContext(Dispatchers.IO) {
        initialize()
        try {
            val info = YoutubeDL.getInfo(androidRequest(url))
            VideoInfo(
                url = url,
                title = info.title ?: "Título desconocido",
                uploader = info.uploader ?: "Autor desconocido",
                durationSeconds = info.duration.toDouble(),
                thumbnailUrl = info.thumbnail.orEmpty(),
                maxHeight = info.formats?.maxOfOrNull { it.height }?.takeIf { it > 0 }
                    ?: info.height.takeIf { it > 0 }
                    ?: 1080,
                collectionTitle = info.property("playlistTitle") as? String,
                collectionCount = (info.property("playlistCount") as? Number)?.toInt() ?: 0,
            )
        } catch (error: Exception) {
            throw DownloadException(error.message ?: "No se pudo analizar el enlace en Android.", error)
        }
    }

    override suspend fun download(
        request: DownloadRequest,
        onProgress: (DownloadProgress) -> Unit,
        onLog: (String) -> Unit,
    ): DownloadResult = withContext(Dispatchers.IO) {
        initialize()
        val treeUri = runCatching { Uri.parse(request.destination) }.getOrNull()
            ?: throw DownloadException("Selecciona una carpeta válida.")
        val workRoot = File(context.cacheDir, "downloads")
        val workDirectory = File(workRoot, System.currentTimeMillis().toString()).apply { mkdirs() }
        val outputTemplate = File(
            workDirectory,
            if (request.downloadCollection) "%(playlist_index)03d - %(title)s.%(ext)s" else "%(title)s.%(ext)s",
        ).absolutePath
        val id = "lunafetch-${System.currentTimeMillis()}"
        processId = id
        DownloadForegroundService.start(context)

        try {
            val command = YtdlpProtocol.buildDownloadArguments(request, outputTemplate).dropLast(2)
            val youtubeRequest = androidRequest(request.url).addCommands(command)
            val response = YoutubeDL.execute(youtubeRequest, id) { percentage, etaSeconds, line ->
                onLog(line)
                YtdlpProtocol.parseProgress(line)?.let(onProgress)
                val progress = DownloadProgress(
                    percentage = percentage.toDouble().coerceIn(0.0, 100.0),
                    eta = etaSeconds.takeIf { it >= 0 }?.let { "${it}s" }.orEmpty(),
                )
                onProgress(progress)
                DownloadForegroundService.update(context, percentage.toInt())
            }
            if (response.exitCode != 0) {
                throw DownloadException(response.err.ifBlank { "yt-dlp terminó con código ${response.exitCode}." })
            }
            response.out.lineSequence().filter(String::isNotBlank).forEach(onLog)
            val downloaded = response.out.lineSequence()
                .mapNotNull(YtdlpProtocol::outputPath)
                .map(::File)
                .filter(File::isFile)
                .distinctBy { it.absolutePath }
                .toList()
                .ifEmpty {
                    workDirectory.walkTopDown()
                        .filter(File::isFile)
                        .filterNot { it.extension.equals("part", true) }
                        .toList()
                }
            if (downloaded.isEmpty()) {
                throw DownloadException("La descarga terminó, pero no se encontró el archivo resultante.")
            }
            val resultUris = copyToTree(downloaded, treeUri)
            DownloadResult(
                outputPaths = resultUris.map(Uri::toString),
                openPath = if (resultUris.size == 1) resultUris.first().toString() else treeUri.toString(),
            )
        } catch (error: Exception) {
            throw DownloadException(error.message ?: "No se pudo completar la descarga en Android.", error)
        } finally {
            processId = null
            DownloadForegroundService.stop(context)
            workDirectory.deleteRecursively()
        }
    }

    override fun cancel() {
        processId?.let(YoutubeDL::destroyProcessById)
        processId = null
        DownloadForegroundService.stop(context)
    }

    @Synchronized
    private fun initialize() {
        if (initialized) return
        try {
            YoutubeDL.init(context)
            FFmpeg.init(context)
            updateYtdlpIfNeeded()
            initialized = true
        } catch (error: Exception) {
            throw DownloadException("No se pudo inicializar el motor local de Android.", error)
        }
    }

    private fun androidRequest(url: String): YoutubeDLRequest = YoutubeDLRequest(url)
        .addOption("--js-runtimes", "quickjs")
        .addOption("--remote-components", "ejs:github")

    private fun updateYtdlpIfNeeded() {
        val preferences = context.getSharedPreferences("lunafetch-engine", Context.MODE_PRIVATE)
        val lastUpdate = preferences.getLong("lastYtdlpUpdate", 0L)
        val now = System.currentTimeMillis()
        if (now - lastUpdate < UpdateIntervalMillis) return

        runCatching {
            YoutubeDL.updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
        }.onSuccess {
            preferences.edit().putLong("lastYtdlpUpdate", now).apply()
        }
    }

    private fun copyToTree(sources: List<File>, treeUri: Uri): List<Uri> {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        return sources.map { source ->
            val created = DocumentsContract.createDocument(
                resolver,
                parent,
                mimeType(source.extension),
                source.name,
            ) ?: throw DownloadException("Android no permitió crear el archivo en la carpeta elegida.")
            resolver.openOutputStream(created, "w")?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: throw DownloadException("Android no permitió escribir el archivo descargado.")
            created
        }
    }

    private fun Any.property(name: String): Any? = runCatching {
        val getter = "get" + name.replaceFirstChar(Char::uppercaseChar)
        javaClass.methods.firstOrNull { it.name == getter && it.parameterCount == 0 }?.invoke(this)
    }.getOrNull()

    private fun mimeType(extension: String): String = when (extension.lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        else -> "application/octet-stream"
    }

    private companion object {
        const val UpdateIntervalMillis = 24L * 60L * 60L * 1000L
    }
}
