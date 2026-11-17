package app.wildling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.wildling.container
import app.wildling.domain.Money
import app.wildling.domain.today
import app.wildling.domain.tr
import app.wildling.ui.art.Guest
import app.wildling.ui.art.LodgeScene
import app.wildling.ui.components.Btn
import app.wildling.ui.more.Draft
import app.wildling.ui.more.Wizard
import app.wildling.ui.more.toHabit
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T
import java.util.Locale

/** The first run: the lodge and the idea, then the first habit. */
@Composable
fun Onboarding() {
    val c = LocalContext.current.container
    var step by remember { mutableIntStateOf(0) }
    val t = today()
    val f = remember { Draft(null, true, "", t, emptySet()) }
    if (step == 1) {
        val fallback = remember(f.kind) { c.nameFor(f.kind) }
        Wizard(f, t, false, fallback, { c.nameFor(it) }, onCancel = { step = 0 }, onDone = {
            val cur = Money.guess(Locale.getDefault().country, app.wildling.domain.Lang.ru)
            val h = f.toHabit(null, t, fallback)
            c.write { begin(h, cur) }
        })
        return
    }
    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (step == 0) {
                Text("Wildling", style = T.title, modifier = Modifier.padding(top = 16.dp))
                Text(tr("Приручи свои привычки", "Tame your habits"), style = T.h3.copy(color = C.acc))
                Spacer(Modifier.height(12.dp))
                LodgeScene(app.wildling.ui.art.View.YARD, listOf(Guest(1, "smudge", 0), Guest(2, "toffee", 1), Guest(3, "rambler", 2, bad = false), Guest(4, "drowse", 3, bad = false)), emptySet(), listOf(null, "sprout" to 0, null),
                    Modifier.clip(RoundedCornerShape(20.dp)))
                Spacer(Modifier.height(16.dp))
                listOf(
                    tr("Вредная привычка – дикий зверь из чащи. Он приходит за своим. Не кормишь – и он шаг за шагом приручается: от тени в лесу до сна у очага.",
                        "A bad habit is a wild beast from the thicket. It comes for its due. Don't feed it, and step by step it tames: from a shadow in the woods to sleep by the hearth."),
                    tr("Полезная привычка – пугливый зверёк. Делай её снова и снова – и он подойдёт ближе.",
                        "A good habit is a shy creature. Do it again and again and it comes closer."),
                    tr("Когда тянет – нажми фонарь внизу. Зверь придёт к окну, и вы вместе переждёте волну.",
                        "When it pulls, tap the lantern below. The beast comes to the window and you wait out the wave together."),
                    tr("Срыв – не обнуление, а шаг назад. Дневник, сад и всё пройденное остаются с тобой.",
                        "A slip isn't a reset, just a step back. The journal, the garden and the way you've come stay with you."),
                ).forEachIndexed { i, s ->
                    Row(Modifier.padding(bottom = 10.dp)) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(C.accSoft), contentAlignment = Alignment.Center) { Text("${i + 1}", style = T.smallB.copy(color = C.acc)) }
                        Spacer(Modifier.width(10.dp))
                        Text(s, style = T.body, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(tr("Начать", "Begin"), Modifier.fillMaxWidth()) { step = 1 }
        }
    }
}
