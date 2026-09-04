package de.orgeljahr.demo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformancePlanTest {
    @Test
    fun emptySectionStillGetsOneCuePage() {
        val section = ServiceSection(title = "Liturgie", type = SectionType.LITURGY)
        val service = serviceWith(section)

        val plan = buildPerformancePlan(service) { 0 }

        assertEquals(1, plan.pages.size)
        assertTrue(plan.pages.single().isSectionPage)
        assertNull(plan.pages.single().attachment)
    }

    @Test
    fun cuePageAlwaysPrecedesEveryAttachmentPage() {
        val pdf = ScoreAttachment(
            displayName = "Choral.pdf",
            storedFileName = "choral.pdf",
            mimeType = "application/pdf"
        )
        val section = ServiceSection(
            title = "Lied",
            type = SectionType.HYMN,
            hymnNumber = "244",
            attachments = listOf(pdf)
        )

        val plan = buildPerformancePlan(serviceWith(section)) { 2 }

        assertEquals(3, plan.pages.size)
        assertTrue(plan.pages[0].isSectionPage)
        assertFalse(plan.pages[1].isSectionPage)
        assertFalse(plan.pages[2].isSectionPage)
        assertEquals(listOf(0, 1), plan.pages.drop(1).map(PerformancePage::pageIndex))
        assertEquals(listOf(pdf, pdf), plan.pages.drop(1).map(PerformancePage::attachment))
    }

    @Test
    fun eachSectionStartsWithItsOwnCuePage() {
        val image = ScoreAttachment(
            displayName = "Nachspiel.jpg",
            storedFileName = "nachspiel.jpg",
            mimeType = "image/jpeg"
        )
        val first = ServiceSection(title = "Lied", type = SectionType.HYMN, hymnNumber = "244")
        val second = ServiceSection(
            title = "Ausgangsspiel",
            type = SectionType.EXIT_MUSIC,
            attachments = listOf(image)
        )

        val plan = buildPerformancePlan(serviceWith(first, second)) { 1 }

        assertEquals(3, plan.pages.size)
        assertEquals(listOf(0, 1, 1), plan.pages.map(PerformancePage::sectionIndex))
        assertTrue(plan.pages[0].isSectionPage)
        assertTrue(plan.pages[1].isSectionPage)
        assertFalse(plan.pages[2].isSectionPage)
        assertEquals("Ausgangsspiel", plan.pages[0].nextSectionTitle)
    }

    private fun serviceWith(vararg sections: ServiceSection) = WorshipService(
        id = "service:test",
        date = "2026-08-30T10:00",
        title = "Testgottesdienst",
        templateName = "Leer",
        sections = sections.toList()
    )
}
