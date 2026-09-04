package de.orgeljahr.demo.data.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

object CatalogRights {
    const val PUBLIC_DOMAIN = "PUBLIC_DOMAIN"
    const val LICENSED = "LICENSED"
    const val METADATA_ONLY = "METADATA_ONLY"
    const val UNKNOWN = "UNKNOWN"
}

object DateRuleTypes {
    const val FIXED_DATE = "FIXED_DATE"
    const val ADVENT_OFFSET = "ADVENT_OFFSET"
    const val CHRISTMAS_OFFSET = "CHRISTMAS_OFFSET"
    const val EPIPHANY_PATTERN = "EPIPHANY_PATTERN"
    const val EASTER_OFFSET = "EASTER_OFFSET"
    const val TRINITY_OFFSET = "TRINITY_OFFSET"
    const val CHURCH_YEAR_END_OFFSET = "CHURCH_YEAR_END_OFFSET"
    const val SUNDAY_AFTER_FIXED_DATE = "SUNDAY_AFTER_FIXED_DATE"
    const val SUNDAY_ON_OR_AFTER_FIXED_DATE = "SUNDAY_ON_OR_AFTER_FIXED_DATE"
}

@Entity(tableName = "catalog_metadata")
data class CatalogMetadataEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val publisher: String = "",
    val url: String = "",
    val accessedOn: String = "",
    val license: String = "",
    val notes: String = ""
)

@Entity(tableName = "hymnal_editions")
data class HymnalEditionEntity(
    @PrimaryKey val id: String,
    val shortName: String,
    val fullName: String,
    val regionCode: String,
    val publicationYear: Int?,
    val validFrom: String?,
    val validUntil: String?,
    val isNationalCore: Boolean
)

