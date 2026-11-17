package app.wildling.ui.art

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.wildling.BuildConfig
import app.wildling.domain.Kinds
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Pictures from res/drawable-nodpi by name, decoded once. */
object Art {
    private val cache = HashMap<String, ImageBitmap?>()
    @SuppressLint("DiscouragedApi")
    @Synchronized fun get(ctx: Context, name: String): ImageBitmap? = cache.getOrPut(name) {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (id == 0) null else BitmapFactory.decodeResource(ctx.resources, id, BitmapFactory.Options().apply { inScaled = false })?.asImageBitmap()
    }
    fun pet(ctx: Context, kind: String, frame: Int) = get(ctx, "pet_${kind}_$frame")
    fun item(ctx: Context, id: String) = get(ctx, "item_$id")
    fun prop(ctx: Context, id: String) = get(ctx, "prop_$id")
}

/** What a creature is doing. */
enum class Pose { IDLE, MOVE, CHEER, SLEEP }

/** The frame for a stage of trust: 0 wild or shy, 1 curious, 2 calm, 3 its own move, 4 cheering, 5 asleep. */
fun frameOf(stage: Int, pose: Pose = Pose.IDLE): Int = when {
    pose == Pose.CHEER -> 4
    pose == Pose.SLEEP -> 5
    pose == Pose.MOVE && stage >= 3 -> 3
    stage <= 1 -> 0
    stage == 2 -> 1
    stage == 5 -> 5
    else -> 2
}

/** How a creature is shown: whole, dimmed at the forest's edge, or only a shadow in the thicket. */
enum class Look { FULL, DIM, SHADOW }

private val still = BuildConfig.SHOTS

private fun shinyFilter(): ColorFilter {
    val a = PI.toFloat() * .55f; val c = cos(a); val s = sin(a)
    return ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        .213f + c * .787f - s * .213f, .715f - c * .715f - s * .715f, .072f - c * .072f + s * .928f, 0f, 0f,
        .213f - c * .213f + s * .143f, .715f + c * .285f + s * .140f, .072f - c * .072f - s * .283f, 0f, 0f,
        .213f - c * .213f - s * .787f, .715f - c * .715f + s * .715f, .072f + c * .928f + s * .072f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f)))
}

private val dimFilter = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    .42f, 0f, 0f, 0f, 0f, 0f, .42f, 0f, 0f, 0f, 0f, 0f, .55f, 0f, 8f, 0f, 0f, 0f, 1f, 0f)))
private val shadowFilter = ColorFilter.tint(Color(0xFF141A22), BlendMode.SrcIn)

/** A creature drawn in a square of [size], standing on its bottom edge. It breathes, hops when cheering, dozes asleep. */
@Composable
fun Creature(kind: String, frame: Int, size: Dp, modifier: Modifier = Modifier, look: Look = Look.FULL, shiny: Boolean = false, phase: Float = 0f, flip: Boolean = false) {
    val ctx = LocalContext.current
    val img = Art.pet(ctx, kind, frame)
    val t by rememberInfiniteTransition(label = "pet").animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val time = if (still) .25f else (t + phase) % 1f
    Canvas(modifier.size(size)) { drawCreature(img, kind, frame, look, shiny, time, flip) }
}

