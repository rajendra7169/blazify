/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.wrapped

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

/**
 * Which year Wrapped looks back on, and when it is offered.
 *
 * The year used to be fixed to 2025, so once that season ended Wrapped never came back. Now
 * December looks back on the year that is ending, and every other month on the last full year.
 * The Home card is offered in December and January, the way a yearly recap is.
 */
object WrappedConstants {
    fun year(today: LocalDate = LocalDate.now()): Int =
        if (today.month == Month.DECEMBER) today.year else today.year - 1

    fun isSeason(today: LocalDate = LocalDate.now()): Boolean =
        today.month == Month.DECEMBER || today.month == Month.JANUARY

    fun playlistName(year: Int): String = "Blazify $year"

    fun start(year: Int): LocalDateTime = LocalDateTime.of(year, 1, 1, 0, 0, 0)

    fun end(year: Int): LocalDateTime = LocalDateTime.of(year, 12, 31, 23, 59, 59)
}
