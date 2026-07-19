package com.biglexj.lunafetch.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.biglexj.lunafetch.core.theme.LunaFetchTheme
import com.biglexj.lunafetch.core.theme.ThemeMode
import com.biglexj.lunafetch.domain.DownloadPhase
import com.biglexj.lunafetch.domain.CollectionEntry
import com.biglexj.lunafetch.domain.LunaFetchPresenter
import com.biglexj.lunafetch.domain.LunaFetchState
import com.biglexj.lunafetch.domain.MediaFormat
import com.biglexj.lunafetch.domain.PlatformBindings
import com.biglexj.lunafetch.domain.QualityOption
import com.biglexj.lunafetch.domain.isCollection
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LunaFetchApp(
    platform: PlatformBindings,
    quickDownloadUrl: String? = null,
    onDismissQuickDownload: () -> Unit = {},
) {
    val presenter = remember(platform) { LunaFetchPresenter(platform) }
    val state by presenter.state.collectAsState()
    var themeMode by remember { mutableStateOf(ThemeMode.System) }

    LunaFetchTheme(themeMode) {
        if (quickDownloadUrl != null) {
            LaunchedEffect(quickDownloadUrl) {
                presenter.setUrl(quickDownloadUrl)
                presenter.analyze()
            }
            QuickDownloadSheet(state, presenter, onDismissQuickDownload)
            return@LunaFetchTheme
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                AppHeader(themeMode, onThemeSelected = { themeMode = it })
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val compact = maxWidth < 720.dp
                    val scroll = rememberScrollState()
                    if (compact) {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            MainCards(state, presenter, platform)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(scroll),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                LinkCard(state, presenter)
                                VideoCard(state, presenter)
                            }
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                DownloadOptionsCard(state, presenter, platform)
                                DownloadStatusCard(state, presenter)
                                LogsCard(state)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun QuickDownloadSheet(
    state: LunaFetchState,
    presenter: LunaFetchPresenter,
    onDismiss: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f)) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Descarga rápida", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                when {
                    state.isAnalyzing -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Text("Analizando enlace…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.video != null -> {
                        val video = state.video
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            CoverThumbnail(video.thumbnailUrl, "Miniatura de ${video.title}", state.selectedFormat.isAudio)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(video.collectionTitle ?: video.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(video.uploader, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (video.isCollection) Text("${video.collectionCount} canciones", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text("Formato", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(MediaFormat.Mp3, MediaFormat.Mp4).forEach { format ->
                                val selected = state.selectedFormat == format
                                if (selected) Button(onClick = { }, modifier = Modifier.weight(1f), enabled = false) { Text(format.displayName) }
                                else OutlinedButton(onClick = { presenter.selectFormat(format) }, modifier = Modifier.weight(1f)) { Text(format.displayName) }
                            }
                        }
                        Text("Calidad: ${state.selectedQuality.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = presenter::download,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !state.isDownloading,
                        ) { Text(if (state.isDownloading) "Descargando…" else if (video.isCollection) "Descargar colección" else "Iniciar descarga") }
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Cancelar") }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun MainCards(state: LunaFetchState, presenter: LunaFetchPresenter, platform: PlatformBindings) {
    LinkCard(state, presenter)
    VideoCard(state, presenter)
    DownloadOptionsCard(state, presenter, platform)
    DownloadStatusCard(state, presenter)
    LogsCard(state)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun AppHeader(mode: ThemeMode, onThemeSelected: (ThemeMode) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 520.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Luna Fetch",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    ThemeModeButton(mode, onThemeSelected)
                }
                Text(
                    "Descarga videos y audio en alta calidad",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Luna Fetch", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Descarga videos y audio en alta calidad",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ThemeModeButton(mode, onThemeSelected)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeButton(currentMode: ThemeMode, onModeSelected: (ThemeMode) -> Unit) {
    val nextMode = when (currentMode) {
        ThemeMode.System -> ThemeMode.Light
        ThemeMode.Light -> ThemeMode.Dark
        ThemeMode.Dark -> ThemeMode.System
    }
    val currentLabel = when (currentMode) {
        ThemeMode.System -> "Sistema"
        ThemeMode.Light -> "Claro"
        ThemeMode.Dark -> "Oscuro"
    }
    val nextLabel = when (nextMode) {
        ThemeMode.System -> "Sistema"
        ThemeMode.Light -> "Claro"
        ThemeMode.Dark -> "Oscuro"
    }
    val actionLabel = "Cambiar al tema $nextLabel"

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(actionLabel) } },
        state = rememberTooltipState(),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .clickable(
                    role = Role.Button,
                    onClickLabel = actionLabel,
                    onClick = { onModeSelected(nextMode) },
                )
                .semantics { contentDescription = "Tema actual: $currentLabel. $actionLabel" },
            contentAlignment = Alignment.Center,
        ) {
            ThemeModeGlyph(
                mode = currentMode,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ThemeModeGlyph(mode: ThemeMode, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val strokeWidth = 1.9.dp.toPx()
        when (mode) {
            ThemeMode.System -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.1f, size.height * 0.16f),
                    size = Size(size.width * 0.8f, size.height * 0.58f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
                drawLine(
                    color,
                    Offset(size.width * 0.5f, size.height * 0.74f),
                    Offset(size.width * 0.5f, size.height * 0.88f),
                    strokeWidth,
                    StrokeCap.Round,
                )
                drawLine(
                    color,
                    Offset(size.width * 0.3f, size.height * 0.88f),
                    Offset(size.width * 0.7f, size.height * 0.88f),
                    strokeWidth,
                    StrokeCap.Round,
                )
            }

            ThemeMode.Light -> {
                drawCircle(color, radius = size.minDimension * 0.19f, style = Stroke(strokeWidth))
                repeat(8) { index ->
                    val angle = Math.toRadians(index * 45.0)
                    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    val center = this.center
                    drawLine(
                        color,
                        center + direction * (size.minDimension * 0.31f),
                        center + direction * (size.minDimension * 0.43f),
                        strokeWidth,
                        StrokeCap.Round,
                    )
                }
            }

            ThemeMode.Dark -> {
                val moon = Path().apply {
                    moveTo(size.width * 0.68f, size.height * 0.08f)
                    cubicTo(
                        size.width * 0.34f,
                        size.height * 0.18f,
                        size.width * 0.22f,
                        size.height * 0.62f,
                        size.width * 0.48f,
                        size.height * 0.86f,
                    )
                    cubicTo(
                        size.width * 0.66f,
                        size.height * 1.02f,
                        size.width * 0.92f,
                        size.height * 0.88f,
                        size.width * 0.96f,
                        size.height * 0.68f,
                    )
                    cubicTo(
                        size.width * 0.66f,
                        size.height * 0.82f,
                        size.width * 0.4f,
                        size.height * 0.56f,
                        size.width * 0.52f,
                        size.height * 0.3f,
                    )
                    cubicTo(
                        size.width * 0.56f,
                        size.height * 0.2f,
                        size.width * 0.62f,
                        size.height * 0.13f,
                        size.width * 0.68f,
                        size.height * 0.08f,
                    )
                    close()
                }
                drawPath(moon, color)
            }
        }
    }
}

@Composable
private fun LinkCard(state: LunaFetchState, presenter: LunaFetchPresenter) = LunaCard {
    Text("Enlace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = state.url,
            onValueChange = presenter::setUrl,
            modifier = Modifier.weight(1f),
            enabled = !state.isAnalyzing && !state.isDownloading,
            singleLine = true,
            label = { Text("URL del video") },
            shape = RoundedCornerShape(14.dp),
        )
        Button(
            onClick = presenter::analyze,
            enabled = !state.isAnalyzing && !state.isDownloading,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(if (state.isAnalyzing) "Analizando…" else "Analizar")
        }
    }
    state.error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun VideoCard(state: LunaFetchState, presenter: LunaFetchPresenter) {
    val video = state.video ?: return
    if (video.isCollection) {
        CollectionEntriesCard(video, state.selectedFormat.isAudio)
        return
    }
    val openModifier = if (state.completedOutput != null) {
        Modifier.clickable(onClick = presenter::openCompletedOutput)
    } else {
        Modifier
    }
    LunaCard(modifier = openModifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val thumbnailModifier = if (state.selectedFormat.isAudio) {
                Modifier.size(88.dp)
            } else {
                Modifier.size(width = 150.dp, height = 88.dp)
            }
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = "Miniatura de ${video.title}",
                modifier = thumbnailModifier.clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = if (state.selectedFormat.isAudio) ContentScale.Crop else ContentScale.Fit,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(video.uploader, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val detail = if (video.isCollection) {
                    "Lista · ${video.collectionCount} elementos"
                } else {
                    formatDuration(video.durationSeconds)
                }
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CollectionEntriesCard(video: com.biglexj.lunafetch.domain.VideoInfo, isAudio: Boolean) {
    val entries = video.collectionEntries
    if (entries.isEmpty()) return

    LunaCard {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverThumbnail(video.thumbnailUrl, "Portada de ${video.collectionTitle ?: "la colección"}", isAudio)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    video.collectionTitle ?: "Lista de reproducción",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(video.uploader, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${entries.size} canciones detectadas",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        entries.forEach { CollectionEntryRow(it) }
    }
}

@Composable
private fun CoverThumbnail(model: String, description: String, isAudio: Boolean) {
    Box(
        modifier = (if (isAudio) Modifier.size(88.dp) else Modifier.size(width = 150.dp, height = 88.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = description,
            modifier = Modifier.fillMaxSize(),
            contentScale = if (isAudio) ContentScale.Crop else ContentScale.Fit,
        )
    }
}

@Composable
private fun CollectionEntryRow(entry: CollectionEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            entry.index.toString().padStart(2, '0'),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (entry.uploader.isNotBlank()) {
                Text(
                    entry.uploader,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (entry.durationSeconds > 0) {
            Text(
                formatDuration(entry.durationSeconds),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DownloadOptionsCard(
    state: LunaFetchState,
    presenter: LunaFetchPresenter,
    platform: PlatformBindings,
) = LunaCard {
    Text("Descarga", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Selector(
            label = "Formato",
            selected = state.selectedFormat.displayName,
            options = MediaFormat.entries,
            optionLabel = MediaFormat::displayName,
            onSelected = presenter::selectFormat,
            modifier = Modifier.weight(1f),
            enabled = !state.isDownloading,
        )
        Selector(
            label = "Calidad",
            selected = state.selectedQuality.displayName,
            options = state.qualities,
            optionLabel = QualityOption::displayName,
            onSelected = presenter::selectQuality,
            modifier = Modifier.weight(1f),
            enabled = !state.isDownloading,
        )
    }
    if (state.selectedFormat.isAudio) {
        Spacer(Modifier.height(10.dp))
        Text(
            "Incluye los metadatos y portada disponibles.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    state.video?.takeIf { it.isCollection }?.let { collection ->
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Descargar colección completa", style = MaterialTheme.typography.labelLarge)
                Text(
                    "${collection.collectionCount} elementos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.downloadCollection,
                onCheckedChange = presenter::setDownloadCollection,
                enabled = !state.isDownloading,
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Destino", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
    OutlinedButton(
        onClick = presenter::chooseDestination,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = !state.isDownloading,
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            platform.destinationLabel(state.destination),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(Modifier.height(14.dp))
    Button(
        onClick = presenter::download,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        enabled = state.video != null && !state.isDownloading,
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            if (state.downloadCollection) "Descargar colección"
            else "Descargar ${state.selectedFormat.extension.uppercase()}",
        )
    }
}

@Composable
private fun DownloadStatusCard(state: LunaFetchState, presenter: LunaFetchPresenter) {
    val progress = state.progress ?: return
    LunaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(progress.statusMessage, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("${progress.percentage.toInt()} %", color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (progress.percentage / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        )
        if (progress.speed.isNotBlank() || progress.size.isNotBlank() || progress.eta.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                listOfNotNull(
                    progress.speed.takeIf(String::isNotBlank),
                    progress.size.takeIf(String::isNotBlank),
                    progress.eta.takeIf(String::isNotBlank)?.let { "ETA $it" },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.isDownloading) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = presenter::cancel, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Cancelar")
            }
        } else if (progress.phase == DownloadPhase.Completed && state.completedOutput != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = presenter::openCompletedOutput) { Text("Abrir resultado") }
        }
    }
}

@Composable
private fun LogsCard(state: LunaFetchState) {
    if (state.logs.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    LunaCard {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (expanded) "Ocultar registro técnico" else "Ver registro técnico (${state.logs.size})")
        }
        if (expanded) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                Text(
                    state.logs.joinToString("\n"),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun <T> Selector(
    label: String,
    selected: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selected, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun LunaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), content = content)
    }
}

private fun formatDuration(seconds: Double): String {
    val total = seconds.toLong().coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val remaining = total % 60
    val minuteText = minutes.toString().padStart(2, '0')
    val secondText = remaining.toString().padStart(2, '0')
    return if (hours > 0) "$hours:$minuteText:$secondText" else "$minutes:$secondText"
}
