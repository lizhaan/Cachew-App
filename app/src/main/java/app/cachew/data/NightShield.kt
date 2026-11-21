package app.cachew.data

import java.time.LocalTime

/**
 * Night shield settings (screen 10): during these hours the picked shops are fully blocked
 * (no pause, no questions, no hearts). Also the quiet hours for reminders.
 *
 *  - [on] = false: shops are never blocked; reminders still stay out of 22:00–09:00.
 *  - The 2-hour rule: from 2 hours before [from] until [to], the schedule can't be changed and the
 *    shield can't be turned off. Making the window longer (or turning the shield on) is always fine.
 */
data class NightShield(
    val on: Boolean = true,
    val from: LocalTime = Rules.DEFAULT_NIGHT_FROM,
    val to: LocalTime = Rules.DEFAULT_NIGHT_TO,
) {
    /** Is [time] inside the shield's window (and the shield on)? */
    fun isNight(time: LocalTime): Boolean = on && Game.isNight(time, from, to)

    /** Is the schedule locked right now (from − 2 h … to)? */
    fun isLocked(time: LocalTime): Boolean =
        on && Game.isNight(time, from.minusHours(Rules.SHIELD_LOCK_HOURS.toLong()), to)

    /**
     * May the user save [next] at [time]? Outside the lock, anything goes. While locked, only
     * changes that keep the shield on and cover every minute of the current window (= longer).
     */
    fun allows(next: NightShield, time: LocalTime): Boolean =
        !isLocked(time) || (next.on && covers(next, this))

    /** Hours when reminders wait for the morning. */
    val quietFrom: LocalTime get() = if (on) from else QUIET_FROM_WHEN_OFF
    val quietTo: LocalTime get() = if (on) to else QUIET_TO_WHEN_OFF

    companion object {
        val QUIET_FROM_WHEN_OFF: LocalTime = Rules.QUIET_FROM_WHEN_OFF
        val QUIET_TO_WHEN_OFF: LocalTime = Rules.QUIET_TO_WHEN_OFF

        /** A window needs two different times ("from 23:00 to 23:00" means nothing). */
        fun isValid(from: LocalTime, to: LocalTime): Boolean = from != to

        /** Does [outer]'s window contain every minute of [inner]'s? (Windows may cross midnight.) */
        fun covers(outer: NightShield, inner: NightShield): Boolean =
            (0 until 24 * 60).all { m ->
                val t = LocalTime.of(m / 60, m % 60)
                !Game.isNight(t, inner.from, inner.to) || Game.isNight(t, outer.from, outer.to)
            }
    }
}
