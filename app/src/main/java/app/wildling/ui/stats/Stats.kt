package app.wildling.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.wildling.data.Habit
import app.wildling.data.Mark
import app.wildling.domain.Engine
import app.wildling.domain.Moods
import app.wildling.domain.Stages
import app.wildling.domain.count
import app.wildling.domain.dayShort
import app.wildling.domain.fmt
import app.wildling.domain.fmt1
import app.wildling.domain.fmtDayYear
import app.wildling.domain.money
import app.wildling.domain.monthName
import app.wildling.domain.tr
import app.wildling.domain.weekStart
import app.wildling.ui.Data
import app.wildling.ui.Nav
import app.wildling.ui.Screen
import app.wildling.ui.art.Ico
import app.wildling.ui.art.Mini
import app.wildling.ui.art.frameOf
import app.wildling.ui.components.Bars
import app.wildling.ui.components.Btn
import app.wildling.ui.components.Empty
import app.wildling.ui.components.Heat
import app.wildling.ui.components.PixTiles
import app.wildling.ui.components.Segments
import app.wildling.ui.components.card
import app.wildling.ui.home.days
import app.wildling.ui.home.label
import app.wildling.ui.journal.MoodIco
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
private fun Head(text: String, icon: String, sub: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = if (sub == null) 8.dp else 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Ico(icon, 22.dp); Spacer(Modifier.width(8.dp)); Text(text.uppercase(), style = T.sec)
    }
    if (sub != null) Text(sub, style = T.tiny, modifier = Modifier.padding(bottom = 8.dp))
}

/** A one-line conclusion under a chart, so the numbers say something. */
@Composable
private fun Says(text: String) = Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
    Ico("sign", 18.dp); Spacer(Modifier.width(6.dp)); Text(text, style = T.small.copy(color = C.ink))
}

@Composable
fun StatsScreen(d: Data, nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(app.wildling.ui.Start.sub.also { app.wildling.ui.Start.sub = 0 }) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Летопись", "Chronicle"), style = T.title, modifier = Modifier.weight(1f))
            Btn(tr("Открытка", "Postcard"), small = true) { nav.share = "stats" }
        }
        Segments(listOf(tr("Обзор", "Overview"), tr("Звери", "Creatures"), tr("Перемены", "Changes"), tr("Погода", "Weather")), tab) { tab = it }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (tab) { 0 -> Overview(d); 1 -> Creatures(d, nav); 2 -> Rhythm(d); else -> WeatherTab(d) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------- overview ----------------

@Composable
private fun Overview(d: Data) {
    val first = d.active.minOfOrNull { it.since } ?: d.today
    val clean = d.bad.sumOf { Engine.cleanDays(it, d.marks, d.today) }
    val saved = d.bad.sumOf { it.cost * Engine.cleanDays(it, d.marks, d.today) }
    val hours = d.bad.sumOf { it.minutes * Engine.cleanDays(it, d.marks, d.today) } / 60.0
    val done = d.marks.count { it.kind == Mark.DONE }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).card().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Ico("map", 40.dp)
        Column(Modifier.padding(start = 10.dp)) {
            Text(count((d.today - first + 1).toInt(), "день", "дня", "дней", "day", "days"), style = T.num)
            Text(tr("пути – с ${fmtDayYear(first)}", "on the way – since ${fmtDayYear(first)}"), style = T.small)
        }
    }
    Head(tr("Что вернулось", "What came back"), "seeds")
    PixTiles(listOf(
        Triple(fmt(clean), tr("дней без зверей", "beast-free days"), "star"),
        Triple(if (saved > 0) money(saved) else "—", tr("не потрачено", "not spent"), "seeds"),
        Triple(if (hours > 0) tr("${fmt1(hours)} ч", "${fmt1(hours)} h") else "—", tr("времени вернулось", "time won back"), "hourglass"),
        Triple(fmt(done), tr("полезных дел", "good deeds"), "heart"),
    ))
    val perDayMoney = d.bad.sumOf { it.cost }; val perDayMin = d.bad.sumOf { it.minutes }
    if (perDayMoney > 0 || perDayMin > 0) Column(Modifier.fillMaxWidth().padding(top = 10.dp).card().padding(12.dp)) {
        Text(tr("Если так держать", "If you keep this up"), style = T.bodyB)
        listOf(30 to tr("за месяц", "a month"), 365 to tr("за год", "a year")).forEach { (n, l) ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(l, style = T.small, modifier = Modifier.weight(1f))
                Text(listOfNotNull(perDayMoney.takeIf { it > 0 }?.let { money(it * n) }, perDayMin.takeIf { it > 0 }?.let { tr("${fmt((it * n / 60.0).roundToInt())} ч", "${fmt((it * n / 60.0).roundToInt())} h") }).joinToString(" · "), style = T.numS)
            }
        }
        if (hours >= 2) Text(tr("Уже вернувшегося времени хватит примерно на ${count((hours / 2).toInt(), "фильм", "фильма", "фильмов", "", "")} или ${count((hours / 8).toInt().coerceAtLeast(1), "книгу", "книги", "книг", "", "")}.",
            "The time already back is about ${(hours / 2).toInt()} films or ${(hours / 8).toInt().coerceAtLeast(1)} books."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
    }
    Head(tr("Рекорды", "Records"), "candle")
    Records(d)
    Head(tr("Год на тропе", "A year on the trail"), "map", tr("Каждая клетка – день. Чем ярче, тем больше в нём было без зверей и с полезными делами.", "Each cell is a day. The brighter, the more of it was beast-free and full of good deeds."))
    Column(Modifier.fillMaxWidth().card().padding(12.dp)) {
        val from = d.today - 7 * 17 + 1
        val m = HashMap<Long, Int>()
        for (day in from..d.today) { var n = 0; d.active.forEach { h -> if (day >= h.since) { if (h.bad && !d.slipped(h, day)) n++; if (!h.bad && d.done(h, day)) n++ } }; if (n > 0) m[day] = n }
        Heat(m, from, d.today)
    }
}

