package app.wildling.ui.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wildling.data.Urge
import app.wildling.domain.Days
import app.wildling.domain.Engine
import app.wildling.domain.Kinds
import app.wildling.domain.Rand
import app.wildling.domain.Triggers
import app.wildling.domain.count
import app.wildling.domain.dayShort
import app.wildling.domain.hourOf
import app.wildling.domain.relDay
import app.wildling.domain.tr
import app.wildling.ui.Data
import app.wildling.ui.Nav
import app.wildling.ui.Screen
import app.wildling.ui.art.Creature
import app.wildling.ui.art.Ico
import app.wildling.ui.art.Mini
import app.wildling.ui.components.Btn
import app.wildling.ui.components.ChipRow
import app.wildling.ui.components.Empty
import app.wildling.ui.components.Popup
import app.wildling.ui.components.card
import app.wildling.ui.home.label
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

// ---------------- the map of hard days ----------------

private const val ROW = 118 // dp between two places on the map

/**
 * The map of hard days as a trail on parchment: a place for each milestone (a gate, a campfire, thorns under a storm,
 * a bridge, a swamp, a tavern of temptation…), the creature standing on today's spot, and fog over what is still ahead.
 */
@Composable
fun AdventureMap(d: Data, nav: Nav, first: Long, inline: Boolean) {
    val bads = d.bad
    if (bads.isEmpty()) { Empty(tr("Карта для вредных привычек: заведи такую, и здесь появится тропа с трудными днями.", "The map is for bad habits: add one and a trail of hard days appears here.")); return }
    var hid by remember { mutableLongStateOf(bads.firstOrNull { it.id == first }?.id ?: bads.first().id) }
    val h = bads.first { it.id == hid }
    val n = Engine.streak(h, d.marks, d.today) + 1 // today is day n of the clean run
    val miles = Days.of(h.kind)
    var open by remember { mutableStateOf<Int?>(null) }
    val ahead = miles.indexOfFirst { it.first.day > n }.let { if (it < 0) miles.size else it } // the first place not reached
    Column(Modifier.fillMaxWidth().then(if (inline) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
        if (bads.size > 1) ChipRow(bads.map { it.name }, bads.indexOf(h), Modifier.padding(bottom = 8.dp)) { hid = bads[it].id }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Mini(h.kind, 40.dp)
            Text(tr("${h.name} на дне $n. Впереди: ${miles.getOrNull(ahead)?.first?.title?.lowercase() ?: "сторожка"}. Нажми на место, чтобы узнать, чего там ждать.",
                "${h.name} is on day $n. Ahead: ${miles.getOrNull(ahead)?.first?.title?.lowercase() ?: "the lodge"}. Tap a place to see what waits there."), style = T.small, modifier = Modifier.padding(start = 8.dp))
        }
        val parch = C.card; val ink = C.frame; val done = C.acc
        BoxWithConstraints(Modifier.fillMaxWidth().height((ROW * miles.size + 60).dp).card()) {
            val w = maxWidth
            fun xOf(i: Int) = if (i % 2 == 0) .3f else .7f
            fun yOf(i: Int) = (40 + ROW * i).dp
            // where the creature stands: between the last place passed and the next, by days
            val pos = run {
                val i = (ahead - 1).coerceAtLeast(0)
                val a = miles[i].first.day; val b = miles.getOrNull(i + 1)?.first?.day ?: a
                i + if (b > a) ((n - a).toFloat() / (b - a)).coerceIn(0f, 1f) else 0f
            }
            Canvas(Modifier.fillMaxSize()) {
                // parchment speckles
                val r = Rand(9)
                repeat(70) { drawRect(ink.copy(alpha = .12f), Offset(r.next() * size.width, r.next() * size.height), Size(3.dp.toPx(), 3.dp.toPx())) }
                fun pt(i: Int) = Offset(size.width * xOf(i), yOf(i).toPx())
                fun seg(path: Path, i: Int) { val a = pt(i); val b = pt(i + 1); val my = (a.y + b.y) / 2; path.cubicTo(a.x, my, b.x, my, b.x, b.y) }
                val all = Path().apply { moveTo(pt(0).x, pt(0).y); for (i in 0 until miles.size - 1) seg(this, i) }
                drawPath(all, ink, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))))
                val walked = Path().apply { moveTo(pt(0).x, pt(0).y); for (i in 0 until pos.toInt().coerceAtMost(miles.size - 1)) seg(this, i) }
                drawPath(walked, done, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                // fog over everything past the next place
                val fogFrom = yOf(ahead.coerceAtMost(miles.size)).toPx() + 30.dp.toPx()
            }
            miles.forEachIndexed { i, (m, _) ->
                val seen = i <= ahead
                val s = 76.dp
                Column(Modifier.offset(x = w * xOf(i) - s / 2, y = yOf(i) - s / 2 - 6.dp).width(s).clickable { open = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Ico("mark_${markOf(i)}", s * .9f, grey = !seen)
                }
                Column(Modifier.offset(x = if (i % 2 == 0) w * .3f + s / 2 + 4.dp else 12.dp, y = yOf(i) - 16.dp).width(w * .3f), horizontalAlignment = if (i % 2 == 0) Alignment.Start else Alignment.End) {
                    Text(if (seen) m.title else "???", style = T.sec.copy(color = if (i < ahead) C.acc else if (i == ahead) C.gold else C.mute), textAlign = if (i % 2 == 0) TextAlign.Start else TextAlign.End)
                    Text(tr("день ${m.day}", "day ${m.day}"), style = T.tiny)
                }
            }
            // the creature on today's spot
            val ci = pos.toInt().coerceAtMost(miles.size - 1); val frac = pos - ci
            val cx = xOf(ci) + (xOf((ci + 1).coerceAtMost(miles.size - 1)) - xOf(ci)) * frac
            val cy = yOf(ci) + (yOf((ci + 1).coerceAtMost(miles.size - 1)) - yOf(ci)) * frac
            Box(Modifier.offset(x = w * cx - 30.dp, y = cy - 56.dp)) { Creature(h.kind, 3.coerceAtMost(app.wildling.ui.art.frameOf(d.trust[h.id]?.stage ?: 0)), 60.dp) }
        }
    }
    open?.let { i ->
        val (m, body) = miles[i]
        val seen = i <= ahead
        Popup({ open = null }) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Ico("mark_${markOf(i)}", 110.dp, grey = !seen) }
            Text(if (seen) m.title else tr("Пока в тумане", "Still in the fog"), style = T.h2, modifier = Modifier.padding(top = 8.dp))
            Text(tr("День ${m.day}", "Day ${m.day}") + " · " + when {
                m.day < n -> tr("пройдено", "passed"); m.day.toLong() == n -> tr("сегодня", "today")
                else -> tr("через ${count((m.day - n).toInt(), "день", "дня", "дней", "day", "days")}", "in ${count((m.day - n).toInt(), "день", "дня", "дней", "day", "days")}") }, style = T.smallB.copy(color = C.acc))
            Text(if (seen) m.text else tr("Место откроется, когда подойдёшь ближе. Но ждать там, скорее всего, будет легче, чем сейчас.",
                "This place opens when you come closer. Most likely it'll be easier there than now."), style = T.body, modifier = Modifier.padding(top = 8.dp))
            if (seen && body != null) Text(body, style = T.small.copy(color = C.acc), modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(14.dp))
            Btn(tr("Понятно", "Got it"), Modifier.fillMaxWidth()) { open = null }
        }
    }
}

