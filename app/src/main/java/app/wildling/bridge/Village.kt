package app.wildling.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import app.wildling.data.BadBackup
import app.wildling.data.Backup
import app.wildling.BuildConfig
import app.wildling.WildlingApp
import app.wildling.AppContainer
import app.wildling.domain.Lang
import app.wildling.data.Mark
import app.wildling.domain.Engine
import app.wildling.domain.Kinds
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The bridge to Lingvale, the village where the pets of all the -ling apps live together.
 *
 * Lingvale asks through ContentResolver.call() and nobody else gets an answer: the caller must be Lingvale signed
 * with its release key (a debug or demo build also answers a Lingvale built with the same debug key).
 * Protocol v2, the same in every -ling app:
 *  - "hello"    → {v}
 *  - "snapshot" → {json}: pets, species, days of activity, the last week's events
 *  - "art"      → {bytes}: one picture from res/drawable-nodpi (pet_*, item_*, prop_*, *_bg)
 *  - "backup"   (extras {fd}: a file open for writing) → {ok, name}: writes the same backup file the settings save
 *  - "restore"  (extras {fd}: a file open for reading) → {ok, error}: loads such a file, as the settings' «Restore» does,
 *    replacing what is on the phone; error is "newer" (a file from a newer version), "busy" or "bad"
 * Only "restore" changes anything here, and Lingvale asks the player before it calls it.
 */
