/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
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
 *
 * It fits any window: the title and the buttons always show, and a card too tall for the space
 * left (a small phone, large text) scrolls on its own. On a phone held sideways each card puts
 * its demo beside its words instead of above them.
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
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Sideways: wider than tall, and too short to stack the demo over the words.
            val sideways = maxWidth > maxHeight && maxHeight < 600.dp
            val outerV = if (sideways) 12.dp else 24.dp
            val innerV = if (sideways) 14.dp else 20.dp
            // What the title row, the buttons and the paddings take; the demo gets the rest, within limits.
            val demoHeight =
                if (sideways) {
                    (maxHeight - outerV * 2 - innerV * 2 - 120.dp).coerceIn(150.dp, 286.dp)
                } else {
                    // About 390 dp go to the title, the card's words and the buttons at normal text
                    // size; the phone gets what is left, full size on most phones, smaller on short
                    // ones so the words still show. Larger text scrolls within the card.
                    (maxHeight - 390.dp).coerceIn(170.dp, 286.dp)
                }
            PopupBody(
                version = version,
                entries = entries,
                sideways = sideways,
                demoHeight = demoHeight,
                innerV = innerV,
                onSkip = onSkip,
                onSeeMore = onSeeMore,
                modifier =
                    Modifier
                        .padding(horizontal = 18.dp, vertical = outerV)
                        .widthIn(max = if (sideways) 760.dp else 460.dp)
                        .heightIn(max = maxHeight - outerV * 2)
                        .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PopupBody(
    version: String,
    entries: List<FeatureEntry>,
    sideways: Boolean,
    demoHeight: Dp,
    innerV: Dp,
    onSkip: () -> Unit,
    onSeeMore: () -> Unit,
    modifier: Modifier,
) {
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
    val drift = rememberDrift(still, pager.settledPage)
    val phones = remember(entries) { entries.map { hasPhoneJourney(it.demo) } }
    // How strong the colour fields are: softer on a light theme, so the words keep their contrast.
    val tint = if (scheme.surface.luminance() > 0.5f) 0.5f else 0.7f

    val enter = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 260f)) }

    Box(
        modifier =
            modifier.graphicsLayer {
                val e = enter.value
                alpha = e.coerceIn(0f, 1f)
                scaleX = 0.92f + 0.08f * e
                scaleY = 0.92f + 0.08f * e
                translationY = (1f - e) * 28.dp.toPx()
            },
    ) {
        AuroraBackdrop(
            position = { pagerPosition(pager) },
            palettes = palettes,
            drift = drift,
            base = scheme.surfaceContainerLow,
            tint = tint,
            modifier = Modifier.matchParentSize(),
        )
        Column(Modifier.padding(vertical = innerV)) {
            Text(
                text = stringResource(R.string.whats_new_title, version),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                // Two lines at most, so large text wraps rather than cutting off the version.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() },
            )
            Spacer(Modifier.height(if (sideways) 10.dp else 16.dp))

            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = 20.dp),
                pageSpacing = 16.dp,
                verticalAlignment = Alignment.Top,
                // Takes what the title and the buttons leave, and no more than its cards need.
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .drawBehind { phoneGlow(pagerPosition(pager), palettes, phones, drift.value, sideways, demoHeight.toPx(), 20.dp.toPx(), tint) },
            ) { page ->
                val position = stringResource(R.string.whats_new_card_position, page + 1, entries.size)
                PopupCard(
                    entry = entries[page],
                    palette = palettes[page],
                    playing = page == pager.settledPage,
                    tint = tint,
                    sideways = sideways,
                    demoHeight = demoHeight,
                    modifier =
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .semantics { stateDescription = position }
                            .graphicsLayer {
                                // The cards beside the current one sit back a little and fade.
                                val away = min(1f, abs(pager.currentPage - page + pager.currentPageOffsetFraction))
                                val near = 1f - away
                                scaleX = 0.9f + 0.1f * near
                                scaleY = 0.9f + 0.1f * near
                                alpha = 0.55f + 0.45f * near
                                translationY = away * 10.dp.toPx()
                                // Fade each drawing as it is made, not the card drawn off screen and
                                // then blended: that extra picture every frame was the swipe's main cost.
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                            },
                )
            }

            Spacer(Modifier.height(if (sideways) 10.dp else 18.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.whats_new_skip), color = scheme.onSurface.copy(alpha = 0.85f), maxLines = 1)
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

