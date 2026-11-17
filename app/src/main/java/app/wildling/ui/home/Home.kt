package app.wildling.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.wildling.container
import app.wildling.data.Habit
import app.wildling.domain.Days
import app.wildling.domain.Engine
import app.wildling.domain.Kinds
import app.wildling.domain.Moods
import app.wildling.domain.Stages
import app.wildling.domain.count
import app.wildling.domain.fmt
import app.wildling.domain.weekStart
import app.wildling.domain.tr
import app.wildling.ui.Data
import app.wildling.ui.Nav
import app.wildling.ui.Screen
import app.wildling.ui.art.Creature
import app.wildling.ui.art.LodgeScene
import app.wildling.ui.art.Mini
import app.wildling.ui.art.Prop
import app.wildling.ui.art.frameOf
import app.wildling.ui.components.Btn
import app.wildling.ui.components.Ghost
import app.wildling.ui.components.Icon
import app.wildling.ui.components.Icons
import app.wildling.ui.components.Meter
import app.wildling.ui.components.Sheet
import app.wildling.ui.components.card
import app.wildling.ui.components.tap
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T
import kotlinx.coroutines.launch

/** «5 стаканов», or just «5» when the habit names no unit. */
fun Habit.ofUnit(n: Any) = if (unit.isBlank()) "$n" else "$n $unit"

/** The habit's own words, or its kind's. */
fun Habit.label() = title.ifBlank { Kinds.of(kind).habit }

/** «12 дней» / «12 days», and «первый день» for none yet. */
fun days(n: Long) = if (n <= 0) tr("первый день", "day one") else count(n.toInt(), "день", "дня", "дней", "day", "days")

/** The line under a habit: how long, or how often this week; and where its creature is. */
fun subline(h: Habit, d: Data): String {
    val st = Stages.name(d.trust[h.id]?.stage ?: 0, h.bad)
    return if (h.bad) tr("${days(Engine.streak(h, d.marks, d.today)).replaceFirstChar { it.uppercase() }} без · $st", "${days(Engine.streak(h, d.marks, d.today)).replaceFirstChar { it.uppercase() }} clean · $st")
    else if (h.counts && h.weekly) Engine.weekText(h, d.marks, d.today).split('/').let { (a, g) -> tr("$a из ${h.ofUnit(g)} за неделю · $st", "$a of ${h.ofUnit(g)} this week · $st") }
    else tr("${Engine.thisWeek(h, d.marks, d.today)} из ${h.perWeek} на неделе · $st", "${Engine.thisWeek(h, d.marks, d.today)} of ${h.perWeek} this week · $st")
}

@Composable
fun ConePill(n: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(modifier.clip(RoundedCornerShape(50)).background(C.goldSoft).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Prop("cone", 22.dp); Spacer(Modifier.width(4.dp))
        Text(fmt(n), style = T.smallB.copy(color = C.ink))
    }
}

