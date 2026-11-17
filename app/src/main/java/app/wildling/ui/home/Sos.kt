package app.wildling.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.wildling.BuildConfig
import app.wildling.container
import app.wildling.data.Urge
import app.wildling.domain.Days
import app.wildling.domain.Engine
import app.wildling.domain.Kinds
import app.wildling.domain.Triggers
import app.wildling.domain.dayOf
import app.wildling.domain.tr
import app.wildling.ui.Data
import app.wildling.ui.Nav
import app.wildling.ui.Screen
import app.wildling.ui.art.Creature
import app.wildling.ui.art.Look
import app.wildling.ui.components.Btn
import app.wildling.ui.components.Field
import app.wildling.ui.components.Icon
import app.wildling.ui.components.Icons
import app.wildling.ui.components.tap
import app.wildling.ui.theme.T
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val NIGHT = Color(0xFF151B2E)
private val NIGHT2 = Color(0xFF222B45)
private val INK = Color(0xFFEDEFF7)
private val MUTE = Color(0xFF9AA3C0)
private val LAMP = Color(0xFFFFC46B)

/** How long the sitting by the window lasts: a craving's wave usually rises and falls within this. */
const val SIT_SECS = 240

/**
 * «It came»: the beast is at the window. Pick who and why, then sit with it while the wave passes –
 * breathing, small things to do, your own reason and letter – and say how it went.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SosPage(habitId: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val choices = d.bad.ifEmpty { d.active }
    var hid by remember { mutableLongStateOf(choices.firstOrNull { it.id == habitId }?.id ?: choices.firstOrNull()?.id ?: 0) }
    val h = d.byId[hid]
    var step by remember { mutableIntStateOf(if (BuildConfig.SHOTS) 1 else 0) }
    var strength by remember { mutableIntStateOf(3) }
    var trigger by remember { mutableStateOf("") }
    var left by remember { mutableIntStateOf(if (BuildConfig.SHOTS) 150 else SIT_SECS) }
    var helped by remember { mutableStateOf("") }
    var letter by remember { mutableStateOf(false) }
    val started = remember { System.currentTimeMillis() }
    BackHandler { nav.pop() }
    LaunchedEffect(step) { if (step == 1 && !BuildConfig.SHOTS) while (left > 0) { delay(1000); left-- } }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NIGHT, NIGHT2))).tap {}.statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.pop() }, contentAlignment = Alignment.Center) { Icon(Icons.CLOSE, MUTE, 22.dp) }
            Text(tr("Он пришёл", "It came"), style = T.h2.copy(color = INK), modifier = Modifier.weight(1f).padding(start = 4.dp))
        }
        if (h == null) {
            Text(tr("Сначала заведи привычку на главном экране.", "Add a habit on the main screen first."), style = T.body.copy(color = INK), modifier = Modifier.padding(24.dp)); return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // the window: the beast on the other side of the glass, calmer as time goes by
            val calm = 1f - left.toFloat() / SIT_SECS
            Window(h.kind, if (step == 0) 0 else if (calm < .4f) 0 else if (calm < .85f) 1 else 2, step == 1)
            when (step) {
                0 -> {
                    if (choices.size > 1) FlowRow(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        choices.forEach { o -> Pill(o.name, o.id == hid) { hid = o.id } }
                    }
                    Text(tr("Насколько сильно тянет?", "How strong is it?"), style = T.bodyB.copy(color = INK), modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { k -> Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (k <= strength) LAMP.copy(alpha = .25f + k * .12f) else Color(0x22FFFFFF))
                            .clickable { strength = k }, contentAlignment = Alignment.Center) { Text("$k", style = T.bodyB.copy(color = if (k <= strength) NIGHT else MUTE)) } }
                    }
                    Text(tr("Что его привело?", "What brought it?"), style = T.bodyB.copy(color = INK), modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                    FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Triggers.all.forEach { (id, _) -> Pill(Triggers.name(id), trigger == id) { trigger = if (trigger == id) "" else id } }
                    }
                    Text(tr("Тяга – как волна: поднимается, стоит и спадает, обычно за несколько минут. Посиди с ней у окна – не нужно ни бороться, ни слушаться.",
                        "A craving is a wave: it rises, holds and falls, usually within minutes. Sit with it by the window – no need to fight it or obey it."),
                        style = T.small.copy(color = MUTE), modifier = Modifier.padding(top = 18.dp))
                }
                1 -> {
                    val m = left / 60; val s = left % 60
                    Text("$m:${s.toString().padStart(2, '0')}", style = T.big.copy(color = INK), modifier = Modifier.padding(top = 8.dp))
                    Breath()
                    val tip = Days.sos[((SIT_SECS - left) / 35) % Days.sos.size]
                    Text(tip, style = T.body.copy(color = INK), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                    if (h.why.isNotBlank()) Column(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x18FFFFFF)).padding(14.dp)) {
                        Text(tr("ТВОЯ ПРИЧИНА", "YOUR REASON"), style = T.sec.copy(color = LAMP))
                        Text(h.why, style = T.body.copy(color = INK), modifier = Modifier.padding(top = 4.dp))
                    }
                    if (d.s.letter.isNotBlank()) Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x18FFFFFF)).clickable { letter = !letter }.padding(14.dp)) {
                        Text(tr("ПИСЬМО СЕБЕ", "A LETTER TO YOURSELF"), style = T.sec.copy(color = LAMP))
                        Text(if (letter) d.s.letter else tr("Нажми, чтобы прочитать", "Tap to read"), style = T.body.copy(color = if (letter) INK else MUTE), modifier = Modifier.padding(top = 4.dp))
                    }
                    val held = d.urges.count { it.habitId == h.id && it.held }
                    if (held > 0) Text(tr("Такая волна уже ${app.wildling.domain.count(held, "раз", "раза", "раз", "", "")} проходила мимо – пройдёт и эта.",
                        "You've waited out this wave ${app.wildling.domain.count(held, "", "", "", "time", "times")} already – this one will pass too."),
                        style = T.small.copy(color = MUTE), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
                }
                else -> {
                    Text(tr("Как прошло?", "How did it go?"), style = T.h2.copy(color = INK), modifier = Modifier.padding(top = 12.dp))
                    Text(tr("Что помогло? Одна строчка – в следующий раз пригодится.", "What helped? One line – it'll come in handy next time."), style = T.small.copy(color = MUTE),
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    Field(helped, { helped = it }, tr("Например: вода и балкон", "E.g. water and the balcony"), Modifier.fillMaxWidth().padding(top = 10.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            fun save(held: Boolean) {
                val now = System.currentTimeMillis()
                val u = Urge(habitId = h.id, at = now, day = c.clock().takeIf { BuildConfig.DEMO } ?: dayOf(now), held = held, strength = strength,
                    secs = ((now - started) / 1000).toInt(), trigger = trigger, helped = helped.trim())
                c.write {
                    urge(u)
                    say(if (held) tr("+${Engine.PER_HELD} шишки. ${h.name} теперь спокойнее", "+${Engine.PER_HELD} pinecones. ${h.name} is calmer")
                        else tr("Бывает. Это шаг назад, а не обнуление", "It happens. A step back, not a reset"))
                }
                nav.pop()
            }
            when (step) {
                0 -> Btn(tr("Сесть у окна", "Sit by the window"), Modifier.fillMaxWidth(), color = LAMP, ink = NIGHT) { step = 1 }
                1 -> Btn(if (left > 0) tr("Отпустило раньше", "It passed sooner") else tr("Волна прошла", "The wave has passed"), Modifier.fillMaxWidth(), color = LAMP, ink = NIGHT) { step = 2 }
                else -> {
                    Btn(tr("Удержаться получилось", "I held on"), Modifier.fillMaxWidth(), color = LAMP, ink = NIGHT) { save(true) }
                    Spacer(Modifier.height(8.dp))
                    Text(tr("Не получилось в этот раз", "Not this time"), style = T.bodyB.copy(color = MUTE), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { save(false) }.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, on: Boolean, onClick: () -> Unit) =
    Text(text, style = T.smallB.copy(color = if (on) NIGHT else INK), modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) LAMP else Color(0x22FFFFFF))
        .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp))

/** A night window with the beast behind the glass and a lamp on the sill. */
@Composable
private fun Window(kind: String, frame: Int, sitting: Boolean) {
    Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(230.dp).clip(RoundedCornerShape(topStart = 110.dp, topEnd = 110.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF0C1124), Color(0xFF1E2A4A)))), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val r = app.wildling.domain.Rand(11)
            repeat(26) { drawCircle(Color.White.copy(alpha = .25f + r.next() * .5f), size.width * .004f, Offset(r.next() * size.width, r.next() * size.height * .6f)) }
            // the frame of the window
            drawLine(Color(0xFF3A3020), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), size.width * .018f)
            drawLine(Color(0xFF3A3020), Offset(0f, size.height * .45f), Offset(size.width, size.height * .45f), size.width * .018f)
        }
        // the sill first, then the beast standing on it, whole
        Box(Modifier.fillMaxWidth().height(14.dp).background(Color(0xFF5A4630)))
        Creature(kind, frame, 170.dp, Modifier.padding(bottom = 8.dp), look = if (sitting) Look.FULL else Look.DIM)
    }
}

/** A circle that grows on the in-breath and shrinks on the out-breath: four seconds each. */
@Composable
private fun Breath() {
    val t by rememberInfiniteTransition(label = "breath").animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "b")
    val time = if (BuildConfig.SHOTS) .25f else t
    val k = (sin((time * 2 - .5f) * PI).toFloat() + 1) / 2
    Box(Modifier.padding(top = 10.dp).size(110.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(LAMP.copy(alpha = .14f), size.width / 2 * (.55f + .45f * k))
            drawCircle(LAMP.copy(alpha = .6f), size.width / 2 * (.55f + .45f * k), style = Stroke(2.dp.toPx()))
        }
        Text(if (time < .5f) tr("вдох", "in") else tr("выдох", "out"), style = T.bodyB.copy(color = INK))
    }
}