private val POPUP_RADIUS = 32.dp

/** How much smaller than the popup its colour fields are painted before being stretched to fit. */
private const val BACKDROP_SCALE = 4

/**
 * The popup's background. The colour fields are so soft that a picture a quarter the size looks
 * the same stretched up, so they are painted into a small layer the graphics chip enlarges: about
 * a sixteenth of the painting, every frame they move. One rounded clip round the whole keeps the
 * corners sharp.
 */
@Composable
private fun AuroraBackdrop(
    position: () -> Float,
    palettes: List<Palette>,
    drift: State<Float>,
    base: Color,
    tint: Float,
    modifier: Modifier,
) {
    Box(modifier.clip(RoundedCornerShape(POPUP_RADIUS)).background(base)) {
        Spacer(
            Modifier
                .layout { measurable, constraints ->
                    val w = constraints.maxWidth
                    val h = constraints.maxHeight
                    val small = measurable.measure(Constraints.fixed((w + BACKDROP_SCALE - 1) / BACKDROP_SCALE, (h + BACKDROP_SCALE - 1) / BACKDROP_SCALE))
                    layout(w, h) {
                        small.placeWithLayer(0, 0) {
                            scaleX = BACKDROP_SCALE.toFloat()
                            scaleY = BACKDROP_SCALE.toFloat()
                            transformOrigin = TransformOrigin(0f, 0f)
                            // Painted at its own small size, then enlarged as a picture.
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                    }
                }.drawBehind { aurora(position(), palettes, drift.value, tint) },
        )
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
    tint: Float,
    sideways: Boolean,
    demoHeight: Dp,
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

    val demo: @Composable (Modifier) -> Unit = { place ->
        if (hasPhoneJourney(entry.demo)) {
            // The real app on a phone, walking from Home to the feature, on a glow of the card's colours.
            PhoneDemo(
                demo = entry.demo,
                playing = playing,
                // Its glow is drawn behind the pager (phoneGlow), where a card's scrolling cannot cut it off.
                modifier = place.height(demoHeight),
            )
        } else {
            FeatureDemo(
                demo = entry.demo,
                icon = entry.icon,
                playing = playing,
                modifier =
                    place
                        .height(min(150.dp, demoHeight))
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.surfaceContainerHigh)
                        .background(
                            Brush.verticalGradient(listOf(palette.first.copy(alpha = 0.26f), palette.second.copy(alpha = 0.10f))),
                        ),
            )
        }
    }

    // The words, on a wash of the card's colours that fades out at its edges, no border.
    val words: @Composable (Modifier) -> Unit = { place ->
        Column(
            modifier =
                place
                    .drawBehind {
                        val radius = CornerRadius(24.dp.toPx())
                        drawRoundRect(
                            Brush.linearGradient(
                                listOf(palette.first.copy(alpha = 0.20f * tint), palette.second.copy(alpha = 0.12f * tint), Color.Transparent),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height * 1.4f),
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
                // Portrait cards keep one height as they are swiped; beside the demo there is room to spare.
                minLines = if (sideways) 1 else 2,
                modifier = Modifier.rise(reveal, 0.18f).semantics { heading() },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(entry.description),
                style = MaterialTheme.typography.bodyMedium,
                // The main text colour, softened: the secondary one is too faint over the colour fields.
                color = scheme.onSurface.copy(alpha = 0.85f),
                minLines = if (sideways) 1 else 3,
                modifier = Modifier.rise(reveal, 0.34f),
            )
        }
    }

    if (sideways) {
        Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            demo(Modifier.weight(0.42f))
            Spacer(Modifier.width(14.dp))
            words(Modifier.weight(0.58f))
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            demo(Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            words(Modifier.fillMaxWidth())
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
        compositingStrategy = CompositingStrategy.ModulateAlpha
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
    // As wide as the dots need, or the room between the buttons if that is less (large text, a
    // narrow phone), with the dots scaled down to fit rather than running under the buttons.
    Canvas(Modifier.widthIn(max = (count * 12 + 14).dp).fillMaxWidth().height(6.dp).clearAndSetSemantics {}) {
        val position = pagerPosition(pager)
        val fit = min(1f, size.width / (count * 12 + 14).dp.toPx())
        val dot = 6.dp.toPx() * fit
        val gap = 6.dp.toPx() * fit
        val stretch = 14.dp.toPx() * fit
        var x = 0f
        for (i in 0 until count) {
            val near = 1f - min(1f, abs(position - i))
            val width = dot + stretch * near
            drawRoundRect(
                color = lerp(idle, palettes[i].first, near),
                topLeft = Offset(x, (size.height - dot) / 2f),
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

/**
 * The slow clock for the drifting colours, in turns. Each time a card settles it glides on by
 * one turn and slows to a stop, so a popup left open draws no frames at all (idle means zero
 * frames). With animations off it never moves.
 */
@Composable
private fun rememberDrift(
    still: Boolean,
    settledPage: Int,
): State<Float> {
    val clock = remember { Animatable(0.15f) }
    LaunchedEffect(still, settledPage) {
        if (still) return@LaunchedEffect
        clock.animateTo(clock.value + 1f, tween(durationMillis = DRIFT_MS, easing = LinearOutSlowInEasing))
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
 * The glow of the card's colours behind the phone mock-up, blending from card to card as they
 * are swiped and fading out for a card whose demo is not a phone. [inset] is the pager's side padding.
 */
private fun DrawScope.phoneGlow(
    position: Float,
    palettes: List<Palette>,
    phones: List<Boolean>,
    time: Float,
    sideways: Boolean,
    demoHeight: Float,
    inset: Float,
    tint: Float,
) {
    val last = phones.lastIndex
    val left = floor(position).toInt().coerceIn(0, last)
    val right = (left + 1).coerceAtMost(last)
    val share = (position - left).coerceIn(0f, 1f)
    val strength = (if (phones[left]) 1f - share else 0f) + (if (phones[right] && right != left) share else 0f)
    if (strength <= 0.01f) return
    val (a, b) = blendedPalette(position, palettes)
    val width = (size.width - inset * 2f) * (if (sideways) 0.42f else 1f)
    val height = min(demoHeight, size.height)
    val middle = Offset(inset + width / 2f, height / 2f)
    val glow = min(width, height) * 0.66f
    val turn = time * 2f * PI.toFloat()
    val first = middle + Offset(-width * 0.12f * cos(turn), -height * 0.08f)
    val second = middle + Offset(width * 0.16f * sin(turn), height * 0.2f)
    drawCircle(Brush.radialGradient(listOf(a.copy(alpha = 0.38f * strength * tint), Color.Transparent), first, glow), glow, first)
    drawCircle(Brush.radialGradient(listOf(b.copy(alpha = 0.26f * strength * tint), Color.Transparent), second, glow * 0.8f), glow * 0.8f, second)
}

/**
 * The popup's colour fields: three soft fields, drifting with [time] and sliding
 * a little as the cards are swiped, their colours blending from one card's into the next.
 */
private fun DrawScope.aurora(
    position: Float,
    palettes: List<Palette>,
    time: Float,
    tint: Float,
) {
    val (a, b) = blendedPalette(position, palettes)
    val w = size.width
    val h = size.height
    val turn = time * 2f * PI.toFloat()
    val slide = (position - floor(position)) * w * 0.12f

    fun field(
        colour: Color,
        x: Float,
        y: Float,
        reach: Float,
        strength: Float,
    ) {
        val centre = Offset(x, y)
        drawCircle(Brush.radialGradient(listOf(colour.copy(alpha = strength * tint), Color.Transparent), centre, reach), reach, centre)
    }

    // Sized by the shorter side: a wide popup on a phone held sideways would otherwise be flooded
    // with colour and its words lose their contrast. Portrait popups are narrower than tall, so
    // they look as before.
    val s = min(w, h)
    field(a, w * (0.18f + 0.08f * sin(turn)) - slide, h * (0.10f + 0.05f * cos(turn)), s * 0.95f, 0.50f)
    field(b, w * (0.88f - 0.08f * cos(turn)) - slide, h * (0.45f + 0.06f * sin(turn * 2f)), s * 0.85f, 0.38f)
    field(lerp(a, b, 0.5f), w * (0.40f + 0.10f * sin(turn + 1f)) + slide, h * 1.02f, s * 0.80f, 0.34f)
    // A faint sheen from the top, like light on glass.
    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent), 0f, h * 0.35f))
}
