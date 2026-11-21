package com.blazify.innertube.videoart

/**
 * Search answers recorded 2026-11-22 (hl=en, gl=NP) for the songs the finder was tuned on: the first
 * ten YouTube Music video rows and the first fifteen everyday YouTube videos, as the finder reads them.
 */
@Suppress("ktlint:standard:max-line-length")
object VideoArtSamples {
    class Sample(val song: VideoArtSong, val music: List<MusicVideoRow>, val web: List<WebVideoRow>)

    val dynamite =
        Sample(
            VideoArtSong("Dynamite", listOf("BTS"), 200),
            listOf(
                MusicVideoRow("gdZLi9oWNZg", "Dynamite", "BTS", "MUSIC_VIDEO_TYPE_OMV", 2100000000L, 224),
                MusicVideoRow("vSfhZXGw1JA", "BTS(방탄소년단) - Dynamite (2020 KBS Song Festival) I KBS WORLD TV 201218", "KBS WORLD TV", "MUSIC_VIDEO_TYPE_UGC", 11000000L, 238),
                MusicVideoRow("_K2v-MmAj7E", "[TinyTAN | CLIP] - Dynamite", "BTS", "MUSIC_VIDEO_TYPE_OFFICIAL_SOURCE_MUSIC", 80000000L, 44),
                MusicVideoRow("tYWaZ9XVxkA", "BLACKPINK reaction to BTS \"Butter\" performance", "BLACK BANGTAN", "MUSIC_VIDEO_TYPE_UGC", 34000000L, 140),
                MusicVideoRow("5iEIExYvCUo", "Dynamite (BTS) Multi-Language Cover in 16 Different Languages - Travys Kim", "Travys Kim", "MUSIC_VIDEO_TYPE_UGC", 1600000L, 202),
                MusicVideoRow("uBZXCkwCKxw", "[CHOREOGRAPHY] BTS (방탄소년단) 2020 MMA 'Dynamite' Dance Break Practice", "BTS", "MUSIC_VIDEO_TYPE_OMV", 62000000L, 231),
                MusicVideoRow("YW7JVCiDgJk", "BTS (방탄소년단) 'Dynamite' @ FNS MUSIC FESTIVAL", "BTS", "MUSIC_VIDEO_TYPE_OMV", 86000000L, 198),
                MusicVideoRow("WMweEpGlu_U", "Butter", "BTS", "MUSIC_VIDEO_TYPE_OMV", 1100000000L, 183),
                MusicVideoRow("kwAhKmSWdx8", "'Dynamite' by V | BTS WORLD TOUR ‘ARIRANG’", "BTS", "MUSIC_VIDEO_TYPE_OMV", 2900000L, 210),
                MusicVideoRow("NO2WL6uFak8", "BTS dynamite: Jungkook cut sences (shot by Kim Teahung /V)", "cloudykooky", "MUSIC_VIDEO_TYPE_UGC", 2300L, 72),
            ),
            listOf(
                WebVideoRow("gdZLi9oWNZg", "BTS (방탄소년단) 'Dynamite' Official MV", "HYBE LABELS", false, true, 2169464543L, 224),
                WebVideoRow("kK29Q_LpVUw", "BTS - Dynamite (Lyrics)", "7clouds K-pop", false, false, 2046174L, 206),
                WebVideoRow("e81ad5MpfQ0", "BTS (방탄소년단) 'Dynamite' @ America's Got Talent 2020", "BANGTANTV", true, true, 174327546L, 202),
                WebVideoRow("c4V0FNZfEv0", "BTS (방탄소년단) 'Dynamite' @ Good Morning America", "BANGTANTV", true, true, 134200015L, 201),
                WebVideoRow("E3nWu1VgRJU", "BTS | FIFA World Cup 2026™ Final Halftime Show | Full Performance", "FIFA and BANGTANTV", false, false, 57561120L, 111),
                WebVideoRow("BflFNMl_UWY", "BTS (방탄소년단) 'Dynamite' Official MV (Choreography ver.)", "HYBE LABELS", false, true, 309094621L, 206),
                WebVideoRow("jWRMXiHhDjc", "BTS (방탄소년단) 'Dynamite' @ 63rd GRAMMY Awards Show", "BANGTANTV", true, true, 95740762L, 181),
                WebVideoRow("Mzk6KkyxXC8", "BTS - Dynamite (Lyrics)", "Cakes & Eclairs", false, true, 17053046L, 199),
                WebVideoRow("MoOSVrd4S-Q", "FANCAM 260321 Dynamite BTS The Comeback Live Arirang Gwanghwamun Concert 방탄소년단", "theultimatedodo", false, true, 4154348L, 216),
                WebVideoRow("J8fQMq4T8Yg", "BTS: Dynamite", "The Late Late Show with James Corden", false, true, 82450759L, 229),
                WebVideoRow("CN4fffh7gmk", "[CHOREOGRAPHY] BTS (방탄소년단) 'Dynamite' Dance Practice", "BANGTANTV", true, true, 145321742L, 206),
                WebVideoRow("BV2FdDmGiW0", "BTS (방탄소년단) 'Dynamite' Official MV (B-side)", "HYBE LABELS", false, true, 210794148L, 228),
                WebVideoRow("WhDsAW1ZzZ8", "[CHOREOGRAPHY] BTS (방탄소년단) ‘Dynamite’ Dance Practice (Cute & Lovely ver.) #2021BTSFESTA", "BANGTANTV", true, true, 64058719L, 206),
                WebVideoRow("4rGbKEFE720", "BTS Dynamite Lyrics", "Lemonade", false, true, 2670768L, 200),
                WebVideoRow("cq7ivv1LbvI", "BTS: Yet To Come - Dynamite | Amazon Prime", "Prime Video México", false, true, 3990959L, 229),
            ),
        )

