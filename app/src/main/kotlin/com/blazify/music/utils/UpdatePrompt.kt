/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.utils

import java.util.concurrent.TimeUnit

/**
 * When a new version is offered, so it is seen without nagging.
 *
 * - The popup shows the first time the app is opened after a version comes out.
 * - "Later" puts it off for a week; then it shows once more, and that version is never offered
 *   in a popup again. The card in Settings and the dot on About stay until the update is in.
 * - The notification is posted once per version, not each time the app is opened.
 */
object UpdatePrompt {
    const val REMIND_AFTER_DAYS = 7L

    /**
     * Whether to open the popup for [tag]: never put off yet, or put off at [declinedAt] a week
     * ago or more and not reminded about since.
     */
    fun shouldOffer(
        tag: String,
        declinedTag: String?,
        declinedAt: Long?,
        remindedTag: String?,
        now: Long,
    ): Boolean {
        if (declinedTag != tag) return true
        if (remindedTag == tag) return false
        return declinedAt != null && now >= declinedAt + TimeUnit.DAYS.toMillis(REMIND_AFTER_DAYS)
    }

    /** Whether this offer is the week-later reminder rather than the first. */
    fun isReminder(tag: String, declinedTag: String?) = declinedTag == tag

    /** Whether to post the notification for [tag]. */
    fun shouldNotify(tag: String, notifiedTag: String?) = notifiedTag != tag
}
