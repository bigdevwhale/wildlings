package app.wildling.domain

/**
 * A creature of the forest. A bad habit brings a wild beast, a good one a shy creature; finds are rare guests.
 * [habitRu]/[habitEn] name the habit it stands for; [color] tints its marks.
 */
class Kind(
    val id: String, val bad: Boolean, val ru: String, val en: String, val habitRu: String, val habitEn: String,
    val aboutRu: String, val aboutEn: String, val color: Long, val namesRu: List<String>, val namesEn: List<String>,
    val find: Boolean = false,
    /** A creature for a habit the user names themselves. */
    val own: Boolean = false,
) {
    val title get() = tr(ru, en)
    val habit get() = tr(habitRu, habitEn)
    val about get() = tr(aboutRu, aboutEn)
    val names get() = if (Lang.ru) namesRu else namesEn
}

object Kinds {
    private fun k(id: String, bad: Boolean, ru: String, en: String, hr: String, he: String, ar: String, ae: String, c: Long, nr: String, ne: String) =
        Kind(id, bad, ru, en, hr, he, ar, ae, c, nr.split(","), ne.split(","))

    val wild = listOf(
        k("smudge", true, "Чадушка", "Smudge", "Курение", "Smoking",
            "Дымный дракончик. Приходит с запахом табака и шепчет «одну, последнюю».", "A little smoke dragon. Comes with the smell of tobacco, whispering «just one, the last one».",
            0x8C7A9E, "Чадушка,Дымок,Уголёк,Пепелица", "Smudge,Puff,Cinder,Ashley"),
        k("froth", true, "Пенник", "Froth", "Алкоголь", "Alcohol",
            "Болотный зверь с пенной гривой. Любит пятницы, праздники и «по чуть-чуть».", "A bog beast with a mane of foam. Loves Fridays, parties and «just a little».",
            0xC9A24A, "Пенник,Бульк,Хмелёк,Пузырь", "Froth,Bubbles,Hops,Fizz"),
        k("toffee", true, "Тянучка", "Toffee", "Сладкое", "Sweets",
            "Липкая леденцовая ящерка. Появляется после обеда и в тяжёлые вечера.", "A sticky candy-glass lizard. Turns up after lunch and on hard evenings.",
            0xD9883A, "Тянучка,Ириска,Карамелька,Леденец", "Toffee,Fudge,Caramel,Drop"),
        k("flicker", true, "Мерцун", "Flicker", "Лента и соцсети", "Feeds and scrolling",
            "Мотылёк с крыльями-экранами. Не даёт уснуть и съедает часы по минуте.", "A moth with screen-panel wings. Keeps you up and eats hours a minute at a time.",
            0x5A8FD0, "Мерцун,Свечик,Листун,Пиксель", "Flicker,Glow,Scroll,Pixel"),
        k("grabble", true, "Хапушка", "Grabble", "Лишние покупки", "Impulse buying",
            "Мешочек с цепкими лапками и ушами-ценниками. Обожает скидки и «последний шанс».", "A little sack with grabby paws and price-tag ears. Adores sales and «last chance».",
            0xB0745A, "Хапушка,Торбочка,Скидка,Корзинка", "Grabble,Sack,Bargain,Basket"),
        k("tumble", true, "Кубарик", "Tumble", "Азарт и ставки", "Gambling",
            "Броненосец с панцирем-костью. Всё время катится и обещает, что вот сейчас повезёт.", "An armadillo with a die for a shell. Always rolling, always promising that this time it'll land.",
            0xE0E0D6, "Кубарик,Шестёрка,Дубль,Фишка", "Tumble,Six,Double,Chip"),
        k("gobble", true, "Жорка", "Gobble", "Переедание и фастфуд", "Overeating and junk food",
            "Круглый, как пельмень, и с огромной улыбкой. Голоден, даже когда ты сыт.", "Round as a dumpling, with a huge grin. Hungry even when you're full.",
            0xE3B062, "Жорка,Пельмешек,Булка,Хрустик", "Gobble,Dumpling,Bun,Crunch"),
        k("gnash", true, "Грызюк", "Gnash", "Грызть ногти", "Nail biting",
            "Колючий лесной гоблин с зубами-стамесками. Приходит, когда нервничаешь или скучаешь.", "A spiky hedge goblin with chisel teeth. Comes when you're nervous or bored.",
            0x7E8F5C, "Грызюк,Щёлк,Колючка,Заусенец", "Gnash,Snip,Prickle,Nibble"),
        k("grumble", true, "Бурчун", "Grumble", "Ругань и срывы", "Swearing and snapping",
            "Тучка-бесёнок с хвостом-молнией. Гремит от усталости и недосыпа.", "A storm-cloud imp with a lightning tail. Thunders when you're tired or short on sleep.",
            0x6B7586, "Бурчун,Гром,Ворчун,Искра", "Grumble,Thunder,Grouch,Spark"),
        k("jitter", true, "Дрожик", "Jitter", "Кофе и энергетики", "Coffee and energy drinks",
            "Ушастый, в кофейных зёрнах и с хвостом-пружиной. Ни минуты не сидит спокойно.", "Long-eared, coffee-bean fur, a spring for a tail. Never sits still for a minute.",
            0x8A5A3A, "Дрожик,Зёрнышко,Пружинка,Эспрессо", "Jitter,Bean,Spring,Espresso"),
        k("nightjar", true, "Полуночник", "Nightjar", "Поздний отбой", "Going to bed late",
            "Тень со звёздной шерстью и полумесяцем на лбу. Уговаривает «ещё полчасика».", "A shadow with starry fur and a crescent on its brow. Talks you into «another half hour».",
            0x3E4E8C, "Полуночник,Совёнок,Лунник,Звездун", "Nightjar,Moony,Owlet,Starry"),
        k("murk", true, "Морок", "Murk", "Своя вредная привычка", "Your own bad habit",
            "Туманный зверь с глазами-фонарями. Принимает форму любой привычки.", "A fog beast with lantern eyes. Takes the shape of any habit.",
            0x9AA3A8, "Морок,Туманчик,Хмарь,Мгла", "Murk,Misty,Haze,Gloam").let { Kind(it.id, true, it.ru, it.en, it.habitRu, it.habitEn, it.aboutRu, it.aboutEn, it.color, it.namesRu, it.namesEn, own = true) },
    )