class VillageProvider : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        if (!trusted(ctx)) throw SecurityException("Only Lingvale may ask")
        return when (method) {
            "hello" -> Bundle().apply { putInt("v", VERSION) }
            "snapshot" -> Bundle().apply { putString("json", runBlocking { snapshot(ctx, container(ctx)) }.toString()) }
            "art" -> art(ctx, arg)
            "backup" -> backup(ctx, extras)
            "restore" -> restore(ctx, extras)
            else -> null
        }
    }

    @Suppress("DEPRECATION")
    private fun fd(extras: Bundle?): ParcelFileDescriptor? = extras?.getParcelable("fd")

    /** Writes the settings' backup file into the file Lingvale opened. */
    private fun backup(ctx: Context, extras: Bundle?): Bundle {
        val fd = fd(extras) ?: return Bundle().apply { putBoolean("ok", false) }
        val ok = runCatching {
            val b = runBlocking { container(ctx).backup() }
            ParcelFileDescriptor.AutoCloseOutputStream(fd).use { Backup.write(it, b) }
        }.onFailure { runCatching { fd.close() } }.isSuccess
        return Bundle().apply { putBoolean("ok", ok); putString("name", "wildling.json") }
    }

    /** Loads a backup file from Lingvale, the way the settings do: all of it, or, if something fails, none of it. */
    private fun restore(ctx: Context, extras: Bundle?): Bundle {
        val fd = fd(extras) ?: return Bundle().apply { putBoolean("ok", false); putString("error", "bad") }
        val error = try {
            val b = ParcelFileDescriptor.AutoCloseInputStream(fd).use { Backup.read(it) }
            runBlocking { container(ctx).restore(b) }
            null
        } catch (e: BadBackup) {
            if (e.newer) "newer" else "bad"
        } catch (e: Exception) {
            runCatching { fd.close() }; "bad"
        }
        return Bundle().apply { putBoolean("ok", error == null); if (error != null) putString("error", error) }
    }

    private fun trusted(ctx: Context): Boolean {
        val pkg = callingPackage ?: return false
        if (pkg != HUB && !pkg.startsWith("$HUB.")) return false
        val pm = ctx.packageManager
        if (HUB_CERTS.any { pm.hasSigningCertificate(pkg, it, PackageManager.CERT_INPUT_SHA256) }) return true
        return BuildConfig.DEBUG && pm.checkSignatures(pkg, ctx.packageName) == PackageManager.SIGNATURE_MATCH
    }

    private fun art(ctx: Context, name: String?): Bundle? {
        if (name == null || !ART.matches(name)) return null
        @Suppress("DiscouragedApi")
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (id == 0) return null
        return Bundle().apply { putByteArray("bytes", ctx.resources.openRawResource(id).use { it.readBytes() }) }
    }

    /** The provider can be asked before the application has finished starting; wait for it a little. */
    private fun container(ctx: Context): AppContainer {
        repeat(100) {
            runCatching { return (ctx.applicationContext as WildlingApp).container }
            Thread.sleep(30)
        }
        error("not ready")
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        const val VERSION = 2
        const val HUB = "app.lingvale"
        /** SHA-256 of Lingvale's release certificate. */
        private val HUB_CERTS = listOf(hex("CEF40A1786896D829CFA53115693AC9A53FC0349EAB84BAF64EFA648592D1ABF"))
        private val ART = Regex("""(pet|item|prop)_[a-z0-9_]+|[a-z]+_bg""")
        private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        private fun color(c: Long) = "#%06X".format(c and 0xFFFFFF)
        private fun dayOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

        /** Everything Lingvale shows of Wildling: the creatures (trust stages as growth), the days, the last week. */
        suspend fun snapshot(ctx: Context, c: AppContainer): JSONObject {
            val habits = c.dao.habitList().filter { !it.archived }
            val marks = c.dao.markList(); val urges = c.dao.urgeList(); val notes = c.dao.noteList()
            val today = LocalDate.now().toEpochDay()
            val petsJson = JSONArray()
            habits.forEachIndexed { i, h ->
                val t = Engine.trust(h, marks, urges, today)
                petsJson.put(JSONObject()
                    .put("id", h.id).put("species", h.kind).put("name", h.name)
                    .put("stage", t.stage).put("progress", t.progress.toDouble())
                    .put("slot", if (i < 3) i else -1).put("shiny", h.shiny).put("found", h.since * 86400_000L).put("hatched", h.since * 86400_000L))
            }
            Engine.finds(habits, marks, urges, today).entries.forEachIndexed { i, (k, d) ->
                petsJson.put(JSONObject().put("id", -1 - i).put("species", k).put("name", Kinds.of(k).title).put("stage", 4).put("progress", 1.0)
                    .put("slot", -1).put("shiny", false).put("found", d * 86400_000L).put("hatched", d * 86400_000L))
            }
            val speciesJson = JSONArray()
            Kinds.all.forEach { k ->
                speciesJson.put(JSONObject().put("id", k.id).put("ru", k.ru).put("en", k.en).put("color", color(k.color))
                    .put("aboutRu", k.habitRu).put("aboutEn", k.habitEn).put("rare", k.find))
            }
            val daysJson = JSONArray()
            val acts = HashMap<Long, Int>()
            marks.filter { it.kind == Mark.DONE }.forEach { acts[it.day] = (acts[it.day] ?: 0) + 1 }
            urges.filter { it.held }.forEach { acts[it.day] = (acts[it.day] ?: 0) + 1 }
            notes.forEach { acts[it.day] = (acts[it.day] ?: 0) + 1 }
            acts.toSortedMap().forEach { (d, n) -> if (d > today - 400) daysJson.put(JSONObject().put("d", d).put("n", n).put("min", 0)) }
            val eventsJson = JSONArray()
            val names = habits.associateBy { it.id }
            marks.filter { it.kind == Mark.DONE && it.day > today - 8 }.forEach { m ->
                val h = names[m.habitId] ?: return@forEach
                eventsJson.put(JSONObject().put("at", m.at).put("min", 0).put("ru", "${h.name}: ${h.title.ifBlank { Kinds.of(h.kind).habitRu }}").put("en", "${h.name}: ${h.title.ifBlank { Kinds.of(h.kind).habitEn }}"))
            }
            urges.filter { it.held && it.day > today - 8 }.forEach { u ->
                val h = names[u.habitId] ?: return@forEach
                eventsJson.put(JSONObject().put("at", u.at).put("min", u.secs / 60).put("ru", "${h.name}: тяга пережита").put("en", "${h.name}: a craving held"))
            }
            return JSONObject()
                .put("v", VERSION).put("app", "wildling").put("version", BuildConfig.VERSION_NAME)
                .put("ru", Lang.ru).put("scene", "house_bg")
                .put("pets", petsJson).put("species", speciesJson).put("days", daysJson).put("events", eventsJson)
        }
    }
}
