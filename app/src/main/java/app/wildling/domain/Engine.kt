package app.wildling.domain

import app.wildling.data.Habit
import app.wildling.data.Mark
import app.wildling.data.Note
import app.wildling.data.Urge
import kotlin.math.ceil
import kotlin.math.max

/**
 * Everything here is worked out from the history, never stored: how close each creature has come, the pinecones
 * earned, the finds. So nothing can drift, and a slip is never a reset – it only takes a step back.
 */
object Engine {
    /** A slip takes away a fifth of the trust and two points more; the rest stays. */
    const val SLIP_KEEP = .8
    const val SLIP_COST = 2.0
    /** A week of a good habit done less than half as often as planned loses a tenth. */
    const val WEEK_KEEP = .9

    class Trust(val points: Double, val stage: Int, val history: List<Pair<Long, Double>>, val best: Int) {
        val toNext: Double get() = if (stage >= 5) 0.0 else Stages.need[stage + 1] - points
        val progress: Float get() = if (stage >= 5) 1f else ((points - Stages.need[stage]) / (Stages.need[stage + 1] - Stages.need[stage])).toFloat().coerceIn(0f, 1f)
    }

    fun trust(h: Habit, marks: List<Mark>, urges: List<Urge>, today: Long): Trust {
        val mine = marks.filter { it.habitId == h.id }
        val held = urges.filter { it.habitId == h.id && it.held }.groupingBy { it.day }.eachCount()
        val hist = ArrayList<Pair<Long, Double>>()
        var p = 0.0; var best = 0
        if (h.bad) {
            val slips = mine.filter { it.kind == Mark.SLIP }.map { it.day }.toSet()
            for (d in h.since..today) {
                if (d in slips) p = max(0.0, p * SLIP_KEEP - SLIP_COST)
                else if (d < today) p += 1.0
                p += .5 * minOf(2, held[d] ?: 0)
                hist += d to p; best = max(best, Stages.of(p))
            }
        } else if (h.counts && h.weekly) {
            // so much a week: each amount brings its share of the week's seven steps, what goes past the goal a little more
            val logs = mine.filter { it.kind == Mark.LOG }.associate { it.day to it.amount }
            val goal = h.goal.toDouble()
            var sum = 0.0
            val firstWeek = weekStart(h.since)
            for (d in h.since..today) {
                if (weekStart(d) == d) sum = 0.0
                val a = (logs[d] ?: 0).toDouble()
                if (a > 0) { p += 7.0 * (minOf(sum + a, goal) - minOf(sum, goal)) / goal; if (sum + a > goal) p += .5; sum += a }
                p += .5 * minOf(2, held[d] ?: 0)
                if (weekStart(d) + 6 == d && weekStart(d) != firstWeek && d < today && sum < goal / 2) p *= WEEK_KEEP
                hist += d to p; best = max(best, Stages.of(p))
            }
        } else {
            val done = mine.filter { it.kind == Mark.DONE }.map { it.day }.toSet()
            val per = h.perWeek.coerceIn(1, 7)
            val step = 7.0 / per
            var weekCount = 0
            val firstWeek = weekStart(h.since)
            for (d in h.since..today) {
                if (weekStart(d) == d) weekCount = 0
                if (d in done) { weekCount++; p += if (weekCount <= per) step else .5 }
                p += .5 * minOf(2, held[d] ?: 0)
                // a week that just ended (Sunday, and not the first, partial one)
                if (weekStart(d) + 6 == d && weekStart(d) != firstWeek && d < today && weekCount < ceil(per / 2.0)) p *= WEEK_KEEP
                hist += d to p; best = max(best, Stages.of(p))
            }
        }
        return Trust(p, Stages.of(p), hist, best)
    }

    // ---------- streaks and sums ----------

    fun slips(h: Habit, marks: List<Mark>) = marks.filter { it.habitId == h.id && it.kind == Mark.SLIP }.map { it.day }.toSortedSet()
    fun dones(h: Habit, marks: List<Mark>) = marks.filter { it.habitId == h.id && it.kind == Mark.DONE }.map { it.day }.toSortedSet()

    /** A bad habit: whole days since the last slip (or the start). */
    fun streak(h: Habit, marks: List<Mark>, today: Long): Long {
        val last = slips(h, marks).lastOrNull { it <= today }
        return today - max(h.since, if (last != null) last + 1 else h.since)
    }

    /** The longest clean run of a bad habit, in whole days. */
    fun longest(h: Habit, marks: List<Mark>, today: Long): Long {
        var start = h.since; var best = 0L
        for (s in slips(h, marks)) { if (s > today) break; best = max(best, s - start); start = s + 1 }
        return max(best, today - start)
    }

