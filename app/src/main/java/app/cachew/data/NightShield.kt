package app.cachew.data

import java.time.LocalTime

/**
 * Night shield settings (screen 10): the user's riskiest hours get a stricter pause.
 *
 *  - [on] = false: no "night" at all for the pause; reminders still stay out of 22:00–09:00.
 *  - [strict]: at night there is no "Buy it anyway" on screen 15 (only wait or use a heart).
 *    Soft keeps the daytime choices at night too.
 */
data class NightShield(
    val on: Boolean = true,
    val from: LocalTime = Game.DEFAULT_NIGHT_FROM,
    val to: LocalTime = Game.DEFAULT_NIGHT_TO,
    val strict: Boolean = true,
) {
    /** Is [time] inside the shield's window (and the shield on)? */
    fun isNight(time: LocalTime): Boolean = on && Game.isNight(time, from, to)

    /** Night and Strict: hide "Buy it anyway". */
    fun blocksBuyAnyway(night: Boolean): Boolean = night && strict

    /** Hours when reminders wait for the morning. */
    val quietFrom: LocalTime get() = if (on) from else QUIET_FROM_WHEN_OFF
    val quietTo: LocalTime get() = if (on) to else QUIET_TO_WHEN_OFF

    companion object {
        val QUIET_FROM_WHEN_OFF: LocalTime = LocalTime.of(22, 0)
        val QUIET_TO_WHEN_OFF: LocalTime = LocalTime.of(9, 0)

        /** A window needs two different times ("from 23:00 to 23:00" means nothing). */
        fun isValid(from: LocalTime, to: LocalTime): Boolean = from != to
    }
}
