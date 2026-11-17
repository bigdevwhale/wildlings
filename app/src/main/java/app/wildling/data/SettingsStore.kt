package app.wildling.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Everything that isn't a habit, a day or a note: the garden, the things bought, what news was shown. */
data class Settings(
    val onboarded: Boolean = false,
    val currency: String = "RUB",
    val spent: Int = 0,                          // pinecones spent
    val items: Set<String> = emptySet(),         // things bought for the lodge and the yard
    val beds: List<String> = listOf("", "", ""), // each bed: "seedId:dayPlanted" or empty
    val harvest: Int = 0,                        // plants grown and gathered
    val theme: String = "system",
    val seen: Set<String> = emptySet(),          // introductions read
    val stages: Map<Long, Int> = emptyMap(),     // the stage last told about, per habit
    val finds: Set<String> = emptySet(),         // finds already told about
    val miles: Set<String> = emptySet(),         // "habitId:day" milestones already told about
    val letter: String = "",                     // a letter for a bad day, from a good one
    val evening: Int = 21,                       // the evening question's hour, −1 off
) {
    fun json(): JSONObject = JSONObject().apply {
        put("onboarded", onboarded); put("currency", currency); put("spent", spent); put("items", JSONArray(items.toList()))
        put("beds", JSONArray(beds)); put("harvest", harvest); put("theme", theme); put("seen", JSONArray(seen.toList()))
        put("stages", JSONObject().apply { stages.forEach { (k, v) -> put(k.toString(), v) } })
        put("finds", JSONArray(finds.toList())); put("miles", JSONArray(miles.toList())); put("letter", letter); put("evening", evening)
    }

    companion object {
        fun of(j: JSONObject): Settings {
            fun list(k: String) = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            val st = j.optJSONObject("stages")?.let { o -> o.keys().asSequence().associate { it.toLong() to o.getInt(it) } } ?: emptyMap()
            return Settings(j.optBoolean("onboarded"), j.optString("currency", "RUB"), j.optInt("spent"), list("items").toSet(),
                list("beds").let { b -> List(3) { b.getOrElse(it) { "" } } }, j.optInt("harvest"), j.optString("theme", "system"), list("seen").toSet(),
                st, list("finds").toSet(), list("miles").toSet(), j.optString("letter"), j.optInt("evening", 21))
        }
    }
}

private val Context.store by preferencesDataStore("wildling")
private val KEY = stringPreferencesKey("settings")

class SettingsStore(private val ctx: Context) {
    val settings: Flow<Settings> = ctx.store.data.map { p -> p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings() }
    suspend fun now(): Settings = settings.first()
    suspend fun update(f: (Settings) -> Settings) { ctx.store.edit { p ->
        val cur = p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings()
        p[KEY] = f(cur).json().toString() } }
}
