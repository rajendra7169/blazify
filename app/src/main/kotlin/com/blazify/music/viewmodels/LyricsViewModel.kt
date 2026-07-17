/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blazify.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.blazify.music.lyrics.LyricsEntry
import com.blazify.music.lyrics.LyricsUtils
import com.blazify.music.ui.component.LyricsListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class LyricsViewModel @Inject constructor() : ViewModel() {
    companion object {
        private val timestampRegex = Regex("\\[\\d{1,2}:\\d{2}")
    }

    private var processJob: kotlinx.coroutines.Job? = null

    private val _lines = MutableStateFlow<List<LyricsEntry>>(emptyList())
    val lines: StateFlow<List<LyricsEntry>> = _lines.asStateFlow()

    private val _mergedLyricsList = MutableStateFlow<List<LyricsListItem>>(emptyList())
    val mergedLyricsList: StateFlow<List<LyricsListItem>> = _mergedLyricsList.asStateFlow()

    fun processLyrics(
        lyrics: String?,
        enabledLanguages: List<String>,
        romanizeCyrillicByLine: Boolean,
        showIntervalIndicator: Boolean,
        songDurationSec: Int = 0
    ) {
        processJob?.cancel()
        processJob = viewModelScope.launch {
            val processedLines = withContext(Dispatchers.Default) {
                if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
                    emptyList()
                } else {
                    val isLrc = timestampRegex.containsMatchIn(lyrics)
                    val parsedLines = if (isLrc) LyricsUtils.parseLyrics(lyrics) else emptyList()
                    
                    if (parsedLines.isNotEmpty()) {
                        listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parsedLines
                    } else {
                        // Fallback for unsynced or invalid LRC
                        val baseTime = 1000000L
                        lyrics.lines()
                            .filter { it.isNotBlank() && !timestampRegex.containsMatchIn(it) }
                            .mapIndexed { index, line ->
                                LyricsEntry(baseTime + index, line)
                            }
                    }
                }
            }
            
            _lines.value = processedLines
            // Only show interlude indicators when the synced timeline actually fits
            // the edition that's playing. A timeline timed to a longer/shorter edit
            // (Apple's full version vs a YouTube remix/cut) puts the instrumental
            // gaps in the wrong places, so the interlude logo "fills" over the vocals
            // and reads as loading. In that case, suppress the indicators.
            val showInterval = showIntervalIndicator && timelineFitsForInterludes(processedLines, songDurationSec)
            updateMergedList(processedLines, showInterval)

            // Romanize in the background after the UI has been updated
            if (lyrics != null && lyrics != LYRICS_NOT_FOUND && enabledLanguages.isNotEmpty()) {
                launch(Dispatchers.Default) {
                    processedLines.forEach { entry ->
                        if (entry == LyricsEntry.HEAD_LYRICS_ENTRY) return@forEach
                        entry.romanizedTextFlow.value = LyricsUtils.romanize(
                            text = lyrics,
                            line = entry.text,
                            enabledLanguages = enabledLanguages,
                            romanizeCyrillicByLine = romanizeCyrillicByLine
                        )
                    }
                }
            }
        }
    }

    /**
     * Whether the synced timeline lands close enough to the playing edit for its
     * instrumental gaps (interludes) to be trustworthy. If the last line's
     * timestamp is far past / short of the song length, the lyrics are timed to a
     * different edition and the gaps would be misplaced. Unknown duration → allow.
     */
    private fun timelineFitsForInterludes(lines: List<LyricsEntry>, durationSec: Int): Boolean {
        if (durationSec <= 0) return true
        val lastMs = lines.filter { it.text.isNotBlank() }.maxOfOrNull { it.time } ?: return true
        val durationMs = durationSec * 1000L
        return lastMs <= durationMs * 1.2 && lastMs >= durationMs * 0.5
    }

    private fun updateMergedList(lines: List<LyricsEntry>, showIntervalIndicator: Boolean) {
        val result = mutableListOf<LyricsListItem>()
        if (lines.isEmpty()) {
            _mergedLyricsList.value = result
            return
        }
        lines.forEachIndexed { i, entry ->
            if (entry.text.isNotBlank()) {
                result.add(LyricsListItem.Line(i, entry))
            }
            if (showIntervalIndicator && i < lines.size - 1) {
                val nextStart = lines[i + 1].time
                val currentEnd = if (!entry.words.isNullOrEmpty()) {
                    (entry.words.last().endTime * 1000).toLong()
                } else if (entry.text.isBlank()) {
                    entry.time
                } else {
                    null
                }

                if (currentEnd != null && currentEnd < nextStart) {
                    val gap = nextStart - currentEnd
                    if (gap > 4000L) {
                        result.add(LyricsListItem.Indicator(i, gap, currentEnd, nextStart, lines[i + 1].agent))
                    }
                }
            }
        }
        _mergedLyricsList.value = result
    }
}
