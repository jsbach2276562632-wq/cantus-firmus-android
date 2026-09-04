package de.orgeljahr.demo.data

import de.orgeljahr.demo.data.catalog.HymnTuneRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HymnTunePresentationTest {
    @Test
    fun primaryTuneNameIsDisplayedInsteadOfRelationshipDescription() {
        val presentation = buildHymnTunePresentation(
            listOf(tuneRow(name = "Nun komm, der Heiden Heiland", isPrimary = true))
        )

        assertEquals("Nun komm, der Heiden Heiland", presentation.names)
        assertEquals("", presentation.origins)
        assertFalse(presentation.hasScore)
    }

    @Test
    fun multipleTunesAreCompleteDistinctAndPrimaryFirst() {
        val presentation = buildHymnTunePresentation(
            listOf(
                tuneRow(name = "Alternative Melodie", origin = "Leipzig", year = "1700"),
                tuneRow(
                    name = "Hauptmelodie",
                    origin = "Wittenberg",
                    year = "1524",
                    isPrimary = true
                ),
                tuneRow(name = "Alternative Melodie", origin = "Leipzig", year = "1700")
            )
        )

        assertEquals("Hauptmelodie · Alternative Melodie", presentation.names)
        assertEquals("Wittenberg · Leipzig", presentation.origins)
        assertEquals("1524 · 1700", presentation.years)
    }

    private fun tuneRow(
        name: String,
        origin: String = "",
        year: String = "",
        isPrimary: Boolean = false
    ) = HymnTuneRow(
        hymnId = "eg-test",
        melodyId = "tune-$name",
        tuneName = name,
        origin = origin,
        yearLabel = year,
        musicXmlAssetPath = null,
        scoreAssetPath = null,
        isPrimary = isPrimary,
        confidence = "VERIFIED"
    )
}
