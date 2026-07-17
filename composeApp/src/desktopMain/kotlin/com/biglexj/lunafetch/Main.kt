package com.biglexj.lunafetch

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.biglexj.lunafetch.feature.LunaFetchApp
import com.biglexj.lunafetch.platform.DesktopPlatformBindings
import lunafetch.composeapp.generated.resources.Res
import lunafetch.composeapp.generated.resources.luna_fetch_icon
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Luna Fetch",
        icon = painterResource(Res.drawable.luna_fetch_icon),
        state = rememberWindowState(width = 1040.dp, height = 780.dp),
    ) {
        LunaFetchApp(remember { DesktopPlatformBindings() })
    }
}