@Composable
private fun Records(d: Data) {
    val items = buildList {
        d.bad.maxByOrNull { Engine.longest(it, d.marks, d.today) }?.let { h -> add(Triple(h.kind, days(Engine.longest(h, d.marks, d.today)), tr("самая долгая серия: ", "longest clean run: ") + h.label().lowercase())) }
        d.urges.filter { it.held }.groupingBy { it.day }.eachCount().maxByOrNull { it.value }?.takeIf { it.value > 1 }?.let { (day, n) ->
            add(Triple("wisp", tr("$n за день", "$n in a day"), tr("тяг выдержано ${fmtDayYear(day)}", "cravings held on ${fmtDayYear(day)}"))) }
        d.good.mapNotNull { h -> d.marks.filter { it.habitId == h.id && it.kind == Mark.DONE }.groupingBy { weekStart(it.day) }.eachCount().maxByOrNull { it.value }?.let { h to it.value } }
            .maxByOrNull { it.second }?.let { (h, n) -> add(Triple(h.kind, tr("$n за неделю", "$n in a week"), tr("лучшая неделя: ", "best week: ") + h.label().lowercase())) }
        val noteDays = d.notes.map { it.day }.toSortedSet(); var best = 0; var run = 0; var prev = Long.MIN_VALUE
        for (x in noteDays) { run = if (x == prev + 1) run + 1 else 1; best = maxOf(best, run); prev = x }
        if (best > 1) add(Triple("folio", days(best.toLong()), tr("дневник без пропусков", "journal without a gap")))
    }
    if (items.isEmpty()) { Empty(tr("Рекорды появятся через несколько дней.", "Records show up in a few days.")); return }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (kind, v, l) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(C.goldSoft).border(1.5.dp, C.gold, RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Mini(kind, 40.dp, frame = 4)
                Column(Modifier.padding(start = 10.dp)) { Text(v, style = T.num.copy(fontSize = T.num.fontSize * .8f)); Text(l, style = T.tiny.copy(color = C.ink)) }
            }
        }
    }
}

// ---------------- creatures ----------------

