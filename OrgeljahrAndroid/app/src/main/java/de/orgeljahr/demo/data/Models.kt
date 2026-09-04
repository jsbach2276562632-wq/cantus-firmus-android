package de.orgeljahr.demo.data

import java.util.UUID

data class OrganWork(
    val id: String = UUID.randomUUID().toString(),
    val composer: String,
    val title: String,
    val catalog: String,
    val category: String,
    val relation: String,
    val duration: String,
    val composerPersonId: String? = null
)

data class Hymn(
    val id: String = UUID.randomUUID().toString(),
    val egNumber: String,
    val title: String,
    val tune: String,
    val textAuthor: String,
    val tuneSource: String,
    val year: String,
    val history: String,
    val verses: List<String>,
    val themes: List<String>,
    val relatedWorks: List<OrganWork>,
    val hasScore: Boolean = false
)

data class MelodyHymnLink(
    val hymn: Hymn,
    val isPrimary: Boolean
)

data class Melody(
    val id: String,
    val name: String,
    val origin: String,
    val year: String,
    val linkedHymns: List<MelodyHymnLink>
)

data class LiturgicalWeek(
    val dateLabel: String,
    val isoDate: String,
    val title: String,
    val subtitle: String,
    val colorName: String,
    val weeklyVerse: String,
    val readings: List<String>,
    val hymnNumbers: List<String>,
    val bachWorks: List<OrganWork>,
    val otherWorks: List<OrganWork>
)

enum class SectionType(
    val label: String,
    val acceptsHymnNumber: Boolean = false
) {
    // A · Eröffnung und Anrufung
    ENTRANCE_MUSIC("Eingangsspiel / Musik zum Eingang"),
    ENTRANCE_HYMN("Eingangslied", acceptsHymnNumber = true),
    GREETING("Gruß"),
    RESPONSE("Antwort"),
    ADDRESS("Anrede"),
    PSALM("Psalm"),
    KYRIE("Bittruf / Kyrie", acceptsHymnNumber = true),
    VERSE("Spruch"),
    PRAYER("Gebet"),
    AMEN("Amen", acceptsHymnNumber = true),
    GLORIA("Gloria", acceptsHymnNumber = true),

    // B · Verkündigung und Beichte
    SCRIPTURE_READING("Schriftlesung"),
    SERMON("Ansprache / Predigt"),
    REFLECTION("Besinnung"),
    SILENCE("Stille"),
    CONFESSION_OF_SIN("Sündenbekenntnis"),
    CONFESSION_QUESTIONS("Beichtfragen"),
    BAPTISMAL_REMEMBRANCE("Taufgedächtnis"),
    ABSOLUTION("Stiftungsworte mit Absolution"),
    BIBLICAL_VOTUM("Biblisches Votum"),
    CREED("Glaubensbekenntnis"),

    // C · Abendmahl
    PREFACE("Lobgebet / Präfation"),
    OPENING_ACCLAMATIONS("Eröffnungsrufe"),
    SANCTUS("Sanctusgesang", acceptsHymnNumber = true),
    WORD_BEFORE_COMMUNION("Wort zum Abendmahl"),
    INSTITUTION_WORDS("Einsetzungsworte"),
    CHRIST_ACCLAMATION("Christuslob", acceptsHymnNumber = true),
    COMMUNION_PRAYER("Abendmahlsgebet"),
    LORDS_PRAYER("Gebet des Herrn / Vaterunser"),
    PEACE_GREETING("Friedensgruß"),
    AGNUS_DEI("Agnus-Dei-Gesang", acceptsHymnNumber = true),
    DISTRIBUTION("Austeilung"),
    DISTRIBUTION_WORDS("Spendewort"),
    THANKSGIVING_PRAYER("Dankgebet"),

    // D · Sendung und Segen
    INTERCESSIONS("Fürbittengebet"),
    SENDING_WORD("Sendungswort"),
    BLESSING("Segen"),
    EXIT_HYMN("Schlusslied", acceptsHymnNumber = true),
    EXIT_MUSIC("Ausgangsspiel / Musik zum Ausgang"),

    // Freie, weiterhin unterstützte Typen
    ORGAN("Orgel"),
    HYMN("Gemeindelied", acceptsHymnNumber = true),
    IMPROVISATION("Improvisation"),
    CHOIR("Chor"),
    LITURGY("Liturgie"),
    NOTE("Notiz")
}

data class ScoreAttachment(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val storedFileName: String,
    val mimeType: String
)

data class ServiceSection(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: SectionType,
    val musicTitle: String = "",
    val composer: String = "",
    val catalog: String = "",
    val hymnNumber: String = "",
    val registration: String = "",
    val notes: String = "",
    val durationMinutes: Int = 0,
    val attachments: List<ScoreAttachment> = emptyList()
)

data class WorshipService(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val title: String,
    val location: String = "",
    val liturgicalColor: String = "Grün",
    val templateName: String,
    val sections: List<ServiceSection>
) {
    val attachmentCount: Int get() = sections.sumOf { it.attachments.size }
    val totalDuration: Int get() = sections.sumOf { it.durationMinutes }
}

data class ServiceTemplate(
    val name: String,
    val sections: List<Pair<String, SectionType>>
)

data class PerformancePage(
    val sectionIndex: Int,
    val section: ServiceSection,
    val attachment: ScoreAttachment? = null,
    val pageIndex: Int = 0,
    val pageCount: Int = 1,
    val nextSectionTitle: String?
) {
    val isSectionPage: Boolean get() = attachment == null
}

data class PerformancePlan(
    val serviceId: String,
    val pages: List<PerformancePage>
)
