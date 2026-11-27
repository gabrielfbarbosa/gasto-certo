package br.com.gabriel.gastocerto.ui

import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatsTest {

    @Test
    fun `formats cents as Brazilian reais with thousands separator`() {
        assertEquals("R$ 0,05", Money(5).toBrl())
        assertEquals("R$ 33,34", Money(3_334).toBrl())
        assertEquals("R$ 1.234,56", Money(123_456).toBrl())
        assertEquals("R$ 1.000.000,00", Money(100_000_000).toBrl())
    }

    @Test
    fun `formats a negative surplus with the minus sign first`() {
        assertEquals("-R$ 150,00", Money(-15_000).toBrl())
    }

    @Test
    fun `formats fixed decimals for the exchange rate and whole numbers`() {
        assertEquals("5,4321", formatFixed(54_321, 4))
        assertEquals("12.345", formatFixed(12_345, 0))
        assertEquals("0,0001", formatFixed(1, 4))
    }

    @Test
    fun `typed digits become a value with implied decimals`() {
        assertEquals(12_345L, digitsToValue("123,45"))
        assertEquals(12_345L, digitsToValue("1.2345"))
        assertEquals(5L, digitsToValue("0,005"))
        assertEquals(0L, digitsToValue("0"))
        assertEquals(null, digitsToValue(""))
        assertEquals(null, digitsToValue(",."))
    }

    @Test
    fun `dates and months use the Brazilian order`() {
        assertEquals("05/01/2027", LocalDate(2027, 1, 5).toDisplay())
        assertEquals("11/2026", MonthYear(2026, 11).toDisplay())
        assertEquals(MonthYear(2026, 2), LocalDate(2026, 2, 28).monthYear())
    }
}
