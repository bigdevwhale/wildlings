package app.wildling.ui.more

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import app.wildling.domain.Kind
import app.wildling.domain.Kinds
import app.wildling.domain.Money
import app.wildling.domain.count
import app.wildling.domain.fmt1
import app.wildling.domain.fmtDayYear
import app.wildling.domain.money
import app.wildling.domain.parseMoney
import app.wildling.domain.plainMoney
import app.wildling.domain.tr
import app.wildling.ui.Data
import app.wildling.ui.Nav
import app.wildling.ui.Screen
import app.wildling.ui.art.Creature
import app.wildling.ui.art.Ico
import app.wildling.ui.art.Look
import app.wildling.ui.art.Mini
import app.wildling.ui.components.Btn
import app.wildling.ui.components.Chip
import app.wildling.ui.components.Field
import app.wildling.ui.components.Ghost
import app.wildling.ui.components.Segments
import app.wildling.ui.components.card
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T

/** The fields of a habit being made or changed. */
class Draft(h: Habit?, bad0: Boolean, kind0: String, today: Long, val taken: Set<String>) {
    var bad by mutableStateOf(h?.bad ?: bad0)
    var kind by mutableStateOf(h?.kind ?: kind0.ifEmpty { Kinds.habits.first { it.bad == bad0 && it.id !in taken }.id })
    var title by mutableStateOf(h?.title ?: "")
    var name by mutableStateOf(h?.name ?: "")
    var ago by mutableIntStateOf(h?.let { (today - it.since).toInt() } ?: 0)
    var cost by mutableStateOf(h?.cost?.takeIf { it > 0 }?.let { plainMoney(it) } ?: "")
    var minutes by mutableStateOf(h?.minutes?.takeIf { it > 0 }?.toString() ?: "")
    var perWeek by mutableIntStateOf(h?.perWeek ?: 7)
    var why by mutableStateOf(h?.why ?: "")
    val own get() = Kinds.of(kind).own
}

fun Draft.toHabit(old: Habit?, today: Long, fallbackName: String) = (old ?: Habit(kind = kind, bad = bad, title = "", name = "", since = today)).copy(
    kind = kind, bad = bad, title = title.trim(), name = name.trim().ifEmpty { fallbackName }, since = today - ago,
    cost = if (bad) parseMoney(cost) ?: 0 else 0, minutes = if (bad) minutes.toIntOrNull() ?: 0 else 0, perWeek = perWeek, why = why.trim())

private enum class Step { TYPE, CREATURE, NAMES, WHEN, WHY }

/**
 * Making a habit, one small screen at a time: bad or good, the creature, the names, since when or how often, why.
 * Changing one skips the first two. Every screen fits without scrolling; the footprints on top show the way.
 */
@Composable
fun Wizard(f: Draft, today: Long, editing: Boolean, fallbackName: String, pickName: (String) -> String, onCancel: () -> Unit, onDone: () -> Unit,
           extra: (@Composable ColumnScope.() -> Unit)? = null) {
    val steps = if (editing) listOf(Step.NAMES, Step.WHEN, Step.WHY) else Step.entries
    var at by remember { mutableIntStateOf(0) }
    val step = steps[at]
    fun back() { if (at > 0) at-- else onCancel() }
    BackHandler { back() }
    val canGo = step != Step.NAMES || !f.own || f.title.isNotBlank()
    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (at == 0) tr("Отмена", "Cancel") else tr("Назад", "Back"), style = T.smallB.copy(color = C.mute), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { back() }.padding(8.dp))
            Spacer(Modifier.weight(1f))
            // footprints: one per step, bright up to here
            steps.indices.forEach { i -> Ico("paw", if (i == at) 24.dp else 18.dp, Modifier.padding(horizontal = 2.dp), dim = i > at) }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(60.dp))
        }
        AnimatedContent(at, Modifier.weight(1f), transitionSpec = {
            (slideInHorizontally { w -> if (targetState > initialState) w / 3 else -w / 3 } + fadeIn()) togetherWith fadeOut()
        }, label = "step") { i ->
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                when (steps[i]) {
                    Step.TYPE -> TypeStep(f) { at++ }
                    Step.CREATURE -> CreatureStep(f)
                    Step.NAMES -> NamesStep(f, pickName, extra)
                    Step.WHEN -> WhenStep(f, today)
                    Step.WHY -> WhyStep(f)
                }
            }
        }
        if (step != Step.TYPE) Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (editing && at < steps.lastIndex) Ghost(tr("Сохранить", "Save"), Modifier.weight(1f)) { if (canGo) onDone() }
            val last = at == steps.lastIndex
            Btn(if (last) (if (editing) tr("Сохранить", "Save") else if (f.bad) tr("Позвать зверя", "Call the beast") else tr("Позвать зверька", "Call the creature")) else tr("Дальше", "Next"),
                Modifier.weight(1f), enabled = canGo) { if (last) onDone() else at++ }
        }
    }
}