@Composable
fun HomeScreen(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var slipFor by remember { mutableStateOf<Habit?>(null) }
    var logFor by remember { mutableStateOf<Habit?>(null) }
    // a creature that stepped back or is new: remember quietly where it is, so only steps forward make news
    LaunchedEffect(d.trust) {
        val quiet = d.active.mapNotNull { h -> val cur = d.trust[h.id]?.stage ?: 0; val told = d.s.stages[h.id]; if (told == null || cur < told) h.id to cur else null }.toMap()
        if (quiet.isNotEmpty()) c.told(stages = quiet)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Сторожка", "The lodge"), style = T.title, modifier = Modifier.weight(1f))
            ConePill(d.cones) { nav.push(Screen.Shop) }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { nav.push(Screen.Settings) }, contentAlignment = Alignment.Center) { app.wildling.ui.art.Ico("gear", 28.dp) }
        }
        Scenes(d, nav)
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("СЕГОДНЯ", "TODAY"), style = T.sec, modifier = Modifier.weight(1f))
            Text(tr("+ привычка", "+ habit"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clip(RoundedCornerShape(50)).clickable { nav.push(Screen.Edit()) }.padding(4.dp))
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (d.bad.isNotEmpty()) {
                item(key = "h-bad") {
                    val holding = d.bad.count { !d.slipped(it) }
                    GroupHead("paw", tr("Дикие · не кормить", "Wild · don't feed"), tr("держатся $holding из ${d.bad.size}", "holding $holding of ${d.bad.size}"), C.acc)
                }
                items(d.bad, key = { it.id }) { h -> HabitRow(h, d, nav, onSlip = { slipFor = h }, onLog = {}) }
            }
            if (d.good.isNotEmpty()) {
                item(key = "h-good") {
                    GroupHead("heart", tr("Пугливые · приманить", "Shy · lure them"), tr("сегодня ${d.good.count { d.done(it) }} из ${d.good.size}", "today ${d.good.count { d.done(it) }} of ${d.good.size}"), C.gold)
                }
                items(d.good, key = { it.id }) { h -> HabitRow(h, d, nav, onSlip = {}, onLog = { logFor = h }) }
            }
            item(key = "note") { NoteCard(d, nav) }
            item(key = "pad") { Spacer(Modifier.height(8.dp)) }
        }
    }
    slipFor?.let { h -> SlipSheet(h, d, onDone = { slipFor = null }) }
    logFor?.let { h -> AmountSheet(h, d, onDone = { logFor = null }) }
}

/** The head of a group in «Today»: an icon, what to do with these creatures, and how it goes. */
@Composable
private fun GroupHead(icon: String, title: String, sub: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        app.wildling.ui.art.Ico(icon, 22.dp)
        Spacer(Modifier.width(8.dp))
        Text(title, style = T.bodyB, modifier = Modifier.weight(1f))
        Text(sub, style = T.tiny.copy(color = color))
    }
}

