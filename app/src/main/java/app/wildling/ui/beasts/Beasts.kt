package app.wildling.ui.beasts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.wildling.container
import app.wildling.data.Habit
import app.wildling.domain.Engine
import app.wildling.domain.Kind
import app.wildling.domain.Kinds
import app.wildling.domain.Stages
import app.wildling.domain.count
import app.wildling.domain.dayShort
import app.wildling.domain.fmt
import app.wildling.domain.fmt1
import app.wildling.domain.fmtDayYear
import app.wildling.domain.money
import app.wildling.domain.tr
import app.wildling.domain.weekStart
import app.wildling.ui.Data
import app.wildling.ui.Nav
import app.wildling.ui.Screen
import app.wildling.ui.art.Creature
import app.wildling.ui.art.Look
import app.wildling.ui.art.Mini
import app.wildling.ui.components.Empty
import app.wildling.ui.art.Ico
import app.wildling.ui.art.frameOf
import app.wildling.ui.components.Btn
import app.wildling.ui.components.Ghost
import app.wildling.ui.components.Line
import app.wildling.ui.components.Meter
import app.wildling.ui.components.Page
import app.wildling.ui.components.Section
import app.wildling.ui.components.Segments
import app.wildling.ui.components.TileGrid
import app.wildling.ui.components.card
import app.wildling.ui.home.days
import app.wildling.ui.home.label
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T

