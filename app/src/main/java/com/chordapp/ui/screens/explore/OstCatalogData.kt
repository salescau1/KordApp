package com.chordapp.ui.screens.explore

data class OstSongItem(
    val title: String,
    val artist: String,
    val artistSlug: String,
    val titleSlug: String,
    val role: String? = null
)

data class OstFranchise(
    val id: String,
    val title: String,
    val category: String,
    val subtitle: String,
    val songs: List<OstSongItem>
)

object OstCatalogData {
    val CATEGORIES: List<String> = listOf(
        "Semua",
        "Film Indonesia",
        "Anime & Jepang",
        "K-Drama",
        "Hollywood & Disney"
    )

    val FRANCHISES: List<OstFranchise> = listOf(
        // ==========================================
        // 1. ANIME & JEPANG
        // ==========================================
        OstFranchise(
            id = "naruto",
            title = "Naruto & Naruto Shippuden",
            category = "Anime & Jepang",
            subtitle = "Akeboshi, Ikimono-Gakari, Flow, KANA-BOON, Sambomaster, nobodyknows+",
            songs = listOf(
                OstSongItem("Wind", "Akeboshi", "akeboshi", "wind", "Ending 1"),
                OstSongItem("Haruka Kanata", "Asian Kung-Fu Generation", "asian-kung-fu-generation", "haruka-kanata", "Naruto OP 2"),
                OstSongItem("Kanashimi wo Yasashisa ni", "little by little", "little-by-little", "kanashimi-wo-yasashisa-ni", "Naruto OP 3"),
                OstSongItem("GO!!!", "Flow", "flow", "go", "Naruto OP 4"),
                OstSongItem("Seishun Kyousoukyoku", "Sambomaster", "sambomaster", "seishun-kyousoukyoku", "Naruto OP 5"),
                OstSongItem("Namikaze Satellite", "Snowkel", "snowkel", "namikaze-satellite", "Naruto OP 7"),
                OstSongItem("Yura Yura", "Hearts Grow", "hearts-grow", "yura-yura", "Naruto OP 9"),
                OstSongItem("Harmonia", "RYTHEM", "rythem", "harmonia", "Naruto ED 2"),
                OstSongItem("Yellow Moon", "Akeboshi", "akeboshi", "yellow-moon", "Ending 13"),
                OstSongItem("Hero's Come Back!!", "nobodyknows+", "nobodyknows", "hero-s-come-back", "Shippuden OP 1"),
                OstSongItem("Blue Bird", "Ikimono-Gakari", "ikimono-gakari", "blue-bird", "Shippuden OP 3"),
                OstSongItem("Hotaru no Hikari", "Ikimono-Gakari", "ikimono-gakari", "hotaru-no-hikari", "Shippuden OP 5"),
                OstSongItem("Sign", "Flow", "flow", "sign", "Shippuden OP 6"),
                OstSongItem("Toumei Datta Sekai", "Motohiro Hata", "motohiro-hata", "toumei-datta-sekai", "Shippuden OP 7"),
                OstSongItem("Lovers", "7!!", "7", "lovers", "Shippuden OP 9"),
                OstSongItem("Totsugeki Rock", "The Cro-Magnons", "the-cro-magnons", "totsugeki-rock", "Shippuden OP 11"),
                OstSongItem("Tsuki no Ookisa", "Nogizaka46", "nogizaka46", "tsuki-no-ookisa", "Shippuden OP 14"),
                OstSongItem("Silhouette", "KANA-BOON", "kana-boon", "silhouette", "Shippuden OP 16"),
                OstSongItem("Kara no Kokoro", "Anly", "anly", "kara-no-kokoro", "Shippuden OP 20"),
                OstSongItem("Utakata Hanabi", "supercell", "supercell", "utakata-hanabi", "Shippuden ED 14"),
                OstSongItem("Kotoba no Iranai Yakusoku", "HoneyWorks", "honeyworks", "kotoba-no-iranai-yakusoku", "Shippuden ED 33"),
                OstSongItem("Pino to Amelie", "Huwie Ishizaki", "huwie-ishizaki", "pino-to-amelie", "Shippuden ED 38"),
                OstSongItem("Hoshi no Utsuwa", "Sukima Switch", "sukima-switch", "hoshi-no-utsuwa", "The Last: Movie Theme"),
                OstSongItem("Baton Road", "KANA-BOON", "kana-boon", "baton-road", "Boruto OP 1")
            )
        ),

        OstFranchise(
            id = "your_name",
            title = "Kimi no Na wa (Your Name)",
            category = "Anime & Jepang",
            subtitle = "RADWIMPS - Makoto Shinkai Masterpiece",
            songs = listOf(
                OstSongItem("Sparkle", "RADWIMPS", "radwimps", "sparkle", "Main Theme"),
                OstSongItem("Nandemonaiya", "RADWIMPS", "radwimps", "nandemonaiya", "Ending Theme"),
                OstSongItem("Zenzenzense", "RADWIMPS", "radwimps", "zenzenzense", "Insert Song")
            )
        ),
        OstFranchise(
            id = "demon_slayer",
            title = "Demon Slayer (Kimetsu no Yaiba)",
            category = "Anime & Jepang",
            subtitle = "LiSA, MAN WITH A MISSION x milet",
            songs = listOf(
                OstSongItem("Gurenge", "LiSA", "lisa", "gurenge", "Opening 1"),
                OstSongItem("Homura", "LiSA", "lisa", "homura", "Mugen Train Theme"),
                OstSongItem("Kizuna no Kiseki", "MAN WITH A MISSION x milet", "man-with-a-mission-x-milet", "kizuna-no-kiseki", "Swordsmith Village OP")
            )
        ),
        OstFranchise(
            id = "attack_on_titan",
            title = "Attack on Titan (Shingeki no Kyojin)",
            category = "Anime & Jepang",
            subtitle = "Linked Horizon, Ai Higuchi, SiM",
            songs = listOf(
                OstSongItem("Shinzou wo Sasageyo!", "Linked Horizon", "linked-horizon", "shinzou-wo-sasageyo", "Season 2 OP"),
                OstSongItem("Guren no Yumiya", "Linked Horizon", "linked-horizon", "guren-no-yumiya", "Season 1 OP 1"),
                OstSongItem("Akuma no Ko", "Ai Higuchi", "ai-higuchi", "akuma-no-ko", "Final Season ED")
            )
        ),
        OstFranchise(
            id = "one_piece",
            title = "One Piece",
            category = "Anime & Jepang",
            subtitle = "Hiroshi Kitadani, Ado, BOYSTYLE, The Babystars",
            songs = listOf(
                OstSongItem("We Are!", "Hiroshi Kitadani", "hiroshi-kitadani", "we-are", "Opening 1"),
                OstSongItem("Hikari E", "The Babystars", "the-babystars", "hikari-e", "Opening 3"),
                OstSongItem("Kokoro no Chizu", "BOYSTYLE", "boystyle", "kokoro-no-chizu", "Opening 5"),
                OstSongItem("Memories", "Maki Otsuki", "maki-otsuki", "memories", "Ending 1"),
                OstSongItem("New Genesis (Shinjidai)", "Ado", "ado", "new-genesis-shinjidai", "Film Red Theme"),
                OstSongItem("Watashi wa Saikyou", "Ado", "ado", "watashi-wa-saikyou", "Film Red Insert")
            )
        ),


        OstFranchise(
            id = "digimon",
            title = "Digimon Adventure",
            category = "Anime & Jepang",
            subtitle = "Koji Wada, Ayumi Miyazaki",
            songs = listOf(
                OstSongItem("Butter-Fly", "Koji Wada", "koji-wada", "butter-fly", "Opening Theme"),
                OstSongItem("Brave Heart", "Ayumi Miyazaki", "ayumi-miyazaki", "brave-heart", "Evolution Theme")
            )
        ),
        OstFranchise(
            id = "dragon_ball",
            title = "Dragon Ball & Dragon Ball GT",
            category = "Anime & Jepang",
            subtitle = "Hironobu Kageyama, Field of View",
            songs = listOf(
                OstSongItem("Dan Dan Kokoro Hikareteku", "Field of View", "field-of-view", "dan-dan-kokoro-hikareteku", "GT Opening"),
                OstSongItem("Cha-La Head-Cha-La", "Hironobu Kageyama", "hironobu-kageyama", "cha-la-head-cha-la", "DBZ Opening 1")
            )
        ),
        OstFranchise(
            id = "tokyo_ghoul",
            title = "Tokyo Ghoul",
            category = "Anime & Jepang",
            subtitle = "TK from Ling Tosite Sigure",
            songs = listOf(
                OstSongItem("Unravel", "TK from Ling Tosite Sigure", "tk-from-ling-tosite-sigure", "unravel", "Opening 1")
            )
        ),
        OstFranchise(
            id = "inuyasha",
            title = "Inuyasha",
            category = "Anime & Jepang",
            subtitle = "Do As Infinity, BoA",
            songs = listOf(
                OstSongItem("Fukai Mori", "Do As Infinity", "do-as-infinity", "fukai-mori", "Ending 2"),
                OstSongItem("Every Heart", "BoA", "boa", "every-heart", "Ending 4")
            )
        ),
        OstFranchise(
            id = "bleach_fma",
            title = "Bleach & Fullmetal Alchemist",
            category = "Anime & Jepang",
            subtitle = "YUI, Orange Range, UVERworld, Aqua Timez, SID",
            songs = listOf(
                OstSongItem("Asterisk", "Orange Range", "orange-range", "asterisk", "Bleach OP 1"),
                OstSongItem("D-tecnolife", "UVERworld", "uverworld", "d-tecnolife", "Bleach OP 2"),
                OstSongItem("Rolling Star", "YUI", "yui", "rolling-star", "Bleach OP 5"),
                OstSongItem("Alones", "Aqua Timez", "aqua-timez", "alones", "Bleach OP 6"),
                OstSongItem("Velonica", "Aqua Timez", "aqua-timez", "velonica", "Bleach OP 9"),
                OstSongItem("Ranbu no Melody", "SID", "sid", "ranbu-no-melody", "Bleach OP 13"),
                OstSongItem("Again", "YUI", "yui", "again", "FMA Brotherhood OP 1")
            )
        ),
        OstFranchise(
            id = "studio_ghibli",
            title = "Studio Ghibli (Hayao Miyazaki)",
            category = "Anime & Jepang",
            subtitle = "Joe Hisaishi, Youmi Kimura, Yumi Matsutoya",
            songs = listOf(
                OstSongItem("Always with Me (Itsumo Nando Demo)", "Youmi Kimura", "youmi-kimura", "always-with-me-itsumo-nando-demo", "Spirited Away Theme"),
                OstSongItem("The Promise of the World (Sekai no Yakusoku)", "Chieko Baisho & Joe Hisaishi", "chieko-baisho-joe-hisaishi", "the-promise-of-the-world-sekai-no-yakusoku", "Howl's Moving Castle Theme"),
                OstSongItem("Tonari no Totoro", "Azumi Inoue & Joe Hisaishi", "azumi-inoue-joe-hisaishi", "tonari-no-totoro", "Totoro Theme"),
                OstSongItem("Rouge no Dengon", "Yumi Matsutoya", "yumi-matsutoya", "rouge-no-dengon", "Kiki's Delivery Service OP"),
                OstSongItem("Mononoke Hime", "Joe Hisaishi", "joe-hisaishi", "mononoke-hime", "Princess Mononoke Theme")
            )
        ),
        OstFranchise(
            id = "hunter_x_hunter",
            title = "Hunter x Hunter",
            category = "Anime & Jepang",
            subtitle = "Masatoshi Ono, Yuzu, Galneryus",
            songs = listOf(
                OstSongItem("Departure!", "Masatoshi Ono", "masatoshi-ono", "departure", "Opening Theme"),
                OstSongItem("Hyori Ittai", "Yuzu", "yuzu", "hyori-ittai", "Chimera Ant ED 2"),
                OstSongItem("Hunting for Your Dream", "Galneryus", "galneryus", "hunting-for-your-dream", "Ending 2")
            )
        ),
        OstFranchise(
            id = "your_lie_in_april",
            title = "Your Lie in April (Shigatsu wa Kimi no Uso)",
            category = "Anime & Jepang",
            subtitle = "Goose house, 7!!",
            songs = listOf(
                OstSongItem("Hikaru Nara", "Goose house", "goose-house", "hikaru-nara", "Opening 1"),
                OstSongItem("Orange", "7!!", "7", "orange", "Ending 2")
            )
        ),
        OstFranchise(
            id = "bocchi_oshi_no_ko",
            title = "Oshi no Ko & Bocchi the Rock!",
            category = "Anime & Jepang",
            subtitle = "YOASOBI, Kessoku Band, Official HIGE DANdism",
            songs = listOf(
                OstSongItem("Idol", "YOASOBI", "yoasobi", "idol", "Oshi no Ko OP - Global Hit"),
                OstSongItem("Seishun Complex", "Kessoku Band", "kessoku-band", "seishun-complex", "Bocchi the Rock OP"),
                OstSongItem("Ano Band", "Kessoku Band", "kessoku-band", "ano-band", "Bocchi the Rock Live"),
                OstSongItem("Guitar, Loneliness and Blue Planet", "Kessoku Band", "kessoku-band", "guitar-loneliness-and-blue-planet", "Bocchi the Rock Insert"),
                OstSongItem("Cry Baby", "Official HIGE DANdism", "official-hige-dandism", "cry-baby", "Tokyo Revengers OP")
            )
        ),
        OstFranchise(
            id = "fairytail_deathnote",
            title = "Fairy Tail & Death Note",
            category = "Anime & Jepang",
            subtitle = "FUNKIST, Nightmare",
            songs = listOf(
                OstSongItem("Snow Fairy", "FUNKIST", "funkist", "snow-fairy", "Fairy Tail OP 1"),
                OstSongItem("The World", "Nightmare", "nightmare", "the-world", "Death Note OP 1")
            )
        ),
        OstFranchise(
            id = "doraemon",
            title = "Stand By Me Doraemon",
            category = "Anime & Jepang",
            subtitle = "Motohiro Hata, Kumiko Osugi",
            songs = listOf(
                OstSongItem("Himawari no Yakusoku", "Motohiro Hata", "motohiro-hata", "himawari-no-yakusoku", "Stand By Me Theme")
            )
        ),
        OstFranchise(
            id = "samurai_x",
            title = "Samurai X (Rurouni Kenshin)",
            category = "Anime & Jepang",
            subtitle = "T.M.Revolution, Judy and Mary, SIAM SHADE",
            songs = listOf(
                OstSongItem("Heart of Sword", "T.M.Revolution", "t-m-revolution", "heart-of-sword", "Ending Theme"),
                OstSongItem("Sobakasu", "Judy and Mary", "judy-and-mary", "sobakasu", "Opening 1")
            )
        ),
        OstFranchise(
            id = "slam_dunk",
            title = "Slam Dunk",
            category = "Anime & Jepang",
            subtitle = "BAAD, WANDS, ZARD",
            songs = listOf(
                OstSongItem("Kimi ga Suki da to Sakebitai", "BAAD", "baad", "kimi-ga-suki-da-to-sakebitai", "Opening Theme"),
                OstSongItem("Sekai ga Owaru Made wa", "WANDS", "wands", "sekai-ga-owaru-made-wa", "Ending Theme")
            )
        ),
        OstFranchise(
            id = "ghost_fighter",
            title = "Ghost Fighter (Yu Yu Hakusho)",
            category = "Anime & Jepang",
            subtitle = "Matsuko Mawatari",
            songs = listOf(
                OstSongItem("Hohoemi no Bakudan (Smile Bomb)", "Matsuko Mawatari", "matsuko-mawatari", "hohoemi-no-bakudan", "Opening Theme")
            )
        ),
        OstFranchise(
            id = "evangelion_modern",
            title = "Evangelion, Jujutsu Kaisen & Spy x Family",
            category = "Anime & Jepang",
            subtitle = "Yoko Takahashi, Eve, Tatsuya Kitani, Official HIGE DANdism",
            songs = listOf(
                OstSongItem("A Cruel Angel's Thesis", "Yoko Takahashi", "yoko-takahashi", "a-cruel-angel-s-thesis", "Evangelion OP"),
                OstSongItem("Kaikai Kitan", "Eve", "eve", "kaikai-kitan", "Jujutsu Kaisen OP 1"),
                OstSongItem("Ao no Sumika (Where Our Blue Is)", "Tatsuya Kitani", "tatsuya-kitani", "ao-no-sumika-where-our-blue-is", "Jujutsu Kaisen S2 OP"),
                OstSongItem("Mixed Nuts", "Official HIGE DANdism", "official-hige-dandism", "mixed-nuts", "Spy x Family OP 1")
            )
        ),
        OstFranchise(
            id = "suzume",
            title = "Suzume (RADWIMPS)",
            category = "Anime & Jepang",
            subtitle = "RADWIMPS ft. Toaka - Makoto Shinkai",
            songs = listOf(
                OstSongItem("Suzume", "RADWIMPS", "radwimps", "suzume", "Main Theme")
            )
        ),


        // ==========================================
        // 2. FILM & SINETRON INDONESIA
        // ==========================================
        OstFranchise(
            id = "aadc",
            title = "Ada Apa Dengan Cinta? (AADC)",
            category = "Film Indonesia",
            subtitle = "Melly Goeslaw, Anda Perdana, Eric",
            songs = listOf(
                OstSongItem("Ada Apa Dengan Cinta", "Melly Goeslaw", "melly-goeslaw", "ada-apa-dengan-cinta", "Main Theme"),
                OstSongItem("Tentang Seseorang", "Anda Perdana", "anda", "tentang-seseorang", "Soundtrack"),
                OstSongItem("Bimbang", "Melly Goeslaw", "melly-goeslaw", "bimbang", "Insert Song"),
                OstSongItem("Ku Bahagia", "Melly Goeslaw", "melly-goeslaw", "ku-bahagia", "Ending Theme"),
                OstSongItem("Suara Hati Seorang Kekasih", "Melly Goeslaw", "melly-goeslaw", "suara-hati-seorang-kekasih", "Soundtrack"),
                OstSongItem("Ratusan Purnama", "Melly Goeslaw", "melly-goeslaw", "ratusan-purnama", "AADC 2 Theme")
            )
        ),
        OstFranchise(
            id = "alexandria",
            title = "Film Alexandria",
            category = "Film Indonesia",
            subtitle = "Peterpan - Original Soundtrack",
            songs = listOf(
                OstSongItem("Tak Bisakah", "Peterpan", "peterpan", "tak-bisakah", "Main Theme"),
                OstSongItem("Menunggu Pagi", "Peterpan", "peterpan", "menunggu-pagi", "Soundtrack"),
                OstSongItem("Mungkin Nanti", "Peterpan", "peterpan", "mungkin-nanti", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "badai_pasti_berlalu",
            title = "Badai Pasti Berlalu",
            category = "Film Indonesia",
            subtitle = "Chrisye, Berlian Hutauruk - Masterpiece",
            songs = listOf(
                OstSongItem("Badai Pasti Berlalu", "Chrisye", "chrisye", "badai-pasti-berlalu", "Main Theme"),
                OstSongItem("Pelangi", "Chrisye", "chrisye", "pelangi", "Soundtrack"),
                OstSongItem("Cintaku", "Chrisye", "chrisye", "cintaku", "Soundtrack")
            )
        ),

        OstFranchise(
            id = "petualangan_sherina",
            title = "Petualangan Sherina (1 & 2)",
            category = "Film Indonesia",
            subtitle = "Sherina Munaf",
            songs = listOf(
                OstSongItem("Lihatlah Lebih Dekat", "Sherina Munaf", "sherina-munaf", "lihatlah-lebih-dekat", "Soundtrack"),
                OstSongItem("Jagoan", "Sherina Munaf", "sherina-munaf", "jagoan", "Soundtrack"),
                OstSongItem("Persahabatan", "Sherina Munaf", "sherina-munaf", "persahabatan", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "heart",
            title = "Heart",
            category = "Film Indonesia",
            subtitle = "Acha Septriasa, Irwansyah",
            songs = listOf(
                OstSongItem("My Heart", "Acha Septriasa & Irwansyah", "acha-septriasa-irwansyah", "my-heart", "Main Theme"),
                OstSongItem("Sampai Menutup Mata", "Acha Septriasa", "acha-septriasa", "sampai-menutup-mata", "Soundtrack"),
                OstSongItem("Pecinta Wanita", "Irwansyah", "irwansyah", "pecinta-wanita", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "pengabdi_setan",
            title = "Pengabdi Setan",
            category = "Film Indonesia",
            subtitle = "The Spouse - Joko Anwar Horror Classic",
            songs = listOf(
                OstSongItem("Kelam Malam", "The Spouse", "the-spouse", "kelam-malam", "Main Theme")
            )
        ),
        OstFranchise(
            id = "laskar_pelangi",
            title = "Laskar Pelangi & Sang Pemimpi",
            category = "Film Indonesia",
            subtitle = "Nidji, Ipang, Peterpan",
            songs = listOf(
                OstSongItem("Laskar Pelangi", "Nidji", "nidji", "laskar-pelangi", "Main Theme"),
                OstSongItem("Sahabat Kecil", "Ipang", "ipang", "sahabat-kecil", "Sang Pemimpi Theme"),
                OstSongItem("Menghapus Jejakmu", "Peterpan", "peterpan", "menghapus-jejakmu", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "dealova",
            title = "Dealova",
            category = "Film Indonesia",
            subtitle = "Once Mekel",
            songs = listOf(
                OstSongItem("Dealova", "Once Mekel", "once-mekel", "dealova", "Main Theme")
            )
        ),
        OstFranchise(
            id = "ayat_ayat_cinta",
            title = "Ayat-Ayat Cinta",
            category = "Film Indonesia",
            subtitle = "Rossa",
            songs = listOf(
                OstSongItem("Ayat-Ayat Cinta", "Rossa", "rossa", "ayat-ayat-cinta", "Main Theme"),
                OstSongItem("Takdir Cinta", "Rossa", "rossa", "takdir-cinta", "Soundtrack")
            )
        ),

        OstFranchise(
            id = "habibie_ainun",
            title = "Habibie & Ainun",
            category = "Film Indonesia",
            subtitle = "Bunga Citra Lestari, Maudy Ayunda",
            songs = listOf(
                OstSongItem("Cinta Sejati", "Bunga Citra Lestari", "bunga-citra-lestari", "cinta-sejati", "Main Theme"),
                OstSongItem("Kamu & Kenangan", "Maudy Ayunda", "maudy-ayunda", "kamu-dan-kenangan", "Habibie & Ainun 3")
            )
        ),
        OstFranchise(
            id = "perahu_kertas",
            title = "Perahu Kertas",
            category = "Film Indonesia",
            subtitle = "Maudy Ayunda, Glenn Fredly",
            songs = listOf(
                OstSongItem("Perahu Kertas", "Maudy Ayunda", "maudy-ayunda", "perahu-kertas", "Main Theme"),
                OstSongItem("Tahu Diri", "Maudy Ayunda", "maudy-ayunda", "tahu-diri", "Soundtrack"),
                OstSongItem("Malaikat Juga Tahu", "Glenn Fredly", "glenn-fredly", "malaikat-juga-tahu", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "dilan",
            title = "Dilan 1990 & 1991",
            category = "Film Indonesia",
            subtitle = "The Panasdalam Bank, Iqbaal Ramadhan",
            songs = listOf(
                OstSongItem("Dulu Kita Masih Remaja", "The Panasdalam Bank", "the-panasdalam-bank", "dulu-kita-masih-remaja", "Main Theme"),
                OstSongItem("Rindu Sendiri", "Iqbaal Ramadhan", "iqbaal-ramadhan", "rindu-sendiri", "Soundtrack"),
                OstSongItem("Dan Bandung", "The Panasdalam Bank", "the-panasdalam-bank", "dan-bandung", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "keluarga_cemara",
            title = "Keluarga Cemara",
            category = "Film Indonesia",
            subtitle = "Nadin Amizah",
            songs = listOf(
                OstSongItem("Seperti Tulang", "Nadin Amizah", "nadin-amizah", "seperti-tulang", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "nkcthi",
            title = "Nanti Kita Cerita Tentang Hari Ini (NKCTHI)",
            category = "Film Indonesia",
            subtitle = "Hindia, Ardhito Pramono, Isyana Sarasvati",
            songs = listOf(
                OstSongItem("Secukupnya", "Hindia", "hindia", "secukupnya", "Main Theme"),
                OstSongItem("Untuk Hati Yang Terluka", "Isyana Sarasvati", "isyana-sarasvati", "untuk-hati-yang-terluka", "Soundtrack"),
                OstSongItem("Bitterlove", "Ardhito Pramono", "ardhito-pramono", "bitterlove", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "imperfect",
            title = "Imperfect",
            category = "Film Indonesia",
            subtitle = "Fiersa Besari",
            songs = listOf(
                OstSongItem("Pelukku Untuk Pelikmu", "Fiersa Besari", "fiersa-besari", "pelukku-untuk-pelikmu", "Main Theme")
            )
        ),
        OstFranchise(
            id = "kukira_kau_rumah",
            title = "Kukira Kau Rumah",
            category = "Film Indonesia",
            subtitle = "Amigdala",
            songs = listOf(
                OstSongItem("Kukira Kau Rumah", "Amigdala", "amigdala-indonesia", "kukira-kau-rumah", "Main Theme")
            )
        ),

        OstFranchise(
            id = "ikatan_cinta",
            title = "Ikatan Cinta",
            category = "Film Indonesia",
            subtitle = "Ade Govinda ft. Fadly",
            songs = listOf(
                OstSongItem("Tanpa Batas Waktu", "Ade Govinda", "ade-govinda", "tanpa-batas-waktu", "Main Theme")
            )
        ),
        OstFranchise(
            id = "5cm",
            title = "5 cm",
            category = "Film Indonesia",
            subtitle = "Nidji",
            songs = listOf(
                OstSongItem("Di Atas Awan", "Nidji", "nidji", "di-atas-awan", "Main Theme"),
                OstSongItem("Rahasia Hati", "Nidji", "nidji", "rahasia-hati", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "mengejar_matahari",
            title = "Mengejar Matahari",
            category = "Film Indonesia",
            subtitle = "Ari Lasso",
            songs = listOf(
                OstSongItem("Mengejar Matahari", "Ari Lasso", "ari-lasso", "mengejar-matahari", "Main Theme")
            )
        ),

        // ==========================================
        // 3. KOREAN DRAMA (K-DRAMA)
        // ==========================================
        OstFranchise(
            id = "goblin",
            title = "Goblin (Guardian)",
            category = "K-Drama",
            subtitle = "Chanyeol & Punch, Crush, Ailee",
            songs = listOf(
                OstSongItem("Stay With Me", "Chanyeol & Punch", "chanyeol-punch", "stay-with-me", "OST Part 1"),
                OstSongItem("Beautiful", "Crush", "crush", "beautiful", "OST Part 4"),
                OstSongItem("I Will Go to You Like the First Snow", "Ailee", "ailee", "i-will-go-to-you-like-the-first-snow", "OST Part 9")
            )
        ),
        OstFranchise(
            id = "dots",
            title = "Descendants of the Sun (DOTS)",
            category = "K-Drama",
            subtitle = "Gummy, Chen & Punch",
            songs = listOf(
                OstSongItem("You Are My Everything", "Gummy", "gummy", "you-are-my-everything", "OST Part 4"),
                OstSongItem("Everytime", "Chen & Punch", "chen-punch", "everytime", "OST Part 2")
            )
        ),
        OstFranchise(
            id = "reply_1988",
            title = "Reply 1988",
            category = "K-Drama",
            subtitle = "Lee Juck",
            songs = listOf(
                OstSongItem("Don't Worry", "Lee Juck", "lee-juck", "don-t-worry", "OST Part 2")
            )
        ),
        OstFranchise(
            id = "itaewon_class",
            title = "Itaewon Class",
            category = "K-Drama",
            subtitle = "Gaho, V (BTS)",
            songs = listOf(
                OstSongItem("Start Over", "Gaho", "gaho", "start-over", "Main Theme"),
                OstSongItem("Sweet Night", "V", "v", "sweet-night", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "hotel_del_luna",
            title = "Hotel Del Luna",
            category = "K-Drama",
            subtitle = "Gummy",
            songs = listOf(
                OstSongItem("Remember Me", "Gummy", "gummy", "remember-me", "OST Part 7")
            )
        ),


        OstFranchise(
            id = "full_house",
            title = "Full House",
            category = "K-Drama",
            subtitle = "Byul - Romantic Comedy Classic",
            songs = listOf(
                OstSongItem("I Think I", "Byul", "byul", "i-think-i", "Theme Song")
            )
        ),
        OstFranchise(
            id = "cloy",
            title = "Crash Landing on You",
            category = "K-Drama",
            subtitle = "IU, Yerin Baek",
            songs = listOf(
                OstSongItem("Give You My Heart", "IU", "iu", "give-you-my-heart", "OST Part 11"),
                OstSongItem("Here I Am Again", "Yerin Baek", "yerin-baek", "here-i-am-again", "OST Part 4")
            )
        ),
        OstFranchise(
            id = "queen_of_tears",
            title = "Queen of Tears",
            category = "K-Drama",
            subtitle = "BSS (SEVENTEEN), Crush",
            songs = listOf(
                OstSongItem("The Reasons of My Smiles", "BSS (SEVENTEEN)", "bss-seventeen", "the-reasons-of-my-smiles", "OST Part 1"),
                OstSongItem("Love You With All My Heart", "Crush", "crush", "love-you-with-all-my-heart", "OST Part 4")
            )
        ),
        OstFranchise(
            id = "hospital_playlist",
            title = "Hospital Playlist (1 & 2)",
            category = "K-Drama",
            subtitle = "Jo Jung Suk, Lee Mujin",
            songs = listOf(
                OstSongItem("I Like You", "Jo Jung Suk", "jo-jung-suk", "i-like-you", "Season 1 OST"),
                OstSongItem("Rain and You", "Lee Mujin", "lee-mujin", "rain-and-you", "Season 2 OST")
            )
        ),
        OstFranchise(
            id = "beloved_summer_moon_lovers",
            title = "Our Beloved Summer & Moon Lovers",
            category = "K-Drama",
            subtitle = "V (BTS), EXO-CBX, Loco & Punch",
            songs = listOf(
                OstSongItem("Christmas Tree", "V", "v", "christmas-tree", "Our Beloved Summer OST"),
                OstSongItem("For You", "EXO-CBX", "exo-cbx", "for-you", "Moon Lovers OST Part 1"),
                OstSongItem("Say Yes", "Loco & Punch", "loco-punch", "say-yes", "Moon Lovers OST Part 2")
            )
        ),

        // ==========================================
        // 4. HOLLYWOOD & DISNEY
        // ==========================================
        OstFranchise(
            id = "little_mermaid_cars",
            title = "The Little Mermaid, Cars & Brave",
            category = "Hollywood & Disney",
            subtitle = "Jodi Benson, Rascal Flatts, Julie Fowlis, Cliff Edwards",
            songs = listOf(
                OstSongItem("Part of Your World", "Jodi Benson", "jodi-benson", "part-of-your-world", "Ariel Main Anthem"),
                OstSongItem("Life Is a Highway", "Rascal Flatts", "rascal-flatts", "life-is-a-highway", "Cars Main Track"),
                OstSongItem("Touch the Sky", "Julie Fowlis", "julie-fowlis", "touch-the-sky", "Disney Pixar's Brave"),
                OstSongItem("When You Wish Upon a Star", "Cliff Edwards", "cliff-edwards", "when-you-wish-upon-a-star", "Pinocchio Classic Theme")
            )
        ),
        OstFranchise(
            id = "greatest_showman",
            title = "The Greatest Showman",
            category = "Hollywood & Disney",
            subtitle = "Keala Settle, Hugh Jackman, Loren Allred, Zendaya",
            songs = listOf(
                OstSongItem("This Is Me", "Keala Settle", "keala-settle", "this-is-me", "Main Anthem"),
                OstSongItem("A Million Dreams", "Hugh Jackman", "hugh-jackman", "a-million-dreams", "Soundtrack"),
                OstSongItem("Rewrite the Stars", "Zendaya", "zendaya", "rewrite-the-stars", "Soundtrack"),
                OstSongItem("Never Enough", "Loren Allred", "loren-allred", "never-enough", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "frozen",
            title = "Disney's Frozen (1 & 2)",
            category = "Hollywood & Disney",
            subtitle = "Idina Menzel, Kristen Bell, Josh Gad",
            songs = listOf(
                OstSongItem("Do You Want to Build a Snowman?", "Kristen Bell", "kristen-bell", "do-you-want-to-build-a-snowman", "Anna Solo"),
                OstSongItem("For the First Time in Forever", "Kristen Bell & Idina Menzel", "kristen-bell-idina-menzel", "for-the-first-time-in-forever", "Anna & Elsa Duet"),
                OstSongItem("Love Is an Open Door", "Kristen Bell & Santino Fontana", "kristen-bell-santino-fontana", "love-is-an-open-door", "Anna & Hans Duet"),
                OstSongItem("Let It Go", "Idina Menzel", "idina-menzel", "let-it-go", "Main Anthem - Oscar Winner"),
                OstSongItem("In Summer", "Josh Gad", "josh-gad", "in-summer", "Olaf Song"),
                OstSongItem("Fixer Upper", "Frozen Cast", "frozen-cast", "fixer-upper", "Trolls Song"),
                OstSongItem("Into the Unknown", "Idina Menzel", "idina-menzel", "into-the-unknown", "Frozen 2 Theme"),
                OstSongItem("Lost in the Woods", "Jonathan Groff", "jonathan-groff", "lost-in-the-woods", "Kristoff (Frozen 2)")
            )
        ),
        OstFranchise(
            id = "aladdin",
            title = "Disney's Aladdin",
            category = "Hollywood & Disney",
            subtitle = "Peabo Bryson, Naomi Scott, Robin Williams",
            songs = listOf(
                OstSongItem("A Whole New World", "Peabo Bryson", "peabo-bryson", "a-whole-new-world", "Classic Theme"),
                OstSongItem("Speechless", "Naomi Scott", "naomi-scott", "speechless", "Live Action Theme"),
                OstSongItem("Friend Like Me", "Robin Williams", "robin-williams", "friend-like-me", "Genie Anthem"),
                OstSongItem("Prince Ali", "Robin Williams", "robin-williams", "prince-ali", "Iconic March"),
                OstSongItem("One Jump Ahead", "Brad Kane", "brad-kane", "one-jump-ahead", "Soundtrack"),
                OstSongItem("Arabian Nights", "Bruce Adler", "bruce-adler", "arabian-nights", "Opening Theme")
            )
        ),
        OstFranchise(
            id = "tarzan",
            title = "Disney's Tarzan",
            category = "Hollywood & Disney",
            subtitle = "Phil Collins - Oscar Winning Soundtrack",
            songs = listOf(
                OstSongItem("You'll Be In My Heart", "Phil Collins", "phil-collins", "youll-be-in-my-heart", "Academy Award Winner"),
                OstSongItem("Strangers Like Me", "Phil Collins", "phil-collins", "strangers-like-me", "Soundtrack"),
                OstSongItem("Son of Man", "Phil Collins", "phil-collins", "son-of-man", "Soundtrack"),
                OstSongItem("Two Worlds", "Phil Collins", "phil-collins", "two-worlds", "Opening Theme")
            )
        ),
        OstFranchise(
            id = "lion_king",
            title = "Disney's The Lion King",
            category = "Hollywood & Disney",
            subtitle = "Elton John, Nathan Lane, Jason Weaver",
            songs = listOf(
                OstSongItem("Circle Of Life", "Elton John", "elton-john", "circle-of-life", "Opening Anthem"),
                OstSongItem("Hakuna Matata", "Nathan Lane", "nathan-lane", "hakuna-matata", "Timon & Pumbaa"),
                OstSongItem("I Just Can't Wait to Be King", "Jason Weaver", "jason-weaver", "i-just-can-t-wait-to-be-king", "Young Simba")
            )
        ),
        OstFranchise(
            id = "beauty_and_the_beast",
            title = "Disney's Beauty and the Beast",
            category = "Hollywood & Disney",
            subtitle = "Celine Dion, Peabo Bryson, Cast",
            songs = listOf(
                OstSongItem("Beauty And The Beast", "Celine Dion", "celine-dion", "beauty-and-the-beast", "Oscar Winner"),
                OstSongItem("Be Our Guest", "Beauty and the Beast Cast", "beauty-and-the-beast-cast", "be-our-guest", "Lumiere & Castle")
            )
        ),
        OstFranchise(
            id = "tangled",
            title = "Disney's Tangled (Rapunzel)",
            category = "Hollywood & Disney",
            subtitle = "Mandy Moore, Donna Murphy, Cast",
            songs = listOf(
                OstSongItem("When Will My Life Begin", "Mandy Moore", "mandy-moore", "when-will-my-life-begin", "Opening Song"),
                OstSongItem("Mother Knows Best", "Donna Murphy", "donna-murphy", "mother-knows-best", "Mother Gothel"),
                OstSongItem("I've Got a Dream", "Tangled Cast", "tangled-cast", "i-ve-got-a-dream", "Snuggly Duckling"),
                OstSongItem("I See The Light", "Mandy Moore", "mandy-moore", "i-see-the-light", "Lantern Scene Duet")
            )
        ),
        OstFranchise(
            id = "toy_story",
            title = "Disney Pixar's Toy Story",
            category = "Hollywood & Disney",
            subtitle = "Randy Newman",
            songs = listOf(
                OstSongItem("You've Got A Friend In Me", "Randy Newman", "randy-newman", "you-ve-got-a-friend-in-me", "Main Theme")
            )
        ),
        OstFranchise(
            id = "moana",
            title = "Disney's Moana",
            category = "Hollywood & Disney",
            subtitle = "Auli'i Cravalho, Dwayne Johnson, Lin-Manuel Miranda",
            songs = listOf(
                OstSongItem("How Far I'll Go", "Auli'i Cravalho", "auli-i-cravalho", "how-far-i-ll-go", "Main Anthem - Oscar Nom"),
                OstSongItem("You're Welcome", "Dwayne Johnson", "dwayne-johnson", "you-re-welcome", "Maui Anthem"),
                OstSongItem("We Know the Way", "Lin-Manuel Miranda & Opetaia Foa'i", "lin-manuel-miranda-opetaia-foa-i", "we-know-the-way", "Voyagers Anthem"),
                OstSongItem("I Am Moana", "Rachel House & Aulii Cravalho", "rachel-house-aulii-cravalho", "i-am-moana", "Song of the Ancestors")
            )
        ),
        OstFranchise(
            id = "encanto",
            title = "Disney's Encanto",
            category = "Hollywood & Disney",
            subtitle = "Lin-Manuel Miranda, Stephanie Beatriz, Jessica Darrow",
            songs = listOf(
                OstSongItem("The Family Madrigal", "Stephanie Beatriz & Encanto Cast", "stephanie-beatriz-encanto-cast", "the-family-madrigal", "Opening"),
                OstSongItem("Waiting On A Miracle", "Stephanie Beatriz", "stephanie-beatriz", "waiting-on-a-miracle", "Mirabel Solo"),
                OstSongItem("Surface Pressure", "Jessica Darrow", "jessica-darrow", "surface-pressure", "Luisa Solo"),
                OstSongItem("We Don't Talk About Bruno", "Lin-Manuel Miranda", "lin-manuel-miranda", "we-don-t-talk-about-bruno", "Billboard #1 Hit"),
                OstSongItem("What Else Can I Do?", "Diane Guerrero & Stephanie Beatriz", "diane-guerrero-stephanie-beatriz", "what-else-can-i-do", "Isabela & Mirabel"),
                OstSongItem("Dos Oruguitas", "Sebastian Yatra", "sebastian-yatra", "dos-oruguitas", "Oscar Nominee")
            )
        ),
        OstFranchise(
            id = "mulan",
            title = "Disney's Mulan",
            category = "Hollywood & Disney",
            subtitle = "Christina Aguilera, Donny Osmond",
            songs = listOf(
                OstSongItem("Reflection", "Christina Aguilera", "christina-aguilera", "reflection", "Main Theme"),
                OstSongItem("I'll Make a Man Out of You", "Donny Osmond", "donny-osmond", "i-ll-make-a-man-out-of-you", "Training Anthem")
            )
        ),
        OstFranchise(
            id = "hercules_pocahontas",
            title = "Disney's Hercules & Pocahontas",
            category = "Hollywood & Disney",
            subtitle = "Michael Bolton, Susan Egan, Vanessa Williams",
            songs = listOf(
                OstSongItem("Go The Distance", "Michael Bolton", "michael-bolton", "go-the-distance", "Hercules Theme"),
                OstSongItem("I Won't Say (I'm in Love)", "Susan Egan", "susan-egan", "i-won-t-say-i-m-in-love", "Megara Solo"),
                OstSongItem("Zero to Hero", "Hercules Cast", "hercules-cast", "zero-to-hero", "Muses Anthem"),
                OstSongItem("Colors Of The Wind", "Vanessa Williams", "vanessa-williams", "colors-of-the-wind", "Pocahontas Theme")
            )
        ),
        OstFranchise(
            id = "coco",
            title = "Disney Pixar's Coco",
            category = "Hollywood & Disney",
            subtitle = "Anthony Gonzalez, Gael García Bernal",
            songs = listOf(
                OstSongItem("Remember Me", "Anthony Gonzalez", "anthony-gonzalez", "remember-me", "Oscar Winner"),
                OstSongItem("Un Poco Loco", "Anthony Gonzalez & Gael Garcia Bernal", "anthony-gonzalez-gael-garcia-bernal", "un-poco-loco", "Talent Show Duet"),
                OstSongItem("Proud Corazon", "Anthony Gonzalez", "anthony-gonzalez", "proud-corazon", "Finale")
            )
        ),
        OstFranchise(
            id = "shrek",
            title = "Shrek (1 & 2)",
            category = "Hollywood & Disney",
            subtitle = "Smash Mouth, Counting Crows, Rufus Wainwright, Bonnie Tyler",
            songs = listOf(
                OstSongItem("All Star", "Smash Mouth", "smash-mouth", "all-star", "Opening Anthem"),
                OstSongItem("Im A Believer", "Smash Mouth", "smash-mouth", "im-a-believer", "Ending Celebration"),
                OstSongItem("Accidentally In Love", "Counting Crows", "counting-crows", "accidentally-in-love", "Shrek 2 Honeymoon"),
                OstSongItem("Holding Out For A Hero", "Bonnie Tyler", "bonnie-tyler", "holding-out-for-a-hero", "Fairy Godmother Climax"),
                OstSongItem("Hallelujah", "Rufus Wainwright", "rufus-wainwright", "hallelujah", "Emotional Montage")
            )
        ),
        OstFranchise(
            id = "lotr_hobbit",
            title = "The Lord of the Rings & The Hobbit",
            category = "Hollywood & Disney",
            subtitle = "Enya, Ed Sheeran, Annie Lennox, Howard Shore",
            songs = listOf(
                OstSongItem("May It Be", "Enya", "enya", "may-it-be", "Fellowship of the Ring"),
                OstSongItem("Into the West", "Annie Lennox", "annie-lennox", "into-the-west", "Return of the King (Oscar Winner)"),
                OstSongItem("Misty Mountains", "Richard Armitage", "richard-armitage", "misty-mountains", "The Hobbit: An Unexpected Journey"),
                OstSongItem("I See Fire", "Ed Sheeran", "ed-sheeran", "i-see-fire", "The Desolation of Smaug")
            )
        ),
        OstFranchise(
            id = "zootopia",
            title = "Disney's Zootopia",
            category = "Hollywood & Disney",
            subtitle = "Shakira",
            songs = listOf(
                OstSongItem("Try Everything", "Shakira", "shakira", "try-everything", "Main Theme")
            )
        ),
        OstFranchise(
            id = "rudderless",
            title = "Rudderless (Billy Crudup)",
            category = "Hollywood & Disney",
            subtitle = "Billy Crudup, Selena Gomez, Ben Kweller",
            songs = listOf(
                OstSongItem("Sing Along", "Billy Crudup", "billy-crudup", "sing-along", "Soundtrack"),
                OstSongItem("Home", "Billy Crudup", "billy-crudup", "home", "Soundtrack"),
                OstSongItem("Real Friends", "Billy Crudup", "billy-crudup", "real-friends", "Soundtrack"),
                OstSongItem("Stay With You", "Billy Crudup", "billy-crudup", "stay-with-you", "Soundtrack"),
                OstSongItem("Over Your Shoulder", "Billy Crudup", "billy-crudup", "over-your-shoulder", "Soundtrack"),
                OstSongItem("Asshole", "Billy Crudup", "billy-crudup", "asshole", "Soundtrack"),
                OstSongItem("Hold On", "Selena Gomez", "selena-gomez", "hold-on", "Duet with Ben Kweller")
            )
        ),
        OstFranchise(
            id = "almost_famous",
            title = "Almost Famous",
            category = "Hollywood & Disney",
            subtitle = "Stillwater, Elton John - Cameron Crowe Classic",
            songs = listOf(
                OstSongItem("Fever Dog", "Stillwater", "stillwater", "fever-dog", "Main Track"),
                OstSongItem("Tiny Dancer", "Elton John", "elton-john", "tiny-dancer", "Iconic Bus Scene")
            )
        ),
        OstFranchise(
            id = "high_school_musical",
            title = "High School Musical",
            category = "Hollywood & Disney",
            subtitle = "Zac Efron, Vanessa Hudgens",
            songs = listOf(
                OstSongItem("Breaking Free", "High School Musical Cast", "high-school-musical-cast", "breaking-free", "Climax Duet"),
                OstSongItem("Start of Something New", "High School Musical Cast", "high-school-musical-cast", "start-of-something-new", "Opening Duet")
            )
        ),
        OstFranchise(
            id = "a_star_is_born",
            title = "A Star Is Born",
            category = "Hollywood & Disney",
            subtitle = "Lady Gaga & Bradley Cooper",
            songs = listOf(
                OstSongItem("Shallow", "Lady Gaga", "lady-gaga", "shallow-radio-edit", "Oscar Winner"),
                OstSongItem("Always Remember Us This Way", "Lady Gaga", "lady-gaga", "always-remember-us-this-way", "Soundtrack")
            )
        ),

        OstFranchise(
            id = "titanic",
            title = "Titanic",
            category = "Hollywood & Disney",
            subtitle = "Celine Dion",
            songs = listOf(
                OstSongItem("My Heart Will Go On", "Celine Dion", "celine-dion", "my-heart-will-go-on", "Main Theme")
            )
        ),
        OstFranchise(
            id = "the_bodyguard",
            title = "The Bodyguard",
            category = "Hollywood & Disney",
            subtitle = "Whitney Houston - All-Time Best-Selling Soundtrack",
            songs = listOf(
                OstSongItem("I Will Always Love You", "Whitney Houston", "whitney-houston", "i-will-always-love-you", "Main Theme"),
                OstSongItem("I Have Nothing", "Whitney Houston", "whitney-houston", "i-have-nothing", "Oscar Nominee"),
                OstSongItem("Run to You", "Whitney Houston", "whitney-houston", "run-to-you", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "dirty_dancing",
            title = "Dirty Dancing",
            category = "Hollywood & Disney",
            subtitle = "Eric Carmen, Patrick Swayze",
            songs = listOf(
                OstSongItem("Hungry Eyes", "Eric Carmen", "eric-carmen", "hungry-eyes", "Soundtrack"),
                OstSongItem("She's Like the Wind", "Patrick Swayze", "patrick-swayze", "she-s-like-the-wind", "Soundtrack")
            )
        ),
        OstFranchise(
            id = "ghost_pretty_woman",
            title = "Ghost & Pretty Woman",
            category = "Hollywood & Disney",
            subtitle = "The Righteous Brothers, Roy Orbison",
            songs = listOf(
                OstSongItem("Unchained Melody", "The Righteous Brothers", "the-righteous-brothers", "unchained-melody", "Pottery Scene Classic"),
                OstSongItem("Oh, Pretty Woman", "Roy Orbison", "roy-orbison", "oh-pretty-woman", "Main Theme")
            )
        ),
        OstFranchise(
            id = "school_of_rock_iris",
            title = "School of Rock & City of Angels",
            category = "Hollywood & Disney",
            subtitle = "Jack Black, Goo Goo Dolls",
            songs = listOf(
                OstSongItem("School of Rock", "School of Rock Cast", "school-of-rock-cast", "school-of-rock", "Battle of the Bands"),
                OstSongItem("Iris", "Goo Goo Dolls", "goo-goo-dolls", "iris", "City of Angels Theme")
            )
        ),
        OstFranchise(
            id = "john_carney_trilogy",
            title = "Begin Again, Sing Street & Once",
            category = "Hollywood & Disney",
            subtitle = "Adam Levine, Keira Knightley, Sing Street, Glen Hansard",
            songs = listOf(
                OstSongItem("Lost Stars", "Adam Levine", "adam-levine", "lost-stars", "Begin Again Main Theme"),
                OstSongItem("Lost Stars", "Keira Knightley", "keira-knightley", "lost-stars", "Begin Again Acoustic"),
                OstSongItem("Like a Fool", "Keira Knightley", "keira-knightley", "like-a-fool", "Begin Again"),
                OstSongItem("Drive It Like You Stole It", "Sing Street", "sing-street", "drive-it-like-you-stole-it", "Sing Street Climax"),
                OstSongItem("Up", "Sing Street", "sing-street", "up", "Sing Street"),
                OstSongItem("Falling Slowly", "Glen Hansard", "glen-hansard", "falling-slowly", "Once - Oscar Winner")
            )
        ),
        OstFranchise(
            id = "la_la_land_guardians",
            title = "La La Land & Guardians of the Galaxy",
            category = "Hollywood & Disney",
            subtitle = "Ryan Gosling, Emma Stone, Blue Swede, Redbone",
            songs = listOf(
                OstSongItem("City of Stars", "Ryan Gosling & Emma Stone", "ryan-gosling-emma-stone", "city-of-stars", "Oscar Winner"),
                OstSongItem("Audition (The Fools Who Dream)", "Emma Stone", "emma-stone", "audition-the-fools-who-dream", "La La Land"),
                OstSongItem("Hooked on a Feeling", "Blue Swede", "blue-swede", "hooked-on-a-feeling", "Awesome Mix Vol. 1"),
                OstSongItem("Come and Get Your Love", "Redbone", "redbone", "come-and-get-your-love", "Awesome Mix Vol. 1")
            )
        ),
        OstFranchise(
            id = "fast_furious",
            title = "Fast & Furious 7",
            category = "Hollywood & Disney",
            subtitle = "Wiz Khalifa ft. Charlie Puth",
            songs = listOf(
                OstSongItem("See You Again", "Wiz Khalifa", "wiz-khalifa", "see-you-again", "Paul Walker Tribute")
            )
        ),
        OstFranchise(
            id = "twilight",
            title = "The Twilight Saga",
            category = "Hollywood & Disney",
            subtitle = "Christina Perri, Paramore, Sleeping At Last",
            songs = listOf(
                OstSongItem("A Thousand Years", "Christina Perri", "christina-perri", "a-thousand-years", "Breaking Dawn Theme"),
                OstSongItem("Decode", "Paramore", "paramore", "decode", "Twilight Theme"),
                OstSongItem("Turning Page", "Sleeping At Last", "sleeping-at-last", "turning-page", "Breaking Dawn")
            )
        ),
        OstFranchise(
            id = "top_gun",
            title = "Top Gun & Maverick",
            category = "Hollywood & Disney",
            subtitle = "Kenny Loggins, Berlin, OneRepublic",
            songs = listOf(
                OstSongItem("Danger Zone", "Kenny Loggins", "kenny-loggins", "danger-zone", "Top Gun Theme"),
                OstSongItem("Take My Breath Away", "Berlin", "berlin", "take-my-breath-away", "Love Theme"),
                OstSongItem("I Ain't Worried", "OneRepublic", "onerepublic", "i-ain-t-worried", "Maverick Beach Scene")
            )
        ),
        OstFranchise(
            id = "notting_hill",
            title = "Notting Hill",
            category = "Hollywood & Disney",
            subtitle = "Ronan Keating, Elvis Costello",
            songs = listOf(
                OstSongItem("When You Say Nothing At All", "Ronan Keating", "ronan-keating", "when-you-say-nothing-at-all", "Main Theme"),
                OstSongItem("She", "Elvis Costello", "elvis-costello", "she", "Opening Theme")
            )
        ),
        OstFranchise(
            id = "armageddon",
            title = "Armageddon",
            category = "Hollywood & Disney",
            subtitle = "Aerosmith",
            songs = listOf(
                OstSongItem("I Don't Want to Miss a Thing", "Aerosmith", "aerosmith", "i-dont-want-to-miss-a-thing", "Main Theme")
            )
        ),
        OstFranchise(
            id = "spider_man",
            title = "Spider-Man",
            category = "Hollywood & Disney",
            subtitle = "Post Malone, Chad Kroeger, Dashboard Confessional",
            songs = listOf(
                OstSongItem("Sunflower", "Post Malone", "post-malone", "sunflower", "Spider-Verse Theme"),
                OstSongItem("Hero", "Chad Kroeger", "chad-kroeger", "hero", "Spider-Man Theme"),
                OstSongItem("Vindicated", "Dashboard Confessional", "dashboard-confessional", "vindicated", "Spider-Man 2 Theme")
            )
        ),
        OstFranchise(
            id = "barbie",
            title = "Barbie (2023)",
            category = "Hollywood & Disney",
            subtitle = "Billie Eilish, Ryan Gosling, Dua Lipa",
            songs = listOf(
                OstSongItem("What Was I Made For?", "Billie Eilish", "billie-eilish", "what-was-i-made-for", "Oscar Winner"),
                OstSongItem("I'm Just Ken", "Ryan Gosling", "ryan-gosling", "i-m-just-ken", "Ken Anthem - Oscar Nominee"),
                OstSongItem("Dance the Night", "Dua Lipa", "dua-lipa", "dance-the-night", "Party Theme")
            )
        ),

        OstFranchise(
            id = "james_bond",
            title = "James Bond (007)",
            category = "Hollywood & Disney",
            subtitle = "Adele, Billie Eilish",
            songs = listOf(
                OstSongItem("Skyfall", "Adele", "adele", "skyfall", "Skyfall Theme"),
                OstSongItem("No Time to Die", "Billie Eilish", "billie-eilish", "no-time-to-die", "No Time to Die Theme")
            )
        ),
        OstFranchise(
            id = "rocky_ghostbusters",
            title = "Rocky & Ghostbusters",
            category = "Hollywood & Disney",
            subtitle = "Survivor, Ray Parker Jr.",
            songs = listOf(
                OstSongItem("Eye of the Tiger", "Survivor", "survivor", "eye-of-the-tiger", "Rocky III Theme"),
                OstSongItem("Ghostbusters", "Ray Parker Jr.", "ray-parker-jr", "ghostbusters", "Ghostbusters Theme")
            )
        )
    )
}
