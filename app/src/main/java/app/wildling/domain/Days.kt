package app.wildling.domain

/**
 * The map of hard days: what usually happens on the way, so a bad day is expected and not a verdict.
 * General for any habit, with a few lines for the ones whose body side is well known.
 */
object Days {
    class Mile(val day: Int, val titleRu: String, val titleEn: String, val ru: String, val en: String) {
        val title get() = tr(titleRu, titleEn)
        val text get() = tr(ru, en)
    }

    private val general = listOf(
        Mile(1, "Первый день", "Day one", "Решение принято. Сегодня зверь удивлён: миска пустая. Он будет ходить кругами – это нормально.",
            "The decision is made. Today the beast is surprised: the bowl is empty. It will pace around – that's normal."),
        Mile(2, "Второй день", "Day two", "Часто тяжелее первого: запал прошёл, а привычка ещё громкая. Держись за мелочи – стакан воды, прогулка, ранний сон.",
            "Often harder than the first: the rush is gone and the habit is still loud. Hold on to small things – a glass of water, a walk, an early night."),
        Mile(3, "Третий день", "Day three", "Обычно пик раздражения и тяги. Если сегодня всё бесит – значит, зверь голоден, а не ты слаб. К вечеру отпустит.",
            "Usually the peak of irritation and craving. If everything annoys you today, the beast is hungry – you aren't weak. It eases by evening."),
        Mile(5, "Пятый день", "Day five", "Тяга приходит реже, но неожиданно: в знакомых местах, с знакомыми людьми. Заметь, где именно.",
            "Cravings come less often but by surprise: in familiar places, with familiar people. Notice where."),
        Mile(7, "Неделя", "A week", "Первая неделя позади. Тело уже перестраивается. Похвали себя по-настоящему.",
            "The first week is behind you. Your body is already adjusting. Give yourself real praise."),
        Mile(10, "Десятый день", "Day ten", "Бывает «яма»: скучно, пусто, кажется, что ничего не изменилось. Это тоже проходит – обычно за пару дней.",
            "There can be a dip: bored, empty, it seems nothing has changed. That passes too – usually in a couple of days."),
        Mile(14, "Две недели", "Two weeks", "Сон и настроение выравниваются. Тяга теперь – скорее воспоминание, чем приказ.",
            "Sleep and mood settle. A craving is now more a memory than an order."),
        Mile(21, "Три недели", "Three weeks", "Опасная уверенность: «уже можно одну». Это голос зверя. Перечитай свою причину.",
            "Dangerous confidence: «surely one is fine now». That's the beast talking. Re-read why you started."),
        Mile(30, "Месяц", "A month", "Целый месяц. Новые вечера, новые выходные – и они уже свои.", "A whole month. New evenings, new weekends – and they're already yours."),
        Mile(45, "Полтора месяца", "Six weeks", "Тяга может вернуться волной без причины. Волна приходит и уходит – пересиди её у окна.",
            "A craving may come back as a wave for no reason. A wave comes and goes – sit it out by the window."),
        Mile(60, "Два месяца", "Two months", "Привычка теряет хватку. Ты всё реже думаешь о ней – и это лучший знак.",
            "The habit loses its grip. You think of it less and less – the best sign of all."),
        Mile(90, "Три месяца", "Three months", "Новый образ жизни прижился. Зверь почти ручной – ему уже не нужна твоя миска.",
            "The new way of life has taken root. The beast is nearly tame – it no longer needs your bowl."),
        Mile(180, "Полгода", "Half a year", "Полгода. Если тянет – это ностальгия, а не нужда. Вспомни, каким был первый день.",
            "Half a year. If it tugs at you, that's nostalgia, not need. Remember what day one was like."),
        Mile(365, "Год", "A year", "Год без зверя за столом. Он спит у очага, а дом – твой.", "A year without the beast at your table. It sleeps by the hearth, and the house is yours."),
    )

    private val body = mapOf(
        "smudge" to mapOf(
            1 to tr("Через сутки угарного газа в крови заметно меньше, кислорода – больше.", "After a day there is much less carbon monoxide in the blood and more oxygen."),
            3 to tr("Никотин почти вышел из тела – отсюда и раздражение. Дальше станет легче дышать.", "Nicotine has nearly left the body – hence the irritation. Breathing gets easier from here."),
            14 to tr("Многие замечают, что вкус и запахи стали ярче.", "Many notice tastes and smells getting brighter."),
            90 to tr("Кашель и одышка обычно заметно отступают.", "Coughing and shortness of breath usually ease noticeably."),
        ),
        "froth" to mapOf(
            3 to tr("Сон может быть беспокойным первые ночи – он восстановится.", "Sleep can be restless for the first nights – it will recover."),
            14 to tr("Многие замечают, что утром голова яснее, а сон глубже.", "Many notice clearer mornings and deeper sleep."),
            30 to tr("Через месяц обычно ровнее давление и лучше кожа.", "After a month blood pressure is usually steadier and skin better."),
        ),
        "toffee" to mapOf(
            3 to tr("Тяга к сладкому сильнее всего первые дни, потом вкус перестраивается.", "Sweet cravings are strongest the first days; then your taste adjusts."),
            14 to tr("Фрукты начинают казаться слаще – вкус перенастроился.", "Fruit starts tasting sweeter – your taste has reset."),
        ),
        "flicker" to mapOf(
            3 to tr("Рука сама тянется к телефону – это рефлекс, он угасает за пару недель.", "Your hand reaches for the phone by itself – a reflex that fades in a couple of weeks."),
            14 to tr("Многие замечают, что внимание держится дольше, а вечера длиннее.", "Many notice attention lasting longer and evenings feeling longer."),
        ),
    )

    /** The milestones with a body line where one is known for [kind]. */
    fun of(kind: String): List<Pair<Mile, String?>> = general.map { it to body[kind]?.get(it.day) }

    /** The milestone for day [n] (the last one reached), and the next one. */
    fun at(n: Long): Pair<Mile?, Mile?> = general.lastOrNull { it.day <= n } to general.firstOrNull { it.day > n }

    /** The milestone on exactly day [n], if any. */
    fun exactly(n: Long): Mile? = general.firstOrNull { it.day.toLong() == n }

    /** Small things to do while it passes, shown in the window. */
    val sos = listOf(
        tr("Выпей стакан воды медленно, маленькими глотками.", "Drink a glass of water slowly, in small sips."),
        tr("Выйди на балкон или к окну. Пять глубоких вдохов.", "Step onto the balcony or to the window. Five deep breaths."),
        tr("Назови пять вещей, которые видишь, и три, которые слышишь.", "Name five things you can see and three you can hear."),
        tr("Займи руки: умойся холодной водой, разомни ладони.", "Busy your hands: splash cold water, knead your palms."),
        tr("Тяга – это волна. Она поднимается, стоит и спадает. Просто смотри на неё.", "A craving is a wave. It rises, holds and falls. Just watch it."),
        tr("Напиши кому-нибудь пару слов. Не о тяге – просто так.", "Write a few words to someone. Not about the craving – just because."),
        tr("Вспомни, ради чего всё это. Прочитай свою причину ещё раз.", "Remember why you started. Read your reason once more."),
    )
}
