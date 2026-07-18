package com.biglexj.lunafetch.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LunaFetchState(
    val url: String = "",
    val destination: String = "",
    val video: VideoInfo? = null,
    val selectedFormat: MediaFormat = MediaFormat.Mp4,
    val qualities: List<QualityOption> = FormatCatalog.qualities(MediaFormat.Mp4, 1080),
    val selectedQuality: QualityOption = qualities.first(),
    val downloadCollection: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isDownloading: Boolean = false,
    val progress: DownloadProgress? = null,
    val logs: List<String> = emptyList(),
    val error: String? = null,
    val completedOutput: String? = null,
)

class LunaFetchPresenter(
    private val platform: PlatformBindings,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val _state = MutableStateFlow(LunaFetchState(destination = platform.defaultDestination))
    val state: StateFlow<LunaFetchState> = _state.asStateFlow()
    private var operation: Job? = null

    fun setUrl(value: String) = _state.update { it.copy(url = value, error = null) }

    fun selectFormat(format: MediaFormat) {
        _state.update { current ->
            val qualities = FormatCatalog.qualities(format, current.video?.maxHeight ?: 1080)
            current.copy(selectedFormat = format, qualities = qualities, selectedQuality = qualities.first())
        }
    }

    fun selectQuality(quality: QualityOption) = _state.update { it.copy(selectedQuality = quality) }

    fun setDownloadCollection(value: Boolean) = _state.update { current ->
        current.copy(downloadCollection = value && current.video?.isCollection == true)
    }

    fun analyze() {
        val url = state.value.url.trim()
        if (!isSupportedUrl(url)) {
            _state.update { it.copy(error = "Escribe una URL http o https válida.") }
            return
        }
        operation?.cancel()
        operation = scope.launch {
            _state.update { it.copy(isAnalyzing = true, video = null, error = null, completedOutput = null) }
            runCatching { platform.engine.analyze(url) }
                .onSuccess { video ->
                    _state.update { current ->
                        val qualities = FormatCatalog.qualities(current.selectedFormat, video.maxHeight)
                        current.copy(
                            video = video,
                            qualities = qualities,
                            selectedQuality = qualities.first(),
                            downloadCollection = video.isCollection,
                            isAnalyzing = false,
                        )
                    }
                }
                .onFailure { error ->
                    if (error !is CancellationException) {
                        _state.update { it.copy(isAnalyzing = false, error = error.userMessage("No se pudo analizar el enlace.")) }
                    }
                }
        }
    }

    fun chooseDestination() {
        operation = scope.launch {
            platform.chooseDestination(state.value.destination)?.let { destination ->
                platform.rememberDestination(destination)
                _state.update { it.copy(destination = destination, error = null) }
            }
        }
    }

    fun download() {
        val current = state.value
        val video = current.video ?: run {
            _state.update { it.copy(error = "Analiza un enlace antes de descargar.") }
            return
        }
        if (current.destination.isBlank()) {
            _state.update { it.copy(error = "Selecciona una carpeta de destino.") }
            return
        }

        val request = DownloadRequest(
            url = video.url,
            destination = current.destination,
            format = current.selectedFormat,
            quality = current.selectedQuality,
            downloadCollection = current.downloadCollection,
        )
        operation?.cancel()
        operation = scope.launch {
            _state.update {
                it.copy(
                    isDownloading = true,
                    progress = DownloadProgress(0.0, phase = DownloadPhase.Preparing),
                    logs = emptyList(),
                    error = null,
                    completedOutput = null,
                )
            }
            try {
                val result = platform.engine.download(
                    request = request,
                    onProgress = { progress -> _state.update { it.copy(progress = progress) } },
                    onLog = ::appendLog,
                )
                _state.update {
                    it.copy(
                        isDownloading = false,
                        progress = DownloadProgress(100.0, phase = DownloadPhase.Completed),
                        completedOutput = result.openPath,
                    )
                }
            } catch (cancelled: CancellationException) {
                _state.update {
                    it.copy(isDownloading = false, progress = DownloadProgress(0.0, phase = DownloadPhase.Cancelled))
                }
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        isDownloading = false,
                        error = error.userMessage("La descarga no pudo completarse."),
                    )
                }
            }
        }
    }

    fun cancel() {
        platform.engine.cancel()
        operation?.cancel()
    }

    fun openCompletedOutput() = state.value.completedOutput?.let(platform::openOutput)

    private fun appendLog(line: String) {
        if (line.isBlank()) return
        _state.update { current -> current.copy(logs = (current.logs + line).takeLast(400)) }
    }

    companion object {
        fun isSupportedUrl(value: String): Boolean {
            val normalized = value.trim().lowercase()
            return (normalized.startsWith("https://") || normalized.startsWith("http://")) &&
                normalized.length > 10 && !normalized.any(Char::isWhitespace)
        }
    }
}

private fun Throwable.userMessage(fallback: String): String = message
    ?.takeIf { it.isNotBlank() }
    ?: fallback
