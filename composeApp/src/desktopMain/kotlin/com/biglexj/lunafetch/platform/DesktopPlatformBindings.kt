package com.biglexj.lunafetch.platform

import com.biglexj.lunafetch.domain.DownloadEngine
import com.biglexj.lunafetch.domain.PlatformBindings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.util.prefs.Preferences
import javax.swing.JFileChooser

class DesktopPlatformBindings : PlatformBindings {
    private val preferences = Preferences.userRoot().node("com/biglexj/lunafetch")
    override val engine: DownloadEngine = DesktopDownloadEngine()
    override val defaultDestination: String
        get() = preferences.get("downloadDirectory", systemDownloadsDirectory())

    override suspend fun chooseDestination(current: String): String? = withContext(Dispatchers.IO) {
        val chooser = JFileChooser(current.takeIf { it.isNotBlank() } ?: systemDownloadsDirectory()).apply {
            dialogTitle = "Selecciona la carpeta de destino"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile.absolutePath else null
    }

    override fun destinationLabel(destination: String): String = destination.ifBlank { "Seleccionar carpeta" }

    override fun rememberDestination(destination: String) {
        preferences.put("downloadDirectory", destination)
    }

    override fun openOutput(path: String) {
        val target = File(path)
        val openTarget = if (target.exists()) target else target.parentFile
        if (openTarget != null && openTarget.exists() && Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(openTarget)
        }
    }

    private fun systemDownloadsDirectory(): String {
        val home = System.getProperty("user.home") ?: "."
        return File(home, "Downloads").absolutePath
    }
}
