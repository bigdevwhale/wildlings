package app.wildling.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * A habit: a bad one (a wild beast that comes for its due) or a good one (a shy creature that comes when it is fed).
 * [kind] is a key of [app.wildling.domain.Kinds]; it fixes the creature. Days are epoch days.
 */
@Entity
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val bad: Boolean,
    val title: String,           // what the user calls the habit («Сигареты»); empty = the kind's own name
    val name: String,            // the creature's name
    val since: Long,             // a bad habit: the day it was given up; a good one: the day it was started
    val cost: Long = 0,          // a bad habit: what one day of it cost, minor units
    val minutes: Int = 0,        // a bad habit: what one day of it took, minutes
    val perWeek: Int = 7,        // a good habit: how many days a week
    val why: String = "",        // why: shown when the beast comes
    val order: Int = 0,
    val archived: Boolean = false,
    val shiny: Boolean = false,
    /** A good habit counted in amounts: the target (0 = just done or not), per day or, if [weekly], per week. */
    @ColumnInfo(defaultValue = "0") val goal: Int = 0,
    @ColumnInfo(defaultValue = "") val unit: String = "",   // what is counted, as said after a number: «стаканов», «слов»
    @ColumnInfo(defaultValue = "0") val weekly: Boolean = false,
) {
    /** Counted in amounts rather than ticked. */
    val counts get() = !bad && goal > 0
}

/** A day of a habit: a slip of a bad one, or a good one done. */
@Entity
data class Mark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val day: Long,
    val kind: Int,               // SLIP, DONE, LOG or REST
    val at: Long = System.currentTimeMillis(),
    /** A LOG: how much of a counted habit was done that day, in all. */
    @ColumnInfo(defaultValue = "0") val amount: Int = 0,
) { companion object { const val SLIP = 1; const val DONE = 2; const val LOG = 3; const val REST = 4 } }

/** «It came»: a craving lived through by the window, held or not. */
@Entity
data class Urge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val at: Long,
    val day: Long,
    val held: Boolean,
    val strength: Int = 3,       // 1..5
    val secs: Int = 0,           // how long the sitting lasted
    val trigger: String = "",    // a key of Triggers or own words
    val helped: String = "",     // one line: what helped
)

/** A note of how the day felt. */
@Entity
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val at: Long,
    val mood: Int,               // index into Moods
    val text: String,
)

@Dao
interface Wild {
    @Query("SELECT * FROM Habit ORDER BY `order`, id") fun habits(): Flow<List<Habit>>
    @Query("SELECT * FROM Habit ORDER BY `order`, id") suspend fun habitList(): List<Habit>
    @Query("SELECT * FROM Mark ORDER BY day") fun marks(): Flow<List<Mark>>
    @Query("SELECT * FROM Mark ORDER BY day") suspend fun markList(): List<Mark>
    @Query("SELECT * FROM Urge ORDER BY at DESC") fun urges(): Flow<List<Urge>>
    @Query("SELECT * FROM Urge ORDER BY at DESC") suspend fun urgeList(): List<Urge>
    @Query("SELECT * FROM Note ORDER BY at DESC") fun notes(): Flow<List<Note>>
    @Query("SELECT * FROM Note ORDER BY at DESC") suspend fun noteList(): List<Note>

    @Insert suspend fun insert(h: Habit): Long
    @Update suspend fun update(h: Habit)
    @Insert suspend fun insert(m: Mark): Long
    @Insert suspend fun insert(u: Urge): Long
    @Insert suspend fun insert(n: Note): Long
    @Update suspend fun update(n: Note)

    @Query("DELETE FROM Habit WHERE id = :id") suspend fun deleteHabit(id: Long)
    @Query("DELETE FROM Mark WHERE habitId = :id") suspend fun deleteMarksOf(id: Long)
    @Query("DELETE FROM Urge WHERE habitId = :id") suspend fun deleteUrgesOf(id: Long)
    @Query("DELETE FROM Mark WHERE habitId = :habit AND day = :day AND kind = :kind") suspend fun unmark(habit: Long, day: Long, kind: Int)
    @Query("DELETE FROM Note WHERE id = :id") suspend fun deleteNote(id: Long)
    /** Ends a holiday: the days off after [day] are taken back (#3). */
    @Query("DELETE FROM Mark WHERE habitId = :habit AND kind = 4 AND day > :day") suspend fun endRest(habit: Long, day: Long)

    // a copy read back from a file: everything out, everything in
    @Query("DELETE FROM Habit") suspend fun clearHabits()
    @Query("DELETE FROM Mark") suspend fun clearMarks()
    @Query("DELETE FROM Urge") suspend fun clearUrges()
    @Query("DELETE FROM Note") suspend fun clearNotes()
    @Insert suspend fun insertHabits(l: List<Habit>)
    @Insert suspend fun insertMarks(l: List<Mark>)
    @Insert suspend fun insertUrges(l: List<Urge>)
    @Insert suspend fun insertNotes(l: List<Note>)
}

@Database(entities = [Habit::class, Mark::class, Urge::class, Note::class], version = 2, exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)])
abstract class Db : RoomDatabase() {
    abstract fun dao(): Wild

    companion object {
        fun create(ctx: Context) = Room.databaseBuilder(ctx, Db::class.java, "wildling.db").build()
    }
}
