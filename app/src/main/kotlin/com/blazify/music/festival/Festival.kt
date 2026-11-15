/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * Festival touches on the Home logo. A festival shows only in the countries that celebrate
 * it (where it is a public holiday; Halloween and the global days everywhere), from a week
 * before it until two days after: what a country's own calendar celebrates, never a guess
 * about the person holding the phone. Everything is decided on the phone; nothing about it
 * is sent anywhere.
 */

package com.blazify.music.festival

import android.content.Context
import android.telephony.TelephonyManager
import androidx.annotation.StringRes
import com.blazify.music.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class Festival(
    val id: String, // stable, persisted — never rename
    @StringRes val greetingRes: Int,
    /** Where it is a public holiday, as ISO country codes; empty means everywhere. */
    val countries: Set<String>,
    /** Its days, first and last included. Lunar dates come from each country's official calendar. */
    val days: List<ClosedRange<LocalDate>>,
    /** With [countries] empty, the countries where it is not a public holiday. */
    val notIn: Set<String> = emptySet(),
) {
    // Tihar in Nepal, Diwali or Deepavali elsewhere: one festival of lights, with its local name.
    // Nepal: Kag Tihar to Bhai Tika, per the Nepal Calendar Determination Committee; elsewhere
    // Dhanteras to Bhai Dooj. The two fall within a day of each other, so one span covers both.
    DIWALI(
        id = "diwali",
        greetingRes = R.string.festival_greeting_diwali,
        countries = setOf("NP", "IN", "MY", "SG", "LK", "MU", "FJ", "TT", "GY", "SR"),
        days = listOf(
            LocalDate.of(2026, 11, 6)..LocalDate.of(2026, 11, 11),
            LocalDate.of(2027, 10, 27)..LocalDate.of(2027, 10, 31),
        ),
    ),

    // Not a public holiday anywhere, but kept in cities all over the world, so it shows everywhere.
    // Where Diwali falls on the same days, as in 2027, Diwali shows instead.
    HALLOWEEN(
        id = "halloween",
        greetingRes = R.string.festival_greeting_halloween,
        countries = emptySet(),
        days = listOf(
            LocalDate.of(2027, 10, 31)..LocalDate.of(2027, 10, 31),
            LocalDate.of(2028, 10, 31)..LocalDate.of(2028, 10, 31),
        ),
    ),

    CHRISTMAS(
        id = "christmas",
        greetingRes = R.string.festival_greeting_christmas,
        countries = emptySet(),
        days = listOf(
            LocalDate.of(2026, 12, 24)..LocalDate.of(2026, 12, 26),
            LocalDate.of(2027, 12, 24)..LocalDate.of(2027, 12, 26),
        ),
        notIn = setOf(
            "CN", "JP", "TW", "TH", "VN", "KH", "LA", "MN", "BT", "MV", "AF", "IR", "SA", "AE", "QA",
            "KW", "BH", "OM", "YE", "TR", "IL", "LY", "MA", "TN", "MR", "SO", "DZ", "KP",
            // Orthodox Christmas on 7 January, with no holiday on 25 December: New Year shows there.
            "RU", "RS", "ME", "MK", "GE", "AM", "KZ", "KG", "UZ", "TJ", "TM", "AZ", "EG", "ET", "ER",
        ),
    ),

    NEW_YEAR(
        id = "new_year",
        greetingRes = R.string.festival_greeting_new_year,
        countries = emptySet(),
        days = listOf(
            LocalDate.of(2026, 12, 31)..LocalDate.of(2027, 1, 1),
            LocalDate.of(2027, 12, 31)..LocalDate.of(2028, 1, 1),
        ),
    ),

    MUSIC_DAY(
        id = "music_day",
        greetingRes = R.string.festival_greeting_music_day,
        countries = emptySet(),
        days = listOf(
            LocalDate.of(2027, 6, 21)..LocalDate.of(2027, 6, 21),
            LocalDate.of(2028, 6, 21)..LocalDate.of(2028, 6, 21),
        ),
    ),

    // The festivals below follow lunar calendars. Their days are added from each country's
    // official calendar once it is published; until then they never show by themselves.
    HOLI(id = "holi", greetingRes = R.string.festival_greeting_holi, countries = setOf("IN", "NP"), days = emptyList()),
    DASHAIN(id = "dashain", greetingRes = R.string.festival_greeting_dashain, countries = setOf("NP"), days = emptyList()),
    LUNAR_NEW_YEAR(
        id = "lunar_new_year",
        greetingRes = R.string.festival_greeting_lunar_new_year,
        countries = setOf("CN", "TW", "HK", "MO", "SG", "MY", "VN", "KR", "BN"),
        days = emptyList(),
    ),
    EID_AL_FITR(id = "eid_al_fitr", greetingRes = R.string.festival_greeting_eid, countries = EID_COUNTRIES, days = emptyList()),
    EID_AL_ADHA(id = "eid_al_adha", greetingRes = R.string.festival_greeting_eid, countries = EID_COUNTRIES, days = emptyList()),
    ;

    /** Its greeting where it is celebrated: Diwali is Tihar in Nepal. */
    @StringRes
    fun greetingIn(country: String?): Int =
        if (this == DIWALI && country.equals("NP", ignoreCase = true)) R.string.festival_greeting_tihar else greetingRes

    companion object {
        /** The celebration starts this many days before a festival's first day… */
        const val DAYS_BEFORE = 7L

        /** …and lasts this many days after its last. */
        const val DAYS_AFTER = 2L

        /**
         * The festival for [country] on [date], if any: from a week before it until two days after.
         * When two overlap, as Christmas and New Year do, the one nearer to [date] shows, and on a
         * tie the one still to come, so Christmas hands over to New Year once it is past.
         */
        fun on(date: LocalDate, country: String?): Festival? {
            val code = country?.uppercase(Locale.ROOT) ?: return null
            return entries
                .filter { if (it.countries.isEmpty()) code !in it.notIn else code in it.countries }
                .flatMap { festival -> festival.days.map { festival to it } }
                .filter { (_, days) -> date in days.start.minusDays(DAYS_BEFORE)..days.endInclusive.plusDays(DAYS_AFTER) }
                .minWithOrNull(
                    compareBy<Pair<Festival, ClosedRange<LocalDate>>> { (_, days) -> distance(date, days) }
                        .thenBy { (_, days) -> if (date < days.start) 0 else 1 },
                )?.first
        }

        /** Days from [date] to [days]; 0 on the festival itself. */
        private fun distance(date: LocalDate, days: ClosedRange<LocalDate>): Long =
            when {
                date < days.start -> ChronoUnit.DAYS.between(date, days.start)
                date > days.endInclusive -> ChronoUnit.DAYS.between(days.endInclusive, date)
                else -> 0L
            }
    }
}

