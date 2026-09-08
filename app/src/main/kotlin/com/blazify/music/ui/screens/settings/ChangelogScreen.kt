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
import androidx.compose.ui.unit.sp

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


/**
 * Release notes, with the parts this cannot draw taken out.
 *
 * Notes are written for the releases page first, where GitHub renders HTML,
 * image badges and tables. None of that means anything to the few lines below,
 * which knew only headings, bullets and links — so a download button arrived
 * here as the literal text of its markdown, and a `<div>` as the word "div".
 * Better to show the words and drop the scaffolding than to print the
 * scaffolding at somebody.
 */
/** One piece of a release note, once its page scaffolding is gone. */
private sealed interface Note {
    data class Heading(val level: Int, val text: String) : Note

    data class Paragraph(val text: String) : Note

    data class Bullet(val text: String) : Note
}

/**
 * Turn a release note into something worth reading on a phone.
 *
 * Notes are written for the releases page, so they arrive wrapped at about
 * eighty columns, wearing HTML, image badges and tables. Drawing each of those
 * lines on its own broke sentences wherever the author happened to press
 * return, which is the single thing that made these hard to read.
 *
 * Lines are cleaned, then folded back into paragraphs the way the writer meant
 * them: a blank line ends one, a heading or a bullet stands alone.
 */
private fun notes(text: String): List<Note> {
    val html = Regex("<[^>]+>")
    val badge = Regex("""\[!\[[^\]]*]\([^)]*\)]\([^)]*\)""")
    val image = Regex("""!\[[^\]]*]\([^)]*\)""")

    val cleaned =
        text.split("\n").map { line ->
            var l = line.trim()
                // A linked badge is a button, and a button is not a sentence.
                .replace(badge, "")
                .replace(image, "")
                .replace(html, "")
                .replace("`", "")
                .trim()
            // Rules and the dashes under a table heading say nothing, and have to
            // go before the next step, which would otherwise turn them into text.
            if (l.isNotEmpty() && l.all { it == '-' || it == '=' || it == '|' || it == ':' || it == ' ' }) {
                l = ""
            }
            // A table row has no columns here; its cells are sentences, so they
            // are joined rather than thrown away.
            if (l.length > 1 && l.startsWith("|") && l.endsWith("|")) {
                l = l.trim('|').split('|').map { it.trim() }.filter { it.isNotEmpty() }
                    .joinToString(" — ")
            }
            l
        }

    val out = mutableListOf<Note>()
    val para = StringBuilder()
    fun flush() {
        if (para.isNotBlank()) out += Note.Paragraph(para.toString().trim())
        para.clear()
    }
    cleaned.forEach { line ->
        when {
            line.isBlank() -> flush()
            line.startsWith("#") -> {
                flush()
                val level = line.takeWhile { it == '#' }.length
                val body = line.drop(level).trim()
                if (body.isNotEmpty()) out += Note.Heading(level.coerceIn(1, 3), body)
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                flush()
                out += Note.Bullet(line.drop(2).trim())
            }
            // A wrapped sentence continues the paragraph it belongs to.
            else -> {
                if (para.isNotEmpty()) para.append(' ')
                para.append(line)
            }
        }
    }
    flush()
    return out
}

@Suppress("DEPRECATION")
@Composable
fun MarkdownText(text: String) {
    val uriHandler = LocalUriHandler.current
    val linkColour = MaterialTheme.colorScheme.primary
    val bodyColour = MaterialTheme.colorScheme.onSurface

    // Inline marks are the same wherever a piece of text appears, so the work
    // of reading them is done once here.
    @Composable
    fun inline(raw: String) = buildAnnotatedString {
        var lastIndex = 0
        markdownInlineRegex.findAll(raw).forEach { result ->
            append(raw.substring(lastIndex, result.range.first))
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
                        SpanStyle(
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
        append(raw.substring(lastIndex))
    }

    @Composable
    fun body(annotated: androidx.compose.ui.text.AnnotatedString, modifier: Modifier = Modifier) {
        ClickableText(
            text = annotated,
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge.copy(color = bodyColour, lineHeight = 26.sp),
            onClick = { offset ->
                annotated.getStringAnnotations("URL", offset, offset)
                    .firstOrNull()?.let { uriHandler.openUri(it.item) }
            },
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        notes(text).forEachIndexed { index, note ->
            when (note) {
                is Note.Heading -> {
                    // Space above a heading, not below: it belongs to what follows.
                    Spacer(Modifier.height(if (index == 0) 0.dp else 22.dp))
                    Text(
                        text = note.text,
                        style = when (note.level) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.Bold,
                        color = bodyColour,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    )
                }

                is Note.Paragraph -> {
                    body(inline(note.text), Modifier.fillMaxWidth().padding(bottom = 12.dp))
                }

                is Note.Bullet -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = stringResource(R.string.list_bullet),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 10.dp),
                        )
                        body(inline(note.text), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
