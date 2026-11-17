package app.wildling.ui.share

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.wildling.R
import app.wildling.data.Habit
import app.wildling.domain.Days
import app.wildling.domain.Engine
import app.wildling.domain.Kinds
import app.wildling.domain.Moods
import app.wildling.domain.Stages
import app.wildling.domain.count
import app.wildling.domain.dayShort
import app.wildling.domain.fmt
import app.wildling.domain.fmt1
import app.wildling.domain.fmtDayYear
import app.wildling.domain.money
import app.wildling.domain.tr
import app.wildling.domain.weekStart
import app.wildling.ui.Data
import app.wildling.ui.art.frameOf
import app.wildling.ui.components.Btn
import app.wildling.ui.components.Chip
import app.wildling.ui.components.Sheet
import app.wildling.ui.home.label
import app.wildling.ui.theme.C
import app.wildling.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** What a postcard shows. */
sealed class Card(val key: String) {
    data class Creature(val habitId: Long) : Card("habit:$habitId")
    data class Mile(val habitId: Long, val days: Int) : Card("mile:$habitId:$days")
    data object Stats : Card("stats")
    data object Weather : Card("weather")
    data object Lodge : Card("lodge")

    companion object {
        fun of(key: String): Card? = when {
            key == "stats" -> Stats; key == "weather" -> Weather; key == "lodge" -> Lodge
            key.startsWith("habit:") -> Creature(key.drop(6).toLong())
            key.startsWith("mile:") -> key.split(':').let { Mile(it[1].toLong(), it[2].toInt()) }
            else -> null
        }
    }
}

/** Every card this life can make right now: the stats, each creature, the latest milestones, the weather, the lodge. */
fun cardsFor(d: Data): List<Card> = buildList {
    add(Card.Stats)
    d.active.forEach { add(Card.Creature(it.id)) }
    d.bad.forEach { h -> val n = Engine.streak(h, d.marks, d.today); Days.at(n).first?.takeIf { it.day >= 3 }?.let { add(Card.Mile(h.id, it.day)) } }
    if (d.notes.isNotEmpty()) add(Card.Weather)
    add(Card.Lodge)
}

