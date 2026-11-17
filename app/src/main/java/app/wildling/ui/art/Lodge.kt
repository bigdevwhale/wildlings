package app.wildling.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.wildling.BuildConfig
import app.wildling.domain.Rand
import app.wildling.domain.Shop
import app.wildling.domain.Thing
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/** A creature in the scene: its key (a habit id, or below zero a rare guest), kind, stage of trust; [bad] picks its room in the house. */
class Guest(val key: Long, val kind: String, val stage: Int, val shiny: Boolean = false, val cheer: Boolean = false, val bad: Boolean = true)

/** The two views of the place: the yard at the forest's edge, and the house cut open. */
enum class View { YARD, HOUSE }

/** Which view a stage of trust stands in. 6 = a rare guest (in the attic). */
fun viewOf(stage: Int) = if (stage <= 3) View.YARD else View.HOUSE

/** The light: [day] 0 night .. 1 full day, [glow] how much dawn or dusk colours the sky. */
data class Light(val day: Float, val glow: Float) {
    val night get() = day < .3f
    companion object {
        fun now(): Light {
            if (BuildConfig.SHOTS) return Light(1f, .15f)
            val t = LocalTime.now(); val h = t.hour + t.minute / 60f
            val day = minOf(((h - 6f) / 1.5f).coerceIn(0f, 1f), ((20.5f - h) / 1.5f).coerceIn(0f, 1f))
            val glow = maxOf(1f - abs(h - 6.8f) / 1.4f, 1f - abs(h - 19.8f) / 1.4f).coerceIn(0f, 1f)
            return Light(day, glow)
        }
    }
}

private class Zone(val x0: Float, val x1: Float, val y: Float, val h: Float, val look: Look = Look.FULL)

/** Where each place is, as parts of the picture. */
private object Zones {
    val thicket = Zone(.15f, .3f, .665f, .17f, Look.SHADOW)
    val edge = Zone(.02f, .12f, .8f, .21f, Look.DIM)
    val fence = Zone(.22f, .6f, .705f, .2f)
    val porch = Zone(.74f, .95f, .69f, .21f)
    val hearth = Zone(.56f, .93f, .905f, .22f)
    val kitchen = Zone(.08f, .37f, .905f, .2f)
    val bedroom = Zone(.27f, .46f, .462f, .17f)
    val attic = Zone(.6f, .76f, .462f, .17f)
    fun of(g: Guest): Zone = when (g.stage) { 0 -> thicket; 1 -> edge; 2 -> fence; 3 -> porch; 4 -> if (g.bad) hearth else kitchen; 5 -> bedroom; else -> attic }
}

/** The lights that glow at night in each view: x, y, radius. */
private val lights = mapOf(
    View.YARD to listOf(Triple(.755f, .48f, .07f), Triple(.915f, .48f, .07f)),
    View.HOUSE to listOf(Triple(.72f, .74f, .17f), Triple(.13f, .61f, .06f), Triple(.84f, .6f, .06f), Triple(.445f, .3f, .06f), Triple(.675f, .24f, .06f), Triple(.6f, .69f, .05f)),
)

/**
 * One view of the lodge: the sky of this hour with drifting clouds, the picture, the beds and things,
 * and the creatures standing where their trust has brought them.
 */
@Composable
fun LodgeScene(view: View, guests: List<Guest>, items: Set<String>, beds: List<Pair<String, Int>?>, modifier: Modifier = Modifier, light: Light = Light.now(),
               preview: String? = null, onGuest: ((Guest) -> Unit)? = null, onBed: ((Int) -> Unit)? = null) {
    val ctx = LocalContext.current
    val tt by rememberInfiniteTransition(label = "sky").animateFloat(0f, 1f, infiniteRepeatable(tween(240000, easing = LinearEasing)), label = "t")
    val t = if (BuildConfig.SHOTS) .3f else tt
    val bg = Art.get(ctx, if (view == View.YARD) "yard_bg" else "house_bg")
    val clouds = remember { (0..5).mapNotNull { Art.get(ctx, "cloud_$it") } }
    val shown = Shop.things.filter { (it.id in items || it.id == preview) && it.inside == (view == View.HOUSE) }
    val here = guests.filter { viewOf(it.stage) == view }
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.5f)) {
        val wDp = maxWidth; val hDp = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            drawSky(light, t, clouds, view)
            if (view == View.HOUSE) drawRect(Color(0xFF4E6B34), Offset(0f, size.height * .94f), size.copy(height = size.height * .06f))
            if (bg != null) drawImage(bg, IntOffset.Zero, IntSize(bg.width, bg.height), IntOffset.Zero, IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.Medium)
            for (th in shown.filter { it.back || it.hang }) drawThing(ctx, th, th.id == preview && th.id !in items)
        }
        if (view == View.YARD && onBed != null) Shop.beds.forEachIndexed { i, (cx, cy) ->
            Box(Modifier.offset(x = wDp * (cx - .08f), y = hDp * (cy - .12f)).size(wDp * .16f, hDp * .16f).clickable(remember { MutableInteractionSource() }, null) { onBed(i) })
        }
        // the creatures, place by place, spread evenly; the far ones first
        val tap = remember { MutableInteractionSource() }
        here.groupBy { Zones.of(it) }.entries.sortedBy { it.key.y }.forEach { (z, list) ->
            list.forEachIndexed { i, g ->
                key(g.key) {
                    val s = hDp * z.h
                    val fx = if (list.size == 1) (z.x0 + z.x1) / 2 else z.x0 + (z.x1 - z.x0) * i / (list.size - 1)
                    val frame = if (g.cheer) 4 else if (g.stage == 6) 2 else frameOf(g.stage)
                    Box(Modifier.offset(x = wDp * fx - s / 2, y = hDp * (z.y + (i % 2) * .01f) - s)
                        .then(if (onGuest != null) Modifier.clickable(tap, null) { onGuest(g) } else Modifier)) {
                        Creature(g.kind, frame, s, look = z.look, shiny = g.shiny, phase = (g.key % 7) / 7f, flip = i % 2 == 1 && g.stage >= 4)
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            if (view == View.YARD) beds.forEachIndexed { i, b ->
                if (b == null) return@forEachIndexed
                val (id, g) = b
                val img = Art.prop(ctx, if (g == 0) "sprout" else if (g == 1) "seedling" else id) ?: return@forEachIndexed
                val (cx, cy) = Shop.beds[i]
                val hh = size.height * when (g) { 0 -> .07f; 1 -> .1f; else -> if (id == "apple" || id == "sunflower") .22f else .14f }
                val ww = hh * img.width / img.height
                drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset((size.width * cx - ww / 2).roundToInt(), (size.height * cy - hh).roundToInt()),
                    IntSize(ww.roundToInt(), hh.roundToInt()), filterQuality = FilterQuality.Medium)
            }
            for (th in shown.filter { !it.back && !it.hang }.sortedBy { it.y }) drawThing(ctx, th, th.id == preview && th.id !in items)
            drawNight(light, t, view, "lamp" in items)
        }
    }
}

