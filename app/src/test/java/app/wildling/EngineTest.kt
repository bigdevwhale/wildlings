package app.wildling

import app.wildling.data.Habit
import app.wildling.data.Mark
import app.wildling.domain.Engine
import app.wildling.domain.weekStart
import org.junit.Assert.assertEquals
import org.junit.Test

class EngineTest {
    private val monday = weekStart(20_000L)

    /** On the day of a slip the run is nought, never -1 (#6). */
    @Test fun aSlipTodayLeavesARunOfNought() {
        val h = Habit(id = 1, kind = "x", bad = true, title = "", name = "", since = monday - 10)
        val marks = listOf(Mark(habitId = 1, day = monday, kind = Mark.SLIP))
        assertEquals(0L, Engine.streak(h, marks, monday))
        assertEquals(1L, Engine.streak(h, marks, monday + 2))
    }

    /** A week off on holiday neither breaks the run of weeks nor costs trust (#3). */
    @Test fun aHolidayWeekKeepsTheRun() {
        val h = Habit(id = 2, kind = "x", bad = false, title = "", name = "", since = monday - 21, perWeek = 3)
        fun done(w: Long) = listOf(0, 2, 4).map { Mark(habitId = 2, day = w + it, kind = Mark.DONE) }
        val weeks = done(monday - 21) + done(monday - 14) + done(monday)
        val holiday = (monday - 7 until monday).map { Mark(habitId = 2, day = it, kind = Mark.REST) }
        val today = monday + 5
        assertEquals(1, Engine.weeksInRow(h, weeks, today))
        assertEquals(3, Engine.weeksInRow(h, weeks + holiday, today))
        val plain = Engine.trust(h, weeks, emptyList(), today).points
        val rested = Engine.trust(h, weeks + holiday, emptyList(), today).points
        assert(rested > plain) { "rested $rested, plain $plain" }
    }
}
