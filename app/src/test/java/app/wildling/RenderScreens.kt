package app.wildling

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.wildling.data.DemoSeed
import app.wildling.domain.Lang
import app.wildling.domain.Money
import app.wildling.ui.Onboarding
import app.wildling.ui.Root
import app.wildling.ui.Start
import app.wildling.ui.theme.WildTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Every screen of the demo drawn to PNGs in app/build/art/screens (run with `-Prender`).
 * The screen is very tall, so whole scrolling pages fit in one picture.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h2000dp-xxhdpi")
class RenderScreens {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val out = File(System.getProperty("render.out") ?: "build/art", "screens").apply { mkdirs() }
    private val dark = System.getProperty("render.dark") == "1"

    private fun shot(name: String) {
        repeat(6) { rule.mainClock.advanceTimeBy(400); Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "${if (dark) "d-" else ""}$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun render() {
        Lang.ru = true
        val c = ApplicationProvider.getApplicationContext<WildlingApp>().container
        var i by mutableIntStateOf(-1)
        rule.mainClock.autoAdvance = false
        rule.setContent { WildTheme(dark) { key(i) { if (i == -2) Onboarding() else { val s by c.settings.settings.collectAsState(null); s?.let { st -> if (i >= 0 && st.onboarded) Root(st) } } } } }
        rule.runOnUiThread { i = -2 }; shot("0-onboarding")
        runBlocking { DemoSeed.seedIfEmpty(c) }
        runBlocking { c.settings.update { it.copy(seen = it.seen + listOf("i.journal", "i.beasts", "i.stats", "i.habit", "i.shop")) } }
        Money.set(runBlocking { c.settings.now().currency })
        val h = runBlocking { c.dao.habitList() }
        val shots = listOf(
            "1-home" to (0 to null), "2-journal" to (1 to null), "3-beasts" to (2 to null), "4-stats" to (3 to null),
            "5-habit" to (0 to "habit:${h[0].id}"), "5b-good" to (0 to "habit:${h[3].id}"), "6-sos" to (0 to "sos"), "7-note" to (1 to "note"),
            "8-map" to (1 to "map"), "9-shop" to (0 to "shop"), "10-new" to (0 to "new"), "11-kind" to (2 to "kind:wisp"), "12-settings" to (0 to "settings"),
            "13-letter" to (0 to "letter"),
        )
        for ((n, sh) in shots.withIndex()) {
            Start.tab = sh.second.first; Start.open = sh.second.second
            rule.runOnUiThread { i = n }
            shot(sh.first)
        }
        runBlocking { c.settings.update { it.copy(items = app.wildling.domain.Shop.things.map { t -> t.id }.toSet()) } }
        Start.tab = 0; Start.open = null; rule.runOnUiThread { i = 300 }; shot("1c-home-full")
        // every postcard
        val d = runBlocking { app.wildling.ui.Data(c.settings.now(), c.dao.habitList(), c.dao.markList(), c.dao.urgeList(), c.dao.noteList(), c.clock()) }
        val cards = File(out.parentFile, "cards").apply { mkdirs() }
        app.wildling.ui.share.cardsFor(d).forEachIndexed { k, card ->
            val bmp = app.wildling.ui.share.Painter(rule.activity, d, false).render(card)
            File(cards, "$k-${card.key.replace(':', '_')}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
        }
    }
}
