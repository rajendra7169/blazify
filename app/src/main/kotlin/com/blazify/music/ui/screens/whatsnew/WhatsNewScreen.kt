/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.blazify.music.BuildConfig
import com.blazify.music.LocalChangelogState
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.whatsnew.FeatureEntry
import com.blazify.music.whatsnew.WhatsNewCatalog

private const val CARD = "card"
private val MaxPageWidth = 600.dp

/** How many demos may animate at once; the rest hold their still frame. */
private const val PLAYING_AT_ONCE = 2

/**
 * The What's new page: the new features first, then Blazify's main features, then the full
 * release notes. The same page opens full screen after an update ([WhatsNewDialog]) and from
 * Settings ([WhatsNewScreen]).
 */
@Composable
fun WhatsNewPage(
    version: String,
    newEntries: List<FeatureEntry>,
    onClose: () -> Unit,
    onOpenReleaseNotes: () -> Unit,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    onPreviewPopup: (() -> Unit)? = null,
) {
    val core = remember { WhatsNewCatalog.availableCore() }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Changes only when a different card becomes one of the most visible, not on every scroll frame.
    val playing by remember(listState) { derivedStateOf { mostVisibleCards(listState, PLAYING_AT_ONCE) } }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(windowInsets),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.whats_new_title, version),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close),
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (newEntries.isNotEmpty()) {
                item(key = "header_new", contentType = "header") {
                    SectionHeader(stringResource(R.string.whats_new_section_new))
                }
                items(newEntries, key = { "new_${it.id}" }, contentType = { CARD }) { entry ->
                    FeatureCard(
                        entry = entry,
                        playing = "new_${entry.id}" in playing,
                        modifier = Modifier.widthIn(max = MaxPageWidth),
                    )
                }
            }

            item(key = "header_core", contentType = "header") {
                SectionHeader(stringResource(R.string.whats_new_section_core))
            }
            val shown = if (showAll) core else core.take(WhatsNewCatalog.HIGHLIGHT_COUNT)
            items(shown, key = { "core_${it.id}" }, contentType = { CARD }) { entry ->
                FeatureCard(
                    entry = entry,
                    playing = "core_${entry.id}" in playing,
                    modifier = Modifier.widthIn(max = MaxPageWidth),
                )
            }
            if (core.size > WhatsNewCatalog.HIGHLIGHT_COUNT) {
                item(key = "see_all", contentType = "see_all") {
                    SeeAllRow(expanded = showAll, onClick = { showAll = !showAll })
                }
            }

            item(key = "release_notes", contentType = "release_notes") {
                TextButton(onClick = onOpenReleaseNotes) {
                    Text(stringResource(R.string.whats_new_release_notes))
                }
            }
            if (onPreviewPopup != null) {
                item(key = "preview_popup", contentType = "preview_popup") {
                    TextButton(onClick = onPreviewPopup) {
                        Text(stringResource(R.string.whats_new_preview_popup))
                    }
                }
            }
        }
    }
}

/** Full screen over everything, mini player and navigation bar included, for after an update. */
@Composable
fun WhatsNewDialog(
    newEntries: List<FeatureEntry>,
    onDismiss: () -> Unit,
    onOpenReleaseNotes: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        WhatsNewPage(
            version = BuildConfig.VERSION_NAME,
            newEntries = newEntries,
            onClose = onDismiss,
            onOpenReleaseNotes = onOpenReleaseNotes,
            windowInsets = WindowInsets.safeDrawing,
        )
    }
}

/**
 * Settings › About › What's new: the latest release's features, whatever was seen before.
 * Debug builds show every card in the catalog instead, the next release's included, so they
 * can be checked before that version number exists.
 */
@Composable
fun WhatsNewScreen(navController: NavController) {
    val showChangelog = LocalChangelogState.current
    val entries =
        remember {
            if (BuildConfig.DEBUG) {
                WhatsNewCatalog.previewEntries()
            } else {
                WhatsNewCatalog.newEntries(WhatsNewCatalog.latestUpTo(BuildConfig.VERSION_NAME))
            }
        }
    val version =
        if (BuildConfig.DEBUG) {
            WhatsNewCatalog.releases.firstOrNull()?.version ?: BuildConfig.VERSION_NAME
        } else {
            BuildConfig.VERSION_NAME
        }
    var previewPopup by remember { mutableStateOf(false) }
    WhatsNewPage(
        version = version,
        newEntries = entries,
        onClose = { navController.navigateUp() },
        onOpenReleaseNotes = { showChangelog.value = true },
        windowInsets = LocalPlayerAwareWindowInsets.current,
        onPreviewPopup = if (BuildConfig.DEBUG) ({ previewPopup = true }) else null,
    )
    if (previewPopup) {
        WhatsNewPopup(
            version = version,
            entries = entries,
            onSkip = { previewPopup = false },
            onSeeMore = { previewPopup = false },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .widthIn(max = MaxPageWidth)
                .fillMaxWidth()
                .padding(start = 4.dp, top = 12.dp)
                .semantics { heading() },
    )
}

@Composable
private fun SeeAllRow(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .widthIn(max = MaxPageWidth)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(if (expanded) R.string.whats_new_see_fewer else R.string.whats_new_see_all),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(if (expanded) R.drawable.expand_less else R.drawable.expand_more),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** The keys of the [count] cards most on screen, of those at least half on screen. */
private fun mostVisibleCards(
    state: LazyListState,
    count: Int,
): Set<Any> {
    val info = state.layoutInfo
    val start = info.viewportStartOffset
    val end = info.viewportEndOffset
    return info.visibleItemsInfo
        .asSequence()
        .filter { it.contentType == CARD && it.size > 0 }
        .map { item ->
            val onScreen = (minOf(end, item.offset + item.size) - maxOf(start, item.offset)).coerceAtLeast(0)
            item.key to onScreen.toFloat() / item.size
        }.filter { (_, share) -> share >= 0.5f }
        .sortedByDescending { (_, share) -> share }
        .take(count)
        .map { (key, _) -> key }
        .toSet()
}
