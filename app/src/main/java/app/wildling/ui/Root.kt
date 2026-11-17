package app.wildling.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wildling.container
import app.wildling.data.Habit
import app.wildling.data.Mark
import app.wildling.data.Note
import app.wildling.data.Settings
import app.wildling.data.Urge
import app.wildling.domain.Engine
import app.wildling.domain.Shop
import app.wildling.domain.tr
import app.wildling.ui.art.Guest
import app.wildling.ui.components.Icon
import app.wildling.ui.components.Icons
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T
import kotlinx.coroutines.delay

private data class Quad(val h: List<Habit>, val m: List<Mark>, val u: List<Urge>, val n: List<Note>)

/** Where a demo/screenshot launch opens (set from the intent in demo builds only). */
object Start { var tab = 0; var sub = 0; var open: String? = null }

/** A page over the tabs. */
sealed class Screen {
    data class HabitInfo(val id: Long) : Screen()
    data class Edit(val id: Long = 0, val bad: Boolean = true, val kind: String = "") : Screen()
    data class Sos(val habitId: Long = 0) : Screen()
    data class NoteEdit(val id: Long = 0, val mood: Int = -1) : Screen()
    data class KindInfo(val kind: String) : Screen()
    data class Map(val habitId: Long = 0) : Screen()
    data object Shop : Screen()
    data object Settings : Screen()
    data object Letter : Screen()
}

class Nav {
    var tab by mutableIntStateOf(0)
    val stack = mutableStateListOf<Screen>()
    /** The postcard shelf, opened on this card key. */
    var share by mutableStateOf<String?>(null)
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
}

/** Everything the screens read, worked out once per change. */
class Data(val s: Settings, val habits: List<Habit>, val marks: List<Mark>, val urges: List<Urge>, val notes: List<Note>, val today: Long) {
    val active = habits.filter { !it.archived }
    val bad = active.filter { it.bad }
    val good = active.filter { !it.bad }
    val byId = habits.associateBy { it.id }
    val trust = habits.associate { it.id to Engine.trust(it, marks, urges, today) }
    val finds = Engine.finds(habits, marks, urges, today)
    val earned = Engine.earned(habits, marks, urges, notes, today)
    val cones = earned - s.spent
    /** Each bed: the seed and how grown it is, or null when empty. */
    val beds: List<Pair<String, Int>?> = s.beds.map { b ->
        if (b.isEmpty()) null else { val (id, d) = b.split(':').let { it[0] to it[1].toLong() }; Shop.seedById[id]?.let { id to Shop.growth(it, today - d) } }
    }
    val guests: List<Guest> = active.map { Guest(it.id, it.kind, trust[it.id]?.stage ?: 0, it.shiny, bad = it.bad) } +
        finds.keys.mapIndexed { i, k -> Guest(-1L - i, k, 6) }
    /** The habit whose days count the journal: the oldest active bad habit, else the oldest habit. */
    val main: Habit? = bad.minByOrNull { it.since } ?: active.minByOrNull { it.since }
    fun slipped(h: Habit, day: Long = today) = marks.any { it.habitId == h.id && it.day == day && it.kind == Mark.SLIP }
    fun done(h: Habit, day: Long = today) = marks.any { it.habitId == h.id && it.day == day && it.kind == Mark.DONE }
    val noteToday = notes.firstOrNull { it.day == today }
}

@Composable
fun Root(s: Settings) {
    val c = LocalContext.current.container
    // one flow for all four tables, so the screens never see habits without their days (that would look like steps back and forth)
    val all by remember { kotlinx.coroutines.flow.combine(c.dao.habits(), c.dao.marks(), c.dao.urges(), c.dao.notes()) { h, m, u, n -> Quad(h, m, u, n) } }
        .collectAsState(initial = null)
    val q = all ?: return
    val habits = q.h
    val data = remember(s, q) { Data(s, q.h, q.m, q.u, q.n, c.clock()) }
    val nav = remember { Nav().apply { tab = Start.tab } }
    var note by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.news.collect { note = it; delay(2600); note = null } }
    LaunchedEffect(habits.isNotEmpty()) {
        if (habits.isEmpty()) return@LaunchedEffect
        when (val o = Start.open) {
            null -> {}
            "shop" -> nav.push(Screen.Shop); "settings" -> nav.push(Screen.Settings); "letter" -> nav.push(Screen.Letter)
            "sos" -> nav.push(Screen.Sos(habits.first { it.bad }.id)); "note" -> nav.push(Screen.NoteEdit(mood = 1))
            "new" -> nav.push(Screen.Edit()); "map" -> nav.push(Screen.Map(habits.first { it.bad }.id)); "share" -> nav.share = "stats"
            else -> when {
                o.startsWith("habit:") -> nav.push(Screen.HabitInfo(o.drop(6).toLongOrNull() ?: habits.first().id))
                o.startsWith("kind:") -> nav.push(Screen.KindInfo(o.drop(5)))
                o.startsWith("edit:") -> nav.push(Screen.Edit(o.drop(5).toLong()))
            }
        }
        Start.open = null
    }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).statusBarsPadding()) {
                when (nav.tab) {
                    0 -> app.wildling.ui.home.HomeScreen(data, nav)
                    1 -> app.wildling.ui.journal.JournalScreen(data, nav)
                    2 -> app.wildling.ui.beasts.BeastsScreen(data, nav)
                    else -> app.wildling.ui.stats.StatsScreen(data, nav)
                }
            }
            Dock(nav.tab, { nav.tab = it }) { nav.push(Screen.Sos(data.bad.firstOrNull()?.id ?: 0)) }
        }
        for (scr in nav.stack) Box(Modifier.fillMaxSize()) {
            when (scr) {
                is Screen.HabitInfo -> app.wildling.ui.beasts.HabitPage(scr.id, data, nav)
                is Screen.Edit -> app.wildling.ui.more.EditPage(scr, data, nav)
                is Screen.Sos -> app.wildling.ui.home.SosPage(scr.habitId, data, nav)
                is Screen.NoteEdit -> app.wildling.ui.journal.NotePage(scr, data, nav)
                is Screen.KindInfo -> app.wildling.ui.beasts.KindPage(scr.kind, data, nav)
                is Screen.Map -> app.wildling.ui.journal.MapPage(scr.habitId, data, nav)
                Screen.Shop -> app.wildling.ui.more.ShopPage(data, nav)
                Screen.Settings -> app.wildling.ui.more.SettingsPage(data, nav)
                Screen.Letter -> app.wildling.ui.more.LetterPage(data, nav)
            }
        }
        nav.share?.let { k -> app.wildling.ui.share.ShareSheet(k, data) { nav.share = null } }
        val shots = app.wildling.BuildConfig.SHOTS
        val news = if (shots) null else app.wildling.ui.home.nextNews(data)
        if (nav.stack.isEmpty() && news != null) app.wildling.ui.home.NewsSheet(news, data, nav)
        else if (!shots) introFor(nav.stack.lastOrNull(), nav.tab)?.takeIf { it.id !in s.seen }?.let { IntroSheet(it) }
        AnimatedVisibility(note != null, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp, start = 18.dp, end = 18.dp), enter = fadeIn(), exit = fadeOut()) {
            Text(note ?: "", style = T.bodyB.copy(color = C.bg), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.ink).padding(horizontal = 16.dp, vertical = 11.dp))
        }
    }
}

