package app.wildling.data

import android.content.Context
import android.net.Uri
import app.wildling.domain.Shop
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/** The file is not a copy of Wildling, or ([newer]) a later version of the app made it. */
class BadBackup(val newer: Boolean = false) : Exception()

/**
 * Everything the app keeps, as one JSON file: for moving to another phone or for keeping just in case.
 * Habits keep their ids, since the settings point at them; days, cravings and notes get new ones when read back.
 */
class Backup(val habits: List<Habit>, val marks: List<Mark>, val urges: List<Urge>, val notes: List<Note>, val settings: Settings,
             val made: Long = System.currentTimeMillis()) {

    fun json(): JSONObject = JSONObject().apply {
        put("format", FORMAT); put("version", VERSION); put("made", made)
        put("habits", JSONArray(habits.map { h -> JSONObject().apply {
            put("id", h.id); put("kind", h.kind); put("bad", h.bad); put("title", h.title); put("name", h.name); put("since", h.since)
            put("cost", h.cost); put("minutes", h.minutes); put("perWeek", h.perWeek); put("why", h.why); put("order", h.order)
            put("archived", h.archived); put("shiny", h.shiny); put("goal", h.goal); put("unit", h.unit); put("weekly", h.weekly) } }))
        put("marks", JSONArray(marks.map { m -> JSONObject().apply {
            put("habitId", m.habitId); put("day", m.day); put("kind", m.kind); put("at", m.at); put("amount", m.amount) } }))
        put("urges", JSONArray(urges.map { u -> JSONObject().apply {
            put("habitId", u.habitId); put("at", u.at); put("day", u.day); put("held", u.held); put("strength", u.strength)
            put("secs", u.secs); put("trigger", u.trigger); put("helped", u.helped) } }))
        put("notes", JSONArray(notes.map { n -> JSONObject().apply { put("day", n.day); put("at", n.at); put("mood", n.mood); put("text", n.text) } }))
        put("settings", settings.json())
    }

    companion object {
        const val FORMAT = "wildling"
        const val VERSION = 1
        /** No copy is anywhere near this big; a file that is was picked by mistake. */
        private const val MAX = 32 * 1024 * 1024

        /** Reads what [json] wrote. Nothing is trusted: what can't be read right is a [BadBackup], not a broken app later. */
        fun of(text: String): Backup {
            try {
                val j = JSONObject(text)
                if (j.optString("format") != FORMAT) throw BadBackup()
                if (j.optInt("version", 1) > VERSION) throw BadBackup(newer = true)
                fun rows(k: String) = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } } ?: emptyList()
                val habits = rows("habits").map { o ->
                    Habit(id = o.getLong("id"), kind = o.getString("kind"), bad = o.getBoolean("bad"), title = o.optString("title"), name = o.getString("name"),
                        since = o.getLong("since"), cost = o.optLong("cost").coerceAtLeast(0), minutes = o.optInt("minutes").coerceAtLeast(0),
                        perWeek = o.optInt("perWeek", 7).coerceIn(1, 7), why = o.optString("why"), order = o.optInt("order"), archived = o.optBoolean("archived"),
                        shiny = o.optBoolean("shiny"), goal = o.optInt("goal").coerceAtLeast(0), unit = o.optString("unit"), weekly = o.optBoolean("weekly"))
                }
                val ids = habits.map { it.id }.toSet()
                if (ids.size != habits.size || ids.any { it <= 0 }) throw BadBackup()
                val marks = rows("marks").map { o -> Mark(habitId = o.getLong("habitId"), day = o.getLong("day"), kind = o.getInt("kind"), at = o.optLong("at"), amount = o.optInt("amount")) }
                    .filter { it.habitId in ids && it.kind in Mark.SLIP..Mark.LOG }
                val urges = rows("urges").map { o ->
                    Urge(habitId = o.getLong("habitId"), at = o.getLong("at"), day = o.getLong("day"), held = o.getBoolean("held"), strength = o.optInt("strength", 3).coerceIn(1, 5),
                        secs = o.optInt("secs").coerceAtLeast(0), trigger = o.optString("trigger"), helped = o.optString("helped"))
                }.filter { it.habitId in ids }
                val notes = rows("notes").map { o -> Note(day = o.getLong("day"), at = o.getLong("at"), mood = o.optInt("mood"), text = o.optString("text")) }
                val s = Settings.of(j.optJSONObject("settings") ?: JSONObject())
                val beds = s.beds.map { b -> val p = b.split(':'); if (p.size == 2 && p[0] in Shop.seedById && p[1].toLongOrNull() != null) b else "" }
                // a copy with habits in it was made past the first steps, whatever it says
                return Backup(habits, marks, urges, notes, s.copy(beds = beds, onboarded = s.onboarded || habits.isNotEmpty()), j.optLong("made"))
            } catch (e: org.json.JSONException) {
                throw BadBackup()
            } catch (e: RuntimeException) {   // a number that is not one
                throw BadBackup()
            }
        }

        fun write(ctx: Context, uri: Uri, b: Backup) =
            (ctx.contentResolver.openOutputStream(uri) ?: throw java.io.IOException()).use { write(it, b) }

        /** The file itself, wherever it goes: the settings' «Save a copy» or Lingvale's copy of the whole family. */
        fun write(out: OutputStream, b: Backup) = out.write(b.json().toString().toByteArray())

        fun read(ctx: Context, uri: Uri): Backup =
            (ctx.contentResolver.openInputStream(uri) ?: throw java.io.IOException()).use { read(it) }

        fun read(input: InputStream): Backup {
            val out = ByteArrayOutputStream(); val chunk = ByteArray(64 * 1024)
            while (true) { val n = input.read(chunk); if (n < 0) break; out.write(chunk, 0, n); if (out.size() > MAX) throw BadBackup() }
            return of(out.toByteArray().decodeToString())
        }
    }
}
