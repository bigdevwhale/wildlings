package app.wildling.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.wildling.container
import app.wildling.domain.tr
import app.wildling.ui.art.Creature
import app.wildling.ui.components.Btn
import app.wildling.ui.components.tap
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T

/** A first-visit introduction to one screen: who explains it, the title and a few short points. */
class Intro(val id: String, val kind: String, val title: String, val points: List<String>)

/** The introduction for what is on top now: the page over the tabs, or the tab itself. */
fun introFor(top: Screen?, tab: Int): Intro? = when (top) {
    null -> when (tab) {
        0 -> Intro("i.home", "rambler", tr("Это твоя сторожка", "This is your lodge"), listOf(
            tr("Звери стоят там, куда их привело доверие: тень в чаще, опушка, забор, крыльцо, дом, у очага.", "Creatures stand where trust has brought them: a shadow in the thicket, the edge, the fence, the porch, the house, by the hearth."),
            tr("Вредная привычка: если сегодня было – нажми «Было». Зверь отступит на шаг, но не к нулю.", "A bad habit: if it happened today, tap «Slipped». The beast steps back, never to zero."),
            tr("Полезная: отметь кружок, когда сделано.", "A good one: tick the circle when done."),
            tr("Фонарь внизу – когда тянет. Шишки – в лавку: семена для грядок и вещи для дома.", "The lantern below is for cravings. Pinecones go to the shop: seeds for the beds and things for the lodge.")))
        1 -> Intro("i.journal", "folio", tr("Дневник", "The journal"), listOf(
            tr("Пара слов в день: что чувствуешь, что было трудно. Каждая запись помнит свой день с начала пути.", "A few words a day: what you feel, what was hard. Each note remembers its day on the way."),
            tr("«Карта дней» показывает, какие дни обычно трудные, – чтобы плохой день был ожидаемым.", "«Day map» shows which days are usually hard – so a bad day is expected."),
            tr("«Тяга» – когда и почему приходил зверь и что помогло.", "«Cravings» – when and why the beast came and what helped.")))
        2 -> Intro("i.beasts", "stargaze", tr("Звери", "Creatures"), listOf(
            tr("Двенадцать диких, двенадцать пугливых и четыре редких гостя.", "Twelve wild, twelve shy and four rare guests."),
            tr("Нажми на тёмный силуэт – узнаешь, какую привычку он означает.", "Tap a dark shape to see which habit it stands for."),
            tr("Редкие гости приходят сами: за возвращение после срыва, за тягу, пережитую ночью, за сто дней, за десять тяг.", "Rare guests come on their own: for coming back after a slip, a craving held at night, a hundred days, ten cravings.")))
        else -> Intro("i.stats", "elder", tr("Летопись", "The chronicle"), listOf(
            tr("Сколько дней без зверей, что вернулось – деньги и время.", "How many beast-free days, and what you got back – money and time."),
            tr("Когда и почему приходит тяга: часы, причины и что помогает.", "When and why cravings come: the hours, the reasons and what helps."),
            tr("Настроение за месяц – из записей дневника.", "The month's mood – from the journal.")))
    }
    is Screen.Sos -> null
    is Screen.HabitInfo -> Intro("i.habit", "glimmer", tr("Страница зверя", "A creature's page"), listOf(
        tr("Кнопки под зверем показывают позы, которые он уже успел показать.", "The buttons under the creature show the poses it has shown so far."),
        tr("«Две недели» – нажми на день, чтобы поправить отметку.", "«Two weeks» – tap a day to fix its mark."),
        tr("График доверия: срыв – шаг назад, но никогда не к нулю.", "The trust chart: a slip is a step back, but never to zero.")))
    Screen.Shop -> Intro("i.shop", "whisk", tr("Лавка", "The shop"), listOf(
        tr("Семена сажают в три грядки, и они растут сами – срыв им не мешает.", "Seeds go into three beds and grow on their own – a slip doesn't hurt them."),
        tr("Созревший урожай возвращает шишки с прибавкой.", "A ripe harvest brings the pinecones back and some more."),
        tr("Нажми на вещь – примерь её в сторожке до покупки.", "Tap a thing to try it at the lodge before buying.")))
    else -> null
}

/** The introduction as a card sliding up over the screen; «Got it» stores its id in Settings.seen. */
@Composable
fun IntroSheet(intro: Intro) {
    val c = LocalContext.current.container
    fun done() = c.write { settings.update { it.copy(seen = it.seen + intro.id) } }
    BackHandler { done() }
    val a = remember(intro.id) { Animatable(0f) }
    LaunchedEffect(intro.id) { a.animateTo(1f, tween(320)) }
    Box(Modifier.fillMaxSize().background(Color(0x99000000).copy(alpha = .6f * a.value)).tap {}, contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().offset(y = ((1 - a.value) * 220).dp).alpha(a.value).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.bg)
            .navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Creature(intro.kind, 4, 96.dp)
                Text(intro.title, style = T.h2, modifier = Modifier.weight(1f).padding(start = 6.dp, bottom = 14.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                intro.points.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(C.accSoft), contentAlignment = Alignment.Center) { Text("${i + 1}", style = T.smallB.copy(color = C.acc)) }
                        Spacer(Modifier.width(10.dp))
                        Text(s, style = T.body, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Btn(tr("Понятно", "Got it"), Modifier.fillMaxWidth()) { done() }
        }
    }
}
