package com.biglexj.lunafetch.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.biglexj.lunafetch.domain.DownloadEngine
import com.biglexj.lunafetch.domain.PlatformBindings

class AndroidPlatformBindings(
    context: Context,
    private val directoryPicker: suspend (Uri?) -> Uri?,
) : PlatformBindings {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("lunafetch", Context.MODE_PRIVATE)
    override val engine: DownloadEngine = AndroidDownloadEngine(appContext)
    override val defaultDestination: String
        get() = preferences.getString("downloadTree", "").orEmpty()

    override suspend fun chooseDestination(current: String): String? = directoryPicker(
        current.takeIf(String::isNotBlank)?.let(Uri::parse),
    )?.toString()

    override fun destinationLabel(destination: String): String {
        if (destination.isBlank()) return "Seleccionar carpeta"
        return Uri.decode(Uri.parse(destination).lastPathSegment.orEmpty())
            .substringAfterLast(':')
            .ifBlank { "Carpeta seleccionada" }
    }

    override fun rememberDestination(destination: String) {
        preferences.edit().putString("downloadTree", destination).apply()
    }

    override fun openOutput(path: String) {
        val uri = Uri.parse(path)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, appContext.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { appContext.startActivity(intent) }.onFailure {
            appContext.startActivity(
                Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
