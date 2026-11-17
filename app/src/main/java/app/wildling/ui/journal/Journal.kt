package app.wildling.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.wildling.container
import app.wildling.data.Note
import app.wildling.domain.Days
import app.wildling.domain.Engine
import app.wildling.domain.Moods
import app.wildling.domain.Triggers
import app.wildling.domain.fmtDayYear
import app.wildling.domain.relDay
import app.wildling.domain.tr
import app.wildling.ui.Data
import app.wildling.ui.Nav
import app.wildling.ui.Screen
import app.wildling.ui.art.Mini
import app.wildling.ui.art.Prop
import app.wildling.ui.components.Btn
import app.wildling.ui.components.ChipRow
import app.wildling.ui.components.Empty
import app.wildling.ui.components.Field
import app.wildling.ui.components.Ghost
import app.wildling.ui.components.Page
import app.wildling.ui.components.Segments
import app.wildling.ui.components.card
import app.wildling.ui.components.ruled
import app.wildling.ui.home.label
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T

@Composable
fun JournalScreen(d: Data, nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(app.wildling.ui.Start.sub.also { app.wildling.ui.Start.sub = 0 }) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Дневник", "Journal"), style = T.title, modifier = Modifier.weight(1f))
            if (tab == 0) Btn(tr("+ Страница", "+ Page"), small = true) { nav.push(Screen.NoteEdit(d.noteToday?.id ?: 0)) }
        }
        Segments(listOf(tr("Тропа", "Trail"), tr("Карта дней", "Day map"), tr("Тяга", "Cravings")), tab) { tab = it }
        Spacer(Modifier.height(10.dp))
        when (tab) {
            0 -> Trail(d, nav)
            1 -> AdventureMap(d, nav, d.bad.firstOrNull()?.id ?: d.main?.id ?: 0, inline = true)
            else -> CravingsView(d, nav)
        }
    }
}

/** The day number in the main habit's count, as a short tag. */
private fun dayTag(d: Data, day: Long): String? = d.main?.let { Engine.dayNo(it, day) }?.takeIf { it >= 1 }?.let { tr("день $it", "day $it") }

@Composable
fun NotePage(scr: Screen.NoteEdit, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val old = d.notes.firstOrNull { it.id == scr.id }
    var mood by remember { mutableIntStateOf(old?.mood ?: scr.mood.takeIf { it >= 0 } ?: 0) }
    var text by remember { mutableStateOf(old?.text ?: "") }
    val day = old?.day ?: d.today
    Page(if (old == null) tr("Как прошёл день", "How the day went") else fmtDayYear(day), { nav.pop() }) {
        dayTag(d, day)?.let { Text(it.replaceFirstChar { ch -> ch.uppercase() } + " · " + (d.main?.label() ?: ""), style = T.smallB.copy(color = C.acc)) }
        Text(tr("Какая погода на душе?", "What's the weather inside?"), style = T.bodyB, modifier = Modifier.padding(top = 12.dp))
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Moods.all.indices.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { i ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (i == mood) C.accSoft else C.card)
                            .border(1.5.dp, if (i == mood) C.acc else C.frame, RoundedCornerShape(8.dp)).clickable { mood = i }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            MoodIco(i, 40.dp)
                            Text(Moods.name(i), style = T.tiny.copy(color = if (i == mood) C.ink else C.mute))
                        }
                    }
                }
            }
        }
        var shift by remember { mutableIntStateOf(0) }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(Prompts.of(day, shift), style = T.bodyB, modifier = Modifier.weight(1f))
            Text(tr("другой вопрос", "another"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { shift++ }.padding(6.dp))
        }
        Field(text, { text = it }, tr("Пара слов – как есть, без красивостей…", "A few words – as it is, nothing fancy…"),
            Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(top = 8.dp), single = false, style = handStyle)
        // what the map says about this day, if anything
        d.main?.takeIf { it.bad }?.let { m -> Days.at(Engine.streak(m, d.marks, d.today)).first?.let { mile ->
            Column(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(16.dp)).background(C.card2).padding(14.dp)) {
                Text(mile.title.uppercase(), style = T.sec)
                Text(mile.text, style = T.small.copy(color = C.ink), modifier = Modifier.padding(top = 4.dp))
            }
        } }
        Spacer(Modifier.height(18.dp))
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth()) {
            c.write { saveNote((old ?: Note(day = day, at = System.currentTimeMillis(), mood = mood, text = "")).copy(mood = mood, text = text.trim())) }
            nav.pop()
        }
        if (old != null) {
            Spacer(Modifier.height(8.dp))
            var sure by remember { mutableStateOf(false) }
            Ghost(if (sure) tr("Точно удалить?", "Delete for sure?") else tr("Удалить запись", "Delete note"), Modifier.fillMaxWidth(), color = C.bad) {
                if (sure) { c.write { deleteNote(old.id) }; nav.pop() } else sure = true
            }
        }
    }
}

// ---------------- the map of hard days ----------------

@Composable
fun MapPage(habitId: Long, d: Data, nav: Nav) = Page(tr("Карта трудных дней", "The map of hard days"), { nav.pop() }) { AdventureMap(d, nav, habitId, inline = false) }

private fun clock(ms: Long): String = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalTime().let { "%d:%02d".format(it.hour, it.minute) }