/**
 * The dock: a wooden shelf with the four places as pixel things on it; the chosen one stands up on a paper tag.
 * In the middle hangs the lantern – «it pulls» – for a craving.
 */
@Composable
private fun Dock(sel: Int, onSel: (Int) -> Unit, onSos: () -> Unit) {
    val tabs = listOf(tr("Сторожка", "Lodge") to "lodge", tr("Дневник", "Journal") to "journal", tr("Звери", "Creatures") to "paw", tr("Летопись", "Chronicle") to "scroll")
    val wood = C.wood; val wood2 = C.wood2
    // the system bar's height below the shelf, so the dark plank lies exactly under the words
    val navPx = WindowInsets.navigationBars.getBottom(androidx.compose.ui.platform.LocalDensity.current)
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp).drawBehind {
            // planks: two tones, grain lines, nails, a dark top edge
            drawRect(wood)
            val ph = size.height - navPx - 6.dp.toPx() - 16.dp.toPx()
            drawRect(wood2, androidx.compose.ui.geometry.Offset(0f, ph), androidx.compose.ui.geometry.Size(size.width, size.height - ph))
            drawRect(androidx.compose.ui.graphics.Color(0x33000000), size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()))
            val r = app.wildling.domain.Rand(4)
            repeat(14) {
                val y = (if (it % 2 == 0) r.next() * ph else ph + r.next() * (size.height - ph)); val x = r.next() * size.width; val len = size.width * (.08f + r.next() * .15f)
                drawRect(androidx.compose.ui.graphics.Color(0x22000000), androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Size(len, 2.dp.toPx()))
            }
            listOf(.03f, .97f).forEach { fx -> listOf(.25f, .75f).forEach { fy ->
                drawRect(androidx.compose.ui.graphics.Color(0x66000000), androidx.compose.ui.geometry.Offset(size.width * fx, size.height * fy), androidx.compose.ui.geometry.Size(4.dp.toPx(), 4.dp.toPx())) } }
        }.navigationBarsPadding().padding(top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
            tabs.forEachIndexed { i, (label, ic) ->
                if (i == 2) Spacer(Modifier.weight(1.1f))
                val on = i == sel
                Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { onSel(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    app.wildling.ui.art.Ico(ic, if (on) 40.dp else 32.dp, dim = !on)
                    Text(label, style = T.tiny.copy(color = if (on) C.ink else androidx.compose.ui.graphics.Color(0xFFF1E3C4), fontFamily = app.wildling.ui.theme.Pixel, fontSize = 7.sp, lineHeight = 10.sp),
                        modifier = Modifier.padding(top = 2.dp).then(if (on) Modifier.clip(RoundedCornerShape(4.dp)).background(C.card).padding(horizontal = 6.dp, vertical = 1.dp) else Modifier.padding(vertical = 1.dp)))
                }
            }
        }
        // the lantern hangs over the shelf
        Column(Modifier.align(Alignment.TopCenter).clickable(remember { MutableInteractionSource() }, null, onClick = onSos), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(62.dp).padding(bottom = 3.dp).clip(CircleShape).background(C.dusk).border(2.dp, androidx.compose.ui.graphics.Color(0xFFFFC46B), CircleShape),
                contentAlignment = Alignment.Center) { app.wildling.ui.art.Ico("lantern", 40.dp) }
            Text(tr("Тянет", "Craving"), style = T.tiny.copy(color = androidx.compose.ui.graphics.Color(0xFFFFE2A8), fontFamily = app.wildling.ui.theme.Pixel, fontSize = 7.sp, lineHeight = 10.sp))
        }
    }
}
