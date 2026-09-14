/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.blazify.music.viewmodels.LyricsViewModel

@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean,
    lyricsViewModel: LyricsViewModel = hiltViewModel()
) {
    // The newer lyrics view is the only one now. It was already the default, and
    // the old view only survived behind a switch that still called the new one beta.
    ExperimentalLyrics(
        sliderPositionProvider = sliderPositionProvider,
        modifier = modifier,
        showLyrics = showLyrics,
        lyricsViewModel = lyricsViewModel
    )
}
