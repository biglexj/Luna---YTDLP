package com.biglexj.lunafetch.domain

object YtdlpProtocol {
    const val ProgressPrefix = "LUNAFETCH_PROGRESS|"
    const val OutputPrefix = "LUNAFETCH_FILE|"

    private val classicProgress = Regex(
        """\[download]\s+(\d+(?:\.\d+)?)%\s+of\s+(?:~)?([^\s]+)\s+at\s+([^\s]+)\s+ETA\s+([^\s]+)""",
        RegexOption.IGNORE_CASE,
    )

    fun parseProgress(line: String): DownloadProgress? {
        val clean = line.trim()
        if (clean.startsWith(ProgressPrefix)) {
            val fields = clean.removePrefix(ProgressPrefix).split('|')
            val percentage = fields.getOrNull(0)
                ?.replace("%", "")
                ?.trim()
                ?.toDoubleOrNull()
                ?: return null
            return DownloadProgress(
                percentage = percentage.coerceIn(0.0, 100.0),
                size = fields.getOrNull(1).normalizedMetric(),
                speed = fields.getOrNull(2).normalizedMetric(),
                eta = fields.getOrNull(3).normalizedMetric(),
            )
        }

        val match = classicProgress.find(clean)
        if (match != null) {
            return DownloadProgress(
                percentage = match.groupValues[1].toDouble().coerceIn(0.0, 100.0),
                size = match.groupValues[2],
                speed = match.groupValues[3],
                eta = match.groupValues[4],
            )
        }

        if (clean.contains("[Merger]", true) ||
            clean.contains("[ExtractAudio]", true) ||
            clean.contains("[VideoConvertor]", true)
        ) {
            return DownloadProgress(100.0, phase = DownloadPhase.Processing)
        }
        return null
    }

    fun outputPath(line: String): String? = line.trim()
        .takeIf { it.startsWith(OutputPrefix) }
        ?.removePrefix(OutputPrefix)
        ?.takeIf { it.isNotBlank() }

    fun buildDownloadArguments(request: DownloadRequest, outputTemplate: String): List<String> = buildList {
        addAll(
            listOf(
                "--ignore-config",
                "--no-colors",
                "--newline",
                "--progress",
                "--progress-template",
                "download:${ProgressPrefix}%(progress._percent_str)s|%(progress._total_bytes_str)s|%(progress._speed_str)s|%(progress._eta_str)s",
                "--print",
                "after_move:${OutputPrefix}%(filepath)s",
                "-f",
                request.quality.formatSelector,
            ),
        )
        if (request.format.isAudio) {
            addAll(listOf("-x", "--audio-format", request.format.extension))
            request.quality.audioQuality?.let { addAll(listOf("--audio-quality", it)) }
        } else {
            addAll(listOf("--merge-output-format", request.format.extension))
        }
        addAll(listOf("-o", outputTemplate, "--no-playlist", "--", request.url))
    }

    private fun String?.normalizedMetric(): String = this
        ?.trim()
        ?.takeUnless { it.isBlank() || it.equals("NA", true) || it.equals("N/A", true) }
        .orEmpty()
}