/** Where both Eids are public holidays: Muslim-majority countries and South Asia. */
private val EID_COUNTRIES =
    setOf(
        "SA", "AE", "QA", "KW", "BH", "OM", "YE", "JO", "IQ", "SY", "LB", "PS", "EG", "LY", "TN", "DZ", "MA",
        "MR", "SD", "SO", "DJ", "TR", "AZ", "IR", "AF", "PK", "BD", "IN", "MV", "MY", "ID", "BN", "SN", "ML",
        "NE", "TD", "GM", "GN", "SL", "NG", "KZ", "KG", "TJ", "TM", "UZ", "AL", "BA", "XK", "LK",
    )

/**
 * The country the festivals follow, read on the phone and never sent anywhere: the one chosen
 * in Settings › Content, else the mobile network's, else the SIM's, else the phone's region.
 * The network and SIM come first among the automatic ones because so many phones are set to
 * "English (United States)" wherever they are.
 */
fun festivalCountry(context: Context, chosenCountry: String?): String? {
    chosenCountry?.takeIf { it.isNotBlank() }?.let { return it }
    val telephony = context.getSystemService(TelephonyManager::class.java)
    val fromPhone =
        runCatching { telephony?.networkCountryIso }.getOrNull()?.takeIf { it.length == 2 }
            ?: runCatching { telephony?.simCountryIso }.getOrNull()?.takeIf { it.length == 2 }
    return fromPhone ?: Locale.getDefault().country.takeIf { it.length == 2 }
}
