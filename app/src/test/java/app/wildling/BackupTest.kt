package app.wildling

import androidx.test.core.app.ApplicationProvider
import app.wildling.data.BadBackup
import app.wildling.data.Backup
import app.wildling.data.DemoSeed
import app.wildling.data.Habit
import app.wildling.data.Note
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A copy saved to a file and read back is the same life; a wrong file changes nothing. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupTest {
    private val c get() = ApplicationProvider.getApplicationContext<WildlingApp>().container

    @Test fun roundTrip() = runBlocking {
        DemoSeed.seedIfEmpty(c)
        val before = c.backup()
        val text = before.json().toString()
        assertTrue(before.habits.size == 5 && before.marks.isNotEmpty() && before.urges.isNotEmpty() && before.notes.isNotEmpty())

        // life goes on after the copy: all of it gives way to the file
        c.saveHabit(Habit(kind = "rambler", bad = false, title = "x", name = "y", since = c.clock()))
        c.saveNote(Note(day = c.clock(), at = 1, mood = 0, text = "later"))
        c.settings.update { it.copy(letter = "later", items = emptySet(), spent = 0) }

        c.restore(Backup.of(text))
        val after = c.backup()
        assertEquals(before.habits, after.habits)
        assertEquals(before.marks.map { it.copy(id = 0) }.toSet(), after.marks.map { it.copy(id = 0) }.toSet())
        assertEquals(before.urges.map { it.copy(id = 0) }.toSet(), after.urges.map { it.copy(id = 0) }.toSet())
        assertEquals(before.notes.map { it.copy(id = 0) }.toSet(), after.notes.map { it.copy(id = 0) }.toSet())
        assertEquals(before.marks.size, after.marks.size)
        assertEquals(before.settings, after.settings)
    }

    @Test fun wrongFiles() {
        assertFalse(assertThrows(BadBackup::class.java) { Backup.of("not json") }.newer)
        assertFalse(assertThrows(BadBackup::class.java) { Backup.of("""{"habits":[]}""") }.newer)
        assertFalse(assertThrows(BadBackup::class.java) { Backup.of("""{"format":"wildling","version":1,"habits":[{"id":1}]}""") }.newer)
        assertTrue(assertThrows(BadBackup::class.java) { Backup.of("""{"format":"wildling","version":${Backup.VERSION + 1}}""") }.newer)
    }

    @Test fun strayRowsAndBedsAreDropped() {
        val b = Backup.of("""{"format":"wildling","version":1,
            "habits":[{"id":3,"kind":"smudge","bad":true,"name":"a","since":100,"perWeek":0}],
            "marks":[{"habitId":3,"day":101,"kind":1},{"habitId":9,"day":101,"kind":1}],
            "settings":{"beds":["nothing:1","junk",""]}}""")
        assertEquals(1, b.marks.size)
        assertEquals(1, b.habits[0].perWeek)
        assertEquals(listOf("", "", ""), b.settings.beds)
        assertTrue(b.settings.onboarded)
    }
}
