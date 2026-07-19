package com.biglexj.lunafetch.platform

import com.biglexj.lunafetch.domain.DownloadEngine
import com.biglexj.lunafetch.domain.DownloadException
import com.biglexj.lunafetch.domain.DownloadPhase
import com.biglexj.lunafetch.domain.DownloadProgress
import com.biglexj.lunafetch.domain.DownloadRequest
import com.biglexj.lunafetch.domain.DownloadResult
import com.biglexj.lunafetch.domain.CollectionEntry
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.contentOrNull
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
            listOf(
                executable,
                "--ignore-config",
                "--no-colors",
                "--dump-single-json",
                "--flat-playlist",
                "--yes-playlist",
                "--",
                url,
            ),
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

        val outputTemplate = File(
            destination,
            if (request.downloadCollection) "%(playlist_index)03d - %(title)s.%(ext)s" else "%(title)s.%(ext)s",
        ).absolutePath
        val arguments = listOf(executable) + YtdlpProtocol.buildDownloadArguments(request, outputTemplate)
        val process = startProcess(arguments)
        activeProcess.set(process)
        val finalPaths = mutableListOf<String>()
        onProgress(DownloadProgress(0.0, phase = DownloadPhase.Preparing))

        try {
            coroutineScope {
                val stdout = async(Dispatchers.IO) {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            onLog(line)
                            YtdlpProtocol.parseProgress(line)?.let(onProgress)
                            YtdlpProtocol.outputPath(line)?.let(finalPaths::add)
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
            val completedPaths = finalPaths.distinct().ifEmpty {
                destination.walkTopDown()
                    .filter(File::isFile)
                    .filterNot { it.extension.equals("part", true) }
                    .map { it.absolutePath }
                    .toList()
            }
            DownloadResult(
                outputPaths = completedPaths,
                openPath = if (completedPaths.size == 1) completedPaths.first() else destination.absolutePath,
            )
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
            val entries = collectionEntries(root)
            val isCollection = entries.size > 1

            val thumbnail = if (isCollection) {
                root.thumbnail().ifBlank {
                    (root["entries"] as? kotlinx.serialization.json.JsonArray)?.firstYoutubeThumbnail().orEmpty()
                }
            } else {
                root.thumbnail()
            }

            return VideoInfo(
                url = url,
                title = root.string("title").ifBlank { "Título desconocido" },
                uploader = root.string("uploader").ifBlank { root.string("channel").ifBlank { "Autor desconocido" } },
                durationSeconds = root["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                thumbnailUrl = thumbnail,
                maxHeight = root["height"]?.jsonPrimitive?.intOrNull ?: findMaxHeight(root),
                collectionTitle = collectionTitle(root),
                collectionCount = collectionCount(root),
                collectionEntries = entries,
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

    private fun collectionTitle(root: kotlinx.serialization.json.JsonObject): String? =
        root["playlist_title"]?.jsonPrimitive?.contentOrNull
            ?: root.takeIf { it["_type"]?.jsonPrimitive?.contentOrNull == "playlist" }
                ?.get("title")?.jsonPrimitive?.contentOrNull

    private fun collectionCount(root: kotlinx.serialization.json.JsonObject): Int =
        root["playlist_count"]?.jsonPrimitive?.intOrNull
            ?: (root["entries"] as? kotlinx.serialization.json.JsonArray)?.size
            ?: 0

    private fun collectionEntries(root: kotlinx.serialization.json.JsonObject): List<CollectionEntry> =
        (root["entries"] as? kotlinx.serialization.json.JsonArray)
            ?.mapIndexedNotNull { index, element ->
                val entry = element as? kotlinx.serialization.json.JsonObject ?: return@mapIndexedNotNull null
                CollectionEntry(
                    index = entry["playlist_index"]?.jsonPrimitive?.intOrNull ?: index + 1,
                    title = entry["title"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null,
                    uploader = entry["uploader"]?.jsonPrimitive?.contentOrNull
                        ?: entry["channel"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    durationSeconds = entry["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                )
            }
            .orEmpty()

    private fun kotlinx.serialization.json.JsonObject.string(name: String): String =
        this[name]?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun kotlinx.serialization.json.JsonObject.thumbnail(): String {
        val directThumbnail = string("thumbnail")
        if (directThumbnail.isNotBlank() && (!directThumbnail.contains("/s_p/") || directThumbnail.contains("?"))) {
            return directThumbnail
        }
        val list = this["thumbnails"]?.jsonArray
            ?.mapNotNull {
                val u = it.jsonObject.string("url")
                if (u.contains("/s_p/") && !u.contains("?")) null else u.takeIf { it.isNotBlank() }
            }
            .orEmpty()
        return list.lastOrNull() ?: directThumbnail
    }

    private fun kotlinx.serialization.json.JsonArray.firstYoutubeThumbnail(): String =
        firstOrNull()?.jsonObject?.string("id")
            ?.takeIf { it.isNotBlank() }
            ?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
            .orEmpty()
}