/** Paints the 1080×1350 postcards: parchment in a wooden frame, pixel letters, the creatures from the sprite sheets. */
class Painter(private val ctx: Context, private val d: Data, private val hideSums: Boolean) {
    private val W = 1080; private val H = 1350
    private val pixel: Typeface = ResourcesCompat.getFont(ctx, R.font.press_start) ?: Typeface.MONOSPACE
    private val bold: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_bold) ?: Typeface.DEFAULT_BOLD
    private val body: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_regular) ?: Typeface.DEFAULT
    private val hand: Typeface = ResourcesCompat.getFont(ctx, R.font.caveat_bold) ?: Typeface.DEFAULT
    private val ink = 0xFF2E2618.toInt(); private val mute = 0xFF857A62.toInt(); private val moss = 0xFF4F7A45.toInt(); private val gold = 0xFFC98A2E.toInt()
    private val paper = 0xFFF6EEDA.toInt(); private val wood = 0xFF8A5A34.toInt(); private val wood2 = 0xFF6B4426.toInt()

    @SuppressLint("DiscouragedApi")
    private fun bmp(name: String): Bitmap? {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        return if (id == 0) null else BitmapFactory.decodeResource(ctx.resources, id, BitmapFactory.Options().apply { inScaled = false })
    }

    private fun Canvas.pic(name: String, cx: Float, bottom: Float, h: Float, alpha: Int = 255, top: Boolean = false) {
        val b = bmp(name) ?: return
        val w = h * b.width / b.height
        val y = if (top) bottom else bottom - h
        drawBitmap(b, null, RectF(cx - w / 2, y, cx + w / 2, y + h), Paint().apply { isFilterBitmap = false; this.alpha = alpha })
    }

    private fun Canvas.text(s: String, x: Float, y: Float, size: Float, color: Int, face: Typeface, align: Paint.Align = Paint.Align.LEFT) =
        drawText(s, x, y, Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = face; textAlign = align })

    private fun Canvas.block(s: String, x: Float, y: Float, width: Int, size: Float, color: Int, face: Typeface, center: Boolean = false): Float {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = face }
        val l = StaticLayout.Builder.obtain(s, 0, s.length, p, width).setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.08f).build()
        save(); translate(x, y); l.draw(this); restore()
        return y + l.height
    }

    /** The frame every card shares: wood border, parchment, the signature at the bottom. */
    private fun frame(c: Canvas) {
        c.drawColor(wood)
        val p = Paint().apply { color = wood2 }
        for (i in 0 until 12) c.drawRect(0f, i * 113f, W.toFloat(), i * 113f + 4f, p)
        c.drawRect(RectF(30f, 30f, W - 30f, H - 30f), Paint().apply { color = 0xFF4A2E18.toInt() })
        c.drawRect(RectF(36f, 36f, W - 36f, H - 42f), Paint().apply { color = paper })
        c.pic("ic_paw", 92f, H - 66f, 46f)
        c.text("WILDLING", 126f, H - 76f, 22f, mute, pixel)
        c.text(tr("приручи свои привычки", "tame your habits"), W - 70f, H - 76f, 28f, mute, body, Paint.Align.RIGHT)
    }

    /** A scene picture with the sky painted behind its see-through top. */
    private fun Canvas.scene(name: String, r: RectF) {
        drawRect(r, Paint().apply { shader = LinearGradient(0f, r.top, 0f, r.bottom, 0xFF6FA8DC.toInt(), 0xFFCDE6F2.toInt(), Shader.TileMode.CLAMP) })
        bmp("cloud_1")?.let { drawBitmap(it, null, RectF(r.left + r.width() * .3f, r.top + 30f, r.left + r.width() * .5f, r.top + 30f + r.width() * .2f * it.height / it.width), Paint().apply { isFilterBitmap = false }) }
        bmp(name)?.let { drawBitmap(it, null, r, Paint().apply { isFilterBitmap = true }) }
        drawRect(r, Paint().apply { style = Paint.Style.STROKE; strokeWidth = 6f; color = 0xFF4A2E18.toInt() })
    }

    /** The way to the hearth: six stops and the creature where it stands. */
    private fun Canvas.lane(h: Habit, y: Float) {
        val t = d.trust[h.id]; val st = t?.stage ?: 0; val f = ((st + if (st < 5) (t?.progress ?: 0f) else 0f) / 5f).coerceIn(0f, 1f)
        val x0 = 190f; val x1 = W - 190f; val col = if (h.bad) moss else gold
        drawLine(x0, y, x1, y, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC9B48A.toInt(); strokeWidth = 8f; pathEffect = android.graphics.DashPathEffect(floatArrayOf(22f, 16f), 0f) })
        drawLine(x0, y, x0 + (x1 - x0) * f, y, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; strokeWidth = 10f })
        for (i in 0..5) drawCircle(x0 + (x1 - x0) * i / 5f, y, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (i <= st) col else 0xFFC9B48A.toInt() })
        pic("ic_sign", x0 - 75f, y + 36f, 72f)
        pic("ic_lodge", x1 + 75f, y + 36f, 72f)
        pic("pet_${h.kind}_${frameOf(st).let { if (it == 5) 2 else it }}", x0 + (x1 - x0) * f, y - 8f, 110f)
    }

    /** The last fourteen days as squares: clean or slipped, done or not. */
    private fun Canvas.fortnight(h: Habit, y: Float) {
        val cell = 54f; val gap = 8f; val x0 = (W - (cell * 14 + gap * 13)) / 2
        for (k in 0 until 14) {
            val day = d.today - 13 + k
            val col = when { day < h.since -> 0xFFE4D9BE.toInt(); h.bad && d.slipped(h, day) -> 0xFFC4533A.toInt(); h.bad -> moss
                d.done(h, day) -> gold; else -> 0xFFE4D9BE.toInt() }
            drawRoundRect(RectF(x0 + k * (cell + gap), y, x0 + k * (cell + gap) + cell, y + cell), 10f, 10f, Paint().apply { color = col })
        }
        text(tr("последние две недели", "the last two weeks"), W / 2f, y + cell + 44f, 28f, mute, body, Paint.Align.CENTER)
    }

    fun render(card: Card): Bitmap {
        val b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        frame(c)
        when (card) {
            is Card.Creature -> creature(c, d.byId[card.habitId] ?: d.active.first())
            is Card.Mile -> mile(c, d.byId[card.habitId] ?: d.active.first(), card.days)
            Card.Stats -> stats(c)
            Card.Weather -> weather(c)
            Card.Lodge -> lodge(c)
        }
        return b
    }

    private fun creature(c: Canvas, h: Habit) {
        val t = d.trust[h.id]; val st = t?.stage ?: 0
        val r = RectF(70f, 70f, W - 70f, 70f + (W - 140f) * 2f / 3f)
        c.scene(if (st >= 4) "house_bg" else "yard_bg", r)
        c.drawRect(r, Paint().apply { color = 0x55FFFFFF })
        c.pic("pet_${h.kind}_${frameOf(st).let { if (it == 5) 5 else 4 }}", W / 2f, r.bottom - 10f, 520f)
        var y = r.bottom + 90f
        c.text(h.name.uppercase(), W / 2f, y, 52f, ink, pixel, Paint.Align.CENTER); y += 60f
        c.text(h.label(), W / 2f, y, 42f, moss, bold, Paint.Align.CENTER); y += 70f
        val big = if (h.bad) { val n = Engine.streak(h, d.marks, d.today); if (n <= 0) tr("первый день", "day one") else count(n.toInt(), "день", "дня", "дней", "day", "days") + tr(" без", " clean") }
            else tr("${Engine.dones(h, d.marks).size} раз · ${Engine.weeksInRow(h, d.marks, d.today)} нед. подряд", "${Engine.dones(h, d.marks).size} times · ${Engine.weeksInRow(h, d.marks, d.today)} wks in a row")
        c.text(big, W / 2f, y, 40f, ink, pixel, Paint.Align.CENTER); y += 56f
        c.text(Stages.name(st, h.bad), W / 2f, y, 36f, gold, bold, Paint.Align.CENTER)
        c.lane(h, y + 100f)
        c.fortnight(h, y + 150f)
    }

    private fun mile(c: Canvas, h: Habit, days: Int) {
        val m = Days.exactly(days.toLong()) ?: Days.at(days.toLong()).first
        val idx = listOf(1, 2, 3, 5, 7, 10, 14, 21, 30, 45, 60, 90, 180, 365).indexOf(m?.day ?: 1).coerceAtLeast(0)
        c.drawCircle(W / 2f, 360f, 300f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEFE3C2.toInt() })
        c.pic("ic_mark_$idx", W / 2f, 90f, 540f, top = true)
        c.pic("pet_${h.kind}_4", W / 2f + 300f, 640f, 300f)
        var y = 760f
        c.text((m?.title ?: "").uppercase(), W / 2f, y, 68f, ink, pixel, Paint.Align.CENTER); y += 66f
        c.text(tr("без зверя: ", "beast-free: ") + h.label().lowercase(), W / 2f, y, 44f, moss, bold, Paint.Align.CENTER)
        y = c.block(m?.text ?: "", 110f, y + 30f, W - 220, 48f, ink, hand, center = true)
        val clean = Engine.cleanDays(h, d.marks, d.today); val held = d.urges.count { it.habitId == h.id && it.held }
        val parts = listOfNotNull(tr("${count(clean.toInt(), "чистый день", "чистых дня", "чистых дней", "clean day", "clean days")}", "${count(clean.toInt(), "", "", "", "clean day", "clean days")}"),
            (h.cost * clean).takeIf { it > 0 && !hideSums }?.let { money(it) + tr(" не потрачено", " not spent") },
            held.takeIf { it > 0 }?.let { tr("$it тяг выдержано", "$it cravings held") })
        c.block(parts.joinToString("  ·  "), 110f, H - 270f, W - 220, 32f, ink, bold, center = true)
        c.text(fmtDayYear(d.today), W / 2f, H - 140f, 30f, mute, body, Paint.Align.CENTER)
    }

    private fun stats(c: Canvas) {
        c.text(tr("ЛЕТОПИСЬ", "CHRONICLE"), W / 2f, 140f, 56f, ink, pixel, Paint.Align.CENTER)
        val first = d.active.minOfOrNull { it.since } ?: d.today
        c.text(tr("${count((d.today - first + 1).toInt(), "день", "дня", "дней", "day", "days")} пути", "${count((d.today - first + 1).toInt(), "день", "дня", "дней", "day", "days")} on the way"), W / 2f, 200f, 38f, moss, bold, Paint.Align.CENTER)
        val clean = d.bad.sumOf { Engine.cleanDays(it, d.marks, d.today) }
        val saved = d.bad.sumOf { it.cost * Engine.cleanDays(it, d.marks, d.today) }
        val hours = d.bad.sumOf { it.minutes * Engine.cleanDays(it, d.marks, d.today) } / 60.0
        val tiles = listOf(
            Triple("star", fmt(clean), tr("дней без зверей", "beast-free days")),
            Triple("seeds", if (hideSums || saved <= 0) fmt(d.marks.count { it.kind == app.wildling.data.Mark.DONE }) else money(saved), if (hideSums || saved <= 0) tr("полезных дел", "good deeds") else tr("не потрачено", "not spent")),
            Triple("hourglass", if (hours > 0) tr("${fmt1(hours)} ч", "${fmt1(hours)} h") else fmt(d.notes.size), if (hours > 0) tr("времени вернулось", "time won back") else tr("записей", "notes")),
            Triple("lantern", "${d.urges.count { it.held }}/${d.urges.size}", tr("тяг выдержано", "cravings held")),
        )
        tiles.forEachIndexed { i, (ic, v, l) ->
            val x = 90f + (i % 2) * 460f; val y = 270f + (i / 2) * 220f
            c.drawRoundRect(RectF(x, y, x + 440f, y + 196f), 16f, 16f, Paint().apply { color = 0xFFFBF5E4.toInt() })
            c.drawRoundRect(RectF(x, y, x + 440f, y + 196f), 16f, 16f, Paint().apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = 0xFFC9B48A.toInt() })
            c.pic("ic_$ic", x + 70f, y + 40f, 100f, top = true)
            c.text(v, x + 140f, y + 95f, 38f, ink, pixel)
            c.text(l, x + 140f, y + 150f, 32f, mute, body)
        }
        // twelve weeks of the trail: each day a cell, brighter when cleaner and fuller
        run {
            val weeks = 12; val cell = 46f; val gap = 7f; val x0 = (W - (weeks * cell + (weeks - 1) * gap)) / 2; val y0 = 740f
            val start = weekStart(d.today) - (weeks - 1) * 7L
            val greens = intArrayOf(0xFFE4D9BE.toInt(), 0xFFC9DDB6.toInt(), 0xFF9CC27F.toInt(), 0xFF6E9F55.toInt(), 0xFF4A7A3A.toInt())
            val max = d.active.size.coerceAtLeast(1)
            for (w in 0 until weeks) for (k in 0..6) {
                val day = start + w * 7 + k; if (day > d.today) continue
                var n = 0; d.active.forEach { h -> if (day >= h.since) { if (h.bad && !d.slipped(h, day)) n++; if (!h.bad && d.done(h, day)) n++ } }
                val col = greens[if (n == 0) 0 else (1 + n * 3 / max).coerceAtMost(4)]
                c.drawRoundRect(RectF(x0 + w * (cell + gap), y0 + k * (cell * .62f + gap * .6f), x0 + w * (cell + gap) + cell, y0 + k * (cell * .62f + gap * .6f) + cell * .62f), 6f, 6f, Paint().apply { color = col })
            }
            c.text(tr("12 недель на тропе", "12 weeks on the trail"), W / 2f, y0 - 18f, 30f, mute, body, Paint.Align.CENTER)
        }
        // the creatures in a row, each with its stage
        val list = d.active.take(6)
        val step = (W - 180f) / list.size.coerceAtLeast(1)
        list.forEachIndexed { i, h ->
            val cx = 90f + step * (i + .5f)
            val st = d.trust[h.id]?.stage ?: 0
            c.pic("pet_${h.kind}_${frameOf(st).let { if (it == 5) 2 else it }}", cx, 1130f, step.coerceAtMost(200f) * .9f)
            c.text(h.name, cx, 1172f, 28f, ink, bold, Paint.Align.CENTER)
            c.text(Stages.name(st, h.bad).lowercase(), cx, 1206f, 24f, mute, body, Paint.Align.CENTER)
        }
    }

    private fun weather(c: Canvas) {
        c.text(tr("ПОГОДА НА ДУШЕ", "WEATHER INSIDE"), W / 2f, 150f, 48f, ink, pixel, Paint.Align.CENTER)
        c.text(tr("последние 4 недели", "the last 4 weeks"), W / 2f, 210f, 36f, mute, body, Paint.Align.CENTER)
        val first = weekStart(d.today) - 21
        val moods = d.notes.groupBy { it.day }.mapValues { it.value.first().mood }
        val cell = 128f; val x0 = (W - cell * 7) / 2
        (0..6).forEach { c.text(dayShort(it), x0 + cell * (it + .5f), 300f, 32f, mute, body, Paint.Align.CENTER) }
        for (w in 0 until 4) for (k in 0..6) {
            val day = first + w * 7 + k; if (day > d.today) continue
            val cx = x0 + cell * (k + .5f); val cy = 330f + w * cell
            val m = moods[day]
            if (m != null) c.pic("ic_mood_$m", cx, cy + 8f, cell - 16f, top = true)
            else c.drawCircle(cx, cy + cell / 2, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC9B48A.toInt() })
        }
        val counted = moods.filterKeys { it >= first }.values
        if (counted.isNotEmpty()) {
            val top = counted.groupingBy { it }.eachCount().maxBy { it.value }.key
            c.text(tr("Чаще всего: ", "Most often: ") + Moods.name(top).lowercase(), W / 2f, 890f, 44f, moss, bold, Paint.Align.CENTER)
        }
        val by = moods.filterKeys { it >= first }.values.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3)
        val mx = by.firstOrNull()?.value ?: 1
        by.forEachIndexed { i, (m, n) ->
            val y = 920f + i * 66f
            c.pic("ic_mood_$m", 160f, y + 56f, 56f)
            c.text(Moods.name(m), 210f, y + 40f, 32f, ink, bold)
            c.drawRoundRect(RectF(460f, y + 14f, 460f + 440f * n / mx, y + 44f), 8f, 8f, Paint().apply { color = if (Moods.tone(m) > 0) moss else if (Moods.tone(m) < 0) 0xFF3B4766.toInt() else mute })
            c.text("$n", 920f, y + 40f, 30f, mute, pixel)
        }
        d.notes.firstOrNull { it.text.isNotBlank() }?.let { n -> c.block("«${n.text.take(80).let { t -> if (n.text.length > 80) "$t…" else t }}»", 120f, 1130f, W - 240, 36f, ink, hand, center = true) }
    }

    private fun lodge(c: Canvas) {
        c.text(tr("МОЯ СТОРОЖКА", "MY LODGE"), W / 2f, 140f, 52f, ink, pixel, Paint.Align.CENTER)
        val r = RectF(70f, 190f, W - 70f, 190f + (W - 140f) * 2f / 3f)
        c.scene("house_bg", r)
        // the tamer ones in their rooms, as in the app
        val inside = d.guests.filter { it.stage >= 4 }
        fun place(list: List<app.wildling.ui.art.Guest>, x0: Float, x1: Float, y: Float, hh: Float) = list.forEachIndexed { i, g ->
            val fx = if (list.size == 1) (x0 + x1) / 2 else x0 + (x1 - x0) * i / (list.size - 1)
            c.pic("pet_${g.kind}_${if (g.stage == 5) 5 else 2}", r.left + r.width() * fx, r.top + r.height() * y, r.height() * hh)
        }
        place(inside.filter { it.stage == 4 && it.bad }, .56f, .93f, .905f, .22f)
        place(inside.filter { it.stage == 4 && !it.bad }, .08f, .37f, .905f, .2f)
        place(inside.filter { it.stage == 5 }, .27f, .46f, .462f, .17f)
        place(inside.filter { it.stage == 6 }, .6f, .76f, .462f, .17f)
        val out = d.guests.filter { it.stage < 4 }
        var y = r.bottom + 70f
        c.text(tr("дома ${inside.size}  ·  во дворе ${out.count { it.stage >= 2 }}  ·  в лесу ${out.count { it.stage <= 1 }}", "home ${inside.size}  ·  yard ${out.count { it.stage >= 2 }}  ·  forest ${out.count { it.stage <= 1 }}"),
            W / 2f, y, 38f, ink, bold, Paint.Align.CENTER); y += 40f
        if (out.isNotEmpty()) {
            c.drawRoundRect(RectF(70f, y, W - 70f, y + 290f), 16f, 16f, Paint().apply { color = 0xFFDCE8C8.toInt() })
            c.text(tr("Ещё во дворе", "Still in the yard"), W / 2f, y + 52f, 32f, mute, body, Paint.Align.CENTER)
            val step = (W - 180f) / out.size.coerceAtMost(5)
            out.take(5).forEachIndexed { i, g -> c.pic("pet_${g.kind}_${frameOf(g.stage)}", 90f + step * (i + .5f), y + 280f, 200f, alpha = if (g.stage <= 1) 150 else 255) }
        } else c.text(tr("Все звери дома", "Every creature is home"), W / 2f, y + 80f, 48f, moss, bold, Paint.Align.CENTER)
    }
}