    val rollingInTheDeep =
        Sample(
            VideoArtSong("Rolling in the Deep", listOf("Adele"), 229),
            listOf(
                MusicVideoRow("sXJ5Lm8bI4I", "Adele - Rolling In The Deep (Acapella)(Tradução/Legenda)", "ANGELS", "MUSIC_VIDEO_TYPE_UGC", 10000000L, 225),
                MusicVideoRow("kkFmaCTRb6w", "Rolling In The Deep (Live on Letterman)", "Adele", "MUSIC_VIDEO_TYPE_OMV", 7500000L, 242),
                MusicVideoRow("fpez7uwqSac", "Go Radio - \"Rolling In The Deep\" (Adele Cover) Lyric Video - Punk Goes Pop 4", "Fearless Records", "MUSIC_VIDEO_TYPE_UGC", 10000000L, 238),
                MusicVideoRow("QADBxqwV-9k", "ADELE - Rolling in the Deep (Lyrics -Türkçe Çeviri)", "Funk Craft", "MUSIC_VIDEO_TYPE_UGC", 83000L, 228),
                MusicVideoRow("yfkICas8YYE", "Adele - Rolling In The Deep  (自制 - 中英文對照歌詞)", "mikoyip313", "MUSIC_VIDEO_TYPE_UGC", 1000000L, 227),
                MusicVideoRow("G5eTh5lHF5M", "Jackie Chan Rolling in the Deep - Adele", "Retro", "MUSIC_VIDEO_TYPE_UGC", 3900000L, 185),
                MusicVideoRow("QNVBLxngwhc", "MASSIVE Crowd Goes CRAZY For This Song | Adele - Rolling In The Deep", "Luke Silva", "MUSIC_VIDEO_TYPE_OMV", 136000L, 232),
                MusicVideoRow("t0F80Dhd0aY", "Adele - Rolling in the Deep (New Orleans Bounce Mix)", "James Editor", "MUSIC_VIDEO_TYPE_UGC", 8500000L, 211),
                MusicVideoRow("0AS_tT6wTb0", "Adele - Rolling In The Deep (Worst Behaviour & We Dem Boyz Bootleg)", "South African MusiQ", "MUSIC_VIDEO_TYPE_UGC", 39000L, 307),
                MusicVideoRow("v0RWnHOce44", "Rolling in the Deep - Sub español 2021", "CARLOS CARRASCO", "MUSIC_VIDEO_TYPE_UGC", 10000000L, 238),
            ),
            listOf(
                WebVideoRow("rYEDA3JcQqw", "Adele - Rolling in the Deep (Official Music Video)", "Adele", true, true, 2959308317L, 234),
                WebVideoRow("1CigMPtm4FM", "adele - Rolling in the deep ll LYRICS", "MUSICRAFT", false, false, 5212121L, 231),
                WebVideoRow("2kRaPCY70-U", "Adele - Rolling In The Deep (Tradução/Legendado) (Live An Audience With Adele)", "M.T.V Traduções", false, false, 14321257L, 242),
                WebVideoRow("1_S0JYS4KT4", "Adele - Rolling in the deep - LIVE  3FM", "TheTudodetudo", false, false, 419980L, 250),
                WebVideoRow("AIYpdjQVidc", "Rolling In The Deep - Adele (Lyrics) 🎵", "Pillow", false, false, 73426254L, 247),
                WebVideoRow("PaEhQ51BvSw", "Adele - Rolling in the deep (Live Royal Albert Hall)", "vancel2005", false, false, 31010545L, 309),
                WebVideoRow("bDtjO-R0QSo", "Adele - Rolling In The Deep", "Adele - Topic", false, false, 4374900L, 228),
                WebVideoRow("Mi2cURoOAYY", "someone like you, Rolling in the deep - Adele   Live at the Royal Albert Hall", "Tatan Wonka", false, false, 155573681L, 772),
                WebVideoRow("dHtwZ07N1ic", "Rolling In The Deep Cover [Live from iTunes Festival 2011] - Linkin Park", "Linkin Park", true, true, 68425118L, 265),
                WebVideoRow("q2gCMx8Jabg", "Rolling In The Deep by ADELE----(Live at Royal Albert Hall)", "wuffkey", false, false, 16191602L, 312),
                WebVideoRow("B3O1OlTWXSA", "Rolling in The Deep - Adele (Karaoke Songs With Lyrics - Original Key)", "Musisi Karaoke", false, true, 29107506L, 262),
                WebVideoRow("SsIKtPZ2HRQ", "Rolling in the deep - ADELE - NMIXX Lily and Haewon COVER ", "Bae", false, false, 358947L, 226),
                WebVideoRow("G5eTh5lHF5M", "Jackie Chan Rolling in the Deep - Adele", "Retro", false, false, 3942783L, 185),
                WebVideoRow("LOd3u6FtkAk", "Adele - Rolling In The Deep (Karaoke Version)", "Sing King", false, true, 30243814L, 247),
                WebVideoRow("yXWxgpUyMtU", "Adele - Rolling In The Deep (Lyrics)", "Deep Ocean", false, false, 2018091L, 229),
            ),
        )