    val shy = listOf(
        k("rambler", false, "Бродяжка", "Rambler", "Прогулка", "A walk",
            "Моховой зверёк с рожками-веточками. Выходит к тем, кто ходит пешком.", "A mossy creature with twig antlers. Comes out to those who walk.",
            0x6E9A4A, "Бродяжка,Мшинка,Тропка,Веточка", "Rambler,Mossy,Trail,Twig"),
        k("drip", false, "Капелька", "Drip", "Вода", "Water",
            "Прозрачная водяная кроха с плавничками. Любит, когда рядом стоит стакан воды.", "A see-through water sprite with little fins. Likes a glass of water nearby.",
            0x5BB6D6, "Капелька,Росинка,Брызг,Ручеёк", "Drip,Dewy,Splash,Brook"),
        k("folio", false, "Листовушка", "Folio", "Чтение", "Reading",
            "Ушки-страницы и веснушки из чернил. Садится рядом, когда открываешь книгу.", "Page ears and ink-dot freckles. Sits beside you when a book is open.",
            0xE6D3A8, "Листовушка,Страничка,Закладка,Буквица", "Folio,Page,Bookmark,Inky"),
        k("boing", false, "Прыгунок", "Boing", "Спорт", "Exercise",
            "Длинноногий, с ушами-шляпками. Подпрыгивает от каждой тренировки.", "Long-legged, with mushroom-cap ears. Bounces at every workout.",
            0xD0543E, "Прыгунок,Скакун,Пружинка,Мухомор", "Boing,Hopper,Bouncy,Toadstool"),
        k("drowse", false, "Дрёма", "Drowse", "Сон вовремя", "Bed on time",
            "Облачный барашек в колпаке из тумана. Приходит к тем, кто ложится вовремя.", "A cloud-wool lamb in a nightcap of mist. Comes to those who go to bed on time.",
            0xB7B4D9, "Дрёма,Облачко,Барашек,Зевушка", "Drowse,Cloudy,Lamby,Yawn"),
        k("lotus", false, "Тишайка", "Lotus", "Тишина и медитация", "Stillness and meditation",
            "Парящая кроха с лепестками вместо ушей. Слышит, когда внутри стихает.", "A floating little one with petal ears. Hears when it goes quiet inside.",
            0xE8B3C6, "Тишайка,Лепесток,Кувшинка,Штиль", "Lotus,Petal,Lily,Calm"),
        k("radish", false, "Редиска", "Radish", "Овощи и фрукты", "Fruit and veg",
            "Круглая редиска с ботвой-причёской. Хрустит морковкой за компанию.", "A round radish with leafy hair. Crunches a carrot to keep you company.",
            0xD9566A, "Редиска,Ботвушка,Хрустик,Морковка", "Radish,Leafy,Crunch,Carrot"),
        k("glimmer", false, "Огонёк", "Glimmer", "Благодарность", "Gratitude",
            "Зверёк с фонариком в животе. Светит ярче от каждого «спасибо».", "A creature with a lantern belly. Glows brighter with every «thank you».",
            0xF2C14E, "Огонёк,Фонарик,Искорка,Лучик", "Glimmer,Lantern,Spark,Ray"),
        k("stargaze", false, "Звездочёт", "Stargaze", "Учёба", "Learning",
            "Крылья-плащ в звёздных картах. Приходит туда, где что-то узнают.", "Cloak wings covered in star charts. Comes wherever something is learned.",
            0x4C5FA8, "Звездочёт,Компас,Созвездие,Карта", "Stargaze,Compass,Orion,Chart"),
        k("whisk", false, "Метёлка", "Whisk", "Порядок дома", "A tidy home",
            "Пушистая, с хвостом-веником. Обожает чистые полки.", "Fluffy, with a broom for a tail. Adores clean shelves.",
            0xC9A86A, "Метёлка,Веничек,Пылинка,Чистюля", "Whisk,Broomy,Dusty,Neat"),
        k("dawnling", false, "Зорька", "Dawnling", "Ранний подъём", "Rising early",
            "Персиковая, с хохолком из лучей. Встаёт вместе с солнцем.", "Peach and gold with a crest of sunrays. Rises with the sun.",
            0xF29B5C, "Зорька,Рассвет,Солнышко,Утро", "Dawnling,Sunny,Aurora,Morning"),
        k("snowdrop", false, "Подснежка", "Snowdrop", "Своя полезная привычка", "Your own good habit",
            "Кроха в капюшоне-колокольчике. Звенит, когда делаешь что-то хорошее для себя.", "A tiny one in a bell-flower hood. Rings when you do something good for yourself.",
            0xDDE8E0, "Подснежка,Колокольчик,Звоночек,Бутон", "Snowdrop,Bell,Chime,Bud").let { Kind(it.id, false, it.ru, it.en, it.habitRu, it.habitEn, it.aboutRu, it.aboutEn, it.color, it.namesRu, it.namesEn, own = true) },
    )