/** Which landmark picture stands for the i-th milestone: gate, campfire, thorns, crossroads, bridge, swamp, spring, tavern, hill, ford, mill, oak, pass, lodge. */
private fun markOf(i: Int) = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13).getOrElse(i) { 13 }

// ---------------- cravings ----------------

/**
 * The cravings: the beast's clock (when it comes), the sea of the last month (each craving a wave, held ones carry the
 * lantern boat), what brings it, notes of what helps, and the log.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CravingsView(d: Data, nav: Nav) {
    if (d.urges.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Ico("lantern", 80.dp)
            Empty(tr("Когда тянет, нажми фонарь внизу: зверь придёт к окну, и вы вместе переждёте волну. Здесь появятся часы зверя, море волн и то, что помогает.",
                "When it pulls, tap the lantern below: the beast comes to the window and you wait out the wave together. The beast's clock, the sea of waves and what helps show up here."))
        }
        return
    }
    var view by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val held = d.urges.count { it.held }
    // a short summary that stays on top, then one small view at a time
    Row(Modifier.fillMaxWidth().card().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Ico("lantern", 36.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(tr("$held из ${d.urges.size} волн пережито", "$held of ${d.urges.size} waves sat out"), style = T.bodyB)
            val last = d.urges.firstOrNull()
            Text(last?.let { tr("последняя – ${relDay(it.day)}", "the last – ${relDay(it.day)}") } ?: "", style = T.tiny)
        }
        Text("${held * 100 / d.urges.size.coerceAtLeast(1)}%", style = T.num.copy(color = C.ok))
    }
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("hourglass" to tr("Часы", "Clock"), "map" to tr("Море", "Sea"), "sign" to tr("Причины", "Causes"), "journal" to tr("Журнал", "Log")).forEachIndexed { i, (ic, l) ->
            val on = i == view
            Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (on) C.accSoft else C.card).border(if (on) 2.dp else 1.dp, if (on) C.acc else C.frame, RoundedCornerShape(8.dp))
                .clickable { view = i }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Ico(ic, 26.dp, dim = !on)
                Text(l, style = T.tiny.copy(color = if (on) C.ink else C.mute))
            }
        }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        when (view) {
            0 -> BeastClock(d)
            1 -> Sea(d.urges.filter { it.day > d.today - 30 }, d.today)
            2 -> Causes(d)
            else -> Log(d)
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** What brings it (bigger when more often, coloured by how often it was held) and the notes of what helps. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Causes(d: Data) {
    val trig = d.urges.filter { it.trigger.isNotBlank() }.groupingBy { it.trigger }.eachCount().entries.sortedByDescending { it.value }
    Text(tr("Что его приводит", "What brings it"), style = T.bodyB, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
    if (trig.isEmpty()) Text(tr("Отмечай у окна, что привело тягу, – здесь соберутся причины.", "Mark what brought the craving by the window – the causes gather here."), style = T.small)
    else {
        val top = trig.first().value
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            trig.forEach { (k, c) ->
                val l = d.urges.filter { it.trigger == k }; val share = l.count { it.held } * 100 / l.size
                Column(Modifier.card().padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(Triggers.name(k), style = T.bodyB.copy(fontSize = (13 + 7f * c / top).sp))
                    Text(tr("${count(c, "раз", "раза", "раз", "", "")} · выдержано $share%", "$c× · held $share%"), style = T.tiny.copy(color = if (share >= 70) C.ok else if (share >= 40) C.gold else C.bad))
                }
            }
        }
    }
    val helped = d.urges.filter { it.held && it.helped.isNotBlank() }.distinctBy { it.helped.lowercase() }.take(6)
    Text(tr("Что помогает", "What helps"), style = T.bodyB, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
    if (helped.isEmpty()) Text(tr("После тяги запиши одной строчкой, что помогло, – записки повесим сюда.", "After a craving, write one line about what helped – the notes get pinned here."), style = T.small)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        helped.forEachIndexed { i, u ->
            Text(u.helped, style = handStyle.copy(color = Color(0xFF2E2618)), modifier = Modifier.rotate(if (i % 2 == 0) -3f else 2.5f).background(Color(0xFFF6E7A8)).padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}

/** Every craving, newest first. */
@Composable
private fun Log(d: Data) {
    d.urges.take(60).forEach { u ->
        val h = d.byId[u.habitId]
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).card().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (h != null) Mini(h.kind, 32.dp, frame = if (u.held) 2 else 0)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("${relDay(u.day).replaceFirstChar { it.uppercase() }}, ${hourOf(u.at)}:${"%02d".format(java.time.Instant.ofEpochMilli(u.at).atZone(java.time.ZoneId.systemDefault()).minute)}" +
                    (u.trigger.takeIf { it.isNotBlank() }?.let { " · " + Triggers.name(it).lowercase() } ?: ""), style = T.smallB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (u.helped.isNotBlank()) Text(u.helped, style = T.tiny)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (u.held) tr("выдержано", "held") else tr("не вышло", "slipped"), style = T.tiny.copy(color = if (u.held) C.ok else C.bad))
                Text("●".repeat(u.strength), style = T.tiny.copy(color = C.dusk, fontSize = 8.sp))
            }
        }
    }
}

