package app.wildling.ui.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wildling.R
import app.wildling.data.Habit
import app.wildling.data.Mark
import app.wildling.data.Note
import app.wildling.data.Urge
import app.wildling.domain.Days
import app.wildling.domain.Moods
import app.wildling.domain.Stages
import app.wildling.domain.count
import app.wildling.domain.dayShort
import app.wildling.domain.fmtDayYear
import app.wildling.domain.relDay
import app.wildling.domain.tr
import app.wildling.ui.Data
import app.wildling.ui.Nav
import app.wildling.ui.Screen
import app.wildling.ui.art.Ico
import app.wildling.ui.art.Mini
import app.wildling.ui.art.frameOf
import app.wildling.ui.components.Btn
import app.wildling.ui.components.card
import app.wildling.ui.components.ruled
import app.wildling.ui.home.label
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T
import java.time.LocalDate

/** Handwriting for what the user writes. */
val Hand = FontFamily(Font(R.font.caveat_medium, FontWeight.Normal), Font(R.font.caveat_bold, FontWeight.Bold))
val handStyle get() = TextStyle(fontFamily = Hand, fontSize = 21.sp, lineHeight = 24.sp, color = C.ink)

/** A mood as its weather on the soul: a pixel picture. */
@Composable
fun MoodIco(mood: Int, size: Dp, modifier: Modifier = Modifier, dim: Boolean = false) = Ico("mood_$mood", size, modifier, dim)

/** Questions to start a page with, when words don't come. */
object Prompts {
    private val all = listOf(
        "Что сегодня было самым трудным?" to "What was hardest today?",
        "За что сегодня можно себя похвалить?" to "What can you praise yourself for today?",
        "Что помогло, когда тянуло?" to "What helped when it pulled?",
        "Чем сегодня получилось заменить привычку?" to "What stood in for the habit today?",
        "Что стало лучше с первого дня?" to "What's better than on day one?",
        "Где и с кем сегодня было тяжелее всего?" to "Where and with whom was it hardest today?",
        "Какая маленькая радость была сегодня?" to "What small joy was there today?",
        "Что пожелать себе на завтра?" to "What do you wish yourself for tomorrow?",
        "Что ты чувствуешь в теле прямо сейчас?" to "What do you feel in your body right now?",
        "Какой момент дня хочется запомнить?" to "Which moment of the day is worth keeping?",
        "Что сегодня удивило?" to "What surprised you today?",
        "Кому сегодня хочется сказать спасибо?" to "Who would you like to thank today?",
    )
    fun of(day: Long, shift: Int = 0) = all[((day + shift) % all.size).toInt().let { if (it < 0) it + all.size else it }].let { tr(it.first, it.second) }
}

/** One day on the trail: its note, what happened, and a milestone if one was passed. */
private class DayPage(val day: Long, val notes: List<Note>, val urges: List<Urge>, val slips: List<Habit>, val done: List<Habit>, val miles: List<Pair<Habit, Days.Mile>>)

private fun pages(d: Data): List<DayPage> {
    val byDay = HashMap<Long, MutableList<Any>>()
    d.notes.forEach { byDay.getOrPut(it.day) { mutableListOf() } }
    d.urges.forEach { byDay.getOrPut(it.day) { mutableListOf() } }
    d.marks.filter { it.kind == Mark.SLIP }.forEach { byDay.getOrPut(it.day) { mutableListOf() } }
    // milestones: the day a clean run of a bad habit reached a mark on the map
    val miles = HashMap<Long, MutableList<Pair<Habit, Days.Mile>>>()
    for (h in d.bad) {
        val slips = d.marks.filter { it.habitId == h.id && it.kind == Mark.SLIP }.map { it.day }.toSortedSet()
        var start = h.since
        for (day in h.since..d.today) {
            if (day in slips) { start = day + 1; continue }
            Days.exactly(day - start)?.let { m -> if (m.day >= 7) miles.getOrPut(day) { mutableListOf() } += h to m }
        }
    }
    miles.keys.forEach { byDay.getOrPut(it) { mutableListOf() } }
    return byDay.keys.sortedDescending().map { day ->
        DayPage(day, d.notes.filter { it.day == day }, d.urges.filter { it.day == day },
            d.marks.filter { it.day == day && it.kind == Mark.SLIP }.mapNotNull { d.byId[it.habitId] },
            d.marks.filter { it.day == day && it.kind == Mark.DONE }.mapNotNull { d.byId[it.habitId] }, miles[day].orEmpty())
    }
}

