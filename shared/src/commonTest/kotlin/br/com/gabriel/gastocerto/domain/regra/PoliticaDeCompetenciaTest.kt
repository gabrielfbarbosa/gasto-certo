package br.com.gabriel.gastocerto.domain.regra

import br.com.gabriel.gastocerto.domain.model.MesAno
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PoliticaDeCompetenciaTest {

    // Cartão com fechamento dia 5 e vencimento dia 12: vence no mesmo mês do fechamento.
    private val cartao = CompetenciaPorFechamentoDeFatura(diaFechamento = 5, diaVencimento = 12)

    // ---------- RN01: cartão de crédito ----------

    @Test
    fun `RN01 - compra antes do fechamento entra na fatura que vence no mesmo mes`() {
        assertEquals(MesAno(2026, 11), cartao.competenciaDe(LocalDate(2026, 11, 3)))
    }

    @Test
    fun `RN01 - compra no proprio dia do fechamento ainda entra no ciclo atual`() {
        assertEquals(MesAno(2026, 11), cartao.competenciaDe(LocalDate(2026, 11, 5)))
    }

    @Test
    fun `RN01 - compra no dia seguinte ao fechamento vai para a fatura seguinte`() {
        assertEquals(MesAno(2026, 12), cartao.competenciaDe(LocalDate(2026, 11, 6)))
    }

    @Test
    fun `RN01 - vencimento antes do fechamento vence no mes seguinte ao fechamento`() {
        // fecha dia 25, vence dia 2: compra 20/11 fecha em 25/11 e vence em 02/12
        val politica = CompetenciaPorFechamentoDeFatura(diaFechamento = 25, diaVencimento = 2)
        assertEquals(MesAno(2026, 12), politica.competenciaDe(LocalDate(2026, 11, 20)))
        // compra 26/11 fecha em 25/12 e vence em 02/01
        assertEquals(MesAno(2027, 1), politica.competenciaDe(LocalDate(2026, 11, 26)))
    }

    @Test
    fun `RN01 - vencimento no mesmo dia do fechamento vence no mes seguinte`() {
        val politica = CompetenciaPorFechamentoDeFatura(diaFechamento = 10, diaVencimento = 10)
        assertEquals(MesAno(2026, 12), politica.competenciaDe(LocalDate(2026, 11, 8)))
    }

    @Test
    fun `RN01 - compra depois do fechamento de dezembro vai para janeiro do ano seguinte`() {
        assertEquals(MesAno(2027, 1), cartao.competenciaDe(LocalDate(2026, 12, 20)))
    }

    @Test
    fun `RN01 - fechamento dia 31 em mes de 30 dias fecha no ultimo dia`() {
        // fecha dia 31 (em novembro: 30/11), vence dia 8 do mês seguinte
        val politica = CompetenciaPorFechamentoDeFatura(diaFechamento = 31, diaVencimento = 8)
        assertEquals(MesAno(2026, 12), politica.competenciaDe(LocalDate(2026, 11, 30)))
        assertEquals(MesAno(2027, 1), politica.competenciaDe(LocalDate(2026, 12, 1)))
    }

    @Test
    fun `RN01 - fechamento dia 30 em fevereiro fecha no dia 28`() {
        val politica = CompetenciaPorFechamentoDeFatura(diaFechamento = 30, diaVencimento = 7)
        assertEquals(MesAno(2027, 3), politica.competenciaDe(LocalDate(2027, 2, 28)))
        assertEquals(MesAno(2027, 4), politica.competenciaDe(LocalDate(2027, 3, 1)))
    }

    @Test
    fun `RN01 - dia de fechamento ou vencimento fora de 1 a 31 e rejeitado`() {
        assertFailsWith<IllegalArgumentException> { CompetenciaPorFechamentoDeFatura(0, 12) }
        assertFailsWith<IllegalArgumentException> { CompetenciaPorFechamentoDeFatura(5, 32) }
    }

    // ---------- RN01: demais meios ----------

    @Test
    fun `RN01 - conta corrente usa o mes da data do gasto`() {
        val politica = CompetenciaPorDataDoGasto()
        assertEquals(MesAno(2026, 11), politica.competenciaDe(LocalDate(2026, 11, 30)))
        assertEquals(MesAno(2026, 12), politica.competenciaDe(LocalDate(2026, 12, 1)))
    }
}