/** A round 24-hour clock face: the hours the beast comes glow darker. The peak hour is named in the middle. */
@Composable
private fun BeastClock(d: Data) {
    val byHour = IntArray(24).also { a -> d.urges.forEach { a[hourOf(it.at)]++ } }
    val max = max(1, byHour.max())
    val peak = byHour.indices.maxBy { byHour[it] }
    val dusk = C.dusk; val track = C.track; val ink = C.mute; val face = C.card
    val mostHabit = d.urges.groupingBy { it.habitId }.eachCount().maxByOrNull { it.value }?.key?.let { d.byId[it] }
    Row(Modifier.fillMaxWidth().card().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height / 2); val r = size.width / 2
                for (h in 0 until 24) {
                    val a = h * 15f - 90f - 7f
                    val v = byHour[h].toFloat() / max
                    drawArc(if (byHour[h] == 0) track else dusk.copy(alpha = .25f + .75f * v), a, 14f, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2))
                }
                drawCircle(face, r * .52f, c)
                for (h in listOf(0, 6, 12, 18)) {
                    val a = Math.toRadians(h * 15.0 - 90)
                    drawCircle(ink, 2.dp.toPx(), Offset(c.x + (r * .58f * kotlin.math.cos(a)).toFloat(), c.y + (r * .58f * kotlin.math.sin(a)).toFloat()))
                }
            }
            if (mostHabit != null) Mini(mostHabit.kind, 64.dp, frame = 0)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(tr("Чаще всего", "Most often"), style = T.small)
            Text("$peak:00", style = T.num.copy(color = C.dusk))
            Text(tr("Подготовь на этот час замену: воду, прогулку, звонок. Зверь приходит по расписанию – значит, его можно встретить.",
                "Have a stand-in ready for that hour: water, a walk, a call. The beast keeps a schedule – so you can meet it."), style = T.tiny.copy(color = C.ink), modifier = Modifier.padding(top = 4.dp))
            val wd = IntArray(7).also { a -> d.urges.forEach { a[java.time.LocalDate.ofEpochDay(it.day).dayOfWeek.value - 1]++ } }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                (0..6).forEach { i ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(if (wd[i] == 0) C.track else C.dusk.copy(alpha = .3f + .7f * wd[i] / max(1, wd.max()))))
                        Text(dayShort(i), style = T.tiny.copy(fontSize = 9.sp))
                    }
                }
            }
        }
    }
}

