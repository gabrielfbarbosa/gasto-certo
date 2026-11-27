package br.com.gabriel.gastocerto.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MesAnoTest {

    @Test
    fun `proximo avanca um mes no mesmo ano`() {
        assertEquals(MesAno(2026, 12), MesAno(2026, 11).proximo())
    }

    @Test
    fun `proximo de dezembro e janeiro do ano seguinte`() {
        assertEquals(MesAno(2027, 1), MesAno(2026, 12).proximo())
    }

    @Test
    fun `anterior volta um mes e de janeiro vai para dezembro do ano anterior`() {
        assertEquals(MesAno(2026, 10), MesAno(2026, 11).anterior())
        assertEquals(MesAno(2026, 12), MesAno(2027, 1).anterior())
    }

    @Test
    fun `mes fora de 1 a 12 e rejeitado`() {
        assertFailsWith<IllegalArgumentException> { MesAno(2026, 0) }
        assertFailsWith<IllegalArgumentException> { MesAno(2026, 13) }
    }

    @Test
    fun `RN06 - dia existente no mes vira a propria data`() {
        assertEquals(LocalDate(2026, 11, 12), MesAno(2026, 11).dia(12))
    }

    @Test
    fun `RN06 - dia 31 em mes de 30 dias vira o ultimo dia`() {
        assertEquals(LocalDate(2026, 11, 30), MesAno(2026, 11).dia(31))
    }

    @Test
    fun `RN06 - dia 30 em fevereiro de ano comum vira dia 28`() {
        assertEquals(LocalDate(2027, 2, 28), MesAno(2027, 2).dia(30))
    }

    @Test
    fun `RN06 - dia 30 em fevereiro de ano bissexto vira dia 29`() {
        assertEquals(LocalDate(2028, 2, 29), MesAno(2028, 2).dia(30))
    }

    @Test
    fun `RN09 - dia fora de 1 a 31 e rejeitado`() {
        assertFailsWith<IllegalArgumentException> { MesAno(2026, 11).dia(0) }
        assertFailsWith<IllegalArgumentException> { MesAno(2026, 11).dia(32) }
    }
}
