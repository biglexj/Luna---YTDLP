package com.biglexj.lunafetch.domain

interface DownloadEngine {
    suspend fun analyze(url: String): VideoInfo

    suspend fun download(
        request: DownloadRequest,
        onProgress: (DownloadProgress) -> Unit,
        onLog: (String) -> Unit,
    ): DownloadResult

    fun cancel()
}

interface PlatformBindings {
    val engine: DownloadEngine
    val defaultDestination: String

    suspend fun chooseDestination(current: String): String?
    fun destinationLabel(destination: String): String
    fun rememberDestination(destination: String)
    fun openOutput(path: String)
}