@Composable
fun BeastsScreen(d: Data, nav: Nav) {
    val met = d.habits.map { it.kind }.toSet() + d.finds.keys
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(app.wildling.ui.Start.sub.also { app.wildling.ui.Start.sub = 0 }) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Звери", "Creatures"), style = T.title, modifier = Modifier.weight(1f))
            Text("${met.size}/${Kinds.all.size}", style = T.numS.copy(color = C.mute))
        }
        Segments(listOf(tr("Мои", "Mine"), tr("Дикие", "Wild"), tr("Пугливые", "Shy"), tr("Редкие", "Rare")), tab) { tab = it }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (tab) {
                0 -> Mine(d, nav, met)
                1 -> Catalogue(true, d, met, nav)
                2 -> Catalogue(false, d, met, nav)
                else -> Rare(d, nav)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** The background of a creature's card: where it stands now. */
private fun placeColor(stage: Int): Color = when (stage) { 0 -> Color(0xFF1E2A22); 1 -> Color(0xFF34442E); 2, 3 -> Color(0xFFCFE0B4); else -> Color(0xFFE8C99A) }

/** My creatures as big cards, each on the colour of its place, and the collection so far. */
@Composable
private fun Mine(d: Data, nav: Nav, met: Set<String>) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp).card().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ico("paw", 30.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(tr("Встречено ${met.size} из ${Kinds.all.size}", "Met ${met.size} of ${Kinds.all.size}"), style = T.bodyB)
                Text(tr("Дома: ${d.active.count { (d.trust[it.id]?.stage ?: 0) >= 4 }} · во дворе: ${d.active.count { (d.trust[it.id]?.stage ?: 0) in 2..3 }} · в лесу: ${d.active.count { (d.trust[it.id]?.stage ?: 0) <= 1 }}",
                    "Home: ${d.active.count { (d.trust[it.id]?.stage ?: 0) >= 4 }} · yard: ${d.active.count { (d.trust[it.id]?.stage ?: 0) in 2..3 }} · forest: ${d.active.count { (d.trust[it.id]?.stage ?: 0) <= 1 }}"), style = T.tiny)
            }
        }
        Meter(met.size.toFloat() / Kinds.all.size, Modifier.padding(top = 8.dp), color = C.gold)
    }
    if (d.active.isEmpty()) { Empty(tr("Пока никого. Заведи привычку на главном экране.", "Nobody yet. Add a habit on the main screen.")); return }
    Spacer(Modifier.height(10.dp))
    val all = d.active.map { Triple(it.kind, it as Habit?, d.trust[it.id]?.stage ?: 0) } + d.finds.keys.map { Triple(it, null as Habit?, 6) }
    all.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (kind, h, st) ->
                val k = Kinds.of(kind)
                Column(Modifier.weight(1f).card().clickable { if (h != null) nav.push(Screen.HabitInfo(h.id)) else nav.push(Screen.KindInfo(kind)) }) {
                    Box(Modifier.fillMaxWidth().height(120.dp).background(if (h == null) Color(0xFF3B4766) else placeColor(st)), contentAlignment = Alignment.BottomCenter) {
                        app.wildling.ui.art.Creature(kind, if (h == null) 2 else frameOf(st), 110.dp, look = if (st == 0) app.wildling.ui.art.Look.SHADOW else if (st == 1) app.wildling.ui.art.Look.DIM else app.wildling.ui.art.Look.FULL, shiny = h?.shiny == true)
                    }
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(h?.name ?: k.title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(h?.label() ?: k.habit, style = T.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (h == null) tr("редкий гость", "a rare guest") else Stages.name(st, h.bad), style = T.tiny.copy(color = if (h == null) C.dusk else if (h.bad) C.acc else C.gold))
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** All wild or all shy creatures: the ready ones and the ones for the user's own habits. */
@Composable
private fun Catalogue(bad: Boolean, d: Data, met: Set<String>, nav: Nav) {
    Text(if (bad) tr("Дикие звери – вредные привычки. Их приручают, когда не кормят.", "Wild beasts are bad habits. You tame them by not feeding them.")
        else tr("Пугливые зверьки – полезные привычки. Их приманивают, когда делают привычку снова и снова.", "Shy creatures are good habits. You lure them by doing the habit again and again."),
        style = T.small, modifier = Modifier.padding(top = 12.dp))
    Group(tr("Готовые привычки", "Ready habits"), Kinds.ready(bad), d, met, nav)
    Group(tr("Для своей привычки", "For your own habit"), Kinds.owned(bad), d, met, nav)
}

@Composable
private fun Group(title: String, kinds: List<Kind>, d: Data, met: Set<String>, nav: Nav) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = T.sec, modifier = Modifier.weight(1f))
        Text("${kinds.count { it.id in met }}/${kinds.size}", style = T.tiny)
    }
    kinds.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { k ->
                val h = d.habits.firstOrNull { it.kind == k.id }
                val has = k.id in met
                Column(Modifier.weight(1f).card(color = if (has) C.card else C.card2).clickable { if (h != null) nav.push(Screen.HabitInfo(h.id)) else nav.push(Screen.KindInfo(k.id)) }.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Mini(k.id, 72.dp, dim = !has, frame = if (h != null) frameOf(d.trust[h.id]?.stage ?: 0).let { if (it == 5) 2 else it } else 2)
                    Text(if (has) h?.name ?: k.title else k.title, style = T.smallB.copy(color = if (has) C.ink else C.mute), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (h != null) Stages.name(d.trust[h.id]?.stage ?: 0, h.bad) else k.habit, style = T.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** The four rare guests: how to meet each and how far along that is. */
@Composable
private fun Rare(d: Data, nav: Nav) {
    Text(tr("Редкие гости приходят сами – за особые моменты пути.", "Rare guests come on their own – for special moments on the way."), style = T.small, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    val held = d.urges.count { it.held }
    val bestRun = d.bad.maxOfOrNull { Engine.streak(it, d.marks, d.today) } ?: 0L
    val goals = mapOf(
        "ashling" to (tr("После срыва продержаться ещё 7 дней", "Hold 7 more days after a slip") to null),
        "wisp" to (tr("Выдержать тягу глубокой ночью (с 23 до 5)", "Hold a craving deep in the night (23:00–5:00)") to null),
        "elder" to (tr("100 дней подряд без одной вредной привычки", "100 days in a row without one bad habit") to (bestRun.toFloat() / 100f to "$bestRun/100")),
        "pebble" to (tr("Выдержать 10 тяг у окна", "Hold 10 cravings by the window") to (held / 10f to "$held/10")),
    )
    Kinds.finds.forEach { k ->
        val found = d.finds[k.id]
        val (goal, prog) = goals[k.id] ?: ("" to null)
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).card().clickable { nav.push(Screen.KindInfo(k.id)) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(8.dp)).background(if (found != null) Color(0xFF3B4766) else C.card2), contentAlignment = Alignment.BottomCenter) {
                app.wildling.ui.art.Creature(k.id, if (found != null) 4 else 2, 80.dp, look = if (found != null) app.wildling.ui.art.Look.FULL else app.wildling.ui.art.Look.SHADOW)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(k.title, style = T.h3)
                Text(goal, style = T.small.copy(color = C.ink))
                if (found != null) Text(tr("В гостях с ${fmtDayYear(found)}", "Here since ${fmtDayYear(found)}"), style = T.tiny.copy(color = C.ok), modifier = Modifier.padding(top = 4.dp))
                else if (prog != null) {
                    Meter(prog.first.coerceIn(0f, 1f), Modifier.padding(top = 6.dp), color = C.dusk, h = 6.dp)
                    Text(prog.second, style = T.tiny, modifier = Modifier.padding(top = 2.dp))
                } else Text(tr("Пока не встречен", "Not met yet"), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** A creature's page for a habit: how close it is, the numbers, the last weeks, the reason. */
@Composable
fun HabitPage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val h = d.byId[id] ?: run { nav.pop(); return }
    val trust = d.trust[h.id] ?: Engine.trust(h, d.marks, d.urges, d.today)
    val k = Kinds.of(h.kind)
    // a counted habit: the day being put in, its sheet over the whole page (not inside the scrolling one)
    var logDay by remember { mutableStateOf<Long?>(null) }
    Box(Modifier.fillMaxSize()) {
    Page(h.name, { nav.pop() }, action = {
        Text(tr("Открытка", "Card"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { nav.share = "habit:${h.id}" }.padding(10.dp))
        Text(tr("Изменить", "Edit"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { nav.push(Screen.Edit(h.id)) }.padding(10.dp)) }) {
        Poses(h.kind, trust.best, trust.stage, h.shiny)
        Text(h.label(), style = T.h2, modifier = Modifier.padding(top = 6.dp))
        Text("${k.title} · ${Stages.name(trust.stage, h.bad)}", style = T.smallB.copy(color = C.acc))
        Text(Stages.line(trust.stage, h.bad), style = T.body, modifier = Modifier.padding(top = 6.dp))
        if (trust.stage < 5) {
            Meter(trust.progress, Modifier.padding(top = 10.dp), color = if (h.bad) C.acc else C.gold)
            Text(tr("Дальше: ${Stages.name(trust.stage + 1, h.bad).lowercase()} – ещё ${fmt1(trust.toNext)} ${if (h.bad) "дн. без срыва" else "шагов"}",
                "Next: ${Stages.name(trust.stage + 1, h.bad).lowercase()} – ${fmt1(trust.toNext)} more ${if (h.bad) "clean days" else "steps"}"), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
        }
        Section(tr("Цифры", "Numbers"))
        if (h.bad) {
            val clean = Engine.cleanDays(h, d.marks, d.today)
            val urges = d.urges.filter { it.habitId == h.id }
            app.wildling.ui.components.PixTiles(listOf(
                Triple(days(Engine.streak(h, d.marks, d.today)), tr("без срыва", "clean now"), "star"),
                Triple(days(Engine.longest(h, d.marks, d.today)), tr("самая долгая серия", "longest run"), "candle"),
                Triple(if (h.cost > 0) money(h.cost * clean) else fmt(clean), if (h.cost > 0) tr("не потрачено", "not spent") else tr("чистых дней всего", "clean days in all"), "seeds"),
                Triple(if (h.minutes > 0) tr("${fmt1(h.minutes * clean / 60.0)} ч", "${fmt1(h.minutes * clean / 60.0)} h") else "${urges.count { it.held }}/${urges.size}",
                    if (h.minutes > 0) tr("времени вернулось", "time won back") else tr("тяг выдержано", "cravings held"), "hourglass"),
            ))
        } else {
            app.wildling.ui.components.PixTiles(listOf(
                if (h.counts && !h.weekly) Triple("${Engine.amount(h, d.marks, d.today)}/${h.goal}", tr("${h.unit} сегодня".trim(), "${h.unit} today".trim()), "star")
                else Triple(Engine.weekText(h, d.marks, d.today), if (h.counts) tr("${h.unit} за неделю".trim(), "${h.unit} this week".trim()) else tr("на этой неделе", "this week"), "star"),
                Triple(count(Engine.weeksInRow(h, d.marks, d.today), "нед.", "нед.", "нед.", "wk", "wks"), tr("подряд по плану", "in a row on plan"), "candle"),
                if (h.counts) Triple(fmt(Engine.amount(h, d.marks, h.since, d.today)), tr("${h.unit} всего".trim(), "${h.unit} in all".trim()), "heart")
                else Triple(fmt(Engine.dones(h, d.marks).size), tr("раз всего", "times in all"), "heart"),
                Triple(app.wildling.domain.fmtDayShort(h.since), tr("начало", "started"), "sign"),
            ))
        }
        Section(tr("Две недели", "Two weeks"))
        Fortnight(h, d) { logDay = it }
        if (!h.bad) Holiday(h, d)
        if (trust.history.size > 3) {
            Section(tr("Доверие", "Trust"))
            Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) {
                Line(trust.history.takeLast(120).map { it.first.toDouble() to it.second }, h = 110.dp, color = if (h.bad) C.acc else C.gold, dots = false)
                Text(tr("Срыв или пропуск – шаг назад, но не к нулю.", "A slip or a skip is a step back, never to zero."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (h.why.isNotBlank()) {
            Section(tr("Твоя причина", "Your reason"))
            Text(h.why, style = T.body, modifier = Modifier.fillMaxWidth().card(18.dp).padding(14.dp))
        }
        if (h.bad) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.dusk).clickable { nav.push(Screen.Sos(h.id)) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                app.wildling.ui.art.Ico("lantern", 40.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(tr("Он пришёл", "It came"), style = T.h3.copy(color = C.duskInk))
                    Text(tr("Переждать тягу у окна – 4 минуты", "Wait out the craving by the window – 4 minutes"), style = T.tiny.copy(color = C.duskInk.copy(alpha = .8f)))
                }
            }
            Spacer(Modifier.height(10.dp))
            val next = app.wildling.domain.Days.at(Engine.streak(h, d.marks, d.today) + 1).second
            Row(Modifier.fillMaxWidth().card().clickable { nav.push(Screen.Map(h.id)) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                app.wildling.ui.art.Ico("map", 40.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(tr("Карта трудных дней", "The map of hard days"), style = T.h3)
                    Text(next?.let { tr("Впереди: ${it.title.lowercase()} (день ${it.day})", "Ahead: ${it.title.lowercase()} (day ${it.day})") } ?: tr("Все места пройдены", "Every place passed"), style = T.tiny)
                }
                Text("›", style = T.h2.copy(color = C.mute))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(k.about, style = T.small, modifier = Modifier.padding(top = 8.dp))
    }
    logDay?.let { day -> app.wildling.ui.home.AmountSheet(h, d, day) { logDay = null } }
    }
}

/** The creature big, with the poses it has shown so far as buttons. */
@Composable
private fun Poses(kind: String, best: Int, stage: Int, shiny: Boolean, all: Boolean = false) {
    val frames = if (all) (0..5).toList() else (0..5).filter { f -> when (f) { 0 -> true; 1 -> best >= 2; 2 -> best >= 3; 3 -> best >= 3; 4 -> best >= 1; else -> best >= 4 } }
    var f by remember(kind) { mutableIntStateOf(frameOf(stage).takeIf { it in frames } ?: frames.last()) }
    Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF000000 or Kinds.of(kind).color).copy(alpha = .14f)), contentAlignment = Alignment.BottomCenter) {
        Creature(kind, f, 190.dp, shiny = shiny)
    }
    if (frames.size > 1) {
        val names = listOf(if (Kinds.of(kind).bad) tr("Настороже", "Wary") else tr("Робость", "Shy"), tr("Интерес", "Curious"), tr("Покой", "Calm"), tr("Своё", "Its thing"), tr("Радость", "Joy"), tr("Сон", "Sleep"))
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        // each pose as its own little picture; the ones not shown yet wait as shadows
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (0..5).forEach { fr ->
                val open = fr in frames; val on = fr == f
                Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (on) C.accSoft else C.card)
                    .border(if (on) 2.dp else 1.dp, if (on) C.acc else C.frame, RoundedCornerShape(8.dp))
                    .clickable(enabled = open) { f = fr }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Mini(kind, 40.dp, frame = fr, dim = !open)
                    Text(if (open) names[fr] else "?", style = T.tiny.copy(color = if (on) C.ink else C.mute, fontSize = T.tiny.fontSize * .85f), maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** The last fourteen days: tap a day to mark or unmark it. */
@Composable
private fun Fortnight(h: Habit, d: Data, onLog: (Long) -> Unit) {
    val c = LocalContext.current.container
    Column(Modifier.fillMaxWidth().card(18.dp).padding(12.dp)) {
        listOf(13L downTo 7L, 6L downTo 0L).forEach { span ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                span.forEach { back ->
                    val day = d.today - back
                    val before = day < h.since
                    val on = if (h.bad) d.slipped(h, day) else d.done(h, day)
                    val col = when { before -> C.track; h.bad && on -> C.bad; h.bad -> C.acc; on -> C.gold; else -> C.track }
                    Column(Modifier.weight(1f).aspectRatio(.8f).clip(RoundedCornerShape(10.dp)).background(col.copy(alpha = if (before) .5f else if (on || h.bad) .85f else 1f))
                        .clickable(enabled = !before) { if (h.counts) onLog(day) else c.write { if (h.bad) { if (on) unslip(h, day) else slip(h, day) } else toggleDone(h, day) } },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(dayShort((java.time.LocalDate.ofEpochDay(day).dayOfWeek.value - 1)), style = T.tiny.copy(color = if (before || (!h.bad && !on)) C.mute else Color.White))
                        Text("${java.time.LocalDate.ofEpochDay(day).dayOfMonth}", style = T.smallB.copy(color = if (before || (!h.bad && !on)) C.mute else Color.White))
                        if (h.counts && !before) Engine.amount(h, d.marks, day).takeIf { it > 0 }?.let { a ->
                            Text(fmt(a), style = T.tiny.copy(color = if (on) Color.White else C.gold), maxLines = 1)
                        }
                    }
                }
            }
        }
        Text(if (h.bad) tr("Зелёный – без зверя, красный – было. Нажми на день, чтобы поправить.", "Green – clean, red – it happened. Tap a day to fix it.")
            else if (h.counts) tr("Нажми на день, чтобы вписать, сколько было.", "Tap a day to put in how much.")
            else tr("Нажми на день, чтобы отметить, что привычка была.", "Tap a day to mark the habit done."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
    }
}

/** A creature not tied to a habit: a rare guest, or one nobody has brought yet. */
@Composable
fun KindPage(kind: String, d: Data, nav: Nav) {
    val k = Kinds.of(kind)
    val found = d.finds[kind]
    Page(k.title, { nav.pop() }) {
        if (k.find && found == null || !k.find) Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(22.dp)).background(C.card2), contentAlignment = Alignment.BottomCenter) {
            Creature(kind, 0, 180.dp, look = Look.SHADOW)
        } else Poses(kind, 5, 4, false, all = true)
        Text(k.title, style = T.h2, modifier = Modifier.padding(top = 10.dp))
        Text(k.habit, style = T.smallB.copy(color = C.acc))
        Text(k.about, style = T.body, modifier = Modifier.padding(top = 6.dp))
        if (found != null) Text(tr("В гостях с ${fmtDayYear(found)}.", "Here since ${fmtDayYear(found)}."),
            style = T.small, modifier = Modifier.padding(top = 8.dp))
        if (!k.find) {
            Spacer(Modifier.height(18.dp))
            Btn(if (k.bad) tr("Приручить: бросить эту привычку", "Tame it: quit this habit") else tr("Приманить: завести эту привычку", "Lure it: start this habit"), Modifier.fillMaxWidth()) {
                nav.pop(); nav.push(Screen.Edit(bad = k.bad, kind = k.id))
            }
        } else if (found == null) Text(tr("Пока не в гостях. Как его встретить – написано выше.", "Not here yet. How to meet it is written above."),
            style = T.small, modifier = Modifier.padding(top = 10.dp))
    }
}

/** A holiday from a good habit (#3): ill, away, a break. Those weeks cost no trust and don't break the run. */
@Composable
private fun Holiday(h: Habit, d: Data) {
    val c = LocalContext.current.container
    val until = Engine.restUntil(h, d.marks, d.today)
    Section(tr("Отпуск", "Holiday"))
    Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) {
        if (until != null) {
            Text(tr("${h.name} ждёт тебя до ${app.wildling.domain.fmtDayShort(until)}. Эти недели не отнимают доверия и не прерывают серию.",
                "${h.name} waits for you till ${app.wildling.domain.fmtDayShort(until)}. These weeks cost no trust and don't break the run."), style = T.body)
            Text(tr("Вернуться раньше", "Come back early"), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(6.dp))
                .clickable { c.write { endRest(h, d.today); say(tr("С возвращением!", "Welcome back!")) } }.padding(vertical = 4.dp))
        } else {
            Text(tr("Заболел, уехал или нужна передышка? Возьми отпуск – эти недели не отнимут доверия и не прервут серию.",
                "Ill, away or need a break? Take a holiday – those weeks won't cost trust or break the run."), style = T.body)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(3, 7, 14, 30).forEach { n ->
                    Text(tr("$n дн.", "$n d"), style = T.smallB.copy(color = C.ink), modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(C.card2)
                        .clickable { c.write { rest(h, d.today, n); say(tr("Отпуск на $n дн.", "A holiday of $n days")) } }.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
    }
}