    val chaleya =
        Sample(
            VideoArtSong("Chaleya", listOf("Arijit Singh", "Shilpa Rao"), 201),
            listOf(
                MusicVideoRow("VAdGW7QDJiU", "Chaleya", "Aug 13, 2023", "MUSIC_VIDEO_TYPE_PODCAST_EPISODE", 0L, 0),
                MusicVideoRow("utNv4y-l9xo", "Chaleya ❤️ | Jawan | Shah Rukh Khan, Nayanthara | Arijit Singh | Lyrics Video", "@SoulfulLyricsOfficial", "MUSIC_VIDEO_TYPE_UGC", 902L, 189),
                MusicVideoRow("vk5Hc9tw2kw", "Chaleya ❤️ | Shah Rukh Khan | Jawan | Romantic Love Song | Hindi Song 2026", "Aksara Lyrics", "MUSIC_VIDEO_TYPE_UGC", 758L, 69),
                MusicVideoRow("EzLUqrAyqEU", "Chaleya (Slowed + Reverb) | Arijit Singh & Shilpa Rao | Lofi Vibes", "Midnight Serene", "MUSIC_VIDEO_TYPE_UGC", 722L, 222),
                MusicVideoRow("AF96gOnLrpE", "Chaleya - Anirudh Ravichander, Arijit Singh, Shilpa Rao | Lyrics | D'Tunes", "DHAIRYA SHAH", "MUSIC_VIDEO_TYPE_UGC", 756L, 201),
                MusicVideoRow("qflosUI7pdg", "Chaleya song Arijit Singh", "Alen khan", "MUSIC_VIDEO_TYPE_UGC", 99L, 74),
                MusicVideoRow("oZ2USpUeJII", "Chaleya- Slowed & Reverb | Lofi | SRK | JAWAN | Arijit Singh | Nayanthara", "Prabhu Audio Lofi", "MUSIC_VIDEO_TYPE_UGC", 1800L, 220),
                MusicVideoRow("lmnWOw-iwYw", "Jawan: Chaleya - Lyrics (Hindi) | Shah Rukh Khan | Nayanthara | Atlee | Anirudh | Arijit S, Shilpa R", "B9X Music", "MUSIC_VIDEO_TYPE_UGC", 249L, 216),
                MusicVideoRow("Ognq16f-wpI", "Chaleya Song ❤️ | Shah Rukh Khan & Nayanthara | Jawan Movie | Arijit Singh&ShilpaRao |Bollywood Song", "Sajan Ke Geet Studio", "MUSIC_VIDEO_TYPE_UGC", 815L, 188),
                MusicVideoRow("k99MHjerimg", "Chaleya | Jawan | Shah Rukh Khan & Nayanthara | Arijit Singh | Bollywood Romantic Love Song 2026", "K.S 8d Audio", "MUSIC_VIDEO_TYPE_UGC", 309L, 185),
            ),
            listOf(
                WebVideoRow("VAdGW7QDJiU", "JAWAN: Chaleya (Hindi) | Shah Rukh Khan | Nayanthara | Atlee | Anirudh | Arijit S, Shilpa R | Kumaar", "T-Series", false, true, 683158806L, 188),
                WebVideoRow("Bi7sSC046dk", "Chaleya (Lyrics) - Jawan | Shah Rukh Khan | Nayanthara | Atlee, Anirudh | Arijit Singh | Shilpa Rao", "Love & Lyrics", false, false, 16545602L, 188),
                WebVideoRow("wr9M-CoxP7A", "Jawan: Chaleya (Lyrical Video) | Shah Rukh Khan | Nayanthara | Atlee | Anirudh | Arijit S, Shilpa R", "T-Series", false, true, 34690278L, 221),
                WebVideoRow("86WGLnJ8tCw", "Chaleya | Jawan | Arijit Singh | Live In Concert | Barabati Stadium | Cuttack", "Figments of Imagination", false, false, 10974L, 220),
                WebVideoRow("SgGj8VPzwEI", "ICC WORLD CUP 2023 | CRICKET  | ARIJIT SINGH |  AHMEDABAD | HEERIYE & CHALEYA SONG |", "victoryverse ", false, false, 48608L, 125),
                WebVideoRow("Ah6dEARljtE", "Jawan: Chaleya 8K Full Song | Shah Rukh Khan | Nayanthara | Atlee | Anirudh | Arijit S, Shilpa R", "T-Series", false, true, 2019704L, 187),
                WebVideoRow("6hjH1-2Dy1M", "Arijit Singh, Shilpa Rao - Chaleya (Lyrics) | JAWAN | Nayanthara | Shah Rukh Khan | Anirudh | kumaar", "MusicLoudz India", false, false, 13200L, 201),
                WebVideoRow("F_jU1KI82kw", "Jawan: Chaleya (Audio) | Shah Rukh Khan | Nayanthara | Atlee | Anirudh | Arijit S, Shilpa R | Kumaar", "T-Series", false, true, 4292652L, 201),
                WebVideoRow("_C3JVBw_0fk", "Chaleya (Lyrics) - Jawan || ShahRukh Khan | Nayanthara | Atlee, Anirudh | Arijit Singh | Shilpa Rao", "Ghufi Music.", false, false, 683L, 188),
                WebVideoRow("FSVt2wBIJjY", "CHALEYA -ARIJIT SINGH (Lyrics)", "Frenly Music", false, false, 1974L, 188),
            ),
        )