@Composable
private fun Creatures(d: Data, nav: Nav) {
    Text(tr("Каждый зверь на своей дорожке: от чащи до очага. Нажми, чтобы открыть его страницу.", "Each creature on its own lane, from the thicket to the hearth. Tap to open its page."),
        style = T.small, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    if (d.active.isEmpty()) Empty(tr("Пока никого.", "Nobody yet."))
    d.active.forEach { h -> Lane(h, d, nav) }
    val stages = d.active.groupingBy { d.trust[it.id]?.stage ?: 0 }.eachCount()
    if (d.active.size > 1) {
        Head(tr("Где все сейчас", "Where everyone is"), "lodge")
        Column(Modifier.fillMaxWidth().card().padding(12.dp)) {
            (5 downTo 0).forEach { st ->
                val n = stages[st] ?: 0
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Stages.name(st, true), style = T.small.copy(color = if (n > 0) C.ink else C.mute), modifier = Modifier.width(140.dp))
                    d.active.filter { (d.trust[it.id]?.stage ?: 0) == st }.forEach { Mini(it.kind, 28.dp, frame = frameOf(st).let { f -> if (f == 5) 2 else f }) }
                }
            }
        }
    }
}

/** One creature's lane with its numbers. */
@Composable
private fun Lane(h: Habit, d: Data, nav: Nav) {
    val t = d.trust[h.id]; val st = t?.stage ?: 0; val pr = t?.progress ?: 0f
    val line = C.frame; val done = if (h.bad) C.acc else C.gold
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).card().clickable { nav.push(Screen.HabitInfo(h.id)) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(h.name, style = T.bodyB)
            Text(" · " + h.label().lowercase(), style = T.small, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Stages.name(st, h.bad).lowercase(), style = T.tiny.copy(color = done))
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(56.dp)) {
            val w = maxWidth
            val f = ((st + if (st < 5) pr else 0f) / 5f).coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * .78f; val x0 = 14.dp.toPx(); val x1 = size.width - 14.dp.toPx()
                drawLine(line, Offset(x0, y), Offset(x1, y), 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                drawLine(done, Offset(x0, y), Offset(x0 + (x1 - x0) * f, y), 4.dp.toPx())
                for (i in 0..5) drawCircle(if (i <= st) done else line, 5.dp.toPx(), Offset(x0 + (x1 - x0) * i / 5f, y))
            }
            Box(Modifier.offset(x = 14.dp + (w - 28.dp) * f - 22.dp)) { Mini(h.kind, 42.dp, frame = frameOf(st).let { if (it == 5) 2 else it }) }
            Ico("lodge", 22.dp, Modifier.offset(x = w - 26.dp, y = 2.dp), dim = st < 5)
        }
        val facts = if (h.bad) listOf(
            days(Engine.streak(h, d.marks, d.today)) to tr("сейчас", "now"),
            days(Engine.longest(h, d.marks, d.today)) to tr("рекорд", "best"),
            fmt(Engine.slips(h, d.marks).size) to tr("срывов", "slips"),
            d.urges.filter { it.habitId == h.id }.let { u -> "${u.count { it.held }}/${u.size}" } to tr("тяг", "cravings"),
        ) else listOf(
            Engine.weekText(h, d.marks, d.today) to tr("неделя", "week"),
            fmt(Engine.weeksInRow(h, d.marks, d.today)) to tr("нед. подряд", "wks in row"),
            fmt(Engine.dones(h, d.marks).size) to tr("всего", "in all"),
            "${rate(h, d)}%" to tr("по плану", "on plan"),
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            facts.forEach { (v, l) -> Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text(v, style = T.numS.copy(fontSize = T.numS.fontSize * .85f), maxLines = 1); Text(l, style = T.tiny) } }
        }
    }
}

/** A good habit: how many of its planned days, since it began, were done. */
private fun rate(h: Habit, d: Data): Int {
    val weeks = ((d.today - h.since + 1) / 7.0).coerceAtLeast(1.0)
    // so much a week: how much of the weeks' amounts was done
    if (h.counts && h.weekly) return (Engine.amount(h, d.marks, h.since, d.today) / (weeks * h.goal) * 100).roundToInt().coerceAtMost(100)
    return (Engine.dones(h, d.marks).size / (weeks * h.perWeek) * 100).roundToInt().coerceAtMost(100)
}

// ---------------- rhythm ----------------

private class Span(val from: Long, val to: Long)

private fun cleanIn(d: Data, s: Span) = d.bad.sumOf { h -> (maxOf(s.from, h.since)..minOf(s.to, d.today - 1)).count { day -> !d.slipped(h, day) } }
private fun slipsIn(d: Data, s: Span) = d.marks.count { it.kind == Mark.SLIP && it.day in s.from..s.to }
private fun doneIn(d: Data, s: Span) = d.marks.count { it.kind == Mark.DONE && it.day in s.from..s.to }
private fun heldIn(d: Data, s: Span) = d.urges.count { it.held && it.day in s.from..s.to }
private fun notesIn(d: Data, s: Span) = d.notes.map { it.day }.toSet().count { it in s.from..s.to }