    private fun own(id: String, bad: Boolean, ru: String, en: String, ar: String, ae: String, c: Long, nr: String, ne: String) =
        Kind(id, bad, ru, en, if (bad) "Своя вредная привычка" else "Своя полезная привычка", if (bad) "Your own bad habit" else "Your own good habit",
            ar, ae, c, nr.split(","), ne.split(","), own = true)

    /** Beasts for bad habits the user names: no habit of their own until given one. */
    val wildOwn = listOf(
        own("bramble", true, "Колючник", "Bramble", "Волчонок из терновых веток. Цепляется за всё подряд и не отпускает.", "A wolf cub of briar twigs. Snags on everything and won't let go.", 0x5B3A5E, "Колючник,Терновник,Шип,Репей", "Bramble,Briar,Thorn,Burr"),
        own("embermaw", true, "Угольщик", "Embermaw", "Ящер из остывшей лавы. Тлеет внутри и вспыхивает от любой мелочи.", "A lizard of cooled lava. Smoulders inside and flares up at anything.", 0xE0702A, "Угольщик,Жарок,Тлен,Искрун", "Embermaw,Cinder,Smoulder,Sparky"),
        own("gloomcap", true, "Хмурый Гриб", "Gloomcap", "Бесёнок под фиолетовой шляпкой. Дуется и тянет в тень.", "An imp under a violet cap. Sulks and pulls you into the shade.", 0x7A5A9A, "Хмурик,Грибач,Шляпник,Бука", "Gloomcap,Sulk,Shroom,Mope"),
        own("sludge", true, "Тиныч", "Sludge", "Болотная слизь в шляпе-кувшинке. Затягивает, если стоять на месте.", "Swamp slime in a lily-pad hat. Pulls you in if you stand still.", 0x3E8A82, "Тиныч,Ряска,Булькун,Трясинка", "Sludge,Duckweed,Gloop,Bog"),
        own("rattle", true, "Трещотка", "Rattle", "Жук-бесёнок в костяной броне. Гремит, пока на него не обратят внимание.", "A beetle imp in bone armour. Rattles until someone pays attention.", 0xD8D0BC, "Трещотка,Гремучка,Костяшка,Щелчок", "Rattle,Clatter,Bones,Click"),
        own("snarl", true, "Путаник", "Snarl", "Клубок красной пряжи с зубками. Запутывает дни в один узел.", "A ball of red yarn with teeth. Tangles the days into one knot.", 0xC8443A, "Путаник,Клубок,Узелок,Петля", "Snarl,Tangle,Knot,Loop"),
        own("frostbite", true, "Стужик", "Frostbite", "Ледяной бесёнок в голубых шипах. Замораживает всё, что задумано.", "An ice imp in blue spikes. Freezes every plan you make.", 0x7FC6D9, "Стужик,Ледок,Иней,Сосулька", "Frostbite,Icicle,Rime,Chill"),
        own("clank", true, "Жестянщик", "Clank", "Ржавый гоблин-консерва. Стучит ложкой и требует своё.", "A rusty tin-can goblin. Bangs its spoon and demands its due.", 0xB0603A, "Жестянщик,Бряк,Ржавчик,Болтик", "Clank,Rusty,Tinny,Bolt"),
        own("wailer", true, "Нытик", "Wailer", "Привидение-одеяльце с зашитым ртом. Ноет тихо, но долго.", "A little blanket ghost with a stitched mouth. Whines quietly, but long.", 0xB7A6D9, "Нытик,Хныкса,Простынка,Вздох", "Wailer,Whimper,Sheet,Sigh"),
        own("grimble", true, "Перник", "Grimble", "Гремлин в капюшоне из чёрных перьев. Таскает блестяшки и время.", "A gremlin in a hood of black feathers. Steals shiny things and time.", 0x2E2A36, "Перник,Чернушка,Хват,Пёрышко", "Grimble,Inky,Snatch,Feather"),
    )