@Composable
private fun StepTitle(title: String, sub: String? = null) {
    Text(title, style = T.title.copy(fontSize = T.title.fontSize * .85f), modifier = Modifier.padding(top = 4.dp))
    if (sub != null) Text(sub, style = T.small, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun ColumnScope.TypeStep(f: Draft, next: () -> Unit) {
    StepTitle(tr("Что будем делать?", "What shall we do?"))
    Spacer(Modifier.height(14.dp))
    listOf(true, false).forEach { bad ->
        val first = Kinds.habits.first { it.bad == bad && it.id !in f.taken }
        Row(Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.dp).card().clickable {
            if (f.bad != bad) { f.bad = bad; f.kind = first.id; f.name = "" }; next()
        }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).background(if (bad) Color(0xFF1E2A22) else Color(0xFFDDEBC8)), contentAlignment = Alignment.BottomCenter) {
                Creature(if (bad) "smudge" else "rambler", 0, 110.dp, look = if (bad) Look.DIM else Look.FULL)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(if (bad) tr("Бросить вредную", "Quit a bad one") else tr("Завести полезную", "Start a good one"), style = T.h2)
                Text(if (bad) tr("Привычка станет диким зверем. Его приручают, когда не кормят: каждый день без неё он подходит ближе к дому.",
                    "The habit becomes a wild beast. You tame it by not feeding it: every day without it, it comes closer to the house.")
                else tr("Привычка станет пугливым зверьком. Его приманивают, когда делают её снова и снова.",
                    "The habit becomes a shy creature. You lure it by doing the habit again and again."), style = T.small, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun ColumnScope.CreatureStep(f: Draft) {
    var own by remember { mutableStateOf(f.own) }
    val k = Kinds.of(f.kind)
    StepTitle(if (f.bad) tr("Кто придёт из чащи?", "Who comes from the thicket?") else tr("Кто выглянет из травы?", "Who peeks out of the grass?"))
    // the chosen one, big, with its story
    Row(Modifier.fillMaxWidth().padding(top = 10.dp).height(120.dp).card().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Creature(k.id, 2, 100.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(k.title, style = T.h3)
            Text(k.habit, style = T.smallB.copy(color = C.acc))
            Text(k.about, style = T.tiny.copy(color = C.ink), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
    Segments(listOf(if (f.bad) tr("Готовые привычки", "Ready habits") else tr("Готовые привычки", "Ready habits"), tr("Своя привычка", "Your own")), if (own) 1 else 0, Modifier.padding(top = 12.dp)) { own = it == 1 }
    Spacer(Modifier.height(10.dp))
    Grid(if (own) Kinds.owned(f.bad) else Kinds.ready(f.bad), f)
}

@Composable
private fun Grid(list: List<Kind>, f: Draft) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        list.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    val on = k.id == f.kind; val taken = k.id in f.taken && !on
                    Column(Modifier.weight(1f).alpha(if (taken) .3f else 1f).clip(RoundedCornerShape(8.dp)).background(if (on) C.accSoft else C.card)
                        .border(if (on) 2.dp else 1.dp, if (on) C.acc else C.frame, RoundedCornerShape(8.dp))
                        .clickable(enabled = !taken) { f.kind = k.id; f.name = "" }.padding(vertical = 4.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Mini(k.id, 44.dp, frame = if (on) 4 else 0)
                        Text(if (k.own) k.title else k.habit, style = T.tiny.copy(color = if (on) C.ink else C.mute, fontSize = T.tiny.fontSize * .92f), maxLines = 1,
                            textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ColumnScope.NamesStep(f: Draft, pickName: (String) -> String, extra: (@Composable ColumnScope.() -> Unit)?) {
    val k = Kinds.of(f.kind)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.BottomCenter) { Creature(k.id, 4, 160.dp) }
        Text(if (k.own) tr("Как называется привычка?", "What is the habit called?") else tr("Как назвать привычку?", "What to call the habit?"), style = T.bodyB, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
        Field(f.title, { f.title = it }, if (k.own) (if (f.bad) tr("Например: грызть семечки", "E.g. cracking seeds") else tr("Например: звонить маме", "E.g. calling mum")) else k.habit, Modifier.fillMaxWidth())
        if (k.own && f.title.isBlank()) Text(tr("Своему зверю нужно название привычки.", "Your own creature needs the habit's name."), style = T.tiny.copy(color = C.bad), modifier = Modifier.padding(top = 4.dp))
        Text(tr("Как зовут зверя?", "What's the creature's name?"), style = T.bodyB, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Field(f.name, { f.name = it }, k.names.first(), Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(44.dp).card().clickable { f.name = pickName(k.id) }, contentAlignment = Alignment.Center) { Ico("star", 26.dp) }
        }
        Text(tr("Звезда подскажет другое имя.", "The star suggests another name."), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
        extra?.invoke(this)
    }
}

@Composable
private fun ColumnScope.WhenStep(f: Draft, today: Long) {
    if (f.bad) {
        StepTitle(tr("С какого дня без неё?", "Clean since when?"), tr("Если уже держишься – отметь, с какого дня. Зверь сразу встанет ближе.", "If you're already holding on, pick the day. The beast starts closer."))
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 to tr("Сегодня", "Today"), 1 to tr("Вчера", "Yesterday"), 7 to tr("Неделю", "A week"), 30 to tr("Месяц", "A month")).forEach { (n, l) -> Chip(l, f.ago == n) { f.ago = n } }
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp).card().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Ghost("−", small = true) { f.ago = (f.ago - 1).coerceAtLeast(0) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (f.ago == 0) tr("сегодня", "today") else fmtDayYear(today - f.ago), style = T.bodyB)
                Text(tr("день ${f.ago + 1} пути", "day ${f.ago + 1} on the way"), style = T.num.copy(color = C.acc))
            }
            Ghost("+", small = true) { f.ago = (f.ago + 1).coerceAtMost(3650) }
        }
        val st = app.wildling.domain.Stages.of(f.ago.toDouble())
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.Bottom) {
            Creature(f.kind, app.wildling.ui.art.frameOf(st), 90.dp, look = if (st == 0) Look.SHADOW else if (st == 1) Look.DIM else Look.FULL)
            Text(tr("Начнёт отсюда: ", "Starts here: ") + app.wildling.domain.Stages.name(st, true).lowercase(), style = T.smallB, modifier = Modifier.padding(start = 10.dp, bottom = 16.dp))
        }
    } else {
        StepTitle(tr("Сколько дней в неделю?", "How many days a week?"), tr("Пугливому зверьку важна не идеальность, а регулярность. Лучше честные три, чем недостижимые семь.",
            "A shy creature cares about rhythm, not perfection. An honest three beats an impossible seven."))
        Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            (1..7).forEach { n ->
                val on = n <= f.perWeek
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { f.perWeek = n }.padding(4.dp)) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(if (on) C.gold else C.track).border(2.dp, if (n == f.perWeek) C.ink else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center) { Text("$n", style = T.numS.copy(color = if (on) Color.White else C.mute)) }
                    Text(app.wildling.domain.dayShort(n - 1), style = T.tiny)
                }
            }
        }
        Text(when (f.perWeek) { 7 -> tr("Каждый день", "Every day"); 1 -> tr("Раз в неделю", "Once a week"); else -> tr("${f.perWeek} ${app.wildling.domain.plural(f.perWeek.toLong(), "раз", "раза", "раз")} в неделю", "${f.perWeek} times a week") },
            style = T.h3, modifier = Modifier.padding(top = 16.dp))
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.Bottom) {
            Creature(f.kind, 0, 90.dp)
            Text(tr("Сначала он будет только выглядывать из травы.", "At first it will only peek out of the grass."), style = T.small, modifier = Modifier.padding(start = 10.dp, bottom = 16.dp))
        }
    }
}

@Composable
private fun ColumnScope.WhyStep(f: Draft) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        StepTitle(tr("Зачем это тебе?", "Why does it matter?"), if (f.bad) tr("Это покажется у окна, когда зверь придёт и станет трудно.", "Shown by the window when the beast comes and it gets hard.")
            else tr("Это напомнит, когда захочется пропустить.", "This reminds you when you feel like skipping."))
        Field(f.why, { f.why = it }, if (f.bad) tr("Например: хочу легко подниматься по лестнице", "E.g. I want to climb stairs easily") else tr("Например: хочу больше сил по утрам", "E.g. I want more energy in the mornings"),
            Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(top = 12.dp), single = false, style = app.wildling.ui.journal.handStyle)
        if (f.bad) {
            Text(tr("Сколько она забирала в день?", "What did a day of it take?"), style = T.bodyB, modifier = Modifier.padding(top = 18.dp))
            Text(tr("По желанию. Посчитаем, что вернулось.", "Optional. We'll count what you get back."), style = T.tiny)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Ico("seeds", 30.dp)
                Field(f.cost, { f.cost = it }, "0 " + Money.cur.sign, Modifier.weight(1f), number = true)
                Ico("hourglass", 30.dp)
                Field(f.minutes, { f.minutes = it.filter { ch -> ch.isDigit() }.take(4) }, tr("0 мин", "0 min"), Modifier.weight(1f), number = true)
            }
            val c = parseMoney(f.cost) ?: 0; val m = f.minutes.toIntOrNull() ?: 0
            if (c > 0 || m > 0) Text(tr("За месяц без неё: ", "A month without it: ") + listOfNotNull(c.takeIf { it > 0 }?.let { money(it * 30) },
                m.takeIf { it > 0 }?.let { tr("${fmt1(it * 30 / 60.0)} ч", "${fmt1(it * 30 / 60.0)} h") }).joinToString(" · "),
                style = T.smallB.copy(color = C.ok), modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
fun EditPage(scr: Screen.Edit, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val old = d.byId[scr.id]
    val taken = d.habits.filter { it.id != scr.id }.map { it.kind }.toSet()
    val f = remember(scr) { Draft(old, scr.bad, scr.kind.takeIf { it !in taken }.orEmpty(), d.today, taken) }
    val fallback = remember(f.kind) { c.nameFor(f.kind) }
    Wizard(f, d.today, old != null, fallback, { c.nameFor(it) }, onCancel = { nav.pop() }, onDone = {
        val h = f.toHabit(old, d.today, fallback)
        c.write { saveHabit(h); if (old == null) say(tr("Новый зверь: ${h.name}", "A new creature: ${h.name}")) }
        nav.pop()
    }, extra = if (old == null) null else ({
        Spacer(Modifier.height(24.dp))
        Ghost(if (old.archived) tr("Вернуть в сторожку", "Bring back to the lodge") else tr("Отпустить в лес (в архив)", "Let go to the forest (archive)"), Modifier.fillMaxWidth()) {
            c.write { archive(old, !old.archived) }; nav.pop(); if (!old.archived) nav.pop()
        }
        Spacer(Modifier.height(8.dp))
        var sure by remember { mutableStateOf(false) }
        Ghost(if (sure) tr("Точно? Все отметки пропадут", "Sure? All its days will be gone") else tr("Удалить привычку", "Delete the habit"), Modifier.fillMaxWidth(), color = C.bad) {
            if (sure) { c.write { deleteHabit(old.id) }; nav.stack.clear() } else sure = true
        }
    }))
}