fun DrawScope.drawCreature(img: ImageBitmap?, kind: String, frame: Int, look: Look, shiny: Boolean, time: Float, flip: Boolean = false) {
    val w = size.width; val h = size.height
    if (look != Look.SHADOW) drawOval(Color(0x33000000), Offset(w * .2f, h * .93f), Size(w * .6f, h * .07f))
    val wave = sin(time * 2 * PI).toFloat()
    val (sx, sy, dy) = when (frame) {
        4 -> Triple(1f, 1f, -wave * wave * h * .07f)
        5 -> Triple(1f + wave * .015f, 1f + wave * .02f, 0f)
        else -> Triple(1f + wave * .012f, 1f - wave * .016f, 0f)
    }
    if (img == null) {
        val col = Color(0xFF000000 or Kinds.of(kind).color)
        drawOval(if (look == Look.SHADOW) Color(0xFF141A22) else col, Offset(w * .2f, h * .35f + dy), Size(w * .6f, h * .6f)); return
    }
    val box = minOf(w / img.width, h * .92f / img.height)
    val bw = (img.width * box).roundToInt(); val bh = (img.height * box).roundToInt()
    val foot = Offset(w / 2, h * .965f)
    withTransform({ translate(0f, dy); scale((if (flip) -1f else 1f) * sx, sy, foot) }) {
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((w - bw) / 2).roundToInt(), (foot.y - bh).roundToInt()), IntSize(bw, bh),
            alpha = if (look == Look.SHADOW) .82f else 1f, filterQuality = FilterQuality.Medium,
            colorFilter = when (look) { Look.SHADOW -> shadowFilter; Look.DIM -> dimFilter; else -> if (shiny) shinyFilter() else null })
    }
    if (look == Look.SHADOW) { // two eyes glinting in the dark
        val blink = if (still) 1f else if ((time * 5) % 1f < .08f) .1f else 1f
        val ey = h * .965f - bh * .62f
        listOf(-.07f, .07f).forEach { drawOval(Color(0xFFFFD27A), Offset(w / 2 + w * it - w * .025f, ey), Size(w * .05f, w * .035f * blink)) }
    }
    if (frame == 5 && !still) {
        val z = (time * 3) % 1f
        drawCircle(Color(0x88FFFFFF), w * .022f * (1 + z), Offset(w * (.7f + z * .08f), h * (.4f - z * .15f)))
    }
}

/** A small still picture of a creature (its calm frame), a dark shape when not met yet. */
@Composable
fun Mini(kind: String, size: Dp, modifier: Modifier = Modifier, dim: Boolean = false, frame: Int = 2) {
    val ctx = LocalContext.current
    val img = Art.pet(ctx, kind, frame)
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        if (img == null) { drawCircle(Color(0xFF000000 or Kinds.of(kind).color).copy(alpha = if (dim) .3f else 1f), w * .36f, Offset(w / 2, h / 2)); return@Canvas }
        val box = minOf(w / img.width, h / img.height) * .94f
        val dw = img.width * box; val dh = img.height * box
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((w - dw) / 2).roundToInt(), ((h - dh) / 2).roundToInt()), IntSize(dw.roundToInt(), dh.roundToInt()),
            filterQuality = FilterQuality.Medium, colorFilter = if (dim) shadowFilter else null, alpha = if (dim) .3f else 1f)
    }
}

/** A prop (pinecone, sprout, letter…) or a thing for the lodge, fitted into [size]. */
@Composable
fun Prop(id: String, size: Dp, modifier: Modifier = Modifier, alpha: Float = 1f, item: Boolean = false) {
    val ctx = LocalContext.current
    val img = if (item) Art.item(ctx, id) else Art.prop(ctx, id)
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        if (img == null) { drawCircle(Color(0xFF8A6A3A).copy(alpha = alpha), w * .35f, Offset(w / 2, h / 2)); return@Canvas }
        val box = minOf(w / img.width, h / img.height)
        val dw = img.width * box; val dh = img.height * box
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((w - dw) / 2).roundToInt(), ((h - dh) / 2).roundToInt()), IntSize(dw.roundToInt(), dh.roundToInt()), alpha = alpha, filterQuality = FilterQuality.Medium)
    }
}

/** A pixel icon from the icons sheet (ic_<name>), drawn with crisp pixels; [dim] greys it out. */
@Composable
fun Ico(name: String, size: Dp, modifier: Modifier = Modifier, dim: Boolean = false, grey: Boolean = false) {
    val ctx = LocalContext.current
    val img = Art.get(ctx, "ic_$name")
    Canvas(modifier.size(size)) {
        if (img == null) return@Canvas
        val w = this.size.width; val h = this.size.height
        val box = minOf(w / img.width, h / img.height)
        val dw = img.width * box; val dh = img.height * box
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((w - dw) / 2).roundToInt(), ((h - dh) / 2).roundToInt()), IntSize(dw.roundToInt(), dh.roundToInt()),
            alpha = if (dim) .5f else if (grey) .9f else 1f, filterQuality = FilterQuality.Medium, colorFilter = if (dim) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(.15f) }) else if (grey) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
    }
}