    /** Shy creatures for good habits the user names. */
    val shyOwn = listOf(
        own("petal", false, "Лепестинка", "Petal", "Олешек из лепестков сакуры. Кружится, когда день прожит бережно.", "A fawn of cherry petals. Twirls when a day is lived with care.", 0xF2A7C3, "Лепестинка,Сакура,Цветик,Нежка", "Petal,Blossom,Bloom,Sakura"),
        own("quill", false, "Пёрышко", "Quill", "Синий зверёк с ушами-перьями. Записывает каждый маленький шаг.", "A blue creature with quill ears. Writes down every small step.", 0x3E5FA8, "Пёрышко,Чернилка,Перо,Строчка", "Quill,Inky,Nib,Scribble"),
        own("honeybun", false, "Медовушка", "Honeybun", "Медовый пушок с крылышками. Собирает хорошие дни, как мёд.", "A honey fluff with tiny wings. Gathers good days like honey.", 0xE8B23E, "Медовушка,Пчелка,Сота,Липка", "Honeybun,Buzzy,Comb,Nectar"),
        own("fernlet", false, "Папоротник", "Fernlet", "Кроха из завитков папоротника. Разворачивается от каждого раза.", "A tiny one of curled fern. Unfurls a little each time.", 0x6DB04A, "Папоротник,Завиток,Листок,Росток", "Fernlet,Curl,Frond,Sprout"),
        own("moonmoth", false, "Лунница", "Moonmoth", "Серебристая моль с крыльями-полумесяцами. Любит спокойные вечера.", "A silver moth with crescent wings. Loves quiet evenings.", 0xC7CCE0, "Лунница,Серебрянка,Полумесяц,Пыльца", "Moonmoth,Silver,Crescent,Dust"),
        own("tidekin", false, "Ракушка", "Tidekin", "Морская кроха в ракушке-шапке. Слушает, как идут дни.", "A sea sprite in a shell hat. Listens to the days go by.", 0x6CCBC0, "Ракушка,Волна,Прибой,Жемчужка", "Tidekin,Wave,Surf,Pearl"),
        own("cocoa", false, "Какао", "Cocoa", "Пушистая буханочка с зефирными ушами. Греет, когда делаешь что-то для себя.", "A fluffy loaf with marshmallow ears. Warms you when you do something for yourself.", 0x8A5A3E, "Какао,Зефирка,Плюшка,Булочка", "Cocoa,Mallow,Muffin,Bun"),
        own("lumen", false, "Аметист", "Lumen", "Кристальный зверёк, светится изнутри. Ярче от каждого раза.", "A crystal creature glowing from inside. Brighter every time.", 0x9B6BD6, "Аметист,Кристалл,Сияна,Друза", "Lumen,Crystal,Gleam,Geode"),
        own("plume", false, "Жар-хвост", "Plume", "Закатно-рыжий пушок с длинным хвостом. Распускает хвост, когда гордится тобой.", "A sunset-orange puff with a long tail. Fans it out when proud of you.", 0xF08A3C, "Жар-хвост,Закат,Хохолок,Опахало", "Plume,Sunset,Crest,Fan"),
        own("mistral", false, "Ветерок", "Mistral", "Облачная ласка в вихрях ветра. Подгоняет вперёд.", "A cloud weasel in swirls of wind. Nudges you forward.", 0x9CC8E8, "Ветерок,Вихрь,Сквозняк,Порыв", "Mistral,Gust,Breeze,Swirl"),
    )