    val apnaBanaLe =
        Sample(
            VideoArtSong("Apna Bana Le (From \"Bhediya\")", listOf("Arijit Singh", "Sachin-Jigar"), 262),
            listOf(
                MusicVideoRow("ElZfdU54Cp8", "Apna Bana Le", "Nov 7, 2022", "MUSIC_VIDEO_TYPE_PODCAST_EPISODE", 0L, 0),
                MusicVideoRow("d2-ss1hTBc0", "Apna Bana Le - Lyrics | Full video | Bhediya | Arijit Singh ", "Delta", "MUSIC_VIDEO_TYPE_UGC", 86L, 261),
                MusicVideoRow("Ud8lrmqMtRw", "Apna Bana Le - Bhediya | Varun Dhawan, Kriti Sanon| Sachin-Jigar, Arijit Singh, Amitabh Bhattacharya", "ROCK STAR HINDI SONGS", "MUSIC_VIDEO_TYPE_UGC", 11L, 184),
                MusicVideoRow("xmkyQ3Fn1sc", "🥁Apna Bana Le (Bhediya) - Arijit Singh🥁| Drum Cover - Akash Mehar", "Akash Mehar Productions", "MUSIC_VIDEO_TYPE_UGC", 33000L, 243),
                MusicVideoRow("wUakW8zpKh8", "Slowed Reverb - Apna Bana Le Priya - Bhediya - Arijit Singh - varun Dhawan - Kriti Sanon - jigar", "Zebrox Gaming", "MUSIC_VIDEO_TYPE_UGC", 622L, 216),
                MusicVideoRow("ZSCFzmccEDc", "Apna Banale Le - Bhediya | Varun Dhawan,  Arijit Singh| Lyrics", "Aadhya Official", "MUSIC_VIDEO_TYPE_UGC", 326L, 220),
                MusicVideoRow("u2NAuswnTKs", "Apna Bana Le", "Nov 4, 2022", "MUSIC_VIDEO_TYPE_PODCAST_EPISODE", 0L, 0),
                MusicVideoRow("j83NoThRVu0", "Apna Bana Le - Bhediya | Cover by Somnath Biswas ft. Samrat Tarafdar | Sachin-Jigar, Arijit Singh,", "Gaana World", "MUSIC_VIDEO_TYPE_UGC", 1300L, 196),
                MusicVideoRow("u2kV_kuIMH4", " Apna Bana Le - Bhediya | Varun Dhawan, Kriti Sanon Sachin-Jigar, Arijit Singh, Amitabh Bhattacharya", "Lofi song 🎵", "MUSIC_VIDEO_TYPE_UGC", 6L, 189),
                MusicVideoRow("LL-EWoQa04w", "Apna Bana Le – Bhediya | Arijit Singh | Sachin - Jigar | Lyrics", "Lyric Bee", "MUSIC_VIDEO_TYPE_UGC", 469L, 265),
            ),
            listOf(
                WebVideoRow("UEvOsQBu1jY", "Apna Bana Le - Bhediya | Varun Dhawan, Kriti Sanon| Sachin-Jigar, Arijit Singh, Amitabh Bhattacharya", "Romance Rewind", false, false, 3735516L, 205),
                WebVideoRow("u2NAuswnTKs", "Apna Bana Le - Full Audio | Bhediya | Varun Dhawan, Kriti Sanon| Sachin-Jigar,Arijit Singh,Amitabh B", "Zee Music Company", false, true, 128064572L, 273),
                WebVideoRow("PYLxgPKtzZE", "Apna Bana Le (Lyrics) - Bhediya | Varun Dhawan, Kriti Sanon| Sachin-Jigar, Arijit Singh, Amitabh B", "Love & Lyrics", false, false, 863649L, 258),
                WebVideoRow("3pz_bHeZLkg", "Apna Bana Le - Arijit Singh | Bhediya | Varun Dhawan & Kriti Sanon | Sachin-Jigar | Lyrical", "Zee Music Company", false, true, 627380L, 267),
                WebVideoRow("a3Jog1X4xcc", "Arijit Singh - Apna Bana Le | Varun Dhawan, Kriti Sanon | Sachin-Jigar | Love Song", "Soulful Arijit Singh Songs", false, false, 18256L, 273),
                WebVideoRow("ElZfdU54Cp8", "Apna Bana Le - Bhediya | Varun Dhawan, Kriti Sanon| Sachin-Jigar, Arijit Singh, Amitabh Bhattacharya", "Zee Music Company", false, true, 641356248L, 205),
                WebVideoRow("BgIpNU0EnDk", "Apna Bana Le 8K Video | Arijit Singh | Bhediya | Varun Dhawan & Kriti Sanon | Sachin-Jigar, Amitabh", "Zee Music Company", false, true, 1992805L, 204),
                WebVideoRow("NR55xGziJn0", "Apna Bana le - Arijit Singh LIVE | Iplt20 2023  OPENING CEREMONY | #Aa #arijitsingh", "aLive", false, false, 530865L, 176),
                WebVideoRow("uImKMyjOfXY", "Tu Mera Koi Na Hoke Bhi Kuch Lage | Apna Bana Le | Arijit Singh | Varun Dhawan & Kriti Sanon|Bhediya", "Soulful Arijit Singh Songs", false, false, 4100222L, 205),
                WebVideoRow("6iGSghzcoO8", "Apna Bana Le (Lyrics) - Arijit Singh 🎶 | Tu mera koi na hoke bhi kuch lage✨", "Skulp Lyrics", false, false, 802395L, 324),
                WebVideoRow("3lDJZr6kbsg", "Apna Bana Le - Arijit Singh | Varun Dhawan & Kriti Sanon | Sachin-Jigar | Bhediya | Full Audio", "Soulful Arijit Singh Songs", false, false, 27232978L, 273),
                WebVideoRow("XhPO39Ctdt0", "Apna Bana Le Arijit Singh  Varun Dhawan & Kriti Sanon  Sachin-Jigar  Bhediya  Full Audio", "Ekla Music ", false, false, 2148470L, 275),
                WebVideoRow("BddP6PYo2gs", "Kesariya - Brahmāstra | Ranbir Kapoor, Alia Bhatt | Pritam | Arijit Singh | Amitabh Bhattacharya| 4K", "Sony Music India", false, true, 625154024L, 173),
                WebVideoRow("DImAfaIo37U", "Tu Mera Koyi Na... HoKe Bhi Kuchh Laage! ❤️ Arijit Singh Live Performance | Ahmedabad | IPL 2023", "PM Music", false, true, 572849L, 207),
                WebVideoRow("5tBNuf_TEfc", "Arijit Singh - Apna Bana Le | Varun Dhawan & Kriti Sanon | Sachin-Jigar | Bhediya | 8K Video", "Dil Se Bollywood 24x7", false, false, 1837061L, 204),
            ),
        )

