/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew

import android.text.Annotation
import android.text.Spanned
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.blazify.music.ui.screens.whatsnew.demos.FeatureDemo
import com.blazify.music.whatsnew.FeatureEntry

/** One feature: its demo, a heading with the key words in the accent colour, and a line or two. */
@Composable
fun FeatureCard(
    entry: FeatureEntry,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val title = rememberAccentedString(entry.heading, scheme.primary)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceContainer)
            .padding(16.dp),
    ) {
        FeatureDemo(
            demo = entry.demo,
            icon = entry.icon,
            playing = playing,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surfaceContainerHigh),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(entry.description),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
    }
}

/**
 * The string [id] with the words inside `<annotation type="accent">` in [accent]. Read with
 * getText, which keeps the tag; stringResource drops it.
 */
@Composable
internal fun rememberAccentedString(
    @StringRes id: Int,
    accent: Color,
): AnnotatedString {
    val resources = LocalResources.current
    return remember(id, accent, resources) { accentedString(resources.getText(id), accent) }
}

/**
 * A translation that lost the tag, or has it in the wrong place, shows the heading in one
 * colour. It never fails.
 */
internal fun accentedString(
    text: CharSequence,
    accent: Color,
): AnnotatedString {
    val plain = text.toString()
    val spanned = text as? Spanned ?: return AnnotatedString(plain)
    return runCatching {
        buildAnnotatedString {
            append(plain)
            spanned.getSpans(0, spanned.length, Annotation::class.java).forEach { annotation ->
                if (annotation.key == "type" && annotation.value == "accent") {
                    val start = spanned.getSpanStart(annotation)
                    val end = spanned.getSpanEnd(annotation)
                    if (start in 0 until end && end <= plain.length) {
                        addStyle(SpanStyle(color = accent), start, end)
                    }
                }
            }
        }
    }.getOrElse { AnnotatedString(plain) }
}
