package app.cachew.data

/**
 * The 2-question onboarding quiz and the "Your result" numbers built from it.
 * Pure Kotlin (no Android), so it's easy to test (QuizTest).
 */
object Quiz {

    /** Question 1: "When do impulse buys usually happen?" (option ids, stored in Store.profile["when"]). */
    val WHEN = listOf("late_night", "bored", "hard_day", "payday")

    /** Question 2: "Roughly how much goes to things you regret, per month?" → middle of the range, $. */
    val SPEND_PER_MONTH = linkedMapOf("under_50" to 25, "50_150" to 100, "150_300" to 225, "over_300" to 350)

    /** "Impulse buys cost you about $1,200 a year". */
    fun yearlyCost(spendId: String?): Int = (SPEND_PER_MONTH[spendId] ?: SPEND_PER_MONTH.getValue("50_150")) * 12

    /** Hours on the result chart: 9 AM, 1 PM, 6 PM, 11 PM, 2 AM. */
    val CHART_HOURS = listOf(9, 13, 18, 23, 2)

    /** Which chart bar is "your" window (the tall purple one), from question 1. */
    fun peakBar(whenId: String?): Int = when (whenId) {
        "bored" -> 1     // 1 PM
        "hard_day" -> 2  // 6 PM
        "payday" -> 0    // 9 AM
        else -> 3        // late at night: 11 PM
    }

    /** "Kept instead, that's …": index into strings.xml quiz_kept_things, by how much it is. */
    fun keptThing(yearly: Int): Int = when {
        yearly <= 300 -> 0
        yearly <= 1200 -> 1
        yearly <= 2700 -> 2
        else -> 3
    }
}