    /** Whole days of a bad habit without it. */
    fun cleanDays(h: Habit, marks: List<Mark>, today: Long): Long = max(0L, today - h.since - slips(h, marks).count { it in h.since until today })

    /** A good habit: weeks in a row (counting back from last week) it was done as planned; this week counts once it's met. */
    fun weeksInRow(h: Habit, marks: List<Mark>, today: Long): Int {
        val done = dones(h, marks); val per = h.perWeek.coerceIn(1, 7)
        val met: (Long) -> Boolean = if (h.counts && h.weekly) { w -> amount(h, marks, w, w + 6) >= h.goal } else { w -> done.count { it in w..w + 6 } >= per }
        var w = weekStart(today); var n = 0
        if (met(w)) n++
        w -= 7
        while (w + 6 >= h.since) { if (met(w)) n++ else break; w -= 7 }
        return n
    }

    fun thisWeek(h: Habit, marks: List<Mark>, today: Long): Int { val w = weekStart(today); return dones(h, marks).count { it in w..w + 6 } }

    /** A counted habit: how much was done from [from] to [to]. */
    fun amount(h: Habit, marks: List<Mark>, from: Long, to: Long = from): Int = marks.filter { it.habitId == h.id && it.kind == Mark.LOG && it.day in from..to }.sumOf { it.amount }

    /** This week against the plan: «3/5» days, or «640/1000» of a weekly amount. */
    fun weekText(h: Habit, marks: List<Mark>, today: Long): String =
        if (h.counts && h.weekly) weekStart(today).let { "${amount(h, marks, it, it + 6)}/${h.goal}" } else "${thisWeek(h, marks, today)}/${h.perWeek}"

    /** Day 1 is the day the habit began. */
    fun dayNo(h: Habit, day: Long) = day - h.since + 1

    // ---------- pinecones ----------

    const val PER_CLEAN_DAY = 1
    const val PER_DONE = 1
    const val PER_HELD = 2
    const val PER_SAT = 1
    const val PER_NOTE_DAY = 1
    const val PER_FIND = 10
    const val PER_STAGE = 5

    /** Every pinecone ever earned. What was spent is kept in the settings. */
    fun earned(habits: List<Habit>, marks: List<Mark>, urges: List<Urge>, notes: List<Note>, today: Long): Int {
        var n = 0
        for (h in habits) {
            n += if (h.bad) cleanDays(h, marks, today).toInt() * PER_CLEAN_DAY else dones(h, marks).size * PER_DONE
            val best = trust(h, marks, urges, today).best
            n += PER_STAGE * best * (best + 1) / 2
        }
        n += urges.count { it.held } * PER_HELD + urges.count { !it.held && it.secs >= 60 } * PER_SAT
        n += notes.map { it.day }.toSet().size * PER_NOTE_DAY
        n += finds(habits, marks, urges, today).size * PER_FIND
        return n
    }

    // ---------- finds ----------

    /** Rare guests and the day each one came. */
    fun finds(habits: List<Habit>, marks: List<Mark>, urges: List<Urge>, today: Long): Map<String, Long> {
        val out = LinkedHashMap<String, Long>()
        val bad = habits.filter { it.bad }
        // came back after a slip and held seven whole days more
        bad.flatMap { h -> val ss = slips(h, marks); ss.mapNotNull { s -> (s + 7).takeIf { it <= today && ss.none { o -> o in s + 1..s + 7 } } } }.minOrNull()?.let { out["ashling"] = it }
        // a craving held deep in the night
        urges.filter { it.held && hourOf(it.at).let { hr -> hr >= 23 || hr < 5 } }.minOfOrNull { it.day }?.let { out["wisp"] = it }
        // a hundred days of one bad habit without a slip
        bad.mapNotNull { h ->
            var start = h.since; var hit: Long? = null
            for (s in slips(h, marks) + (today + 1)) { if (s - start >= 100) { hit = start + 100; break }; start = s + 1 }
            hit?.takeIf { it <= today }
        }.minOrNull()?.let { out["elder"] = it }
        // ten cravings held
        urges.filter { it.held }.sortedBy { it.at }.getOrNull(9)?.let { out["pebble"] = it.day }
        return out
    }

    // ---------- the craving map ----------

    /** Cravings by weekday (0 Mon) and hour. */
    fun heat(urges: List<Urge>): Array<IntArray> {
        val a = Array(7) { IntArray(24) }
        for (u in urges) { val d = java.time.LocalDate.ofEpochDay(u.day).dayOfWeek.value - 1; a[d][hourOf(u.at)]++ }
        return a
    }

    /** The hour cravings come most, or null. */
    fun peakHour(urges: List<Urge>): Int? = urges.groupingBy { hourOf(it.at) }.eachCount().maxByOrNull { it.value }?.key?.takeIf { urges.size >= 3 }
}
