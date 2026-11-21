package app.cachew.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Answer to "How are you feeling right now?" (screen 14). */
enum class Mood { Tired, Bored, Stressed, Fine }

/** What the user chose on screen 15. */
enum class Decision { Waiting, Heart, Bought }

/**
 * The game rules in one place, as plain functions with no Android dependencies,
 * so they are easy to unit-test (see GameTest) and easy to explain.
 */
object Game {

    // Kept until stage 3 replaces "Add to waiting list" (v2: cashews come from Share instead).
    const val CASHEWS_PER_PAUSE = Rules.CASHEWS_PER_SAVED_ITEM
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

    // ---- Decision headline (screen 15) ---------------------------------------------------

    /** Pieces the headline is built from; the UI turns them into words. */
    enum class Part { Tired, Bored, Stressed, Late, NotOnList }

    /**
     * Risk signals in a fixed order: mood (unless Fine), "late" at night, "not on your list".
     * Empty list = "On your list and feeling fine."
     * Non-empty and on the list = "<parts>, but on your list." Otherwise "<parts>."
     */
    fun headlineParts(mood: Mood, onList: Boolean, night: Boolean): List<Part> = buildList {
        when (mood) {
            Mood.Tired -> add(Part.Tired)
            Mood.Bored -> add(Part.Bored)
            Mood.Stressed -> add(Part.Stressed)
            Mood.Fine -> Unit
        }
        if (night) add(Part.Late)
        if (!onList) add(Part.NotOnList)
    }
}