    val phoolkoAankhama =
        Sample(
            VideoArtSong("Phoolko Aankhama", listOf("Ani Choying Drolma"), 311),
            listOf(
                MusicVideoRow("bJNLrJ7MUzM", "Ani Choying Drolma - Phoolko Aankhama [Official Lyrical Video]", "Ani Choying Drolma", "MUSIC_VIDEO_TYPE_OMV", 18000000L, 306),
                MusicVideoRow("aeR2kpSr32g", "Phool Ko Aanka Ma - Ani Choying Drolma | It's My Show with Suraj Singh Thakuri", "Kantipur TV HD", "MUSIC_VIDEO_TYPE_UGC", 5300000L, 366),
                MusicVideoRow("sBoGqVhCtjM", "Phool Ko Aankha Ma, A famous nepali song..ANI CHOYING DROLMA (Violin Rendition/ Mrinal Das Gupta)", "Mrinal Das gupta", "MUSIC_VIDEO_TYPE_UGC", 61L, 308),
                MusicVideoRow("W4-Eb-P88sk", "Phool Ko Aankha ma by Ani Choing Nepali Song", "Angela Bhujel", "MUSIC_VIDEO_TYPE_UGC", 1200L, 321),
                MusicVideoRow("JvcmNqryIFM", "Ani Choying Drolma - Phoolko Aankhama", "Aayush karn", "MUSIC_VIDEO_TYPE_UGC", 88L, 28),
                MusicVideoRow("jeZupzFjUpk", "Phool ko Aankha ma Lofi // Ani Choying Drolma / neplai lofi prod. by fukkey music.", "Fukkey Music", "MUSIC_VIDEO_TYPE_UGC", 1800L, 305),
                MusicVideoRow("q3drMYeEzSQ", "phool ko aankha ma phoolai sansara- Ani choying Dolma", "bibash thapa", "MUSIC_VIDEO_TYPE_UGC", 205L, 314),
                MusicVideoRow("f2LvWyytzlk", "Phool Ko Aankha Ma | Ani Choying Dolma | Santosh Prs Feat | Nepali Song | Cover Song", "Santosh Prs", "MUSIC_VIDEO_TYPE_UGC", 83L, 313),
                MusicVideoRow("2yeq82J7FW0", "Phool ko aankha ma phulai sansaraSong by Ani Choying Drolma #coversong #nepalisong #guitar", "DH CI", "MUSIC_VIDEO_TYPE_UGC", 1900L, 181),
                MusicVideoRow("tE-TELze85c", "Phool Ko Aankha Ma|Guitar Play By Sanjay Hitan|Ani Choying Drolma|Nhyoo Bajracharya|Nepali Hit Song", "Sanjay Hitan Official", "MUSIC_VIDEO_TYPE_UGC", 1500L, 290),
            ),
            listOf(
                WebVideoRow("bJNLrJ7MUzM", "Ani Choying Drolma - Phoolko Aankhama [Official Lyrical Video]", "Ani Choying Drolma", false, false, 18519345L, 306),
                WebVideoRow("H4Ckogg87fg", "Phoolko Akhama - Ani Choying Drolma", "hamido511", false, false, 403239L, 320),
                WebVideoRow("aeR2kpSr32g", "Phool Ko Aanka Ma - Ani Choying Drolma | It's My Show with Suraj Singh Thakuri", "Kantipur TV HD", false, true, 5342525L, 366),
                WebVideoRow("w3U5YjY18O8", "Ani Choying Drolma - Phool ko Aakha Ma", "Vien Lac", false, false, 62858L, 853),
                WebVideoRow("a_DfAfhFtPk", "Phool Ko Ankha Ma - Ani Choying Drolma | Nhyoo Bajracharya  (First time in UK)", "HAB amoment", false, false, 75805L, 475),
                WebVideoRow("ybLdS99h8yM", "Phool ko Aakha Ma - Ani Choying Dolma | Vote For Sajja Chaulagain | Nepal Idol Season 3 | AP1HD", "Nepal Idol", false, true, 823935L, 437),
                WebVideoRow("flR_h_WXI8k", "फूलको आँखामा || आनी छोइङ डोल्मा (पूर्ण वाचन) || Nepali full Audiobook || fulko aankhama ||", "Bishwa Magar Podcast - Novels & Stories. ", false, false, 9922L, 21736),
                WebVideoRow("qMbyoTqYNL8", "Phool Ko Aankha Ma Lyrics | Ani Choying Dolma | Nepali Song Lyrics🎵", "words ly (words lyrics)", false, false, 534850L, 313),
                WebVideoRow("R2JR4c91DyU", "Phoolko Ankhama.wmv", "magnumnepal", false, false, 69279L, 295),
                WebVideoRow("KMGAnZ6G8DE", "Inner peace by Ani Choeyang Dolma#buddhistsongs#amazingvoice", "Dewang123", false, false, 2665L, 3358),
                WebVideoRow("P6HzFmxTDUs", "Ani Choying Drolma explains the Words of Wisdom through her Song \"Phool ko Aankha Ma Fulai Sansara\"", "प्रेरणाकाे लागी", false, false, 141L, 796),
                WebVideoRow("nUypq6XFWr0", "Phoolko aakhama by Ani Choying Dolma | ful ko aakha ma | Np production", "Bhumika Prashant Phuyal", false, false, 174305L, 317),
                WebVideoRow("L0rajSDiP2A", "Ani Choying Drolma Phool Ko Aanka Ma | Musical Performance | PYL SHOW", "Yoho Television HD", false, true, 1809L, 319),
                WebVideoRow("2hQB8Ce_BxU", "phool ko aakhama (Ani choying drolma in Nepal festival 2012 SYDNEY)", "4K4D Vid Studio", false, false, 54808L, 412),
                WebVideoRow("wLjt2ESEh-E", "Ani Choying Drolma - Phoolko Aankhama ! Child Version Cover By Abhiyanta Mukarung Rai", "Abhiyanta Rai Official", false, false, 1425976L, 305),
            ),
        )
}