    val finds = listOf(
        Kind("ashling", true, "Пеплыш", "Ashling", "Возвращение", "Coming back",
            "Приходит, когда после срыва удаётся снова продержаться неделю. Из пепла – снова огонь.", "Came when you came back after a slip and held a week more. From ash – fire again.",
            0xE0703A, listOf("Пеплыш"), listOf("Ashling"), find = true),
        Kind("wisp", true, "Блуждайка", "Wisp", "Ночная тяга", "A night craving",
            "Огонёк, что светит тем, кто выдержал тягу глубокой ночью.", "A little light for those who held a craving deep in the night.",
            0x8FC3F0, listOf("Блуждайка"), listOf("Wisp"), find = true),
        Kind("elder", true, "Старожил", "Elder", "Сто дней", "A hundred days",
            "Старый дух леса. Кланяется тем, кто прожил сто дней без зверя.", "An old forest spirit. Bows to those who lived a hundred days without the beast.",
            0x7A8F5A, listOf("Старожил"), listOf("Elder"), find = true),
        Kind("pebble", true, "Камушек", "Pebble", "Десять тяг", "Ten cravings",
            "Маленький голем. Сидит спокойно – как ты все десять раз, пока тяга приходила и уходила.", "A tiny golem. Sits as still as you did, waiting out a craving ten times.",
            0x9C9488, listOf("Камушек"), listOf("Pebble"), find = true),
    )

    val habits = wild + shy + wildOwn + shyOwn
    val all = wild + shy + wildOwn + shyOwn + finds
    private val byId = all.associateBy { it.id }
    /** Ready-made beasts and shy ones (each for its habit), and the ones for the user's own habits. */
    fun ready(bad: Boolean) = (if (bad) wild else shy).filter { !it.own }
    fun owned(bad: Boolean) = (if (bad) wild + wildOwn else shy + shyOwn).filter { it.own }
    fun of(id: String) = byId[id] ?: wild.last()
}

