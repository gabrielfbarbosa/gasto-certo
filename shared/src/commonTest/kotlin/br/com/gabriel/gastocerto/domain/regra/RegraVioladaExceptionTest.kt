package br.com.gabriel.gastocerto.domain.regra

import br.com.gabriel.gastocerto.domain.model.Dinheiro
import br.com.gabriel.gastocerto.domain.model.MesAno
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RegraVioladaExceptionTest {

    @Test
    fun `cada excecao informa a RN que foi violada`() {
        val esperado = mapOf(
            ValorInvalido(Dinheiro(0)) to "RN09",
            CotacaoInvalida(0) to "RN05",
            ParcelasForaDoLimite(49) to "RN02",
            CompetenciaFechada(MesAno(2026, 11)) to "RN08",
            ReembolsoMaiorQueSaldo(Dinheiro(5_000), Dinheiro(3_000)) to "RN03",
            MeioComLancamentos("Nubank") to "RN09",
        )
        esperado.forEach { (excecao, regra) -> assertEquals(regra, excecao.regra) }
    }

    @Test
    fun `a mensagem para o usuario traz os dados informados`() {
        assertTrue("49" in ParcelasForaDoLimite(49).mensagem)
        assertTrue("11/2026" in CompetenciaFechada(MesAno(2026, 11)).mensagem)
        assertTrue("Nubank" in MeioComLancamentos("Nubank").mensagem)
    }

    @Test
    fun `todas podem ser capturadas como RegraVioladaException`() {
        val capturada = runCatching { throw CompetenciaFechada(MesAno(2026, 11)) }.exceptionOrNull()
        assertIs<RegraVioladaException>(capturada)
        assertEquals("RN08: ${capturada.mensagem}", capturada.message)
    }
}
