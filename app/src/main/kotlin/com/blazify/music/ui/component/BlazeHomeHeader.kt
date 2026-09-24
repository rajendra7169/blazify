/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImage
import com.blazify.music.R
import com.blazify.music.constants.DarkModeKey
import com.blazify.music.constants.ShowHomeGreetingKey
import com.blazify.music.constants.ShowHomeSearchBarKey
import com.blazify.music.ui.screens.settings.DarkMode
import com.blazify.music.ui.theme.BlazeGradientEnd
import com.blazify.music.ui.theme.BlazeThemeColor
import com.blazify.music.utils.rememberEnumPreference
import com.blazify.music.utils.rememberPreference
import java.util.Calendar

/**
 * Blazify home header, ported from the original Flutter app:
 * top row (account | logo + wordmark | settings), greeting card with the
 * hero image overflowing above the card, and a rounded search bar.
 */
@Composable
fun BlazeHomeHeader(
    userName: String = stringResource(R.string.blaze_greeting_default_name),
    onAccountClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMicClick: () -> Unit = {},
    // Two ways to start music from the card itself, each showing the cover of the
    // song it starts with. Null hides the button; with neither, the card keeps its
    // old "Enjoy the music" line instead.
    onForYouClick: (() -> Unit)? = null,
    forYouArt: String? = null,
    onSpeedDialClick: (() -> Unit)? = null,
    speedDialArt: String? = null,
) {
    val darkMode by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isDark = if (darkMode == DarkMode.AUTO) isSystemInDarkTheme() else darkMode == DarkMode.ON
    // Both blocks are optional — some people want a compact home (Look & Feel → Home).
    val showGreeting by rememberPreference(ShowHomeGreetingKey, defaultValue = true)
    val showSearchBar by rememberPreference(ShowHomeSearchBarKey, defaultValue = true)
    val iconTint = if (isDark) Color.White else Color(0xDE000000)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row: account | logo + "Blazify" | settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        ) {
            IconButton(onClick = onAccountClick) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Image(
                    painter = painterResource(
                        if (isDark) R.drawable.blaze_logo_white else R.drawable.blaze_logo,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Blazify",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    maxLines = 1,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = if (isDark) {
                            Brush.linearGradient(listOf(Color.White, Color.White))
                        } else {
                            Brush.linearGradient(listOf(BlazeThemeColor, BlazeGradientEnd))
                        },
                    ),
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Greeting card: theme-aware gradient (follows the dynamic album-art
        // colour), text left, hero image overflowing the top.
        val cardStart = MaterialTheme.colorScheme.primary
        val cardEnd = lerp(cardStart, Color.Black, if (isDark) 0.30f else 0.20f)
        val onCard = if (cardStart.luminance() > 0.6f) Color.Black else Color.White
        // The buttons' fill: the card's colour where they sit, darkened (or lightened
        // under dark text). Solid, because a shadow shows through a see-through fill.
        val cardMiddle = lerp(cardStart, cardEnd, 0.5f)
        val buttonFill =
            if (onCard.luminance() > 0.5f) lerp(cardMiddle, Color.Black, 0.30f) else lerp(cardMiddle, Color.White, 0.40f)
        if (showGreeting) BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(160.dp),
        ) {
            // Where the photo begins. Words that reach it go behind her and fade out.
            val photoLeft = maxWidth - 200.dp
            val greeting = stringResource(greetingRes())
            val hasButtons = onForYouClick != null || onSpeedDialClick != null

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(cardStart, cardEnd)),
                    ),
            )

            // The words come first, so she stands in front of them: a long name, or a
            // greeting that runs long in another language, stays on its line and fades
            // out behind her instead of wrapping onto a new one.
            CardWords(
                greeting = greeting,
                userName = userName,
                onCard = onCard,
                showWords = true,
                hasButtons = hasButtons,
                photoLeft = photoLeft,
                modifier = Modifier.align(Alignment.CenterStart),
            )

            // Hero image: 200x240, bottom-aligned with the card, spilling 80dp above it
            Image(
                painter = painterResource(
                    if (isDark) R.drawable.blaze_home_dark else R.drawable.blaze_home_light,
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .requiredWidth(200.dp)
                    .requiredHeight(240.dp)
                    // requiredHeight overflows evenly (40dp top and bottom);
                    // shift up so the bottom edge sits flush with the card
                    .offset(y = (-40).dp)
                    .clip(RoundedCornerShape(12.dp)),
            )

            // The buttons come last, in front of her. They are laid out as a copy of the
            // words with the words hidden, so they sit exactly where one column puts them.
            if (hasButtons) {
                CardWords(
                    greeting = greeting,
                    userName = userName,
                    onCard = onCard,
                    showWords = false,
                    hasButtons = true,
                    photoLeft = photoLeft,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    onForYouClick?.let {
                        CardButton(forYouArt, R.drawable.star, stringResource(R.string.home_for_you), onCard, buttonFill, it)
                    }
                    // Just the cover: the label would crowd the photo, and a cover with
                    // a play mark next to For you already says what it does.
                    onSpeedDialClick?.let {
                        CardButton(speedDialArt, R.drawable.grid_view, stringResource(R.string.speed_dial), onCard, buttonFill, it, showLabel = false)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Search bar
        if (showSearchBar) Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFEEEEEE))
                .clickable(onClick = onSearchClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0x8A000000),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            // Keep the placeholder on one line: on narrow screens it used to wrap and
            // grow the search bar's height, so ellipsize instead.
            Text(
                text = stringResource(R.string.home_search_hint),
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0x8A000000),
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // Song recognition
            Icon(
                painter = painterResource(R.drawable.mic),
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0x8A000000),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onMicClick),
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

/**
 * The greeting card's column: greeting, name, then the buttons or the "Enjoy the music"
 * line. Every line stays on one line in any language and at any length.
 *
 * Drawn twice. With [showWords] the words show and the buttons' row is left empty; without
 * it the words are invisible, kept only so the buttons land where they belong, and
 * silent so a screen reader does not read them twice.
 */
@Composable
private fun CardWords(
    greeting: String,
    userName: String,
    onCard: Color,
    showWords: Boolean,
    hasButtons: Boolean,
    photoLeft: Dp,
    modifier: Modifier = Modifier,
    buttons: @Composable () -> Unit = {},
) {
    // A slight drop shadow, so white words stay readable on a pale card.
    val shadow = with(LocalDensity.current) {
        Shadow(
            color = (if (onCard.luminance() > 0.5f) Color.Black else Color.White).copy(alpha = 0.25f),
            offset = Offset(0f, 1.dp.toPx()),
            blurRadius = 3.dp.toPx(),
        )
    }
    val words = if (showWords) Modifier else Modifier.alpha(0f).clearAndSetSemantics {}
    // Where each line fades, measured from the photo's left edge (the lines start 20dp
    // into the card). Her hand is in the first ~27dp at the name's height, with a gap
    // before her sweater, so the name is gone by then; the greeting sits beside her
    // hair and fades later, which keeps the sun or moon after it whole.
    val fromLine = photoLeft - 20.dp
    val greetingFade = Modifier.fadeOutBetween(fromLine + 16.dp, fromLine + 56.dp)
    val lowerFade = Modifier.fadeOutBetween(fromLine - 4.dp, fromLine + 24.dp)
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp),
    ) {
        // Each line gets its own room below it, inside its fade, so the fade does
        // not clip the shadow under the letters.
        Box(greetingFade.padding(bottom = 6.dp)) {
            Text(
                text = greeting,
                color = onCard,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                lineHeight = 27.sp,
                softWrap = false,
                style = TextStyle(shadow = shadow),
                modifier = words,
            )
        }
        Box(lowerFade.padding(bottom = if (hasButtons) 8.dp else 6.dp)) {
            Text(
                text = userName,
                color = onCard.copy(alpha = 0.95f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(shadow = shadow),
                modifier = words,
            )
        }
        if (!hasButtons) {
            Box(lowerFade) {
                Text(
                    text = stringResource(R.string.home_enjoy_music),
                    color = onCard.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                    softWrap = false,
                    style = TextStyle(shadow = shadow),
                    modifier = words,
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showWords) Spacer(Modifier.height(32.dp)) else buttons()
            }
        }
    }
}