/** How close a creature has come, 0..5: the forest, its edge, the fence, the porch, the house, at your feet. */
object Stages {
    val need = intArrayOf(0, 3, 7, 21, 45, 90)
    fun of(points: Double): Int = need.indexOfLast { points >= it }.coerceAtLeast(0)

    fun name(i: Int, bad: Boolean) = when (i) {
        0 -> if (bad) tr("Тень в чаще", "A shadow in the thicket") else tr("Следы на тропе", "Tracks on the path")
        1 -> if (bad) tr("Глаза у опушки", "Eyes at the forest edge") else tr("Мелькнул в траве", "A flash in the grass")
        2 -> tr("У забора", "By the fence")
        3 -> tr("На крыльце", "On the porch")
        4 -> tr("В доме", "In the house")
        else -> tr("Спит у ног", "Asleep at your feet")
    }

    /** What the stage means, one line. */
    fun line(i: Int, bad: Boolean) = if (bad) when (i) {
        0 -> tr("Зверь ещё дикий: прячется в чаще и ждёт, что его покормят.", "Still wild: hiding in the thicket, waiting to be fed.")
        1 -> tr("Смотрит из темноты. Заметил, что миска пустая.", "Watching from the dark. It noticed the bowl is empty.")
        2 -> tr("Подошёл к забору. Злится уже меньше.", "Came up to the fence. Less angry now.")
        3 -> tr("Сидит на крыльце. Можно спокойно пройти мимо.", "Sitting on the porch. You can walk past it calmly.")
        4 -> tr("Зашёл в дом. Он больше не командует, просто живёт рядом.", "Came inside. It doesn't give orders any more, it just lives here.")
        else -> tr("Приручён. Спит у очага и почти не просыпается.", "Tamed. Sleeps by the hearth and hardly ever wakes.")
    } else when (i) {
        0 -> tr("Пока только следы. Сделай привычку – и он подойдёт ближе.", "Only tracks so far. Do the habit and it'll come closer.")
        1 -> tr("Мелькнул в траве. Ещё пугается.", "A flash in the grass. Still easily startled.")
        2 -> tr("Ждёт у забора, когда ты выйдешь.", "Waits by the fence for you to come out.")
        3 -> tr("Пришёл на крыльцо. Привычка становится твоей.", "Came onto the porch. The habit is becoming yours.")
        4 -> tr("Живёт в доме. Скучает, если пропускаешь.", "Lives in the house. Misses you when you skip.")
        else -> tr("Свой. Спит у очага.", "One of the family. Asleep by the hearth.")
    }
}

/** What brought the craving on. */
object Triggers {
    val all = listOf(
        "stress" to ("Стресс" to "Stress"), "bored" to ("Скука" to "Boredom"), "tired" to ("Усталость" to "Tiredness"),
        "company" to ("Компания" to "Company"), "meal" to ("После еды" to "After a meal"), "coffee" to ("Кофе" to "Coffee"),
        "drink" to ("Выпивка" to "A drink"), "evening" to ("Вечер" to "Evening"), "phone" to ("Телефон" to "Phone"),
        "sad" to ("Грусть" to "Sadness"), "angry" to ("Злость" to "Anger"), "habit" to ("По привычке" to "Out of habit"),
    )
    fun name(id: String) = all.firstOrNull { it.first == id }?.second?.let { tr(it.first, it.second) } ?: id
}

/** How the day felt. */
object Moods {
    val all = listOf(
        Triple("😌", "Спокойно", "Calm"), Triple("💪", "Гордость", "Proud"), Triple("🙂", "Легко", "Light"),
        Triple("😐", "Никак", "Flat"), Triple("😟", "Тревожно", "Anxious"), Triple("😤", "Злюсь", "Angry"),
        Triple("😔", "Грустно", "Sad"), Triple("😩", "Тянет", "Craving"),
    )
    fun face(i: Int) = all.getOrNull(i)?.first ?: "·"
    fun name(i: Int) = all.getOrNull(i)?.let { tr(it.second, it.third) } ?: ""
    /** 1 for the bright moods, 0 for the flat one, −1 for the hard ones. */
    fun tone(i: Int) = when (i) { 0, 1, 2 -> 1; 3 -> 0; else -> -1 }
}
