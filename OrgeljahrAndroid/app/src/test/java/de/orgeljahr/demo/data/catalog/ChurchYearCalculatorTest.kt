package de.orgeljahr.demo.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ChurchYearCalculatorTest {
    @Test
    fun firstAdventAndSeriesFollowTheOgtlCycle() {
        assertEquals(LocalDate.of(2018, 12, 2), ChurchYearCalculator.firstSundayOfAdvent(2018))
        assertEquals(1, ChurchYearCalculator.sermonSeriesForStartYear(2018))
        assertEquals(1, ChurchYearCalculator.sermonSeriesForStartYear(2024))
        assertEquals(2, ChurchYearCalculator.sermonSeriesForStartYear(2025))
        assertEquals(3, ChurchYearCalculator.sermonSeriesForStartYear(2026))
    }

    @Test
    fun calendarDateIsAssignedToTheCorrectChurchYear() {
        assertEquals(2025, ChurchYearCalculator.churchYearFor(LocalDate.of(2026, 8, 25)).startYear)
        assertEquals(2, ChurchYearCalculator.churchYearFor(LocalDate.of(2026, 8, 25)).sermonSeries)
        assertEquals(2026, ChurchYearCalculator.churchYearFor(LocalDate.of(2026, 11, 29)).startYear)
    }

    @Test
    fun easterCalculationMatchesKnownDates() {
        assertEquals(LocalDate.of(2026, 4, 5), ChurchYearCalculator.easterSunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), ChurchYearCalculator.easterSunday(2027))
    }

    @Test
    fun thirteenthSundayAfterTrinityFallsOnAugust30In2026() {
        val rule = LiturgicalDateRuleEntity(
            id = "test:trinity-13",
            occasionId = "test:occasion",
            ruleType = DateRuleTypes.TRINITY_OFFSET,
            offsetDays = 13 * 7,
        )

        assertEquals(LocalDate.of(2026, 8, 30), ChurchYearCalculator.resolve(rule, 2025))
    }

    @Test
    fun ordinaryEpiphanySundayIsOmittedWhenItCollidesWithTheFinalSunday() {
        val thirdSunday = LiturgicalDateRuleEntity(
            id = "test:epiphany-3",
            occasionId = "test:occasion",
            ruleType = DateRuleTypes.EPIPHANY_PATTERN,
            ordinal = 3,
        )
        val finalSunday = thirdSunday.copy(id = "test:epiphany-final", ordinal = 0)

        assertNull(ChurchYearCalculator.resolve(thirdSunday, 2029))
        assertEquals(LocalDate.of(2030, 1, 27), ChurchYearCalculator.resolve(finalSunday, 2029))
    }
}
