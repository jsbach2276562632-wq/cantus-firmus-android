package de.orgeljahr.demo.data.catalog

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM hymns WHERE editionId = :editionId ORDER BY sortOrder, numberLabel")
    fun observeHymns(editionId: String = CatalogDatabase.EG_1993_CORE_ID): Flow<List<HymnEntity>>

    @Query("SELECT * FROM hymns WHERE editionId = :editionId ORDER BY sortOrder, numberLabel")
    suspend fun hymns(editionId: String = CatalogDatabase.EG_1993_CORE_ID): List<HymnEntity>

    @Transaction
    @Query("SELECT * FROM hymns WHERE id = :hymnId LIMIT 1")
    suspend fun hymnWithMelodies(hymnId: String): HymnWithMelodies?

    @Transaction
    @Query("SELECT * FROM melodies WHERE id = :melodyId LIMIT 1")
    suspend fun melodyWithWorks(melodyId: String): MelodyWithWorks?

    @Transaction
    @Query("SELECT * FROM liturgical_occasions WHERE slug = :slug LIMIT 1")
    suspend fun liturgicalOccasion(slug: String): LiturgicalOccasionBundle?

    @Transaction
    @Query("SELECT * FROM liturgical_occasions ORDER BY priority DESC, name")
    suspend fun allLiturgicalOccasions(): List<LiturgicalOccasionBundle>

    @Query(
        """
        SELECT h.* FROM hymn_search s
        JOIN hymns h ON h.id = s.hymnId
        WHERE hymn_search MATCH :ftsQuery
        ORDER BY h.sortOrder
        LIMIT :limit
        """
    )
    suspend fun searchHymns(ftsQuery: String, limit: Int = 50): List<HymnEntity>

    @Query(
        """
        SELECT
            hm.hymnId AS hymnId,
            m.id AS melodyId,
            m.tuneName AS tuneName,
            m.origin AS origin,
            m.yearLabel AS yearLabel,
            m.musicXmlAssetPath AS musicXmlAssetPath,
            m.scoreAssetPath AS scoreAssetPath,
            hm.isPrimary AS isPrimary,
            hm.confidence AS confidence
        FROM hymn_melodies hm
        JOIN melodies m ON m.id = hm.melodyId
        ORDER BY hm.hymnId, hm.isPrimary DESC, m.tuneName
        """
    )
    suspend fun allHymnTunes(): List<HymnTuneRow>

    @Query(
        """
        SELECT
            hm.hymnId AS hymnId,
            m.id AS melodyId,
            m.tuneName AS tuneName,
            w.id AS workId,
            w.composerPersonId AS composerPersonId,
            w.composerDisplayName AS composerDisplayName,
            w.title AS workTitle,
            w.catalogNumber AS catalogNumber,
            w.genre AS genre,
            w.scoring AS scoring,
            wm.relationType AS relationType,
            wm.confidence AS confidence
        FROM hymn_melodies hm
        JOIN melodies m ON m.id = hm.melodyId
        JOIN work_melodies wm ON wm.melodyId = m.id
        JOIN musical_works w ON w.id = wm.workId
        WHERE hm.confidence = 'VERIFIED' AND wm.confidence = 'VERIFIED'
        ORDER BY hm.hymnId,
            CASE WHEN w.scoring = 'Orgel' THEN 0 WHEN w.genre = 'Choralkantate' THEN 1 ELSE 2 END,
            w.composerDisplayName, w.catalogNumber
        """
    )
    suspend fun allHymnWorks(): List<HymnWorkRow>

    @Query(
        """
        SELECT
            o.id AS occasionId,
            o.name AS occasionName,
            h.id AS hymnId,
            h.numberLabel AS numberLabel,
            h.title AS hymnTitle,
            m.id AS melodyId,
            m.tuneName AS tuneName,
            w.id AS workId,
            w.composerPersonId AS composerPersonId,
            w.composerDisplayName AS composerDisplayName,
            w.title AS workTitle,
            w.catalogNumber AS catalogNumber,
            w.genre AS genre,
            wm.relationType AS relationType,
            wm.confidence AS confidence
        FROM liturgical_occasions o
        JOIN occasion_hymns oh ON oh.occasionId = o.id
        JOIN occasion_hymn_links ohl ON ohl.occasionHymnId = oh.id
        JOIN hymns h ON h.id = ohl.hymnId
        JOIN hymn_melodies hm ON hm.hymnId = h.id
        JOIN melodies m ON m.id = hm.melodyId
        JOIN work_melodies wm ON wm.melodyId = m.id
        JOIN musical_works w ON w.id = wm.workId
        WHERE o.id = :occasionId
            AND hm.confidence = 'VERIFIED'
            AND wm.confidence = 'VERIFIED'
        ORDER BY oh.position,
            CASE WHEN w.scoring = 'Orgel' THEN 0 WHEN w.genre = 'Choralkantate' THEN 1 ELSE 2 END,
            w.composerDisplayName, w.catalogNumber
        """
    )
    suspend fun recommendationsForOccasion(occasionId: String): List<HymnRecommendation>

    @Query("SELECT COUNT(*) FROM hymns WHERE editionId = :editionId")
    suspend fun hymnCount(editionId: String = CatalogDatabase.EG_1993_CORE_ID): Int

    @Query("SELECT value FROM catalog_metadata WHERE key = :key LIMIT 1")
    suspend fun metadata(key: String): String?
}