private fun DrawScope.drawThing(ctx: android.content.Context, th: Thing, ghost: Boolean) {
    val img = Art.item(ctx, th.id) ?: return
    val w = size.width * th.w; val h = w * img.height / img.width
    val x = size.width * th.x - w / 2; val y = if (th.hang) size.height * th.y else size.height * th.y - h
    drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(x.roundToInt(), y.roundToInt()), IntSize(w.roundToInt(), h.roundToInt()),
        alpha = if (ghost) .8f else 1f, filterQuality = FilterQuality.Medium)
}

private fun mix(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)

/** The sky behind the picture: blue with drifting pixel clouds by day, amber at dawn and dusk, stars and a moon at night. */
private fun DrawScope.drawSky(l: Light, t: Float, clouds: List<ImageBitmap>, view: View) {
    val top = mix(Color(0xFF141B33), Color(0xFF6FA8DC), l.day)
    val low = mix(mix(Color(0xFF2A3256), Color(0xFFCDE6F2), l.day), Color(0xFFF2A35E), l.glow * .8f)
    drawRect(Brush.verticalGradient(listOf(top, low), 0f, size.height * .75f))
    val dark = (1f - l.day / .6f).coerceIn(0f, 1f)
    if (dark > 0f) {
        val r = Rand(7)
        repeat(46) {
            val x = r.next() * size.width; val y = r.next() * size.height * .6f
            val tw = .5f + .5f * sin((t * 40 + r.next()) * 2 * PI).toFloat()
            drawRect(Color.White.copy(alpha = dark * (.35f + .5f * tw)), Offset(x, y), androidx.compose.ui.geometry.Size(size.width * .004f, size.width * .004f))
        }
        val mx = if (view == View.YARD) .5f else .9f
        drawCircle(Color(0xFFFFF4D6).copy(alpha = dark), size.width * .028f, Offset(size.width * mx, size.height * .12f))
        drawCircle(top.copy(alpha = dark), size.width * .024f, Offset(size.width * (mx + .014f), size.height * .11f))
    }
    if (clouds.isNotEmpty()) {
        val r = Rand(if (view == View.YARD) 3 else 5)
        repeat(4) { k ->
            val img = clouds[(k * 2 + if (view == View.YARD) 0 else 1) % clouds.size]
            val w = size.width * (.13f + r.next() * .08f); val h = w * img.height / img.width
            val speed = if (k % 2 == 0) 1f else 2f
            val x = (((k / 4f + r.next() * .1f + t * speed) % 1f) * 1.3f - .15f) * size.width; val y = size.height * (.06f + (k % 3) * .1f + r.next() * .04f)
            drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(x.roundToInt(), y.roundToInt()), IntSize(w.roundToInt(), h.roundToInt()),
                alpha = .35f + .6f * l.day, filterQuality = FilterQuality.None)
        }
    }
}

/** Night over everything, with the lodge's lights glowing through it. */
private fun DrawScope.drawNight(l: Light, t: Float, view: View, lamp: Boolean) {
    val dark = (1f - l.day) * (if (view == View.HOUSE) .35f else .5f)
    if (dark <= .01f) return
    drawRect(Color(0xFF0E1530).copy(alpha = dark))
    val flick = if (BuildConfig.SHOTS) 1f else .92f + .08f * sin(t * 400 * PI).toFloat()
    val ls = lights[view].orEmpty() + if (lamp && view == View.YARD) listOf(Triple(.73f, .8f, .07f)) else emptyList()
    for ((x, y, r) in ls) {
        val c = Offset(size.width * x, size.height * y); val rr = size.width * r * flick
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC46B).copy(alpha = dark * 1.4f), Color.Transparent), c, rr), rr, c)
    }
}
