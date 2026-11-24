/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blazify.music.R
import com.blazify.music.ui.screens.whatsnew.demos.FeatureDemo
import com.blazify.music.ui.screens.whatsnew.demos.rememberReduceMotion
import com.blazify.music.ui.screens.whatsnew.phone.PhoneDemo
import com.blazify.music.ui.screens.whatsnew.phone.hasPhoneJourney
import com.blazify.music.whatsnew.FeatureEntry
import com.blazify.music.whatsnew.FeatureKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** One card's two colours, from the theme so album colours, light, dark and pure black all work. */
private typealias Palette = Pair<Color, Color>

/** How long the background's colour fields take to drift once round, in milliseconds. */
private const val DRIFT_MS = 16_000

/**
 * The popup Home shows once after an update: one card per change, swiped through, each with its
 * demo. Skip closes it; See more opens the full What's new page.
 *
 * Behind the cards, soft colour fields drift and blend from one card's colours into the next as
 * the cards are swiped. Everything that moves is read while drawing, so swiping and drifting
 * redraw without recomposing, and with animations off the popup holds still.
 */
@Composable
fun WhatsNewPopup(
    version: String,
    entries: List<FeatureEntry>,
    onSkip: () -> Unit,
    onSeeMore: () -> Unit,
) {
    if (entries.isEmpty()) return
    Dialog(onDismissRequest = onSkip, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val still = rememberReduceMotion()
        val scheme = MaterialTheme.colorScheme
        val palettes =
            remember(scheme, entries.size) {
                val pairs =
                    listOf(
                        scheme.primary to scheme.tertiary,
                        scheme.tertiary to scheme.secondary,
                        scheme.secondary to scheme.primary,
                        scheme.primary to scheme.secondary,
                        scheme.tertiary to scheme.primary,
                        scheme.secondary to scheme.tertiary,
                    )
                List(entries.size) { pairs[it % pairs.size] }
            }
        val pager = rememberPagerState { entries.size }
        val drift = rememberDrift(still)

        val enter = remember { Animatable(if (still) 1f else 0f) }
        LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 260f)) }

        Box(
            modifier =
                Modifier
                    .padding(horizontal = 18.dp, vertical = 24.dp)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val e = enter.value
                        alpha = e.coerceIn(0f, 1f)
                        scaleX = 0.92f + 0.08f * e
                        scaleY = 0.92f + 0.08f * e
                        translationY = (1f - e) * 28.dp.toPx()
                    }.clip(RoundedCornerShape(32.dp))
                    .drawBehind { aurora(pagerPosition(pager), palettes, drift.value, scheme.surfaceContainerLow) },
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 20.dp)) {
                Text(
                    text = stringResource(R.string.whats_new_title, version),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() },
                )
                Spacer(Modifier.height(16.dp))

                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSpacing = 16.dp,
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    val position = stringResource(R.string.whats_new_card_position, page + 1, entries.size)
                    PopupCard(
                        entry = entries[page],
                        palette = palettes[page],
                        playing = page == pager.settledPage,
                        drift = drift,
                        modifier =
                            Modifier
                                .semantics { stateDescription = position }
                                .graphicsLayer {
                                    // The cards beside the current one sit back a little and fade.
                                    val away = min(1f, abs(pager.currentPage - page + pager.currentPageOffsetFraction))
                                    val near = 1f - away
                                    scaleX = 0.9f + 0.1f * near
                                    scaleY = 0.9f + 0.1f * near
                                    alpha = 0.55f + 0.45f * near
                                    translationY = away * 10.dp.toPx()
                                },
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.whats_new_skip), color = scheme.onSurfaceVariant)
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Dots(pager, palettes, scheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                    GradientButton(
                        text = stringResource(R.string.whats_new_see_more),
                        pager = pager,
                        palettes = palettes,
                        onClick = onSeeMore,
                    )
                }
            }
        }
    }
}

