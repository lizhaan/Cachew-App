package app.cachew.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Answer to "How are you feeling right now?" (screen 14). */
enum class Mood { Tired, Bored, Stressed, Fine }


/**
 * The game rules in one place, as plain functions with no Android dependencies,
 * so they are easy to unit-test (see GameTest) and easy to explain.
 */
object Game {

    const val HEARTS_PER_MONTH = Rules.HEARTS_PER_MONTH

    // ---- Streak: "Days in a row without an impulse buy" (mockup 07) -------------------

    /**
     * Every calendar day without "Buy it anyway" counts, even days with no pauses at all.
     *  - Never bought anyway: days since the first day of using Cachew, counting today (day 1 = 1).
     *  - After "Buy it anyway": that day is 0, the next day 1, and so on.
     * Missed days never break the streak; only "Buy it anyway" does.
     */
    fun streak(today: LocalDate, firstDay: LocalDate, lastBuyAnyway: LocalDate?): Int {
        val days = if (lastBuyAnyway == null) {
            ChronoUnit.DAYS.between(firstDay, today) + 1
        } else {
            ChronoUnit.DAYS.between(lastBuyAnyway, today)
        }
        return days.toInt().coerceAtLeast(0)
    }

    // ---- Hearts: 3 a month, refilled on the 1st ----------------------------------------

    /** [usedIn] = the month in which [used] hearts were spent; a newer month means a full refill. */
    fun heartsLeft(thisMonth: YearMonth, usedIn: YearMonth?, used: Int): Int =
        if (usedIn == thisMonth) (HEARTS_PER_MONTH - used).coerceIn(0, HEARTS_PER_MONTH) else HEARTS_PER_MONTH

    /** The day hearts come back: the 1st of next month. */
    fun heartsRefillDate(today: LocalDate): LocalDate = today.withDayOfMonth(1).plusMonths(1)

    // ---- Night shield window -----------------------------------------------------------

    val DEFAULT_NIGHT_FROM: LocalTime = Rules.DEFAULT_NIGHT_FROM
    val DEFAULT_NIGHT_TO: LocalTime = Rules.DEFAULT_NIGHT_TO

    /** True if [time] is inside the from→to window; windows that cross midnight work too. */
    fun isNight(time: LocalTime, from: LocalTime = DEFAULT_NIGHT_FROM, to: LocalTime = DEFAULT_NIGHT_TO): Boolean =
        if (from < to) time >= from && time < to else time >= from || time < to

    /**
     * Reminders only come in the daytime. If [due] falls inside the night window, the reminder
     * moves to the end of the window (07:00); the Boolean says whether it was moved
     * (then the text adds "Penny waited till morning to ask.").
     */
    fun reminderTime(
        due: LocalDateTime,
        from: LocalTime = DEFAULT_NIGHT_FROM,
        to: LocalTime = DEFAULT_NIGHT_TO,
    ): Pair<LocalDateTime, Boolean> {
        if (!isNight(due.toLocalTime(), from, to)) return due to false
        // After midnight (before "to") the morning is the same date; before midnight it's tomorrow.
        val morning = if (due.toLocalTime() < to) due.toLocalDate() else due.toLocalDate().plusDays(1)
        return morning.atTime(to) to true
    }

    // ---- Levels (by cashews in the stash) ------------------------------------------------

    /**
     * Penny's level: 1 Kit, 2 Forager, 3 Keeper, 4 Stasher, 5 Winter-ready.
     * [from] = cashews needed. Names and rewards ("a winter scarf") live in strings.xml,
     * indexed by [number] − 1. The stash never shrinks, so a level is never lost.
     */
    data class Level(val number: Int, val from: Int)

    val LEVELS: List<Level> = Rules.LEVEL_THRESHOLDS.mapIndexed { i, from -> Level(i + 1, from) }

    fun levelFor(cashews: Int): Level = LEVELS.last { cashews >= it.from }

    /** The level after this one, or null at the top. */
    fun nextLevel(level: Level): Level? = LEVELS.getOrNull(level.number)

    /** "Let it go" reward: one cashew per dollar, rounded ($19.99 → 20). Null price → 0. */
    fun cashewsForLetGo(price: Double?): Int =
        price?.let { Math.round(it * Rules.CASHEWS_PER_DOLLAR_LET_GO).toInt() } ?: 0

    // ---- Choice screen headline (design-v2/02) --------------------------------------------

    /**
     * Built only from the two quiz answers (not the clock):
     *  Tired + not on list → "Tired, just scrolling."      Fine + not on list → "Feeling fine, just scrolling."
     *  Tired + on list     → "Tired, but on your list."    Fine + on list     → "On your list and feeling fine."
     */
    enum class Headline { MoodScrolling, MoodButOnList, FineScrolling, AllGood }

    fun headline(mood: Mood, onList: Boolean): Headline = when {
        mood == Mood.Fine && onList -> Headline.AllGood
        mood == Mood.Fine -> Headline.FineScrolling
        onList -> Headline.MoodButOnList
        else -> Headline.MoodScrolling
    }

    /** Anything other than "fine and on the list" is a risky moment ("That's when most regret buys happen"). */
    fun isRisky(mood: Mood, onList: Boolean): Boolean = headline(mood, onList) != Headline.AllGood
}
