package app.wildling.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.wildling.container
import app.wildling.currentLanguage
import app.wildling.domain.Money
import app.wildling.domain.Shop
import app.wildling.domain.tr
import app.wildling.setLanguage
import app.wildling.ui.Data
import app.wildling.ui.Nav
import app.wildling.ui.Screen
import app.wildling.ui.art.LodgeScene
import app.wildling.ui.art.Mini
import app.wildling.ui.art.Prop
import app.wildling.ui.components.Btn
import app.wildling.ui.components.ChipRow
import app.wildling.ui.components.Field
import app.wildling.ui.components.Page
import app.wildling.ui.components.Row2
import app.wildling.ui.components.Section
import app.wildling.ui.components.Segments
import app.wildling.ui.components.card
import app.wildling.ui.home.ConePill
import app.wildling.ui.home.label
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T

/** Pinecones buy seeds for the beds and things for the lodge; a thing tapped is tried on in the scene first. */
@Composable
fun ShopPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var tab by remember { mutableIntStateOf(0) }
    var preview by remember { mutableStateOf<String?>(null) }
    var bed by remember { mutableIntStateOf(d.beds.indexOfFirst { it == null }.coerceAtLeast(0)) }
    Page(tr("Лавка", "The shop"), { nav.pop() }, action = { ConePill(d.cones, Modifier.padding(end = 8.dp)) }) {
        LodgeScene(if (preview != null && app.wildling.domain.Shop.thingById[preview]?.inside == true) app.wildling.ui.art.View.HOUSE else app.wildling.ui.art.View.YARD, d.guests, d.s.items, d.beds, Modifier.clip(RoundedCornerShape(8.dp)), preview = preview)
        Text(tr("Шишки приносят чистые дни, сделанные привычки, пережитые тяги, записи в дневнике и каждый новый шаг зверей.",
            "Pinecones come from clean days, habits done, cravings sat out, journal notes and every step your creatures take."), style = T.small, modifier = Modifier.padding(top = 8.dp))
        Segments(listOf(tr("Грядки", "Beds"), tr("Для дома", "For the lodge")), tab, Modifier.padding(top = 10.dp)) { tab = it }
        if (tab == 0) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                d.beds.forEachIndexed { i, b ->
                    val seed = b?.let { Shop.seedById[it.first] }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (i == bed) C.accSoft else C.card).border(1.5.dp, if (i == bed) C.acc else C.line, RoundedCornerShape(14.dp))
                        .clickable { bed = i }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Prop(if (b == null) "sprout" else if (b.second == 0) "sprout" else if (b.second == 1) "seedling" else b.first, 44.dp, alpha = if (b == null) .25f else 1f)
                        Text(seed?.title ?: tr("Пусто", "Empty"), style = T.tiny.copy(color = C.ink), maxLines = 1)
                        Text(when (b?.second) { null -> tr("грядка ${i + 1}", "bed ${i + 1}"); 2 -> tr("созрело!", "ripe!"); else -> tr("растёт", "growing") }, style = T.tiny)
                    }
                }
            }
            val cur = d.beds[bed]
            if (cur != null) {
                Spacer(Modifier.height(10.dp))
                if (cur.second == 2) Btn(tr("Собрать урожай", "Gather the harvest"), Modifier.fillMaxWidth()) { c.write { val g = gather(bed); if (g > 0) say(tr("+$g шишек за урожай", "+$g pinecones for the harvest")) } }
                else Text(tr("Растёт сам по себе: срыв ему не мешает.", "It grows on its own: a slip doesn't hurt it."), style = T.small)
            } else Shop.seeds.forEach { s ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Prop(s.id, 40.dp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(s.title, style = T.bodyB)
                        Text(tr("созреет за ${s.days} дн.", "ripe in ${s.days} days"), style = T.small)
                    }
                    Btn("${s.price}", small = true, enabled = d.cones >= s.price) { c.write { if (sow(bed, s.id)) say(tr("Посажено: ${s.title.lowercase()}", "Sown: ${s.title.lowercase()}")) } }
                }
            }
        } else {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Shop.things.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { th ->
                            val have = th.id in d.s.items
                            Column(Modifier.weight(1f).card(16.dp).clickable { preview = if (preview == th.id) null else th.id }.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Prop(th.id, 40.dp, item = true)
                                    Text(th.title, style = T.smallB, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.height(6.dp))
                                if (have) Text(tr("Уже стоит", "In place"), style = T.tiny.copy(color = C.ok))
                                else Btn(if (preview == th.id) tr("Купить · ${th.price}", "Buy · ${th.price}") else "${th.price}", Modifier.fillMaxWidth(), small = true, enabled = d.cones >= th.price) {
                                    c.write { if (buy(th.id)) say(tr("${th.title} – теперь в сторожке", "${th.title} – now at the lodge")) }; preview = null
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Written on a good day, read on a bad one: it opens by the window when the beast comes. */
@Composable
fun LetterPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var text by remember { mutableStateOf(d.s.letter) }
    Page(tr("Письмо себе", "A letter to yourself"), { nav.pop() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Prop("letter", 64.dp)
            Text(tr("Напиши его в хороший день. Оно откроется у окна, когда зверь придёт и станет трудно.",
                "Write it on a good day. It opens by the window when the beast comes and things get hard."), style = T.small, modifier = Modifier.padding(start = 10.dp))
        }
        Field(text, { text = it }, tr("Привет. Если ты это читаешь, значит, сейчас тянет. Вспомни…", "Hi. If you're reading this, it's pulling right now. Remember…"),
            Modifier.fillMaxWidth().heightIn(min = 220.dp).padding(top = 12.dp), single = false)
        Spacer(Modifier.height(16.dp))
        Btn(tr("Запечатать", "Seal it"), Modifier.fillMaxWidth()) { c.write { settings.update { it.copy(letter = text.trim()) }; say(tr("Письмо ждёт у окна", "The letter waits by the window")) }; nav.pop() }
    }
}

@Composable
fun SettingsPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    Page(tr("Настройки", "Settings"), { nav.pop() }) {
        Section(tr("Письмо и привычки", "Letter and habits"))
        Row2(tr("Письмо себе", "A letter to yourself"), if (d.s.letter.isBlank()) tr("Ещё не написано", "Not written yet") else tr("Ждёт у окна", "Waiting by the window"),
            lead = { Prop("letter", 36.dp) }) { nav.push(Screen.Letter) }
        val gone = d.habits.filter { it.archived }
        if (gone.isNotEmpty()) {
            Text(tr("Отпущены в лес", "Let go to the forest"), style = T.smallB, modifier = Modifier.padding(top = 10.dp))
            gone.forEach { h -> Row2(h.name, h.label(), lead = { Mini(h.kind, 36.dp) }) { nav.push(Screen.Edit(h.id)) } }
        }
        Section(tr("Вечерний вопрос", "Evening question"))
        Text(tr("Раз в день спросим, как прошёл день, – если в дневнике ещё пусто.", "Once a day we ask how the day went – if the journal is still empty."), style = T.small)
        val hours = listOf(-1, 19, 20, 21, 22, 23)
        ChipRow(hours.map { if (it < 0) tr("Выкл.", "Off") else "$it:00" }, hours.indexOf(d.s.evening).coerceAtLeast(0), Modifier.padding(top = 8.dp)) { i ->
            c.write { settings.update { it.copy(evening = hours[i]) } }
        }
        Section(tr("Оформление", "Look"))
        val themes = listOf("system", "light", "dark")
        Segments(listOf(tr("Как в системе", "System"), tr("Светлая", "Light"), tr("Тёмная", "Dark")), themes.indexOf(d.s.theme).coerceAtLeast(0)) { i ->
            c.write { settings.update { it.copy(theme = themes[i]) } }
        }
        Section(tr("Язык", "Language"))
        val langs = listOf("", "ru", "en")
        Segments(listOf(tr("Как в системе", "System"), "Русский", "English"), langs.indexOf(currentLanguage()).coerceAtLeast(0)) { setLanguage(langs[it]) }
        Section(tr("Валюта", "Currency"))
        ChipRow(Money.all.map { "${it.sign} ${it.code}" }, Money.all.indexOfFirst { it.code == d.s.currency }.coerceAtLeast(0)) { i ->
            c.write { Money.set(Money.all[i].code); settings.update { it.copy(currency = Money.all[i].code) } }
        }
        Section(tr("Подсказки", "Hints"))
        Row2(tr("Показать знакомство с экранами заново", "Show the screen introductions again"), null) { c.write { settings.update { it.copy(seen = emptySet()) }; say(tr("Подсказки вернутся", "Hints will come back")) } }
        Section(tr("Поддержать", "Support"))
        Text(tr("StillCraft держится на поддержке тех, кому нравятся эти приложения.", "StillCraft lives on the goodwill of people who like these apps."), style = T.small)
        Link("heart", tr("Поддержать разработку", "Support development"), null, Community.SUPPORT)
        Section("StillCraft")
        Text(tr("Wildling – одно из небольших приложений StillCraft.", "Wildling is one of the small StillCraft apps."), style = T.small)
        Link("bell", tr("Сообщество в Telegram", "Telegram community"), tr("новости, обсуждения, другие приложения", "news, talk and the other apps"), Community.CHAT)
        Link("candle", tr("Сообщить об ошибке", "Report a bug"), tr("через бота StillCraft", "through the StillCraft bot"), Community.BUG)
        Link("star", tr("Предложить идею", "Suggest an idea"), null, Community.IDEA)
        Link("lantern", tr("Следить за обновлениями", "Follow updates"), tr("бот напишет, когда выйдет новая версия", "the bot writes when a new version is out"), Community.FOLLOW)
        Link("lodge", tr("Все приложения StillCraft", "All StillCraft apps"), null, Community.SITE)
        Link("scroll", tr("Исходный код", "Source code"), "GitHub", Community.CODE)
        Section(tr("О приложении", "About"))
        Text(tr("Wildling – приручение привычек. Всё хранится только на телефоне: без аккаунтов, рекламы и интернета.",
            "Wildling – taming habits. Everything stays on the phone: no accounts, no ads, no internet."), style = T.small)
        Text("v" + app.wildling.BuildConfig.VERSION_NAME, style = T.tiny, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Start)
        Spacer(Modifier.width(1.dp))
    }
}

/** Where the community lives: the StillCraft bot links carry the app's slug. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "wildling"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
    const val SITE = "https://bigdevwhale.github.io/stillcraft/"
    const val CODE = "https://github.com/bigdevwhale/wildlings"
}

/** A row that opens a link: a pixel icon, a title and a line under it. */
@Composable
private fun Link(icon: String, title: String, sub: String?, url: String) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(top = 6.dp).card().clickable {
        runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        app.wildling.ui.art.Ico(icon, 30.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, style = T.bodyB); if (sub != null) Text(sub, style = T.small) }
        Text("›", style = T.h2.copy(color = C.mute))
    }
}