/**
 * One change, with no box round it: the phone (or the demo) floating on a glow of the card's
 * colours, and under it the badge, heading and words on a soft wash of the same colours. When
 * the card comes into view its words rise in one after another.
 */
@Composable
private fun PopupCard(
    entry: FeatureEntry,
    palette: Palette,
    playing: Boolean,
    drift: State<Float>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val title = rememberAccentedString(entry.heading, palette.first)
    val still = rememberReduceMotion()
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(playing, still) {
        if (playing && !still) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(durationMillis = 700, easing = LinearEasing))
        } else {
            reveal.snapTo(1f)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (hasPhoneJourney(entry.demo)) {
            // The real app on a phone, walking from Home to the feature, on a glow of the card's colours.
            PhoneDemo(
                demo = entry.demo,
                playing = playing,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(286.dp)
                        .drawBehind {
                            val glow = size.minDimension * 0.66f
                            val drift2 = drift.value * 2f * PI.toFloat()
                            val first = center + Offset(-size.width * 0.12f * cos(drift2), -size.height * 0.08f)
                            val second = center + Offset(size.width * 0.16f * sin(drift2), size.height * 0.2f)
                            drawCircle(Brush.radialGradient(listOf(palette.first.copy(alpha = 0.38f), Color.Transparent), first, glow), glow, first)
                            drawCircle(Brush.radialGradient(listOf(palette.second.copy(alpha = 0.26f), Color.Transparent), second, glow * 0.8f), glow * 0.8f, second)
                        },
            )
        } else {
            FeatureDemo(
                demo = entry.demo,
                icon = entry.icon,
                playing = playing,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.surfaceContainerHigh)
                        .background(
                            Brush.verticalGradient(listOf(palette.first.copy(alpha = 0.26f), palette.second.copy(alpha = 0.10f))),
                        ),
            )
        }
        Spacer(Modifier.height(10.dp))

        // The words, on a wash of the card's colours that fades out at its edges, no border.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val radius = CornerRadius(24.dp.toPx())
                        drawRoundRect(
                            Brush.linearGradient(
                                listOf(palette.first.copy(alpha = 0.20f), palette.second.copy(alpha = 0.12f), Color.Transparent),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height * 1.4f),
                            ),
                            cornerRadius = radius,
                        )
                        // A light sweep that drifts across the wash.
                        val sweep = drift.value * size.width * 2f - size.width * 0.5f
                        drawRoundRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.06f), Color.Transparent),
                                start = Offset(sweep - size.width * 0.3f, 0f),
                                end = Offset(sweep + size.width * 0.3f, size.height),
                            ),
                            cornerRadius = radius,
                        )
                    }.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            KindBadge(entry.kind, palette, Modifier.rise(reveal, 0f))
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                minLines = 2,
                modifier = Modifier.rise(reveal, 0.18f).semantics { heading() },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(entry.description),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                minLines = 3,
                modifier = Modifier.rise(reveal, 0.34f),
            )
        }
    }
}

/** Fades and lifts this in as [reveal] passes [start], drawn in a layer so it costs no recomposition. */
private fun Modifier.rise(
    reveal: Animatable<Float, *>,
    start: Float,
): Modifier =
    graphicsLayer {
        val shown = ((reveal.value - start) / 0.5f).coerceIn(0f, 1f)
        val eased = 1f - (1f - shown) * (1f - shown) * (1f - shown)
        alpha = eased
        translationY = (1f - eased) * 12.dp.toPx()
    }

/** New, Improved or Fixed, on a pill shaded with the card's colours. */
@Composable
private fun KindBadge(
    kind: FeatureKind,
    palette: Palette,
    modifier: Modifier = Modifier,
) {
    val (label, icon) =
        when (kind) {
            FeatureKind.NEW -> R.string.whats_new_kind_new to R.drawable.celebration
            FeatureKind.IMPROVED -> R.string.whats_new_kind_improved to R.drawable.trending_up
            FeatureKind.FIXED -> R.string.whats_new_kind_fixed to R.drawable.check
        }
    val onBadge = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(palette.first, palette.second)))
                .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = onBadge, modifier = Modifier.size(14.dp))
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = onBadge)
    }
}

