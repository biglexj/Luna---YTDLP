package com.biglexj.lunafetch.platform

import com.biglexj.lunafetch.domain.DownloadEngine
import com.biglexj.lunafetch.domain.DownloadException
import com.biglexj.lunafetch.domain.DownloadPhase
import com.biglexj.lunafetch.domain.DownloadProgress
import com.biglexj.lunafetch.domain.DownloadRequest
import com.biglexj.lunafetch.domain.DownloadResult
import com.biglexj.lunafetch.domain.VideoInfo
import com.biglexj.lunafetch.domain.YtdlpProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class DesktopDownloadEngine(
    private val executable: String = "yt-dlp",
) : DownloadEngine {
    private val activeProcess = AtomicReference<Process?>(null)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun analyze(url: String): VideoInfo = withContext(Dispatchers.IO) {
        val process = startProcess(
            listOf(executable, "--ignore-config", "--no-colors", "--dump-single-json", "--no-playlist", "--", url),
        )
        activeProcess.set(process)
        try {
            coroutineScope {
                val stdout = async(Dispatchers.IO) { process.inputStream.bufferedReader().readText() }
                val stderr = async(Dispatchers.IO) { process.errorStream.bufferedReader().readText() }
                val exitCode = process.waitFor()
                val output = stdout.await()
                val error = stderr.await()
                if (exitCode != 0) throw DownloadException(error.trim().ifBlank { "yt-dlp terminó con código $exitCode." })
                parseVideoInfo(url, output)
            }
        } finally {
            activeProcess.compareAndSet(process, null)
        }
    }

    override suspend fun download(
        request: DownloadRequest,
        onProgress: (DownloadProgress) -> Unit,
        onLog: (String) -> Unit,
    ): DownloadResult = withContext(Dispatchers.IO) {
        val destination = File(request.destination)
        if (!destination.exists() && !destination.mkdirs()) {
            throw DownloadException("No se pudo crear la carpeta de destino.")
        }
        if (!destination.isDirectory) throw DownloadException("El destino seleccionado no es una carpeta.")

        val outputTemplate = File(destination, "%(title)s.%(ext)s").absolutePath
        val arguments = listOf(executable) + YtdlpProtocol.buildDownloadArguments(request, outputTemplate)
        val process = startProcess(arguments)
        activeProcess.set(process)
        var finalPath: String? = null
        onProgress(DownloadProgress(0.0, phase = DownloadPhase.Preparing))

        try {
            coroutineScope {
                val stdout = async(Dispatchers.IO) {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            onLog(line)
                            YtdlpProtocol.parseProgress(line)?.let(onProgress)
                            YtdlpProtocol.outputPath(line)?.let { finalPath = it }
                        }
                    }
                }
                val stderr = async(Dispatchers.IO) {
                    process.errorStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            onLog(line)
                            YtdlpProtocol.parseProgress(line)?.let(onProgress)
                        }
                    }
                }
                val exitCode = process.waitFor()
                stdout.await()
                stderr.await()
                if (exitCode != 0) throw DownloadException("yt-dlp terminó con código $exitCode. Revisa el registro técnico.")
            }
            DownloadResult(finalPath ?: destination.absolutePath)
        } finally {
            activeProcess.compareAndSet(process, null)
        }
    }

    override fun cancel() {
        activeProcess.getAndSet(null)?.let { process ->
            process.destroy()
            if (process.isAlive) process.destroyForcibly()
        }
    }

    private fun startProcess(arguments: List<String>): Process = try {
        ProcessBuilder(arguments)
            .apply { environment()["PAGER"] = "cat" }
            .start()
    } catch (error: IOException) {
        throw DownloadException(
            "No se encontró yt-dlp. Instálalo y asegúrate de que esté disponible en PATH.",
            error,
        )
    }

    private fun parseVideoInfo(url: String, payload: String): VideoInfo {
        try {
            val root = json.parseToJsonElement(payload).jsonObject
            return VideoInfo(
                url = url,
                title = root["title"]?.jsonPrimitive?.content ?: "Título desconocido",
                uploader = root["uploader"]?.jsonPrimitive?.content
                    ?: root["channel"]?.jsonPrimitive?.content
                    ?: "Autor desconocido",
                durationSeconds = root["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                thumbnailUrl = root["thumbnail"]?.jsonPrimitive?.content.orEmpty(),
                maxHeight = root["height"]?.jsonPrimitive?.intOrNull ?: findMaxHeight(root),
            )
        } catch (error: Exception) {
            throw DownloadException("yt-dlp devolvió metadatos que Luna Fetch no pudo interpretar.", error)
        }
    }

    private fun findMaxHeight(root: kotlinx.serialization.json.JsonObject): Int = root["formats"]
        ?.let { element -> element as? kotlinx.serialization.json.JsonArray }
        ?.maxOfOrNull { format -> format.jsonObject["height"]?.jsonPrimitive?.intOrNull ?: 0 }
        ?.takeIf { it > 0 }
        ?: 1080
}