@Composable
private fun HabitRow(h: Habit, d: Data, nav: Nav, onSlip: () -> Unit, onLog: () -> Unit) {
    val c = LocalContext.current.container
    val tr0 = d.trust[h.id]
    val st = tr0?.stage ?: 0
    val edge = if (h.bad) C.acc else C.gold
    Row(Modifier.fillMaxWidth().card(18.dp).clickable { nav.push(Screen.HabitInfo(h.id)) }.height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(6.dp).fillMaxHeight().background(edge))
        Box(Modifier.padding(start = 8.dp).size(50.dp).clip(RoundedCornerShape(8.dp)).background(if (h.bad) Color(0xFF2B3A27).copy(alpha = if (st <= 1) .9f else .25f) else C.goldSoft),
            contentAlignment = Alignment.BottomCenter) {
            Mini(h.kind, 46.dp, frame = frameOf(st).let { if (it == 5) 2 else it })
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(h.label(), style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${h.name} · ${Stages.name(st, h.bad).lowercase()}", style = T.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Meter(tr0?.progress ?: 0f, Modifier.padding(top = 6.dp), color = edge, h = 4.dp)
        }
        if (h.bad) {
            Column(Modifier.padding(end = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val n = Engine.streak(h, d.marks, d.today)
                Text("$n", style = T.num.copy(color = if (d.slipped(h)) C.bad else C.acc))
                Text(app.wildling.domain.plural(n, "день", "дня", "дней").let { tr(it, if (n == 1L) "day" else "days") }, style = T.tiny)
                if (d.slipped(h)) Text(tr("было ✕", "slipped ✕"), style = T.tiny.copy(color = C.bad), modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(6.dp))
                    .clickable { c.write { unslip(h, d.today); say(tr("Отметка снята", "Mark removed")) } }.padding(horizontal = 6.dp, vertical = 2.dp))
                else Text(tr("было?", "slipped?"), style = T.tiny.copy(color = C.mute), modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(6.dp)).background(C.card2)
                    .clickable(onClick = onSlip).padding(horizontal = 8.dp, vertical = 3.dp))
            }
        } else if (h.counts) {
            val on = d.done(h)
            val today = Engine.amount(h, d.marks, d.today)
            Column(Modifier.padding(end = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(if (on) C.gold else Color.Transparent).border(2.dp, if (on) C.gold else C.line, CircleShape)
                    .clickable(onClick = onLog), contentAlignment = Alignment.Center) {
                    if (today > 0) Text(fmt(today), style = T.smallB.copy(color = if (on) Color.White else C.ink), maxLines = 1)
                    else Text("+", style = T.h3.copy(color = C.mute))
                }
                Text(if (h.weekly) Engine.weekText(h, d.marks, d.today) else "$today/${h.goal}", style = T.tiny, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
            }
        } else {
            val on = d.done(h)
            Column(Modifier.padding(end = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(if (on) C.gold else Color.Transparent).border(2.dp, if (on) C.gold else C.line, CircleShape)
                    .clickable { c.write { val now = toggleDone(h, d.today); if (now) say(tr("${d.byId[h.id]?.name} подходит ближе", "${d.byId[h.id]?.name} comes closer")) } },
                    contentAlignment = Alignment.Center) { if (on) Icon(Icons.CHECK, Color.White, 22.dp) else app.wildling.ui.art.Ico("heart", 22.dp, dim = true) }
                Text("${Engine.thisWeek(h, d.marks, d.today)}/${h.perWeek}", style = T.tiny, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun NoteCard(d: Data, nav: Nav) {
    val n = d.noteToday
    Column(Modifier.fillMaxWidth().card(18.dp).clickable { nav.push(Screen.NoteEdit(n?.id ?: 0)) }.padding(14.dp)) {
        if (n != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                app.wildling.ui.journal.MoodIco(n.mood, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Сегодня: ", "Today: ") + Moods.name(n.mood).lowercase(), style = T.smallB)
                    if (n.text.isNotBlank()) Text(n.text, style = app.wildling.ui.journal.handStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Text(tr("Какая сегодня погода на душе?", "What's the weather inside today?"), style = T.bodyB)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Moods.all.indices.forEach { i ->
                    app.wildling.ui.journal.MoodIco(i, 34.dp, Modifier.clip(RoundedCornerShape(6.dp)).clickable { nav.push(Screen.NoteEdit(mood = i)) })
                }
            }
        }
        echo(d)?.let { (title, text) ->
            Text(title, style = T.tiny.copy(color = C.acc), modifier = Modifier.padding(top = 10.dp))
            Text(text, style = app.wildling.ui.journal.handStyle.copy(color = C.mute), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** An earlier note to look back at: a week ago, a month ago, or the very first one. */
fun echo(d: Data): Pair<String, String>? {
    val main = d.main ?: return null
    val withText = d.notes.filter { it.text.isNotBlank() && it.day < d.today - 2 }
    val pick = listOf(30L, 7L).firstNotNullOfOrNull { back -> withText.firstOrNull { it.day == d.today - back } } ?: withText.lastOrNull()?.takeIf { d.today - it.day >= 5 } ?: return null
    val n = Engine.dayNo(main, pick.day)
    val ago = d.today - pick.day
    return (if (n >= 1) tr("День $n, ${days(ago).let { if (ago == 7L) "неделю" else it }} назад", "Day $n, ${days(ago).let { if (ago == 7L) "a week" else it }} ago")
        else tr("${days(ago)} назад", "${days(ago)} ago")) to pick.text
}

/** A counted habit: how much was done on [day] (today by default), put in by steps or typed. */
@Composable
fun AmountSheet(h: Habit, d: Data, day: Long = d.today, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val was = Engine.amount(h, d.marks, day)
    var text by remember { mutableStateOf(if (was > 0) "$was" else "") }
    val n = text.toIntOrNull() ?: 0
    // a small goal goes one at a time (glasses), a big one by tenths (words)
    val step = if (h.goal <= 20) 1 else maxOf(1, h.goal / 10).let { if (it >= 100) it / 100 * 100 else if (it >= 10) it / 10 * 10 else it }
    Sheet(onDone) {
        Row(verticalAlignment = Alignment.Bottom) {
            Creature(h.kind, 0, 90.dp)
            Column(Modifier.padding(start = 10.dp, bottom = 10.dp)) {
                Text(h.label(), style = T.h2)
                Text(if (h.weekly) tr("Цель – ${h.ofUnit(h.goal)} за неделю", "The goal: ${h.ofUnit(h.goal)} a week") else tr("Цель – ${h.ofUnit(h.goal)} в день", "The goal: ${h.ofUnit(h.goal)} a day"), style = T.small)
            }
        }
        Text(if (day == d.today) tr("Сколько сегодня?", "How much today?") else tr("Сколько было ${app.wildling.domain.fmtDayShort(day)}?", "How much on ${app.wildling.domain.fmtDayShort(day)}?"), style = T.bodyB, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).card().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Ghost("−$step", small = true) { text = maxOf(0, n - step).takeIf { it > 0 }?.toString() ?: "" }
            app.wildling.ui.components.Field(text, { v -> text = v.filter { it.isDigit() }.take(7) }, "0", Modifier.weight(1f).padding(horizontal = 8.dp), number = true, style = T.num, center = true)
            Ghost("+$step", small = true) { text = (n + step).toString() }
        }
        val goalLine = if (h.weekly) {
            val w = weekStart(day); val week = Engine.amount(h, d.marks, w, w + 6) - was + n
            tr("За неделю: $week из ${h.ofUnit(h.goal)}", "This week: $week of ${h.ofUnit(h.goal)}") + if (week >= h.goal) " ✓" else ""
        } else tr("$n из ${h.ofUnit(h.goal)}", "$n of ${h.ofUnit(h.goal)}") + if (n >= h.goal) " ✓" else ""
        Text(goalLine, style = T.smallB.copy(color = C.gold), modifier = Modifier.padding(top = 8.dp))
        Btn(tr("Записать", "Save"), Modifier.fillMaxWidth().padding(top = 14.dp)) {
            c.write {
                val before = Engine.amount(h, d.marks, day)
                val done = log(h, day, n)
                if (n > before && done) say(tr("${h.name} подходит ближе", "${h.name} comes closer"))
            }
            onDone()
        }
    }
}

@Composable
private fun SlipSheet(h: Habit, d: Data, onDone: () -> Unit) {
    val c = LocalContext.current.container
    Sheet(onDone) {
        Row(verticalAlignment = Alignment.Bottom) {
            Creature(h.kind, 0, 90.dp)
            Column(Modifier.padding(start = 10.dp, bottom = 10.dp)) {
                Text(tr("Сегодня было?", "Did it happen today?"), style = T.h2)
                Text(h.name, style = T.small)
            }
        }
        Text(tr("Отметим и пойдём дальше. ${h.name} отступит на шаг, но пройденный путь никуда не денется – это не обнуление.",
            "We'll mark it and move on. ${h.name} steps back a little, but the way you've come stays – this isn't a reset."), style = T.body, modifier = Modifier.padding(top = 8.dp))
        Text(tr("Если хочется – запиши в дневник, как это было. Это поможет в следующий раз.", "If you like, write down how it went. It helps next time."),
            style = T.small, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        Btn(tr("Да, было", "Yes, it did"), Modifier.fillMaxWidth(), color = C.bad) { c.write { slip(h, d.today); say(tr("Ничего. Завтра – новый день", "That's all right. Tomorrow is a new day")) }; onDone() }
        Spacer(Modifier.height(8.dp))
        Ghost(tr("Нет, всё в порядке", "No, all fine"), Modifier.fillMaxWidth()) { onDone() }
    }
}

// ---------------- news ----------------

sealed class News {
    class Step(val h: Habit, val stage: Int) : News()
    class Found(val kind: String, val day: Long) : News()
    class Mile(val h: Habit, val streak: Long, val mile: Days.Mile) : News()
}

/** The next good news not told yet: a creature came closer, a rare guest arrived, a milestone was passed. */
fun nextNews(d: Data): News? {
    for (h in d.active) { val cur = d.trust[h.id]?.stage ?: 0; val told = d.s.stages[h.id] ?: continue; if (cur > told) return News.Step(h, cur) }
    d.finds.entries.firstOrNull { it.key !in d.s.finds }?.let { return News.Found(it.key, it.value) }
    for (h in d.bad) {
        val n = Engine.streak(h, d.marks, d.today); val m = Days.exactly(n) ?: continue
        if (n > 0 && "${h.id}:${h.since}:$n" !in d.s.miles && !d.slipped(h)) return News.Mile(h, n, m)
    }
    return null
}

@Composable
fun NewsSheet(n: News, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val key = when (n) { is News.Step -> "s${n.h.id}:${n.stage}"; is News.Found -> "f${n.kind}"; is News.Mile -> "m${n.h.id}:${n.streak}" }
    fun done() = c.write { when (n) {
        is News.Step -> told(stages = mapOf(n.h.id to n.stage)); is News.Found -> told(find = n.kind); is News.Mile -> told(mile = "${n.h.id}:${n.h.since}:${n.streak}")
    } }
    BackHandler { done() }
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { a.animateTo(1f, tween(360)) }
    val (kind, title, text) = when (n) {
        is News.Step -> Triple(n.h.kind, "${n.h.name}: ${Stages.name(n.stage, n.h.bad).lowercase()}", Stages.line(n.stage, n.h.bad) +
            "\n" + tr("+${Engine.PER_STAGE * n.stage} шишек за новый шаг.", "+${Engine.PER_STAGE * n.stage} pinecones for the new step."))
        is News.Found -> Kinds.of(n.kind).let { Triple(n.kind, tr("Новый гость: ${it.ru}", "A new guest: ${it.en}"), it.about + "\n" + tr("+${Engine.PER_FIND} шишек.", "+${Engine.PER_FIND} pinecones.")) }
        is News.Mile -> Triple(n.h.kind, "${n.mile.title}: ${n.h.label().lowercase()}", n.mile.text)
    }
    Box(Modifier.fillMaxSize().background(Color(0x99000000).copy(alpha = .6f * a.value)).tap {}, contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().offset(y = ((1 - a.value) * 240).dp).alpha(a.value).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.bg)
            .navigationBarsPadding().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Creature(kind, 4, 150.dp)
            Text(title, style = T.h2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            Text(text, style = T.body, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Ghost(tr("Открытка", "Postcard"), Modifier.weight(1f)) {
                    nav.share = when (n) { is News.Mile -> "mile:${n.h.id}:${n.mile.day}"; is News.Step -> "habit:${n.h.id}"; else -> "lodge" }; done()
                }
                Btn(tr("Здорово", "Lovely"), Modifier.weight(1f)) { done() }
            }
        }
    }
}

/** The two views, swiped: the yard and the house. A tab above each says who is there. */
@Composable
fun Scenes(d: Data, nav: Nav) {
    val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = 0) { 2 }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val inYard = d.guests.count { app.wildling.ui.art.viewOf(it.stage) == app.wildling.ui.art.View.YARD }
    Row(Modifier.fillMaxWidth().padding(bottom = 0.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(tr("Двор", "Yard") to inYard, tr("Дом", "House") to d.guests.size - inYard).forEachIndexed { i, (name, n) ->
            val on = pager.currentPage == i
            Row(Modifier.clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(if (on) C.frame else C.track)
                .clickable { scope.launch { pager.animateScrollToPage(i) } }.padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                app.wildling.ui.art.Ico(if (i == 0) "sign" else "lodge", 18.dp, dim = !on)
                Spacer(Modifier.width(6.dp))
                Text("$name · $n", style = T.sec.copy(color = if (on) C.ink else C.mute))
            }
        }
    }
    androidx.compose.foundation.pager.HorizontalPager(pager, Modifier.fillMaxWidth().clip(RoundedCornerShape(topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
        .border(2.dp, C.frame, RoundedCornerShape(topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))) { page ->
        app.wildling.ui.art.LodgeScene(app.wildling.ui.art.View.entries[page], d.guests, d.s.items, d.beds,
            onGuest = { g -> if (g.key > 0) nav.push(Screen.HabitInfo(g.key)) else nav.push(Screen.KindInfo(g.kind)) }, onBed = { nav.push(Screen.Shop) })
    }
}
