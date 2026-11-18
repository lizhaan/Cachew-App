package app.cachew.data

import app.cachew.data.Game.Part
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

class GameTest {

    private val nov10 = LocalDate.of(2026, 11, 10)

    // ---- Streak -------------------------------------------------------------------

    @Test fun streak_firstDayCountsAsOne() {
        assertEquals(1, Game.streak(today = nov10, firstDay = nov10, lastBuyAnyway = null))
    }

    @Test fun streak_growsEveryCalendarDay_evenWithoutPauses() {
        assertEquals(5, Game.streak(today = nov10.plusDays(4), firstDay = nov10, lastBuyAnyway = null))
    }

    @Test fun streak_buyAnywayResetsToZeroThatDay() {
        assertEquals(0, Game.streak(today = nov10.plusDays(3), firstDay = nov10, lastBuyAnyway = nov10.plusDays(3)))
    }

    @Test fun streak_countsDaysAfterBuyAnyway() {
        assertEquals(2, Game.streak(today = nov10.plusDays(5), firstDay = nov10, lastBuyAnyway = nov10.plusDays(3)))
    }

    @Test fun streak_missedDaysDoNotBreakIt() {
        // Cachew not opened for 30 days: those days still count.
        assertEquals(31, Game.streak(today = nov10.plusDays(30), firstDay = nov10, lastBuyAnyway = null))
    }

    // ---- Hearts -------------------------------------------------------------------

    @Test fun hearts_fullWhenNoneUsed() {
        assertEquals(3, Game.heartsLeft(YearMonth.of(2026, 11), usedIn = null, used = 0))
    }

    @Test fun hearts_usedThisMonth() {
        assertEquals(1, Game.heartsLeft(YearMonth.of(2026, 11), usedIn = YearMonth.of(2026, 11), used = 2))
    }

    @Test fun hearts_neverBelowZero() {
        assertEquals(0, Game.heartsLeft(YearMonth.of(2026, 11), usedIn = YearMonth.of(2026, 11), used = 5))
    }

    @Test fun hearts_refillInNewMonth() {
        assertEquals(3, Game.heartsLeft(YearMonth.of(2026, 12), usedIn = YearMonth.of(2026, 11), used = 3))
    }

    @Test fun hearts_refillDateIsFirstOfNextMonth() {
        assertEquals(LocalDate.of(2026, 12, 1), Game.heartsRefillDate(nov10))
        assertEquals(LocalDate.of(2027, 1, 1), Game.heartsRefillDate(LocalDate.of(2026, 12, 31)))
    }

    // ---- Night --------------------------------------------------------------------

    @Test fun night_windowAcrossMidnight() {
        assertTrue(Game.isNight(LocalTime.of(23, 42)))
        assertTrue(Game.isNight(LocalTime.of(2, 0)))
        assertFalse(Game.isNight(LocalTime.of(7, 0)))
        assertFalse(Game.isNight(LocalTime.of(14, 0)))
    }

    @Test fun night_windowWithinOneDay() {
        assertTrue(Game.isNight(LocalTime.of(13, 0), from = LocalTime.of(12, 0), to = LocalTime.of(14, 0)))
        assertFalse(Game.isNight(LocalTime.of(15, 0), from = LocalTime.of(12, 0), to = LocalTime.of(14, 0)))
    }

    // ---- Decision headline -------------------------------------------------------

    @Test fun headline_mockupCase() {
        // "Tired, late, not on your list."
        assertEquals(listOf(Part.Tired, Part.Late, Part.NotOnList), Game.headlineParts(Mood.Tired, onList = false, night = true))
    }

    @Test fun headline_allGood() {
        // "On your list and feeling fine."
        assertEquals(emptyList<Part>(), Game.headlineParts(Mood.Fine, onList = true, night = false))
    }

    @Test fun headline_fineButNotOnList() {
        assertEquals(listOf(Part.NotOnList), Game.headlineParts(Mood.Fine, onList = false, night = false))
    }

    @Test fun headline_stressedButOnList() {
        assertEquals(listOf(Part.Stressed), Game.headlineParts(Mood.Stressed, onList = true, night = false))
    }

    // ---- Reminders: only in the daytime ------------------------------------------

    @Test fun reminder_daytimeStaysAsIs() {
        val due = LocalDateTime.of(2026, 11, 12, 14, 30)
        assertEquals(due to false, Game.reminderTime(due))
    }

    @Test fun reminder_lateEveningMovesToNextMorning() {
        val due = LocalDateTime.of(2026, 11, 12, 23, 43)
        assertEquals(LocalDateTime.of(2026, 11, 13, 7, 0) to true, Game.reminderTime(due))
    }

    @Test fun reminder_afterMidnightMovesToSameMorning() {
        val due = LocalDateTime.of(2026, 11, 13, 2, 15)
        assertEquals(LocalDateTime.of(2026, 11, 13, 7, 0) to true, Game.reminderTime(due))
    }

    // ---- Night shield ---------------------------------------------------------------

    @Test fun shield_offMeansNoNight_butRemindersStillQuiet() {
        val off = NightShield(on = false)
        assertFalse(off.isNight(LocalTime.of(23, 30)))
        // Reminders still wait for the morning: 22:00–09:00 when the shield is off.
        val due = LocalDateTime.of(2026, 11, 12, 22, 30)
        assertEquals(LocalDateTime.of(2026, 11, 13, 9, 0) to true, Game.reminderTime(due, off.quietFrom, off.quietTo))
    }

    @Test fun shield_customWindowAndStrict() {
        val shield = NightShield(from = LocalTime.of(21, 0), to = LocalTime.of(6, 30), strict = true)
        assertTrue(shield.isNight(LocalTime.of(21, 15)))
        assertFalse(shield.isNight(LocalTime.of(6, 30)))
        assertTrue(shield.blocksBuyAnyway(night = true))
        assertFalse(shield.blocksBuyAnyway(night = false))
        assertFalse(shield.copy(strict = false).blocksBuyAnyway(night = true))
    }

    @Test fun shield_sameFromAndToIsInvalid() {
        assertFalse(NightShield.isValid(LocalTime.of(23, 0), LocalTime.of(23, 0)))
        assertTrue(NightShield.isValid(LocalTime.of(23, 0), LocalTime.of(7, 0)))
    }

    // ---- Levels ---------------------------------------------------------------------

    @Test fun levels_byKeptMoney() {
        assertEquals("Kit", Game.levelFor(0.0).name)
        assertEquals("Forager", Game.levelFor(64.0).name)
        assertEquals("Keeper", Game.levelFor(212.0).name)
        assertEquals("Stash Master", Game.levelFor(5000.0).name)
        assertEquals(null, Game.nextLevel(Game.levelFor(5000.0)))
    }

    // ---- Money --------------------------------------------------------------------

    @Test fun money_parsesCommaAndSpaces() {
        assertEquals(1299.5, Money.parse("1 299,50")!!, 0.001)
        assertEquals(null, Money.parse("abc"))
        assertEquals(null, Money.parse("0"))
    }

    @Test fun money_dropsZeroCents() {
        assertEquals("$64", Money.format(64.0, "USD", java.util.Locale.US))
        assertEquals("$64.50", Money.format(64.5, "USD", java.util.Locale.US))
    }
}
