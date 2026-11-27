package br.com.gabriel.gastocerto.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/**
 * A month of a year (e.g. November 2026): the **accounting month** an entry belongs to.
 *
 * Used by RN01 (statement month), RN06/RN07 (due day / pay day of expected entries)
 * and by month closing (UC03).
 */
data class MonthYear(val year: Int, val month: Int) {

    init {
        require(month in 1..12) { "month must be between 1 and 12, but was $month" }
    }

    /** The next month (December becomes January of the following year). */
    fun next(): MonthYear = if (month == 12) MonthYear(year + 1, 1) else MonthYear(year, month + 1)

    /** The previous month (January becomes December of the previous year). Used for the statement closing date. */
    fun previous(): MonthYear = if (month == 1) MonthYear(year - 1, 12) else MonthYear(year, month - 1)

    /**
     * The date in this month on the given [day]. **A day that does not exist in the month becomes
     * the month's last day** (e.g. day 31 in February 2027 → 2027-02-28), used by recurring bills (RN06),
     * salary (RN07) and card statement closing (RN01).
     */
    fun atDay(day: Int): LocalDate {
        require(day in 1..31) { "day must be between 1 and 31, but was $day" }
        return LocalDate(year, month, minOf(day, numberOfDays()))
    }

    private fun numberOfDays(): Int = YearMonth(year, month).numberOfDays
}