/** The page dots: the current one stretches and takes its card's colour as the cards are swiped. */
@Composable
private fun Dots(
    pager: PagerState,
    palettes: List<Palette>,
    idle: Color,
) {
    if (palettes.size < 2) return
    val count = palettes.size
    Canvas(Modifier.width((count * 12 + 14).dp).height(6.dp).clearAndSetSemantics {}) {
        val position = pagerPosition(pager)
        val dot = 6.dp.toPx()
        val gap = 6.dp.toPx()
        val stretch = 14.dp.toPx()
        var x = 0f
        for (i in 0 until count) {
            val near = 1f - min(1f, abs(position - i))
            val width = dot + stretch * near
            drawRoundRect(
                color = lerp(idle, palettes[i].first, near),
                topLeft = Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(width, dot),
                cornerRadius = CornerRadius(dot / 2f),
            )
            x += width + gap
        }
    }
}

/** See more, shaded with the colours of the card being looked at. */
@Composable
private fun GradientButton(
    text: String,
    pager: PagerState,
    palettes: List<Palette>,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .drawBehind {
                    val (from, to) = blendedPalette(pagerPosition(pager), palettes)
                    drawRect(Brush.horizontalGradient(listOf(from, to)))
                }.clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
    }
}

/** A slow 0-to-1 clock for the drifting colours. Holds still, and costs no frames, with animations off. */
@Composable
private fun rememberDrift(still: Boolean): State<Float> {
    val clock = remember { Animatable(0.15f) }
    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        while (true) {
            clock.animateTo(1f, tween(durationMillis = ((1f - clock.value) * DRIFT_MS).toInt().coerceAtLeast(1), easing = LinearEasing))
            clock.snapTo(0f)
        }
    }
    return clock.asState()
}

private fun pagerPosition(pager: PagerState): Float = pager.currentPage + pager.currentPageOffsetFraction

/** The two colours between the cards either side of [position]. */
private fun blendedPalette(
    position: Float,
    palettes: List<Palette>,
): Palette {
    val last = palettes.lastIndex
    val left = floor(position).toInt().coerceIn(0, last)
    val right = (left + 1).coerceAtMost(last)
    val share = (position - left).coerceIn(0f, 1f)
    return lerp(palettes[left].first, palettes[right].first, share) to lerp(palettes[left].second, palettes[right].second, share)
}

/**
 * The popup's background: three soft colour fields over [base], drifting with [time] and sliding
 * a little as the cards are swiped, their colours blending from one card's into the next.
 */
private fun DrawScope.aurora(
    position: Float,
    palettes: List<Palette>,
    time: Float,
    base: Color,
) {
    drawRect(base)
    val (a, b) = blendedPalette(position, palettes)
    val w = size.width
    val h = size.height
    val turn = time * 2f * PI.toFloat()
    val slide = (position - floor(position)) * w * 0.12f

    fun field(
        colour: Color,
        x: Float,
        y: Float,
        radius: Float,
        strength: Float,
    ) {
        val centre = Offset(x, y)
        drawCircle(Brush.radialGradient(listOf(colour.copy(alpha = strength), Color.Transparent), centre, radius), radius, centre)
    }

    field(a, w * (0.18f + 0.08f * sin(turn)) - slide, h * (0.10f + 0.05f * cos(turn)), w * 0.95f, 0.50f)
    field(b, w * (0.88f - 0.08f * cos(turn)) - slide, h * (0.45f + 0.06f * sin(turn * 2f)), w * 0.85f, 0.38f)
    field(lerp(a, b, 0.5f), w * (0.40f + 0.10f * sin(turn + 1f)) + slide, h * 1.02f, w * 0.80f, 0.34f)
    // A faint sheen from the top, like light on glass.
    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent), 0f, h * 0.35f))
}
