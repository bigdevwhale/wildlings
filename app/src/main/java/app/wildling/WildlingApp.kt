package app.wildling

import android.app.Application
import android.content.Context
import androidx.room.withTransaction
import app.wildling.data.Backup
import app.wildling.data.Db
import app.wildling.data.Habit
import app.wildling.data.Mark
import app.wildling.data.Note
import app.wildling.data.SettingsStore
import app.wildling.data.Urge
import app.wildling.domain.Engine
import app.wildling.domain.Kinds
import app.wildling.domain.Lang
import app.wildling.domain.Money
import app.wildling.domain.Shop
import app.wildling.domain.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = Db.create(app)
    val dao = db.dao()
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val news = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private val lock = Mutex()
    /** The day the forest lives by; the demo moves it while seeding. */
    @Volatile var clock: () -> Long = { today() }

    fun write(block: suspend AppContainer.() -> Unit) { scope.launch { block() } }
    fun say(s: String) { news.tryEmit(s) }

    /** A new creature's name, picked from its list. */
    fun nameFor(kind: String) = Kinds.of(kind).names.random()

    // ---------- habits ----------

    suspend fun saveHabit(h: Habit): Long = lock.withLock {
        if (h.id == 0L) {
            val order = (dao.habitList().maxOfOrNull { it.order } ?: -1) + 1
            dao.insert(h.copy(order = order, shiny = Random.nextInt(25) == 0))
        } else {
            dao.update(h)
            // the goal may have changed: the days with amounts are ticked again by the new one
            if (h.counts) dao.markList().filter { it.habitId == h.id && it.kind == Mark.LOG }.forEach { syncDone(h, it.day, it.amount) }
            h.id
        }
    }

    suspend fun begin(h: Habit, currency: String) {
        saveHabit(h)
        Money.set(currency)
        settings.update { it.copy(onboarded = true, currency = currency) }
    }

    suspend fun archive(h: Habit, on: Boolean) = lock.withLock { dao.update(h.copy(archived = on)) }
    suspend fun deleteHabit(id: Long) = lock.withLock { dao.deleteMarksOf(id); dao.deleteUrgesOf(id); dao.deleteHabit(id) }
    /** A holiday of [days] days from [from] for a good habit (#3): those weeks cost nothing and keep the run. */
    suspend fun rest(h: Habit, from: Long, days: Int) = lock.withLock {
        val have = dao.markList().filter { it.habitId == h.id && it.kind == Mark.REST }.map { it.day }.toSet()
        dao.insertMarks((from until from + days).filter { it !in have }.map { Mark(habitId = h.id, day = it, kind = Mark.REST) })
    }
    /** Back from a holiday: the days off after [day] are taken back. */
    suspend fun endRest(h: Habit, day: Long) = lock.withLock { dao.endRest(h.id, day) }

    suspend fun reorder(ids: List<Long>) = lock.withLock { val all = dao.habitList().associateBy { it.id }; ids.forEachIndexed { i, id -> all[id]?.let { dao.update(it.copy(order = i)) } } }

    // ---------- days ----------

    /** A bad habit: it happened on [day]. Fed the beast – one step back, nothing more. */
    suspend fun slip(h: Habit, day: Long) = lock.withLock {
        if (dao.markList().none { it.habitId == h.id && it.day == day && it.kind == Mark.SLIP }) dao.insert(Mark(habitId = h.id, day = day, kind = Mark.SLIP))
    }
    suspend fun unslip(h: Habit, day: Long) = lock.withLock { dao.unmark(h.id, day, Mark.SLIP) }

    /** A good habit done on [day] (again: undone). */
    suspend fun toggleDone(h: Habit, day: Long): Boolean = lock.withLock {
        val had = dao.markList().any { it.habitId == h.id && it.day == day && it.kind == Mark.DONE }
        if (had) dao.unmark(h.id, day, Mark.DONE) else dao.insert(Mark(habitId = h.id, day = day, kind = Mark.DONE))
        !had
    }

    /**
     * A counted habit: how much was done on [day] in all (0 clears it). The day is ticked done by itself:
     * once the day's goal is reached, or, for a weekly amount, once anything was done.
     */
    suspend fun log(h: Habit, day: Long, total: Int): Boolean = lock.withLock {
        dao.unmark(h.id, day, Mark.LOG)
        if (total > 0) dao.insert(Mark(habitId = h.id, day = day, kind = Mark.LOG, amount = total))
        syncDone(h, day, total)
    }

    /** Ticks [day] of a counted habit done or not by its amount; true if it is done. */
    private suspend fun syncDone(h: Habit, day: Long, total: Int): Boolean {
        val should = if (h.weekly) total > 0 else total >= h.goal
        val had = dao.markList().any { it.habitId == h.id && it.day == day && it.kind == Mark.DONE }
        if (had && !should) dao.unmark(h.id, day, Mark.DONE)
        if (!had && should) dao.insert(Mark(habitId = h.id, day = day, kind = Mark.DONE))
        return should
    }

    /** A craving sat through by the window. Not held means the beast was fed today. */
    suspend fun urge(u: Urge) {
        lock.withLock { dao.insert(u) }
        if (!u.held) dao.habitList().firstOrNull { it.id == u.habitId }?.takeIf { it.bad }?.let { slip(it, u.day) }
    }

    // ---------- notes ----------

    suspend fun saveNote(n: Note) = lock.withLock { if (n.id == 0L) dao.insert(n) else dao.update(n) }
    suspend fun deleteNote(id: Long) = lock.withLock { dao.deleteNote(id) }

    // ---------- a copy in a file ----------

    suspend fun backup(): Backup = lock.withLock { Backup(dao.habitList(), dao.markList(), dao.urgeList(), dao.noteList(), settings.now()) }

    /** Everything here gives way to what the file holds: all of it, or, if something fails, none of it. */
    suspend fun restore(b: Backup) = lock.withLock {
        db.withTransaction {
            dao.clearMarks(); dao.clearUrges(); dao.clearNotes(); dao.clearHabits()
            dao.insertHabits(b.habits); dao.insertMarks(b.marks); dao.insertUrges(b.urges); dao.insertNotes(b.notes)
        }
        settings.update { b.settings }
        Money.set(b.settings.currency)
    }

    // ---------- the garden and the lodge ----------

    private suspend fun balance(): Int {
        val t = clock()
        return Engine.earned(dao.habitList(), dao.markList(), dao.urgeList(), dao.noteList(), t) - settings.now().spent
    }

    suspend fun buy(id: String): Boolean = lock.withLock {
        val th = Shop.thingById[id] ?: return@withLock false
        if (id in settings.now().items || balance() < th.price) return@withLock false
        settings.update { it.copy(spent = it.spent + th.price, items = it.items + id) }
        true
    }

    suspend fun sow(bed: Int, seed: String): Boolean = lock.withLock {
        val sd = Shop.seedById[seed] ?: return@withLock false
        if (settings.now().beds[bed].isNotEmpty() || balance() < sd.price) return@withLock false
        settings.update { it.copy(spent = it.spent + sd.price, beds = it.beds.toMutableList().also { b -> b[bed] = "$seed:${clock()}" }) }
        true
    }

    /** A grown plant gathered: its price back and a little more, and the bed is free again. */
    suspend fun gather(bed: Int): Int = lock.withLock {
        val s = settings.now(); val (id, day) = s.beds[bed].split(':').let { it[0] to it[1].toLong() }
        val sd = Shop.seedById[id] ?: return@withLock 0
        if (Shop.growth(sd, clock() - day) < 2) return@withLock 0
        val gain = sd.price + sd.price / 2 + 2
        settings.update { it.copy(spent = it.spent - gain, harvest = it.harvest + 1, beds = it.beds.toMutableList().also { b -> b[bed] = "" }) }
        gain
    }

    suspend fun told(stages: Map<Long, Int>? = null, find: String? = null, mile: String? = null) = settings.update {
        it.copy(stages = if (stages != null) it.stages + stages else it.stages, finds = if (find != null) it.finds + find else it.finds,
            miles = if (mile != null) it.miles + mile else it.miles)
    }
}

class WildlingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Lang.ru = resources.configuration.locales[0].language == "ru"
        container = AppContainer(this)
        container.scope.launch { Money.set(container.settings.now().currency) }
    }
}

val Context.container: AppContainer get() = (applicationContext as WildlingApp).container