@Composable
fun Trail(d: Data, nav: Nav) {
    val list = androidx.compose.runtime.remember(d) { pages(d) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        item(key = "weather") { WeatherMonth(d, nav) }
        if (d.noteToday == null) item(key = "today") { TodayPage(d, nav) }
        items(list.filter { d.noteToday != null || it.day != d.today || it.miles.isNotEmpty() }, key = { it.day }) { p -> TrailDay(p, d, nav, page = d.noteToday != null || p.day != d.today) }
        item(key = "start") { TrailStart(d) }
        item(key = "pad") { Spacer(Modifier.height(20.dp)) }
    }
}

/** Four weeks of the weather on the soul, Monday first; a day without a note is a faint dot. */
@Composable
private fun WeatherMonth(d: Data, nav: Nav) {
    val last = d.today
    val first = app.wildling.domain.weekStart(last) - 21
    val moods = d.notes.groupBy { it.day }.mapValues { (_, l) -> l.first().mood }
    Column(Modifier.fillMaxWidth().card().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("ПОГОДА НА ДУШЕ", "WEATHER INSIDE"), style = T.sec, modifier = Modifier.weight(1f))
            Text(tr("открытка", "postcard"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { nav.share = "weather" }.padding(4.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) { (0..6).forEach { Text(dayShort(it), style = T.tiny, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) } }
        (0 until 4).forEach { w ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                (0..6).forEach { k ->
                    val day = first + w * 7 + k
                    Box(Modifier.weight(1f).height(34.dp).clickable(enabled = day <= last) {
                        val n = d.notes.firstOrNull { it.day == day }
                        if (n != null) nav.push(Screen.NoteEdit(n.id)) else if (day == last) nav.push(Screen.NoteEdit())
                    }, contentAlignment = Alignment.Center) {
                        val m = moods[day]
                        when {
                            day > last -> {}
                            m != null -> MoodIco(m, 30.dp)
                            else -> Box(Modifier.size(5.dp).clip(CircleShape).background(C.line))
                        }
                        if (day == last) Box(Modifier.size(34.dp).border(1.5.dp, C.acc, RoundedCornerShape(6.dp)))
                    }
                }
            }
        }
        val counted = moods.filterKeys { it >= first }.values
        if (counted.isNotEmpty()) {
            val top = counted.groupingBy { it }.eachCount().maxBy { it.value }.key
            Text(tr("Чаще всего: ${Moods.name(top).lowercase()}", "Most often: ${Moods.name(top).lowercase()}"), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** Today's empty page: the question of the day and a pen. */
@Composable
private fun TodayPage(d: Data, nav: Nav) {
    TrailRow(node = { Box(Modifier.size(30.dp).clip(CircleShape).background(C.accSoft).border(2.dp, C.acc, CircleShape), contentAlignment = Alignment.Center) { Ico("pencil", 20.dp) } }) {
        Column(Modifier.fillMaxWidth().card().then(Modifier.ruled(margin = 0.dp)).clickable { nav.push(Screen.NoteEdit()) }.padding(14.dp)) {
            Text(tr("Сегодня", "Today") + (d.main?.let { " · " + tr("день ${app.wildling.domain.Engine.dayNo(it, d.today)}", "day ${app.wildling.domain.Engine.dayNo(it, d.today)}") } ?: ""), style = T.sec.copy(color = C.acc))
            Text(Prompts.of(d.today), style = handStyle.copy(color = C.mute), modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                (0 until 8).forEach { i -> MoodIco(i, 32.dp, Modifier.clip(RoundedCornerShape(6.dp)).clickable { nav.push(Screen.NoteEdit(mood = i)) }) }
            }
        }
    }
}

/** The trail: a dashed path down the left with a node per day, and the day's page beside it. */
@Composable
private fun TrailRow(node: @Composable () -> Unit, page: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(40.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            val line = C.frame
            Canvas(Modifier.fillMaxHeight().width(4.dp)) {
                drawLine(line, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
            Box(Modifier.padding(top = 14.dp)) { node() }
        }
        Box(Modifier.weight(1f).padding(top = 6.dp, bottom = 6.dp)) { page() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrailDay(p: DayPage, d: Data, nav: Nav, page: Boolean = true) {
    val main = d.main
    // a milestone stands on the path as a signpost before the day's page
    p.miles.forEach { (h, m) ->
        TrailRow(node = { Ico("star", 30.dp) }) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(C.goldSoft).border(1.5.dp, C.gold, RoundedCornerShape(8.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Mini(h.kind, 36.dp, frame = 4)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(m.title.uppercase(), style = T.sec.copy(color = C.ink))
                    Text(tr("без зверя: ${h.label2()}", "beast-free: ${h.label2()}"), style = T.tiny)
                }
            }
        }
    }
    if (!page) return
    val n = p.notes.firstOrNull()
    TrailRow(node = {
        if (n != null) MoodIco(n.mood, 30.dp)
        else Box(Modifier.size(14.dp).clip(CircleShape).background(C.card).border(2.dp, C.frame, CircleShape))
    }) {
        Column(Modifier.fillMaxWidth().card().then(if (n != null) Modifier.ruled(margin = 0.dp) else Modifier)
            .clickable(enabled = n != null) { n?.let { nav.push(Screen.NoteEdit(it.id)) } }.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text((main?.let { tr("ДЕНЬ ${app.wildling.domain.Engine.dayNo(it, p.day)}", "DAY ${app.wildling.domain.Engine.dayNo(it, p.day)}") }?.takeIf { (main.since <= p.day) } ?: "")
                        .let { if (it.isEmpty()) relDay(p.day) else it }, style = T.sec.copy(color = C.acc))
                    Text(relDay(p.day).replaceFirstChar { it.uppercase() } + (n?.let { " · " + Moods.name(it.mood).lowercase() } ?: ""), style = T.tiny)
                }
                // a stamp: the main creature as it stood that day
                if (main != null && main.since <= p.day) {
                    val st = d.trust[main.id]?.history?.firstOrNull { it.first == p.day }?.second?.let { Stages.of(it) } ?: 0
                    Box(Modifier.rotate(-8f).size(44.dp).clip(RoundedCornerShape(6.dp)).border(1.5.dp, C.bad.copy(alpha = .45f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                        Mini(main.kind, 38.dp, frame = frameOf(st).let { if (it == 5) 2 else it })
                    }
                }
            }
            p.notes.forEach { note -> if (note.text.isNotBlank()) Text(note.text, style = handStyle, modifier = Modifier.padding(top = 4.dp)) }
            val chips = buildList {
                val held = p.urges.count { it.held }; val lost = p.urges.count { !it.held }
                if (held > 0) add("lantern" to tr("тяга пережита" + if (held > 1) " ×$held" else "", "craving held" + if (held > 1) " ×$held" else ""))
                if (lost > 0 && p.slips.isEmpty()) add("candle" to tr("тяга победила", "craving won"))
                p.slips.forEach { add("candle" to tr("было: ${it.label2()}", "slipped: ${it.label2()}")) }
                p.done.forEach { add("heart" to it.label2()) }
            }
            if (chips.isNotEmpty()) FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chips.forEach { (ic, label) ->
                    Row(Modifier.clip(RoundedCornerShape(6.dp)).background(C.card2).padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Ico(ic, 16.dp); Spacer(Modifier.width(4.dp)); Text(label, style = T.tiny.copy(color = C.ink))
                    }
                }
            }
        }
    }
}

/** Where the trail begins: the first day of the oldest habit. */
@Composable
private fun TrailStart(d: Data) {
    val first = d.active.minByOrNull { it.since } ?: return
    TrailRow(node = { Ico("sign", 30.dp) }) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(tr("НАЧАЛО ТРОПЫ", "WHERE THE TRAIL BEGINS"), style = T.sec)
            Text(tr("${fmtDayYear(first.since)} – ${first.label2()}. ${count((d.today - first.since + 1).toInt(), "день", "дня", "дней", "day", "days")} пути.",
                "${fmtDayYear(first.since)} – ${first.label2()}. ${count((d.today - first.since + 1).toInt(), "день", "дня", "дней", "day", "days")} on the way."), style = T.small)
        }
    }
}

private fun Habit.label2() = label().lowercase()