/** The last thirty days as a sea: each craving a wave as tall as it was strong; a held one carries a lantern, a lost one a red flag. */
@Composable
private fun Sea(urges: List<Urge>, today: Long) {
    val water = Color(0xFF3B5B86); val deep = Color(0xFF233A5E); val foam = Color(0xFFDCEBF7)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val lantern = app.wildling.ui.art.Art.get(ctx, "ic_lantern")
    Column(Modifier.fillMaxWidth().card().padding(10.dp)) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(2.6f).clip(RoundedCornerShape(6.dp))) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFBFD8EC), Color(0xFFE9F1F7))))
            val base = size.height * .78f
            val from = today - 29
            fun xOf(day: Long, at: Long) = ((day - from) + hourOf(at) / 24f) / 30f * size.width
            val path = Path().apply {
                moveTo(0f, size.height); lineTo(0f, base)
                var x = 0f
                while (x <= size.width) {
                    var y = base + sin(x / size.width * 30 * PI).toFloat() * size.height * .02f
                    for (u in urges) {
                        val ux = xOf(u.day, u.at); val wdt = size.width * .055f
                        val k = 1f - ((x - ux) / wdt).let { it * it }
                        if (k > 0) y -= k * k * size.height * (.08f + .07f * u.strength)
                    }
                    lineTo(x, y); x += 3f
                }
                lineTo(size.width, size.height); close()
            }
            drawPath(path, Brush.verticalGradient(listOf(water, deep), base - size.height * .6f, size.height))
            for (u in urges) {
                val ux = xOf(u.day, u.at); val top = base - size.height * (.08f + .07f * u.strength) - 4.dp.toPx()
                drawCircle(foam, 3.dp.toPx(), Offset(ux, top + 6.dp.toPx()))
                if (u.held && lantern != null) {
                    val s = 20.dp.toPx()
                    drawImage(lantern, androidx.compose.ui.unit.IntOffset.Zero, androidx.compose.ui.unit.IntSize(lantern.width, lantern.height),
                        androidx.compose.ui.unit.IntOffset((ux - s / 2).toInt(), (top - s + 4.dp.toPx()).toInt()), androidx.compose.ui.unit.IntSize(s.toInt(), s.toInt()))
                } else if (!u.held) {
                    drawLine(Color(0xFF6B4426), Offset(ux, top), Offset(ux, top - 14.dp.toPx()), 2.dp.toPx())
                    drawRect(Color(0xFFC4533A), Offset(ux, top - 14.dp.toPx()), Size(8.dp.toPx(), 6.dp.toPx()))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(tr("месяц назад", "a month ago"), style = T.tiny, modifier = Modifier.weight(1f))
            Text(tr("сегодня", "today"), style = T.tiny)
        }
        val n = urges.size; val held = urges.count { it.held }
        Text(if (n == 0) tr("Тихий месяц: ни одной волны.", "A calm month: not a single wave.")
            else tr("${count(n, "волна", "волны", "волн", "wave", "waves")} за месяц, фонарь удержался в $held.", "${count(n, "волна", "волны", "волн", "wave", "waves")} this month; the lantern stayed lit in $held."),
            style = T.small, modifier = Modifier.padding(top = 4.dp))
    }
}
