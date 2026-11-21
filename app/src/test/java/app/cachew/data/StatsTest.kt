package app.cachew.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

class StatsTest {

    private val zone = ZoneOffset.UTC
    private val nov = YearMonth.of(2026, 11)

    private fun at(day: Int, hour: Int = 12, month: Int = 11) =
        LocalDateTime.of(2026, month, day, hour, 0).toInstant(zone).toEpochMilli()

    private fun item(id: String, price: Double?, status: WaitItem.Status, decidedDay: Int? = null, month: Int = 11) = WaitItem(
        id = id, title = id, price = price, currency = "USD", shopPackage = "shop", addedAt = at(1),
        waitMillis = 0, status = status, decidedAt = decidedDay?.let { at(it, month = month) },
    )

    private val items = listOf(
        item("waiting", 89.0, WaitItem.Status.Waiting),
        item("sweater", 64.0, WaitItem.Status.Kept, decidedDay = 16),
        item("case", 22.0, WaitItem.Status.Kept, decidedDay = 3),
        item("lamp", 31.0, WaitItem.Status.BoughtWithHeart, decidedDay = 10),
        item("old", 50.0, WaitItem.Status.Kept, decidedDay = 20, month = 10), // October: not counted
    )

    @Test fun waitingListTotals() {
        val t = Stats.listTotals(items, nov, zone)
        assertEquals(Total(89.0, 1), t.waiting)
        assertEquals(Total(86.0, 2), t.saved)   // $64 + $22, as on design-v2/08
        assertEquals(Total(31.0, 1), t.bought)
    }

    @Test fun monthStats() {
        val log = listOf(
            PauseRecord(at(2, 23), "com.zzkko", Mood.Tired, false, EntryAction.JustLook),
            PauseRecord(at(5, 23), "com.zzkko", Mood.Bored, false, EntryAction.Closed),
            PauseRecord(at(6, 14), "com.amazon", Mood.Fine, true, EntryAction.Specific),
        )
        val s = Stats.month(items, log, nov, zone)
        assertEquals(86.0, s.kept, 0.001)
        assertEquals(listOf(22.0, 0.0, 64.0, 0.0), s.weeks)
        assertEquals(3, s.pauses)
        assertEquals(2, s.letGo)
        assertEquals(3, s.decided)
        assertEquals(23, s.riskiestHour)
        assertEquals("com.zzkko", s.topShop)
    }

    @Test fun noDataYet() {
        val s = Stats.month(emptyList(), emptyList(), nov, zone)
        assertEquals(0, s.pauses)
        assertNull(s.riskiestHour)
        assertNull(s.topShop)
    }

    @Test fun goalCountsOnlyWhatWasKeptAfterItWasSet() {
        val goal = SavingsGoal("Tokyo", 900.0, setAt = at(10))
        assertEquals(64.0, Stats.goalProgress(items, goal), 0.001)
    }
}
