/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.settings

import com.blazify.music.ui.component.BlazeLoader
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.blazify.music.R
import com.blazify.music.BuildConfig
import com.blazify.music.utils.BundledChangelog
import com.blazify.music.utils.ReleaseInfo
import com.blazify.music.utils.Updater

// Links, mentions, and the two emphases release notes are actually written in.
// Emphasis used to be missing here, which did not leave it unstyled — it left the
// asterisks on screen, in a page whose whole job is to be read.
private val markdownInlineRegex = Regex(
    "(\\*\\*[^*\\n]+\\*\\*)" +
        "|(\\*[^*\\n]+\\*)" +
        "|(@[a-zA-Z0-9_-]+)" +
        "|(https?://[\\w-]+(\\.[\\w-]+)+[\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChangelogScreen(
    onDismiss: () -> Unit
) {
    var releases by remember { mutableStateOf<List<ReleaseInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        // What shipped in this build, which is known without asking anybody.
        val bundled = BundledChangelog.read(context)
        releases = bundled
        isLoading = bundled.isEmpty()

        // Then whatever the site can add to it — older releases, mostly. It is no
        // longer the only source, so a private repository or no connection at all
        // costs the earlier entries rather than the whole page.
        Updater.getAllReleases().onSuccess { allReleases ->
            val extra = allReleases.filter { release ->
                Updater.compareVersions(BuildConfig.VERSION_NAME, release.tagName) >= 0 &&
                    bundled.none { it.tagName == release.tagName }
            }
            releases = (bundled + extra).sortedWith { a, b ->
                Updater.compareVersions(b.tagName, a.tagName)
            }
        }
        isLoading = false
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    val showFab by remember {
        derivedStateOf { sheetState.targetValue != SheetValue.Hidden }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.changelog),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    val density = LocalDensity.current
                    val stroke = remember(density) {
                        Stroke(width = with(density) { 3.dp.toPx() }, cap = StrokeCap.Round)
                    }
                    LinearWavyProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                        stroke = stroke,
                        trackStroke = stroke,
                        amplitude = { 1f }
                    )
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            BlazeLoader()
                        }
                    }
                } else if (releases.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.changelog_empty))
                        }
                    }
                } else {
                    items(releases) { release ->
                        ReleaseItem(release)
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                val githubReleasesUrl = stringResource(R.string.github_releases_url)
                ExtendedFloatingActionButton(
                    onClick = { uriHandler.openUri(githubReleasesUrl) },
                    icon = { Icon(painterResource(R.drawable.github), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    text = { Text(stringResource(R.string.view_on_github)) },
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ReleaseItem(release: ReleaseInfo) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = release.tagName,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = release.releaseDate.split("T").firstOrNull() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                MarkdownText(release.description)
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun MarkdownText(text: String) {
    val lines = text.split("\n")
    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.filter { it.isNotBlank() }.forEach { line ->
            val trimmedLine = line.trim()

            if (trimmedLine.startsWith("#")) {
                val level = trimmedLine.takeWhile { it == '#' }.length
                val headerText = trimmedLine.substring(level).trim()
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = headerText,
                        style = when (level) {
                            1 -> MaterialTheme.typography.headlineMedium
                            2 -> MaterialTheme.typography.headlineSmall
                            else -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val isListItem = trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ")
                val contentText = if (isListItem) {
                    trimmedLine.substring(2).trim()
                } else {
                    trimmedLine
                }

                val linkColour = MaterialTheme.colorScheme.primary
                val annotatedString = buildAnnotatedString {
                    var lastIndex = 0
                    markdownInlineRegex.findAll(contentText).forEach { result ->
                        append(contentText.substring(lastIndex, result.range.first))

                        val match = result.value
                        when {
                            match.startsWith("**") && match.endsWith("**") ->
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(match.removeSurrounding("**"))
                                }

                            match.startsWith("*") && match.endsWith("*") ->
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(match.removeSurrounding("*"))
                                }

                            else -> {
                                val isMention = match.startsWith("@")
                                val link = if (isMention) "https://github.com/${match.substring(1)}" else match
                                pushStringAnnotation(tag = "URL", annotation = link)
                                withStyle(
                                    style = SpanStyle(
                                        color = linkColour,
                                        fontWeight = if (isMention) FontWeight.Bold else FontWeight.Normal,
                                        textDecoration = if (isMention) TextDecoration.None else TextDecoration.Underline,
                                    ),
                                ) {
                                    append(match)
                                }
                                pop()
                            }
                        }
                        lastIndex = result.range.last + 1
                    }
                    append(contentText.substring(lastIndex))
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (isListItem) {
                            Text(
                                text = stringResource(R.string.list_bullet),
                                modifier = Modifier.padding(end = 8.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        ClickableText(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            onClick = { offset ->
                                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        uriHandler.openUri(annotation.item)
                                    }
                            }
                        )
                    }
                    
                    if (isListItem) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}
