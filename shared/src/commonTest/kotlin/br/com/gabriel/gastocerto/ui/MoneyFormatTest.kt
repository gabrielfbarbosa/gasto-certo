package br.com.gabriel.gastocerto.ui

import br.com.gabriel.gastocerto.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFormatTest {

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
}
