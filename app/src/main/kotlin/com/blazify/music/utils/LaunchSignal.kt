/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.utils

import android.graphics.RectF
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * What the launch screen's logo and Home's header logo tell each other, so the one can
 * land on the other and become it.
 */
object LaunchSignal {
    /** Home has its shelves on screen; started any earlier, the glide stalls halfway. */
    val homeSettled = MutableStateFlow(false)

    /**
     * Home has been built (not yet drawn). The first frame waits for this; it cannot wait for
     * [homeSettled], which needs frames drawn, so it always ran into its time limit instead.
     */
    val homeComposed = MutableStateFlow(false)

    /** Where the header's logo is drawn, in window pixels. */
    val headerLogo = MutableStateFlow<RectF?>(null)

    /** False while the launch logo is on its way, so there are never two logos at once. */
    val headerLogoShown = MutableStateFlow(true)
}
