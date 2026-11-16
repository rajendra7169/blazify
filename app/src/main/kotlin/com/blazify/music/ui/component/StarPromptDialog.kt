/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.blazify.music.R

/**
 * Asks for a star on GitHub: a gold star with a note engraved in it, two lines and two
 * buttons. "Star it" opens GitHub and "Already starred" says so; either way it never asks
 * again. Tapping outside only puts it off until the next ask.
 */
@Composable
fun StarPromptDialog(
    onStar: () -> Unit,
    onLater: () -> Unit,
    onAlreadyStarred: () -> Unit,
) {
    BlazePromptCard(
        title = stringResource(R.string.star_prompt_title_short),
        onDismissRequest = onLater,
        buttons =
            PromptButtons(
                primary = stringResource(R.string.star_prompt_yes),
                onPrimary = onStar,
                secondary = stringResource(R.string.star_prompt_already),
                onSecondary = onAlreadyStarred,
            ),
        art = { time -> drawStar(time) },
    ) {
        PromptBody(stringResource(R.string.star_prompt_body_short))
    }
}
