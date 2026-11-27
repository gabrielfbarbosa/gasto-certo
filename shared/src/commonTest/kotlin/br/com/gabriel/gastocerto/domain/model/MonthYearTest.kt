package br.com.gabriel.gastocerto.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MonthYearTest {

    @Test
    fun `next moves one month ahead in the same year`() {
        assertEquals(MonthYear(2026, 12), MonthYear(2026, 11).next())
    }

    @Test
    fun `next of December is January of the following year`() {
        assertEquals(MonthYear(2027, 1), MonthYear(2026, 12).next())
    }

    @Test
    fun `previous goes back one month and January goes to December of the previous year`() {
        assertEquals(MonthYear(2026, 10), MonthYear(2026, 11).previous())
        assertEquals(MonthYear(2026, 12), MonthYear(2027, 1).previous())
    }

    @Test
    fun `month outside 1 to 12 is rejected`() {
        assertFailsWith<IllegalArgumentException> { MonthYear(2026, 0) }
        assertFailsWith<IllegalArgumentException> { MonthYear(2026, 13) }
    }

    @Test
    fun `RN06 - existing day in the month becomes that date`() {
        assertEquals(LocalDate(2026, 11, 12), MonthYear(2026, 11).atDay(12))
    }

    @Test
    fun `RN06 - day 31 in a 30-day month becomes the last day`() {
        assertEquals(LocalDate(2026, 11, 30), MonthYear(2026, 11).atDay(31))
    }

    @Test
    fun `RN06 - day 30 in February of a common year becomes day 28`() {
        assertEquals(LocalDate(2027, 2, 28), MonthYear(2027, 2).atDay(30))
    }

    @Test
    fun `RN06 - day 30 in February of a leap year becomes day 29`() {
        assertEquals(LocalDate(2028, 2, 29), MonthYear(2028, 2).atDay(30))
    }

    @Test
    fun `RN09 - day outside 1 to 31 is rejected`() {
        assertFailsWith<IllegalArgumentException> { MonthYear(2026, 11).atDay(0) }
        assertFailsWith<IllegalArgumentException> { MonthYear(2026, 11).atDay(32) }
    }
}
