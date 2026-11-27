package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.CompetenciaFechada
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CompetenciaTest {

    private val novembro = MesAno(2026, 11)

    @Test
    fun `RN08 - competencia nova comeca aberta e aceita lancamentos`() {
        val competencia = Competencia(novembro)
        assertEquals(SituacaoDaCompetencia.ABERTA, competencia.situacao)
        competencia.garantirAberta() // não lança
    }

    @Test
    fun `RN08 - fechar torna a competencia fechada`() {
        val competencia = Competencia(novembro)
        competencia.fechar()
        assertEquals(SituacaoDaCompetencia.FECHADA, competencia.situacao)
    }

    @Test
    fun `RN08 - competencia fechada nao aceita lancamentos`() {
        val competencia = Competencia(novembro, SituacaoDaCompetencia.FECHADA)
        val erro = assertFailsWith<CompetenciaFechada> { competencia.garantirAberta() }
        assertEquals("RN08", erro.regra)
    }

    @Test
    fun `RN08 - fechar um mes ja fechado e rejeitado`() {
        val competencia = Competencia(novembro)
        competencia.fechar()
        assertFailsWith<CompetenciaFechada> { competencia.fechar() }
    }

    @Test
    fun `RN08 - reabrir volta a aceitar lancamentos`() {
        val competencia = Competencia(novembro, SituacaoDaCompetencia.FECHADA)
        competencia.reabrir()
        assertEquals(SituacaoDaCompetencia.ABERTA, competencia.situacao)
        competencia.garantirAberta()
    }

    @Test
    fun `existe uma competencia por mes - igualdade pelo mes`() {
        assertEquals(Competencia(novembro), Competencia(MesAno(2026, 11), SituacaoDaCompetencia.FECHADA))
    }
}
