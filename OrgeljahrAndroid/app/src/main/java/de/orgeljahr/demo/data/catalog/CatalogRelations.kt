package de.orgeljahr.demo.data.catalog

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class HymnWithMelodies(
    @Embedded val hymn: HymnEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = HymnMelodyCrossRef::class,
            parentColumn = "hymnId",
            entityColumn = "melodyId"
        )
    )
    val melodies: List<MelodyEntity>
)

data class MelodyWithWorks(
    @Embedded val melody: MelodyEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorkMelodyCrossRef::class,
            parentColumn = "melodyId",
            entityColumn = "workId"
        )
    )
    val works: List<MusicalWorkEntity>
)

data class LiturgicalOccasionBundle(
    @Embedded val occasion: LiturgicalOccasionEntity,
    @Relation(parentColumn = "id", entityColumn = "occasionId")
    val dateRules: List<LiturgicalDateRuleEntity>,
    @Relation(parentColumn = "id", entityColumn = "occasionId")
    val lectionary: LectionaryEntity?,
    @Relation(parentColumn = "id", entityColumn = "occasionId")
    val sermonReadings: List<SermonReadingEntity>,
    @Relation(parentColumn = "id", entityColumn = "occasionId")
    val hymns: List<OccasionHymnEntity>
)

data class HymnRecommendation(
    val occasionId: String,
    val occasionName: String,
    val hymnId: String,
    val numberLabel: String,
    val hymnTitle: String,
    val melodyId: String,
    val tuneName: String,
    val workId: String,
    val composerPersonId: String?,
    val composerDisplayName: String,
    val workTitle: String,
    val catalogNumber: String,
    val genre: String,
    val relationType: String,
    val confidence: String
)

data class HymnTuneRow(
    val hymnId: String,
    val melodyId: String,
    val tuneName: String,
    val origin: String,
    val yearLabel: String,
    val musicXmlAssetPath: String?,
    val scoreAssetPath: String?,
    val isPrimary: Boolean,
    val confidence: String
)

data class HymnWorkRow(
    val hymnId: String,
    val melodyId: String,
    val tuneName: String,
    val workId: String,
    val composerPersonId: String?,
    val composerDisplayName: String,
    val workTitle: String,
    val catalogNumber: String,
    val genre: String,
    val scoring: String,
    val relationType: String,
    val confidence: String
)
