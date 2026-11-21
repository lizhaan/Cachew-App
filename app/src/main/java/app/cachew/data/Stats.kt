package app.cachew.data

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** One of the three totals at the top of the Waiting list tab: "$127 · waiting · 2". */
data class Total(val amount: Double, val count: Int)

/** The three totals of the Waiting list tab (design-v2/08). */
data class ListTotals(val waiting: Total, val saved: Total, val bought: Total)

/** Everything the Stats tab shows (design-v2/09), for one month. Null = no data yet (shown as "–"). */
data class MonthStats(
    /** Money kept instead of spent ("Let it go") this month. */
    val kept: Double,
    /** Kept per week of the month: days 1–7, 8–14, 15–21, 22–end. */
    val weeks: List<Double>,
    /** How many times a shop was opened and Cachew paused it. */
    val pauses: Int,
    /** "6 of 9 wants you let go": let go / all decided this month. */
    val letGo: Int,
    val decided: Int,
    /** Hour of the day (0–23) with the most shop openings. */
    val riskiestHour: Int?,
    /** Package of the shop opened most often. */
    val topShop: String?,
)

/**
 * Stats and totals computed from the real data (items + pause log), as pure functions
 * (no Android), so they are easy to test (StatsTest) and to explain.
 */
object Stats {

    fun listTotals(items: List<WaitItem>, month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): ListTotals {
        fun total(list: List<WaitItem>) = Total(list.sumOf { it.price ?: 0.0 }, list.size)
        val decidedThisMonth = items.filter { it.decidedAt != null && month(it.decidedAt, zone) == month }
        return ListTotals(
            waiting = total(items.filter { it.status == WaitItem.Status.Waiting }),
            saved = total(decidedThisMonth.filter { it.status == WaitItem.Status.Kept }),
            bought = total(decidedThisMonth.filter { it.status == WaitItem.Status.Bought || it.status == WaitItem.Status.BoughtWithHeart }),
        )
    }

    fun month(items: List<WaitItem>, log: List<PauseRecord>, month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): MonthStats {
        val decided = items.filter { it.decidedAt != null && it.status != WaitItem.Status.Waiting && month(it.decidedAt, zone) == month }
        val kept = decided.filter { it.status == WaitItem.Status.Kept }
        val weeks = MutableList(4) { 0.0 }
        kept.forEach { item ->
            val day = Instant.ofEpochMilli(item.decidedAt!!).atZone(zone).dayOfMonth
            weeks[((day - 1) / 7).coerceAtMost(3)] += item.price ?: 0.0
        }
        val entries = log.filter { month(it.at, zone) == month }
        return MonthStats(
            kept = kept.sumOf { it.price ?: 0.0 },
            weeks = weeks,
            pauses = entries.size,
            letGo = kept.size,
            decided = decided.size,
            riskiestHour = entries.groupingBy { Instant.ofEpochMilli(it.at).atZone(zone).hour }.eachCount().maxByOrNull { it.value }?.key,
            topShop = entries.groupingBy { it.shopPackage }.eachCount().maxByOrNull { it.value }?.key,
        )
    }

    /** Money kept ("Let it go") since a savings goal was set. */
    fun goalProgress(items: List<WaitItem>, goal: SavingsGoal): Double =
        items.filter { it.status == WaitItem.Status.Kept && (it.decidedAt ?: 0) >= goal.setAt }.sumOf { it.price ?: 0.0 }

    private fun month(millis: Long, zone: ZoneId): YearMonth = YearMonth.from(Instant.ofEpochMilli(millis).atZone(zone))
}
