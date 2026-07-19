package com.biglexj.lunafetch

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.biglexj.lunafetch.feature.LunaFetchApp
import com.biglexj.lunafetch.platform.AndroidPlatformBindings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var pendingDirectory: CompletableDeferred<Uri?>? = null

    private val directoryPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        pendingDirectory?.complete(uri)
        pendingDirectory = null
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val bindings = remember { AndroidPlatformBindings(this, ::chooseDirectory) }
            LunaFetchApp(bindings)
        }
    }

    private suspend fun chooseDirectory(initial: Uri?): Uri? = withContext(Dispatchers.Main) {
        pendingDirectory?.complete(null)
        val result = CompletableDeferred<Uri?>()
        pendingDirectory = result
        directoryPicker.launch(initial)
        result.await()
    }

}
