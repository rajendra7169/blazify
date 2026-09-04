/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One button of the player's bottom row: queue, cast, sleep timer, lyrics.
 *
 * The icon sits above its name so the row reads as four equal columns rather
 * than four labels of different widths, and so the state of each is legible at
 * a glance. That state is the point of the thing. A lyrics button that looks
 * identical whether lyrics are showing or not leaves you tapping it to find
 * out, which is how you end up turning off what you meant to turn on.
 */
@Composable
fun PlayerBottomButton(
    icon: Int,
    label: String,
    active: Boolean,
    tint: Color,
    activeTint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    // Disabled is dimmer still, so a guest in a listening room can see that the
    // control is not theirs rather than tapping at something inert.
    val color =
        when {
            !enabled -> tint.copy(alpha = 0.35f)
            active -> activeTint
            else -> tint.copy(alpha = 0.85f)
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            modifier
                // No ripple and no shape behind it: on a player whose
                // background is album art, a grey rectangle flashing under a
                // label is louder than the thing it is meant to acknowledge.
                // The colour change is the acknowledgement.
                .clickable(
                    enabled = enabled,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                )
                .padding(vertical = 6.dp, horizontal = 4.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription ?: label,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
