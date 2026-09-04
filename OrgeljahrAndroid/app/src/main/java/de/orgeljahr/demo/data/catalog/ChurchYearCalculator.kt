package de.orgeljahr.demo.data.catalog

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object ChurchYearCalculator {
    data class ChurchYear(
        val startYear: Int,
        val startDate: LocalDate,
        val endDateExclusive: LocalDate,
        val sermonSeries: Int
    )

    fun churchYearFor(date: LocalDate): ChurchYear {
        val adventThisCalendarYear = firstSundayOfAdvent(date.year)
        val startYear = if (date >= adventThisCalendarYear) date.year else date.year - 1
        return ChurchYear(
            startYear = startYear,
            startDate = firstSundayOfAdvent(startYear),
            endDateExclusive = firstSundayOfAdvent(startYear + 1),
            sermonSeries = sermonSeriesForStartYear(startYear)
        )
    }

    fun sermonSeriesForStartYear(startYear: Int): Int =
        Math.floorMod(startYear - 2018, 6) + 1

    fun firstSundayOfAdvent(year: Int): LocalDate =
        LocalDate.of(year, 11, 27).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    fun resolve(rule: LiturgicalDateRuleEntity, churchYearStartYear: Int): LocalDate? {
        val churchYear = ChurchYear(
            startYear = churchYearStartYear,
            startDate = firstSundayOfAdvent(churchYearStartYear),
            endDateExclusive = firstSundayOfAdvent(churchYearStartYear + 1),
            sermonSeries = sermonSeriesForStartYear(churchYearStartYear)
        )
        val offset = rule.offsetDays?.toLong() ?: 0L
        return when (rule.ruleType) {
            DateRuleTypes.FIXED_DATE -> resolveFixedDate(rule, churchYear)
            DateRuleTypes.ADVENT_OFFSET -> churchYear.startDate.plusDays(offset)
            DateRuleTypes.CHRISTMAS_OFFSET -> LocalDate.of(churchYearStartYear, 12, 25).plusDays(offset)
            DateRuleTypes.EASTER_OFFSET -> easterSunday(churchYearStartYear + 1).plusDays(offset)
            DateRuleTypes.TRINITY_OFFSET -> easterSunday(churchYearStartYear + 1).plusDays(56 + offset)
            DateRuleTypes.CHURCH_YEAR_END_OFFSET -> churchYear.endDateExclusive.minusDays(7).plusDays(offset)
            DateRuleTypes.EPIPHANY_PATTERN -> resolveEpiphanyRule(rule, churchYearStartYear + 1)
            DateRuleTypes.SUNDAY_AFTER_FIXED_DATE -> resolveSundayAtFixedDate(rule, churchYear, false)
            DateRuleTypes.SUNDAY_ON_OR_AFTER_FIXED_DATE -> resolveSundayAtFixedDate(rule, churchYear, true)
            else -> null
        }
    }

    private fun resolveFixedDate(rule: LiturgicalDateRuleEntity, churchYear: ChurchYear): LocalDate? {
        val month = rule.month ?: return null
        val day = rule.dayOfMonth ?: return null
        return listOf(churchYear.startYear, churchYear.startYear + 1)
            .mapNotNull { year -> runCatching { LocalDate.of(year, month, day) }.getOrNull() }
            .firstOrNull { it >= churchYear.startDate && it < churchYear.endDateExclusive }
    }

    private fun resolveEpiphanyRule(rule: LiturgicalDateRuleEntity, calendarYear: Int): LocalDate? {
        val lastSundayAfterEpiphany = LocalDate.of(calendarYear, 2, 2)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        if (rule.ordinal == 0) {
            return lastSundayAfterEpiphany
        }
        val firstSundayAfterEpiphany = LocalDate.of(calendarYear, 1, 6)
            .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
        val candidate = firstSundayAfterEpiphany.plusWeeks(((rule.ordinal ?: 1) - 1).toLong())
        return candidate.takeIf { it < lastSundayAfterEpiphany }
    }

    private fun resolveSundayAtFixedDate(
        rule: LiturgicalDateRuleEntity,
        churchYear: ChurchYear,
        includeFixedDate: Boolean
    ): LocalDate? {
        val fixedDate = resolveFixedDate(rule, churchYear) ?: return null
        val firstSunday = if (includeFixedDate) {
            fixedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        } else {
            fixedDate.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
        }
        return firstSunday.plusWeeks(((rule.ordinal ?: 1) - 1).toLong())
    }
}
