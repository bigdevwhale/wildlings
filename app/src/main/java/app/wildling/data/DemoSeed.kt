package app.wildling.data

import app.wildling.AppContainer
import app.wildling.domain.Engine
import app.wildling.domain.Lang
import app.wildling.domain.Money
import app.wildling.domain.Shop
import app.wildling.domain.tr
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Two months of a life with three beasts and two shy creatures, for screenshots and the demo build:
 * smoking given up seven weeks ago with one slip, sweets and the late-night feed, walks and early nights.
 */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.dao.habitList().isNotEmpty()) return
        val t = c.clock()
        val r = Random(7)
        fun at(day: Long, h: Int, m: Int = 0) = LocalDate.ofEpochDay(day).atTime(h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val smoke = c.dao.insert(Habit(kind = "smudge", bad = true, title = tr("Сигареты", "Cigarettes"), name = tr("Уголёк", "Cinder"), since = t - 52, cost = if (Lang.ru) 30000 else 1000, minutes = 45,
            why = tr("Хочу подниматься на пятый этаж и не задыхаться. И чтобы дочка не чувствовала запах.", "I want to climb five floors without gasping. And my daughter not to smell it."), order = 0))
        val sweet = c.dao.insert(Habit(kind = "toffee", bad = true, title = tr("Сладкое после ужина", "Sweets after dinner"), name = tr("Ириска", "Fudge"), since = t - 24, cost = if (Lang.ru) 15000 else 500,
            why = tr("Ровная энергия днём и без тяжести вечером.", "Steady energy in the day, no heaviness at night."), order = 1))
        val feed = c.dao.insert(Habit(kind = "flicker", bad = true, title = tr("Лента перед сном", "Scrolling in bed"), name = tr("Мерцун", "Flicker"), since = t - 11, minutes = 70,
            why = tr("Засыпать с книгой, а не с телефоном.", "Fall asleep with a book, not a phone."), order = 2))
        val walk = c.dao.insert(Habit(kind = "rambler", bad = false, title = tr("Прогулка 30 минут", "A 30-minute walk"), name = tr("Мшинка", "Mossy"), since = t - 60, perWeek = 5, order = 3))
        val sleep = c.dao.insert(Habit(kind = "drowse", bad = false, title = tr("Лечь до полуночи", "In bed by midnight"), name = tr("Облачко", "Cloudy"), since = t - 30, perWeek = 6, order = 4))

        c.dao.insert(Mark(habitId = smoke, day = t - 31, kind = Mark.SLIP, at = at(t - 31, 22)))
        c.dao.insert(Mark(habitId = sweet, day = t - 9, kind = Mark.SLIP, at = at(t - 9, 21)))
        for (d in t - 60..t - 1) if (r.nextFloat() < .78f) c.dao.insert(Mark(habitId = walk, day = d, kind = Mark.DONE, at = at(d, 19)))
        for (d in t - 30..t - 1) if (r.nextFloat() < .7f) c.dao.insert(Mark(habitId = sleep, day = d, kind = Mark.DONE, at = at(d, 23, 30)))
        c.dao.insert(Mark(habitId = walk, day = t, kind = Mark.DONE, at = at(t, 9)))

        val trig = listOf("stress", "coffee", "company", "meal", "bored", "evening", "tired", "stress", "drink", "phone")
        val helped = if (Lang.ru) listOf("Вода и балкон", "Прогулка вокруг дома", "Звонок сестре", "Жевательная резинка", "Холодная вода в лицо", "Письмо себе", "Десять приседаний")
            else listOf("Water and the balcony", "A walk round the block", "Called my sister", "Chewing gum", "Cold water on my face", "Re-read my letter", "Ten squats")
        val urgeDays = listOf(51L, 50, 49, 49, 47, 45, 42, 38, 33, 31, 27, 22, 18, 14, 10, 6, 3, 1)
        urgeDays.forEachIndexed { i, back ->
            val day = t - back
            val habit = if (back < 24 && i % 3 == 0) sweet else if (back < 11 && i % 2 == 0) feed else smoke
            val held = back != 31L && back != 9L
            val hour = when (i % 5) { 0 -> 13; 1 -> 18; 2 -> 21; 3 -> 9; else -> if (i == 4) 23 else 16 }
            c.dao.insert(Urge(habitId = habit, at = at(day, hour, 20), day = day, held = held, strength = 2 + i % 4, secs = 180 + i * 7,
                trigger = trig[i % trig.size], helped = if (held && i % 2 == 0) helped[i % helped.size] else ""))
        }

        val notes = if (Lang.ru) listOf(
            52L to (4 to "Первый день. Выбросил пачку утром, к обеду уже жалел. Держусь."),
            51L to (5 to "Всё раздражает. Коллеги, кофе, погода."),
            50L to (7 to "Третий день – самый тяжёлый, как и обещала карта. Вечером отпустило."),
            48L to (2 to "Утром почувствовал запах кофе по-настоящему."),
            45L to (1 to "Неделя! Купил себе хорошего чаю на сэкономленное."),
            40L to (3 to "Обычный день, почти не вспоминал."),
            31L to (6 to "Сорвался на дне рождения у друга. Обидно, но Уголёк никуда не ушёл."),
            30L to (0 to "Начинаю снова. Это не ноль."),
            24L to (2 to "Добавил вторую привычку – сладкое после ужина."),
            20L to (4 to "Тянуло весь вечер. Пересидел у окна, помогла вода."),
            14L to (1 to "Месяц без сигарет, если не считать тот вечер. Лестница даётся легче."),
            9L to (6 to "Съел торт целиком. Ну и ладно – завтра новый день."),
            7L to (0 to "Гулял полтора часа, Мшинка, кажется, уже совсем ручная."),
            4L to (2 to "Лёг в 23:40 и без телефона. Проснулся раньше будильника."),
            2L to (1 to "Уголёк на крыльце. Никогда не думал, что буду радоваться пиксельному дракону."),
        ) else listOf(
            52L to (4 to "Day one. Threw the pack away in the morning, regretted it by lunch. Holding on."),
            51L to (5 to "Everything annoys me. Colleagues, coffee, the weather."),
            50L to (7 to "Day three is the hardest, just as the map said. It eased in the evening."),
            48L to (2 to "In the morning I really smelled the coffee."),
            45L to (1 to "A week! Bought myself good tea with what I saved."),
            40L to (3 to "An ordinary day, hardly thought of it."),
            31L to (6 to "Slipped at a friend's birthday. A shame, but Cinder didn't run off."),
            30L to (0 to "Starting again. It isn't zero."),
            24L to (2 to "Added a second habit – sweets after dinner."),
            20L to (4 to "It pulled all evening. Sat it out by the window, water helped."),
            14L to (1 to "A month without cigarettes, that evening aside. Stairs feel easier."),
            9L to (6 to "Ate a whole cake. Oh well – tomorrow's a new day."),
            7L to (0 to "Walked an hour and a half; Mossy seems quite tame now."),
            4L to (2 to "In bed at 23:40, no phone. Woke before the alarm."),
            2L to (1 to "Cinder is on the porch. Never thought I'd be happy about a pixel dragon."),
        )
        notes.forEach { (back, p) -> c.dao.insert(Note(day = t - back, at = at(t - back, 22), mood = p.first, text = p.second)) }

        val habits = c.dao.habitList(); val marks = c.dao.markList(); val urges = c.dao.urgeList()
        val items = setOf("rocker", "lamp", "wood", "flags", "geranium", "bench", "birdhouse", "kettle")
        val beds = listOf("sunflower:${t - 9}", "pumpkin:${t - 4}", "")
        val spent = items.sumOf { Shop.thingById[it]!!.price } + 10 + 15
        val stages = habits.associate { it.id to Engine.trust(it, marks, urges, t).stage }
        val finds = Engine.finds(habits, marks, urges, t).keys
        c.settings.update { it.copy(onboarded = true, currency = if (Lang.ru) "RUB" else "USD", items = items, beds = beds, spent = spent, stages = stages, finds = finds,
            miles = habits.flatMap { h -> (1..400).map { n -> "${h.id}:${h.since}:$n" } }.toSet(),
            letter = tr("Привет. Если ты это читаешь, значит, сейчас тянет. Вспомни лестницу на пятый этаж в первый день и как сейчас. Это пройдёт через пять минут, а гордость останется на весь вечер.",
                "Hi. If you're reading this, it's pulling right now. Remember the five flights of stairs on day one, and now. This passes in five minutes; the pride lasts all evening.")) }
        Money.set(c.settings.now().currency)
    }
}
