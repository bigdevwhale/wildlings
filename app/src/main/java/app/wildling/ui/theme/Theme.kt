package app.wildling.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.wildling.R

/** Colour tokens: lichen paper, moss for action, lantern amber for pinecones, dusk blue for the night window. */
class Pal(
    val dark: Boolean, val bg: Color, val card: Color, val card2: Color, val ink: Color, val mute: Color, val line: Color, val track: Color,
    val acc: Color, val accInk: Color, val accSoft: Color, val ok: Color, val okSoft: Color, val gold: Color, val goldSoft: Color, val bad: Color,
    val heat: List<Color>, val nav: Color, val shadow: Color, val snow: Color, val dusk: Color, val duskInk: Color, val frame: Color, val wood: Color, val wood2: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

val Light = Pal(false, c(0xEDE5CF), c(0xFBF5E4), c(0xF1E8D0), c(0x2E2618), c(0x857A62), c(0xDDD0B2), c(0xE4D9BE),
    c(0x4F7A45), c(0xFFFFFF), c(0xDCE8D2), c(0x4F8A5B), c(0xDDEDDC), c(0xC98A2E), c(0xF6E7C8), c(0xC4533A),
    listOf(c(0xE2E2D0), c(0xC9DDB6), c(0x9CC27F), c(0x6E9F55), c(0x4A7A3A)), c(0xF8F7EE), Color(0x22404A2A), c(0x7FA7C9), c(0x3B4766), c(0xFFFFFF), c(0xC9B48A), c(0x8A5A34), c(0x6B4426))

val Dark = Pal(true, c(0x17140F), c(0x241F18), c(0x2E271E), c(0xEEE6D2), c(0xA39882), c(0x3A3225), c(0x342C21),
    c(0x86B474), c(0x10180D), c(0x2C3D25), c(0x76B583), c(0x22352A), c(0xE5AE4E), c(0x3D311A), c(0xEE7558),
    listOf(c(0x2D3427), c(0x3B5230), c(0x55783F), c(0x77A253), c(0xA3CF74)), c(0x1A1E17), Color(0x55000000), c(0x9CC3E3), c(0x6A7DBA), c(0xFFFFFF), c(0x4C412F), c(0x5A3B22), c(0x3E2816))

/** The palette in use; switched by the theme setting before the tree is drawn. */
object C {
    @Volatile var p: Pal = Light
    val dark get() = p.dark
    val bg get() = p.bg; val card get() = p.card; val card2 get() = p.card2; val ink get() = p.ink; val mute get() = p.mute
    val line get() = p.line; val track get() = p.track; val acc get() = p.acc; val accInk get() = p.accInk; val accSoft get() = p.accSoft
    val ok get() = p.ok; val okSoft get() = p.okSoft; val gold get() = p.gold; val goldSoft get() = p.goldSoft; val bad get() = p.bad
    val heat get() = p.heat; val nav get() = p.nav; val shadow get() = p.shadow; val snow get() = p.snow; val dusk get() = p.dusk; val duskInk get() = p.duskInk; val frame get() = p.frame; val wood get() = p.wood; val wood2 get() = p.wood2
}

val Nunito = FontFamily(Font(R.font.nunito_regular, FontWeight.Normal), Font(R.font.nunito_semibold, FontWeight.SemiBold), Font(R.font.nunito_bold, FontWeight.Bold), Font(R.font.nunito_bold, FontWeight.ExtraBold))
/** Pixel letters for titles, labels and numbers: the game side of the app. */
val Pixel = FontFamily(Font(R.font.press_start, FontWeight.Normal), Font(R.font.press_start, FontWeight.Bold))
val Inter = FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_semibold, FontWeight.Bold))

object T {
    val title get() = TextStyle(fontFamily = Pixel, fontSize = 19.sp, lineHeight = 28.sp, color = C.ink)
    val h2 get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, lineHeight = 24.sp, color = C.ink)
    val h3 get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, color = C.ink)
    val body get() = TextStyle(fontFamily = Nunito, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val bodyB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val small get() = TextStyle(fontFamily = Nunito, fontSize = 13.sp, lineHeight = 17.sp, color = C.mute)
    val smallB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp, color = C.ink)
    val tiny get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, color = C.mute)
    val sec get() = TextStyle(fontFamily = Pixel, fontSize = 10.sp, lineHeight = 14.sp, color = C.mute)
    val num get() = TextStyle(fontFamily = Pixel, fontSize = 17.sp, lineHeight = 24.sp, color = C.ink)
    val numS get() = TextStyle(fontFamily = Pixel, fontSize = 12.sp, lineHeight = 16.sp, color = C.ink)
    val big get() = TextStyle(fontFamily = Pixel, fontSize = 36.sp, lineHeight = 44.sp, color = C.ink)
    val btn get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
}

@Composable
fun WildTheme(dark: Boolean, content: @Composable () -> Unit) {
    C.p = if (dark) Dark else Light
    val p = C.p
    val scheme = if (dark) darkColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    else lightColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    MaterialTheme(colorScheme = scheme, content = content)
}
