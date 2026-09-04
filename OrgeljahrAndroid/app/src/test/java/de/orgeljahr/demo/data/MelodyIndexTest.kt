package de.orgeljahr.demo.data

import de.orgeljahr.demo.data.catalog.HymnTuneRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MelodyIndexTest {
    @Test
    fun sharedMelodyListsEveryLinkedHymnInEgOrder() {
        val eg223 = hymn("hymn-223", "223", "Das Wort geht von dem Vater aus")
        val eg79 = hymn("hymn-79", "79", "Wir danken dir, Herr Jesu Christ")
        val rows = listOf(
            tuneRow(hymnId = eg223.id, isPrimary = true),
            tuneRow(hymnId = eg79.id, isPrimary = false)
        )

        val melody = buildMelodyIndex(listOf(eg223, eg79), rows).single()

        assertEquals(listOf("79", "223"), melody.linkedHymns.map { it.hymn.egNumber })
        assertFalse(melody.linkedHymns.first().isPrimary)
        assertTrue(melody.linkedHymns.last().isPrimary)
    }

    private fun hymn(id: String, number: String, title: String) = Hymn(
        id = id,
        egNumber = number,
        title = title,
        tune = "Christus, der uns selig macht",
        textAuthor = "",
        tuneSource = "",
        year = "",
        history = "",
        verses = emptyList(),
        themes = emptyList(),
        relatedWorks = emptyList()
    )

    private fun tuneRow(hymnId: String, isPrimary: Boolean) = HymnTuneRow(
        hymnId = hymnId,
        melodyId = "tune-shared",
        tuneName = "Christus, der uns selig macht",
        origin = "Melchior Vulpius",
        yearLabel = "1609",
        musicXmlAssetPath = null,
        scoreAssetPath = null,
        isPrimary = isPrimary,
        confidence = "VERIFIED"
    )
}
