package com.biglexj.lunafetch.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YtdlpProtocolTest {
    @Test
    fun parsesStableProgressTemplate() {
        val progress = YtdlpProtocol.parseProgress("LUNAFETCH_PROGRESS| 42.5%|50.2MiB|3.4MiB/s|00:12")
        assertEquals(42.5, progress?.percentage)
        assertEquals("50.2MiB", progress?.size)
        assertEquals("3.4MiB/s", progress?.speed)
        assertEquals("00:12", progress?.eta)
    }

    @Test
    fun recognizesPostProcessing() {
        val progress = YtdlpProtocol.parseProgress("[Merger] Merging formats into output.mp4")
        assertEquals(DownloadPhase.Processing, progress?.phase)
    }

    @Test
    fun argumentsRemainSeparatedAndEndWithUrl() {
        val request = DownloadRequest(
            url = "https://example.com/watch?v=1&list=2",
            destination = "unused",
            format = MediaFormat.Mp3,
            quality = FormatCatalog.qualities(MediaFormat.Mp3, 1080).first(),
        )
        val arguments = YtdlpProtocol.buildDownloadArguments(request, "C:/Downloads/%(title)s.%(ext)s")
        assertTrue("-x" in arguments)
        assertTrue("--audio-format" in arguments)
        assertFalse(arguments.any { it.contains("\"https://") })
        assertEquals(request.url, arguments.last())
    }

    @Test
    fun extractsFinalPath() {
        assertEquals("D:/Downloads/video.mp4", YtdlpProtocol.outputPath("LUNAFETCH_FILE|D:/Downloads/video.mp4"))
    }
}