@Entity(
    tableName = "hymns",
    foreignKeys = [
        ForeignKey(
            entity = HymnalEditionEntity::class,
            parentColumns = ["id"],
            childColumns = ["editionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("editionId"), Index("sourceId"), Index(value = ["editionId", "numberLabel"], unique = true)]
)
data class HymnEntity(
    @PrimaryKey val id: String,
    val editionId: String,
    val numberLabel: String,
    val sortOrder: Int,
    val title: String,
    val firstLine: String = "",
    val category: String = "",
    val language: String = "de",
    val history: String = "",
    val rightsStatus: String = CatalogRights.UNKNOWN,
    val sourceId: String? = null
)

@Entity(
    tableName = "hymn_texts",
    foreignKeys = [
        ForeignKey(
            entity = HymnEntity::class,
            parentColumns = ["id"],
            childColumns = ["hymnId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("hymnId"), Index("sourceId")]
)
data class HymnTextEntity(
    @PrimaryKey val id: String,
    val hymnId: String,
    val variantLabel: String = "EG 1993",
    val language: String = "de",
    val authorText: String = "",
    val yearLabel: String = "",
    val rightsStatus: String = CatalogRights.UNKNOWN,
    val sourceId: String? = null
)

@Entity(
    tableName = "hymn_verses",
    primaryKeys = ["textId", "verseNumber"],
    foreignKeys = [
        ForeignKey(
            entity = HymnTextEntity::class,
            parentColumns = ["id"],
            childColumns = ["textId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("textId")]
)
data class HymnVerseEntity(
    val textId: String,
    val verseNumber: Int,
    val text: String
)

@Entity(
    tableName = "melodies",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("sourceId"), Index("tuneName")]
)
data class MelodyEntity(
    @PrimaryKey val id: String,
    val tuneName: String,
    val incipit: String = "",
    val meter: String = "",
    val keySignature: String = "",
    val origin: String = "",
    val yearLabel: String = "",
    val history: String = "",
    val musicXmlAssetPath: String? = null,
    val scoreAssetPath: String? = null,
    val rightsStatus: String = CatalogRights.UNKNOWN,
    val sourceId: String? = null
)

@Entity(
    tableName = "hymn_melodies",
    primaryKeys = ["hymnId", "melodyId"],
    foreignKeys = [
        ForeignKey(entity = HymnEntity::class, parentColumns = ["id"], childColumns = ["hymnId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MelodyEntity::class, parentColumns = ["id"], childColumns = ["melodyId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("hymnId"), Index("melodyId"), Index("sourceId")]
)
data class HymnMelodyCrossRef(
    val hymnId: String,
    val melodyId: String,
    val isPrimary: Boolean = true,
    val variantLabel: String = "",
    val confidence: String = "VERIFIED",
    val sourceId: String? = null,
    val notes: String = ""
)

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val birthYear: Int? = null,
    val deathYear: Int? = null,
    val notes: String = ""
)

@Entity(
    tableName = "hymn_contributors",
    primaryKeys = ["hymnId", "personId", "role"],
    foreignKeys = [
        ForeignKey(entity = HymnEntity::class, parentColumns = ["id"], childColumns = ["hymnId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("hymnId"), Index("personId")]
)
data class HymnContributorCrossRef(
    val hymnId: String,
    val personId: String,
    val role: String,
    val attributionText: String = ""
)

@Entity(
    tableName = "musical_works",
    foreignKeys = [
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["composerPersonId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = SourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("composerPersonId"), Index("sourceId"), Index("catalogNumber")]
)
data class MusicalWorkEntity(
    @PrimaryKey val id: String,
    val composerPersonId: String? = null,
    val composerDisplayName: String,
    val title: String,
    val catalogNumber: String,
    val genre: String,
    val scoring: String = "",
    val keySignature: String = "",
    val durationSeconds: Int? = null,
    val description: String = "",
    val rightsStatus: String = CatalogRights.UNKNOWN,
    val sourceId: String? = null
)

@Entity(
    tableName = "work_melodies",
    primaryKeys = ["workId", "melodyId"],
    foreignKeys = [
        ForeignKey(entity = MusicalWorkEntity::class, parentColumns = ["id"], childColumns = ["workId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MelodyEntity::class, parentColumns = ["id"], childColumns = ["melodyId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("workId"), Index("melodyId"), Index("sourceId")]
)
data class WorkMelodyCrossRef(
    val workId: String,
    val melodyId: String,
    val relationType: String = "CHORALE_BASIS",
    val confidence: String = "VERIFIED",
    val sourceId: String? = null,
    val notes: String = ""
)

@Entity(tableName = "liturgical_occasions", indices = [Index("slug", unique = true), Index("season")])
data class LiturgicalOccasionEntity(
    @PrimaryKey val id: String,
    val slug: String,
    val name: String,
    val season: String,
    val defaultColor: String,
    val alternativeGroup: String? = null,
    val priority: Int = 0,
    val description: String = ""
)

@Entity(
    tableName = "liturgical_date_rules",
    foreignKeys = [
        ForeignKey(entity = LiturgicalOccasionEntity::class, parentColumns = ["id"], childColumns = ["occasionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("occasionId")]
)
data class LiturgicalDateRuleEntity(
    @PrimaryKey val id: String,
    val occasionId: String,
    val ruleType: String,
    val month: Int? = null,
    val dayOfMonth: Int? = null,
    val offsetDays: Int? = null,
    val ordinal: Int? = null,
    val weekday: Int? = null,
    val notes: String = ""
)

@Entity(
    tableName = "lectionaries",
    foreignKeys = [
        ForeignKey(entity = LiturgicalOccasionEntity::class, parentColumns = ["id"], childColumns = ["occasionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("sourceId")]
)
data class LectionaryEntity(
    @PrimaryKey val occasionId: String,
    val gospelReference: String = "",
    val epistleReference: String = "",
    val oldTestamentReference: String = "",
    val psalmReference: String = "",
    val weeklyVerseReference: String = "",
    val hallelujahReference: String = "",
    val additionalReadings: String = "",
    val sourceId: String? = null
)

@Entity(
    tableName = "sermon_readings",
    primaryKeys = ["occasionId", "seriesNumber"],
    foreignKeys = [
        ForeignKey(entity = LiturgicalOccasionEntity::class, parentColumns = ["id"], childColumns = ["occasionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("occasionId")]
)
data class SermonReadingEntity(
    val occasionId: String,
    val seriesNumber: Int,
    val scriptureReference: String
)

@Entity(
    tableName = "occasion_hymns",
    foreignKeys = [
        ForeignKey(entity = LiturgicalOccasionEntity::class, parentColumns = ["id"], childColumns = ["occasionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = HymnEntity::class, parentColumns = ["id"], childColumns = ["hymnId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("occasionId"), Index("hymnId"), Index(value = ["occasionId", "position"], unique = true)]
)
data class OccasionHymnEntity(
    @PrimaryKey val id: String,
    val occasionId: String,
    val position: Int,
    val collectionCode: String,
    val numberLabel: String = "",
    val title: String,
    val hymnId: String? = null
)

@Entity(
    tableName = "occasion_hymn_links",
    primaryKeys = ["occasionHymnId", "hymnId"],
    foreignKeys = [
        ForeignKey(
            entity = OccasionHymnEntity::class,
            parentColumns = ["id"],
            childColumns = ["occasionHymnId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HymnEntity::class,
            parentColumns = ["id"],
            childColumns = ["hymnId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("occasionHymnId"), Index("hymnId")]
)
data class OccasionHymnLinkEntity(
    val occasionHymnId: String,
    val hymnId: String
)

@Fts4
@Entity(tableName = "hymn_search")
data class HymnSearchEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid") val rowId: Int,
    val hymnId: String,
    val numberLabel: String,
    val title: String,
    val firstLine: String,
    val contributors: String,
    val tuneNames: String
)