/** Fades whatever is drawn out to nothing between [start] and [end], left to right. */
private fun Modifier.fadeOutBetween(start: Dp, end: Dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = start.toPx(),
                    endX = end.toPx(),
                ),
                blendMode = BlendMode.DstIn,
            )
        }

/**
 * A small button on the greeting card that starts music: the cover of the song it
 * plays first, with a play mark on it, then the label, or the bare cover alone. A cover says "this plays"
 * where a text-only pill read as a label nobody would think to tap. A drop shadow
 * lifts it off the card.
 */
@Composable
private fun CardButton(
    art: String?,
    icon: Int,
    label: String,
    onCard: Color,
    fill: Color,
    onClick: () -> Unit,
    showLabel: Boolean = true,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(32.dp)
            // A drop shadow lifts the buttons off the card, so they read as things to press.
            .shadow(elevation = 6.dp, shape = shape)
            .clip(shape)
            .background(fill)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            // A cover alone has no words, so a screen reader is given the label instead.
            .then(if (showLabel) Modifier else Modifier.semantics { contentDescription = label }),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(onCard.copy(alpha = 0.12f)),
        ) {
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                // The play mark belongs to the labelled button; a cover on its own
                // stays a clean picture.
                if (showLabel) Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            } else {
                // No cover yet (still loading, or nothing to show): the button's own icon.
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = onCard,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (showLabel) {
            Text(
                text = label,
                color = onCard,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp, end = 12.dp),
            )
        }
    }
}

private fun greetingRes(): Int {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> R.string.home_greeting_morning
        hour in 12..16 -> R.string.home_greeting_afternoon
        hour in 17..20 -> R.string.home_greeting_evening
        else -> R.string.home_greeting_night
    }
}
