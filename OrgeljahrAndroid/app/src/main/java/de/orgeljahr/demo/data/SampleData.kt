package de.orgeljahr.demo.data

object SampleData {
    private val bach137 = OrganWork(
        composer = "Johann Sebastian Bach",
        title = "Lobe den Herren, den mächtigen König der Ehren",
        catalog = "BWV 137",
        category = "Kantate",
        relation = "Kantate zum 12. Sonntag nach Trinitatis",
        duration = "ca. 18 Min."
    )

    private val bach69a = OrganWork(
        composer = "Johann Sebastian Bach",
        title = "Lobe den Herrn, meine Seele",
        catalog = "BWV 69a",
        category = "Kantate",
        relation = "Für den 12. Sonntag nach Trinitatis komponiert",
        duration = "ca. 22 Min."
    )

    private val bux213 = OrganWork(
        composer = "Dieterich Buxtehude",
        title = "Nun lob, mein Seel, den Herren",
        catalog = "BuxWV 213",
        category = "Choralvorspiel",
        relation = "Direkter Bezug zum Wochenlied EG 289",
        duration = "ca. 3 Min."
    )

    private val walther89 = OrganWork(
        composer = "Johann Gottfried Walther",
        title = "Nun lob, mein Seel, den Herren",
        catalog = "LV 89",
        category = "Choralvorspiel",
        relation = "Cantus firmus des Wochenliedes",
        duration = "ca. 4 Min."
    )

    val week = LiturgicalWeek(
        dateLabel = "Sonntag, 23. August 2026",
        isoDate = "2026-08-23T10:00",
        title = "12. Sonntag nach Trinitatis",
        subtitle = "Heilung und neues Hören",
        colorName = "Grün",
        weeklyVerse = "",
        readings = listOf("Jesaja 29,17–24", "Apg 9,1–20", "Markus 7,31–37"),
        hymnNumbers = listOf("289"),
        bachWorks = listOf(bach137, bach69a),
        otherWorks = listOf(bux213, walther89)
    )

    val hymns = listOf(
        Hymn(
            egNumber = "1",
            title = "Macht hoch die Tür",
            tune = "MACHT HOCH DIE TÜR",
            textAuthor = "Georg Weissel",
            tuneSource = "Halle 1704",
            year = "1623 / 1704",
            history = "Ein Adventslied nach Psalm 24. Die heute verbreitete Melodie erschien zu Beginn des 18. Jahrhunderts in Halle.",
            verses = listOf("Macht hoch die Tür, die Tor macht weit; es kommt der Herr der Herrlichkeit, ein König aller Königreich, ein Heiland aller Welt zugleich."),
            themes = listOf("Advent", "Psalm 24", "Einzug"),
            relatedWorks = listOf(
                OrganWork(
                    composer = "Max Reger",
                    title = "Macht hoch die Tür",
                    catalog = "op. 135a, 5",
                    category = "Choralvorspiel",
                    relation = "Gleicher Choral",
                    duration = "ca. 3 Min."
                )
            )
        ),
        Hymn(
            egNumber = "85",
            title = "O Haupt voll Blut und Wunden",
            tune = "HERZLICH TUT MICH VERLANGEN",
            textAuthor = "Paul Gerhardt",
            tuneSource = "Hans Leo Hassler / Görlitz 1613",
            year = "1656",
            history = "Paul Gerhardts Passionslied wurde mit einer ursprünglich weltlichen Melodie Hans Leo Hasslers verbunden.",
            verses = listOf("O Haupt voll Blut und Wunden, voll Schmerz und voller Hohn, o Haupt, zum Spott gebunden mit einer Dornenkron."),
            themes = listOf("Passion", "Karfreitag", "Christus"),
            relatedWorks = listOf(
                OrganWork(
                    composer = "Johann Sebastian Bach",
                    title = "Matthäus-Passion",
                    catalog = "BWV 244",
                    category = "Passion",
                    relation = "Mehrfache Choralsätze dieser Melodie",
                    duration = "ca. 170 Min."
                )
            )
        ),
        Hymn(
            egNumber = "289",
            title = "Nun lob, mein Seel, den Herren",
            tune = "NUN LOB, MEIN SEEL, DEN HERREN",
            textAuthor = "Johann Gramann",
            tuneSource = "Hans Kugelmann, 1540",
            year = "1540",
            history = "Eine freie Nachdichtung von Psalm 103. Das Lied gehört zu den prägenden deutschen Psalmgesängen der Reformationszeit.",
            verses = listOf("Nun lob, mein Seel, den Herren, was in mir ist, den Namen sein. Sein Wohltat tut er mehren, vergiss es nicht, o Herze mein."),
            themes = listOf("Lob", "Psalm 103", "Dank"),
            relatedWorks = listOf(bux213, walther89)
        )
    )

    val templates = listOf(
        ServiceTemplate(
            name = "Evangelischer Hauptgottesdienst",
            sections = listOf(
                "Orgelvorspiel" to SectionType.ORGAN,
                "Eingangslied" to SectionType.HYMN,
                "Kyrie" to SectionType.LITURGY,
                "Gloria" to SectionType.HYMN,
                "Wochenlied" to SectionType.HYMN,
                "Musik zur Predigt" to SectionType.ORGAN,
                "Abendmahl" to SectionType.LITURGY,
                "Schlusslied" to SectionType.HYMN,
                "Orgelnachspiel" to SectionType.ORGAN
            )
        ),
        ServiceTemplate(
            name = "Leerer Ablauf",
            sections = emptyList()
        )
    )
}
