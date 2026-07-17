package com.biglexj.lunafetch.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormatCatalogTest {
    @Test
    fun videoQualitiesRespectAvailableHeight() {
        val qualities = FormatCatalog.qualities(MediaFormat.Mp4, 720)
        assertEquals(listOf("720p · HD", "480p", "360p"), qualities.map(QualityOption::displayName))
    }

    @Test
    fun audioQualitiesUseAudioSelectors() {
        val qualities = FormatCatalog.qualities(MediaFormat.Mp3, 360)
        assertEquals(3, qualities.size)
        assertTrue(qualities.all { it.formatSelector == "bestaudio/best" })
    }

    @Test
    fun videoSelectorsKeepProgressiveAndGenericFallbacks() {
        val selector = FormatCatalog.qualities(MediaFormat.Mp4, 1080).first().formatSelector
        assertTrue("[ext=mp4]" in selector)
        assertTrue("best[height<=1080]" in selector)
        assertTrue(selector.endsWith("/best"))
    }

    @Test
    fun losslessFlacIsNotOfferedForLossySources() {
        assertFalse(MediaFormat.entries.any { it.extension == "flac" })
    }

    @Test
    fun urlValidationAcceptsOnlyHttpUrlsWithoutWhitespace() {
        assertTrue(LunaFetchPresenter.isSupportedUrl("https://youtu.be/example"))
        assertFalse(LunaFetchPresenter.isSupportedUrl("file:///video.mp4"))
        assertFalse(LunaFetchPresenter.isSupportedUrl("https://bad url.example"))
    }
}