@Composable
private fun Rhythm(d: Data) {
    val now = LocalDate.ofEpochDay(d.today)
    val thisM = Span(now.withDayOfMonth(1).toEpochDay(), d.today)
    val lastStart = now.withDayOfMonth(1).minusMonths(1)
    // the same number of days of last month, to be fair
    val lastM = Span(lastStart.toEpochDay(), minOf(lastStart.toEpochDay() + (d.today - thisM.from), now.withDayOfMonth(1).toEpochDay() - 1))
    Head(tr("${monthName(now.monthValue).replaceFirstChar { it.uppercase() }} и ${monthName(lastStart.monthValue)}", "${monthName(now.monthValue)} vs ${monthName(lastStart.monthValue)}"), "scroll",
        tr("Одинаковое число дней: с 1-го по ${now.dayOfMonth}-е.", "The same number of days: the 1st to the ${now.dayOfMonth}th."))
    Column(Modifier.fillMaxWidth().card().padding(12.dp)) {
        Row { Spacer(Modifier.weight(1f)); Text(monthShortOf(lastStart.monthValue), style = T.tiny, modifier = Modifier.width(64.dp), textAlign = TextAlign.End); Text(monthShortOf(now.monthValue), style = T.tiny, modifier = Modifier.width(64.dp), textAlign = TextAlign.End); Spacer(Modifier.width(40.dp)) }
        listOf(
            Triple(tr("дней без срыва", "clean days"), cleanIn(d, lastM) to cleanIn(d, thisM), true),
            Triple(tr("срывов", "slips"), slipsIn(d, lastM) to slipsIn(d, thisM), false),
            Triple(tr("полезных дел", "good deeds"), doneIn(d, lastM) to doneIn(d, thisM), true),
            Triple(tr("тяг выдержано", "cravings held"), heldIn(d, lastM) to heldIn(d, thisM), true),
            Triple(tr("дней с записью", "days with a note"), notesIn(d, lastM) to notesIn(d, thisM), true),
        ).forEach { (l, p, up) ->
            val (a, b) = p; val better = if (up) b > a else b < a
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(l, style = T.small.copy(color = C.ink), modifier = Modifier.weight(1f))
                Text(fmt(a), style = T.numS.copy(color = C.mute), modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                Text(fmt(b), style = T.numS, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                Text(if (a == b) "=" else if (better) "▲" else "▼", style = T.smallB.copy(color = if (a == b) C.mute else if (better) C.ok else C.bad), modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
        }
    }
    val weeks = (11 downTo 0).map { weekStart(d.today) - it * 7L }
    if (d.bad.isNotEmpty()) {
        Head(tr("Чистые дни по неделям", "Clean days by week"), "star", tr("Сколько дней недели прошли без срыва – по всем вредным привычкам вместе.", "How many days of each week were clean – all bad habits together."))
        Column(Modifier.fillMaxWidth().card().padding(12.dp)) {
            val v = weeks.map { w -> val s = Span(w, w + 6); val total = d.bad.sumOf { h -> (maxOf(s.from, h.since)..minOf(s.to, d.today - 1)).count() }; if (total == 0) 0.0 else cleanIn(d, s) * 100.0 / total }
            Bars(weeks.map { "${LocalDate.ofEpochDay(it).dayOfMonth}" to v[weeks.indexOf(it)] }, color = C.acc, hi = 11, h = 110.dp, fmtv = { "${it.roundToInt()}" })
            val recent = v.takeLast(4).average(); val before = v.dropLast(4).filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()
            if (before != null) Says(if (recent >= before) tr("Последний месяц чище, чем раньше: ${recent.roundToInt()}% против ${before.roundToInt()}%.", "The last month is cleaner than before: ${recent.roundToInt()}% vs ${before.roundToInt()}%.")
                else tr("Последний месяц тяжелее: ${recent.roundToInt()}% против ${before.roundToInt()}%. Это бывает – волны приходят и уходят.", "The last month was harder: ${recent.roundToInt()}% vs ${before.roundToInt()}%. It happens – waves come and go."))
        }
    }
    if (d.good.isNotEmpty()) {
        Head(tr("Полезные дела по неделям", "Good deeds by week"), "heart")
        Column(Modifier.fillMaxWidth().card().padding(12.dp)) {
            Bars(weeks.map { w -> "${LocalDate.ofEpochDay(w).dayOfMonth}" to doneIn(d, Span(w, w + 6)).toDouble() }, color = C.gold, hi = 11, h = 110.dp, fmtv = { fmt(it.toInt()) })
            val plan = d.good.sumOf { it.perWeek }
            Says(tr("План – $plan в неделю. На этой неделе: ${doneIn(d, Span(weeks.last(), weeks.last() + 6))}.", "The plan is $plan a week. This week: ${doneIn(d, Span(weeks.last(), weeks.last() + 6))}."))
        }
    }
    Head(tr("Дни недели", "Days of the week"), "candle", tr("Когда труднее, а когда легче всего.", "When it's hardest and when it's easiest."))
    Column(Modifier.fillMaxWidth().card().padding(12.dp)) {
        val slips = IntArray(7); val dones = IntArray(7); val urges = IntArray(7)
        fun wd(day: Long) = LocalDate.ofEpochDay(day).dayOfWeek.value - 1
        d.marks.forEach { if (it.kind == Mark.SLIP) slips[wd(it.day)]++ else dones[wd(it.day)]++ }
        d.urges.forEach { urges[wd(it.day)]++ }
        Row { Spacer(Modifier.width(90.dp)); (0..6).forEach { Text(dayShort(it), style = T.tiny, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) } }
        listOf(Triple(tr("срывы", "slips"), slips, C.bad), Triple(tr("тяга", "cravings"), urges, C.dusk), Triple(tr("полезное", "good"), dones, C.gold)).forEach { (l, a, col) ->
            val max = a.max().coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(l, style = T.small.copy(color = C.ink), modifier = Modifier.width(90.dp))
                (0..6).forEach { i -> Box(Modifier.weight(1f).padding(2.dp).height(22.dp).clip(RoundedCornerShape(4.dp)).background(if (a[i] == 0) C.track else col.copy(alpha = .25f + .75f * a[i] / max)),
                    contentAlignment = Alignment.Center) { if (a[i] > 0) Text("${a[i]}", style = T.tiny.copy(color = Color.White)) } }
            }
        }
        val hard = (0..6).maxBy { slips[it] * 2 + urges[it] }; val easy = (0..6).maxBy { dones[it] }
        Says(tr("Самый трудный день – ${weekdayName(hard)}, лучший для полезного – ${weekdayName(easy)}.", "The hardest day is ${weekdayName(hard)}; the best for good habits is ${weekdayName(easy)}."))
    }
    val allSlips = d.marks.filter { it.kind == Mark.SLIP }
    if (d.bad.isNotEmpty()) {
        Head(tr("Срывы", "Slips"), "candle")
        Column(Modifier.fillMaxWidth().card().padding(12.dp)) {
            val gaps = d.bad.flatMap { h -> val s = listOf(h.since - 1) + Engine.slips(h, d.marks).toList(); s.zipWithNext { a, b -> b - a } }
            Row(Modifier.fillMaxWidth()) {
                listOf(fmt(allSlips.size) to tr("всего", "in all"), (if (gaps.isEmpty()) "—" else fmt1(gaps.average())) to tr("дней между", "days between"),
                    fmt(allSlips.count { it.day > d.today - 30 }) to tr("за месяц", "this month")).forEach { (v, l) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text(v, style = T.num); Text(l, style = T.tiny) }
                }
            }
            Says(tr("Срыв – шаг назад, а не обнуление: зверь отступает, но доверие остаётся.", "A slip is a step back, not a reset: the beast backs off, but the trust stays."))
        }
    }
}

private fun monthShortOf(m: Int) = app.wildling.domain.monthShort(m)
private fun weekdayName(i: Int) = app.wildling.domain.weekday(weekStart(0) + i + 7)

// ---------------- weather ----------------

@Composable
private fun WeatherTab(d: Data) {
    val month = d.notes.filter { it.day > d.today - 30 }
    if (d.notes.isEmpty()) { Empty(tr("Записи в дневнике соберутся здесь в погоду на душе.", "Journal notes gather here into the weather inside.")); return }
    Head(tr("Погода месяца", "The month's weather"), "star")
    Column(Modifier.fillMaxWidth().card().padding(12.dp)) {
        val by = month.groupingBy { it.mood }.eachCount(); val max = by.values.maxOrNull() ?: 1
        if (by.isEmpty()) Text(tr("За месяц записей не было.", "No notes this month."), style = T.small)
        Moods.all.indices.filter { by.containsKey(it) }.sortedByDescending { by[it] }.forEach { i ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                MoodIco(i, 30.dp)
                Text(Moods.name(i), style = T.small.copy(color = C.ink), modifier = Modifier.width(92.dp).padding(start = 8.dp))
                Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(3.dp)).background(C.track)) {
                    Box(Modifier.fillMaxWidth((by[i] ?: 0).toFloat() / max).height(12.dp).background(if (Moods.tone(i) > 0) C.acc else if (Moods.tone(i) < 0) C.dusk else C.mute))
                }
                Text("${by[i]}", style = T.numS, modifier = Modifier.width(36.dp).padding(start = 8.dp))
            }
        }
    }
    // clear share week by week
    val weeks = (7 downTo 0).map { weekStart(d.today) - it * 7L }
    Head(tr("Сколько было ясного", "How much was clear"), "star", tr("Доля ясных дней среди записей – по неделям.", "The share of clear days among notes, week by week."))
    Column(Modifier.fillMaxWidth().card().padding(12.dp)) {
        Bars(weeks.map { w -> val l = d.notes.filter { it.day in w..w + 6 }; "${LocalDate.ofEpochDay(w).dayOfMonth}" to (if (l.isEmpty()) 0.0 else l.count { Moods.tone(it.mood) > 0 } * 100.0 / l.size) },
            color = C.acc, hi = 7, h = 100.dp, fmtv = { "${it.roundToInt()}" })
    }
    // what goes with the clear days
    Head(tr("Что связано с погодой", "What goes with the weather"), "sign", tr("Сравниваем дни с записью: что было в ясные дни и что в ненастные.", "Comparing days with a note: what came with clear days and what with stormy ones."))
    Column(Modifier.fillMaxWidth().card().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val noteDays = d.notes.associate { it.day to Moods.tone(it.mood) }
        fun clearShare(days: Collection<Long>): Int? = days.mapNotNull { noteDays[it] }.takeIf { it.size >= 2 }?.let { l -> (l.count { it > 0 } * 100.0 / l.size).roundToInt() }
        val lines = buildList {
            d.good.forEach { h ->
                val doneDays = Engine.dones(h, d.marks); val a = clearShare(doneDays); val b = clearShare(noteDays.keys - doneDays)
                if (a != null && b != null && abs(a - b) >= 10) add(Triple(h.kind, if (a > b) tr("В дни, когда «${h.label().lowercase()}», ясно на ${a - b}% чаще.", "On days with «${h.label().lowercase()}» it's clear ${a - b}% more often.")
                    else tr("«${h.label()}» пока не делает дни яснее – и это нормально.", "«${h.label()}» doesn't make days clearer yet – and that's fine."), a > b))
            }
            val urgeDays = d.urges.map { it.day }.toSet(); val a = clearShare(urgeDays); val b = clearShare(noteDays.keys - urgeDays)
            if (a != null && b != null) add(Triple("smudge", tr("В дни с тягой ясно в $a% случаев, без неё – в $b%.", "On craving days it's clear $a% of the time, without – $b%."), b >= a))
            val slipDays = d.marks.filter { it.kind == Mark.SLIP }.map { it.day }.toSet(); val before = clearShare(slipDays.map { it - 1 })
            if (before != null) add(Triple("wailer", tr("Накануне срывов ясно лишь в $before% дней – стоит беречь себя в ненастье.", "The day before a slip it was clear only $before% of the time – look after yourself in stormy weather."), false))
        }
        if (lines.isEmpty()) Text(tr("Нужно ещё несколько записей, чтобы увидеть связи.", "A few more notes are needed to see links."), style = T.small)
        lines.forEach { (k, s, good) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mini(k, 36.dp, frame = if (good) 4 else 0)
                Text(s, style = T.small.copy(color = C.ink), modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}
