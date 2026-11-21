package app.cachew.data

import java.time.LocalTime

/**
 * Every number the game rules use, in one place (CLAUDE.md v2, "Ігрова механіка").
 * Change a rule here and the whole app follows. No Android dependencies, so tests can use it too.
 */
object Rules {

    // ---- Pause and sessions -----------------------------------------------------------

    /** The breathing countdown on top of a shop. */
    const val PAUSE_SECONDS = 10

    /** "Just look" / "I need something specific": how long the shop stays open. */
    const val SESSION_MINUTES = 10

    /** "Just look": the check-in sheet slides up at this minute. */
    const val CHECK_IN_MINUTE = 5

    /** "Specific": Penny peeks in once, this many minutes before the end. */
    const val SPECIFIC_WARNING_MINUTES_LEFT = 2

    /** "Buy it" from the waiting list: short pause, then this long in the shop. */
    const val BUY_SESSION_MINUTES = 5

    // ---- Cashews (the only currency; never spent, the stash only grows) ---------------

    /** Every item saved through Share. */
    const val CASHEWS_PER_SAVED_ITEM = 15

    /** "Close <shop>" on the choice screen or "I'm done" on the check-in sheet... */
    const val CASHEWS_PER_CLOSE = 5

    /** ...but only this many times a day. */
    const val REWARDED_CLOSES_PER_DAY = 3

    /** "Let it go": one cashew per dollar of the item's price. */
    const val CASHEWS_PER_DOLLAR_LET_GO = 1

    /** On the 1st of the month, for every heart left unused last month. */
    const val CASHEWS_PER_UNUSED_HEART = 50

    /** Streak milestones (days → bonus cashews). */
    val STREAK_BONUSES: Map<Int, Int> = mapOf(7 to 50, 30 to 200)

    // ---- Levels: by cashews in the stash; a level is never lost ------------------------

    /** Cashews needed for level 1..5 (names and rewards are in strings.xml: level_names, level_rewards). */
    val LEVEL_THRESHOLDS: List<Int> = listOf(0, 50, 150, 400, 1000)

    // ---- Hearts ------------------------------------------------------------------------

    /** Refilled on the 1st of every month. Only use: buy a waiting item before its timer ends. */
    const val HEARTS_PER_MONTH = 3

    // ---- Waiting list ------------------------------------------------------------------

    val WAIT_HOURS_OPTIONS: List<Int> = listOf(24, 48, 72)
    const val DEFAULT_WAIT_HOURS = 48

    /**
     * Fixed rates for prices found in other currencies (how many dollars one unit is worth).
     * Approximate (November 2026); only used when a shop page shows a non-dollar price.
     */
    val USD_PER_UNIT: Map<String, Double> = mapOf(
        "USD" to 1.0,
        "UAH" to 1 / 41.5,
        "EUR" to 1.08,
        "GBP" to 1.27,
        "PLN" to 0.25,
        "CAD" to 0.72,
    )

    /** No answer to "Still want it?" for this long → one more reminder, "Let it go?". */
    const val LET_GO_NUDGE_DAYS = 7

    // ---- Night shield ------------------------------------------------------------------

    val DEFAULT_NIGHT_FROM: LocalTime = LocalTime.of(23, 0)
    val DEFAULT_NIGHT_TO: LocalTime = LocalTime.of(7, 0)

    /** From this many hours before the shield starts until it ends, the schedule is locked. */
    const val SHIELD_LOCK_HOURS = 2

    /** Quiet hours for reminders when the shield is off. */
    val QUIET_FROM_WHEN_OFF: LocalTime = LocalTime.of(22, 0)
    val QUIET_TO_WHEN_OFF: LocalTime = LocalTime.of(9, 0)
}
