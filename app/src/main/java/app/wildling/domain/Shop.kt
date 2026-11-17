package app.wildling.domain

/**
 * What pinecones buy: seeds for the three garden beds and things for the yard and the house.
 * Positions are parts of the picture's width and height; [y] is where the thing stands (its bottom), or its top when [hang].
 * [inside] things go into the house view, the rest into the yard.
 */
class Thing(val id: String, val ru: String, val en: String, val price: Int, val x: Float, val y: Float, val w: Float,
            val hang: Boolean = false, val back: Boolean = false, val inside: Boolean = false) {
    val title get() = tr(ru, en)
}

class Seed(val id: String, val ru: String, val en: String, val price: Int, val days: Int) {
    val title get() = tr(ru, en)
}

object Shop {
    val things = listOf(
        Thing("rocker", "Кресло-качалка", "Rocking chair", 40, .705f, .695f, .065f),
        Thing("lamp", "Фонарь на столбе", "Lantern post", 30, .73f, .93f, .045f),
        Thing("bench", "Скамейка", "Bench", 35, .31f, .975f, .1f),
        Thing("wood", "Поленница", "Woodpile", 25, .975f, .82f, .07f),
        Thing("birdhouse", "Скворечник", "Birdhouse", 30, .08f, .7f, .04f, back = true),
        Thing("well", "Колодец", "Well", 60, .07f, .985f, .1f),
        Thing("chimes", "Музыка ветра", "Wind chimes", 20, .685f, .41f, .03f, hang = true),
        Thing("mailbox", "Почтовый ящик", "Mailbox", 25, .64f, .9f, .04f),
        Thing("swing", "Качели", "Swing", 50, .185f, .985f, .09f),
        Thing("scarecrow", "Пугало", "Scarecrow", 35, .44f, .985f, .065f),
        Thing("geranium", "Герань", "Geranium", 15, .305f, .405f, .035f, inside = true),
        Thing("shelf", "Книжная полка", "Bookshelf", 45, .575f, .462f, .05f, inside = true),
        Thing("flags", "Флажки", "Bunting", 20, .85f, .395f, .2f, hang = true),
        Thing("hammock", "Гамак", "Hammock", 55, .55f, .99f, .11f),
        Thing("sign", "Указатель", "Signpost", 20, .885f, .985f, .045f),
        Thing("kettle", "Чайник", "Kettle", 25, .62f, .73f, .028f, inside = true),
    )
    val seeds = listOf(
        Seed("sunflower", "Подсолнух", "Sunflower", 10, 7),
        Seed("cabbage", "Капуста", "Cabbage", 8, 6),
        Seed("lavender", "Лаванда", "Lavender", 12, 8),
        Seed("pumpkin", "Тыква", "Pumpkin", 15, 10),
        Seed("blueberry", "Черника", "Blueberry", 15, 10),
        Seed("tomato", "Помидор", "Tomato", 12, 9),
        Seed("rose", "Роза", "Rose", 20, 12),
        Seed("apple", "Яблонька", "Apple tree", 30, 21),
    )
    val thingById = things.associateBy { it.id }
    val seedById = seeds.associateBy { it.id }

    /** The garden beds in the yard picture: centre x and the soil line. */
    val beds = listOf(.205f to .765f, .385f to .765f, .565f to .765f)

    /** A bed's plant, 0 sprout · 1 seedling · 2 grown, after [days] since sowing. */
    fun growth(seed: Seed, days: Long) = when { days >= seed.days -> 2; days >= seed.days / 3 -> 1; else -> 0 }
}