/** Saves the card and opens the system share sheet. */
fun share(ctx: Context, b: Bitmap) {
    val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
    val f = File(dir, "wildling.png"); f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), tr("Поделиться открыткой", "Share the card")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** The card shelf: swipe between the cards, hide the sums if wanted, share. */
@Composable
fun ShareSheet(start: String, d: Data, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val cards = remember(d) { cardsFor(d) }
    val first = cards.indexOfFirst { it.key == start }.coerceAtLeast(0)
    val pager = rememberPagerState(first) { cards.size }
    var hide by remember { mutableStateOf(false) }
    Sheet(onDone) {
        Text(tr("Открытка", "A postcard"), style = T.h2)
        Text(tr("Листай – выбери, чем поделиться.", "Swipe to pick what to share."), style = T.small)
        HorizontalPager(pager, Modifier.fillMaxWidth().padding(top = 10.dp), pageSpacing = 12.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 40.dp)) { i ->
            val img by produceState<Bitmap?>(null, cards[i], hide) { value = withContext(Dispatchers.Default) { Painter(ctx, d, hide).render(cards[i]) } }
            Box(Modifier.fillMaxWidth().aspectRatio(1080f / 1350f).clip(RoundedCornerShape(8.dp)).background(C.card2), contentAlignment = Alignment.Center) {
                img?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth()) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
            cards.indices.forEach { i -> Box(Modifier.padding(3.dp).size(if (i == pager.currentPage) 9.dp else 6.dp).clip(CircleShape).background(if (i == pager.currentPage) C.acc else C.line)) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip(tr("Скрыть суммы", "Hide sums"), hide) { hide = !hide }
            Spacer(Modifier.weight(1f))
            Btn(tr("Поделиться", "Share")) { share(ctx, Painter(ctx, d, hide).render(cards[pager.currentPage])) }
        }
        Spacer(Modifier.height(8.dp))
    }
}
