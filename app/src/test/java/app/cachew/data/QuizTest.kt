package app.cachew.data

import org.junit.Assert.assertEquals
import org.junit.Test

class QuizTest {

    @Test fun mockupCase() {
        // "$50 – $150" a month → "about $1,200 a year"; late at night → the 11 PM bar.
        assertEquals(1200, Quiz.yearlyCost("50_150"))
        assertEquals(23, Quiz.CHART_HOURS[Quiz.peakBar("late_night")])
    }

    @Test fun ranges() {
        assertEquals(300, Quiz.yearlyCost("under_50"))
        assertEquals(4200, Quiz.yearlyCost("over_300"))
        assertEquals(1200, Quiz.yearlyCost(null)) // skipped → the middle answer
    }
}
