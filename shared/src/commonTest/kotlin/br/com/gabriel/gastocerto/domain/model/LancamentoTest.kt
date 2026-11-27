package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.CotacaoInvalida
import br.com.gabriel.gastocerto.domain.regra.ValorInvalido
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LancamentoTest {

    private val gabriel = Pessoa("Gabriel", Papel.USUARIO)
    private val pai = Pessoa("Pai", Papel.TERCEIRO)
    private val inter = ContaCorrente("Inter", gabriel, Instituicao("Inter"))
    private val novembro = MesAno(2026, 11)

    private fun gasto(valor: Long, terceiro: Pessoa? = null, situacao: SituacaoDoLancamento = SituacaoDoLancamento.REALIZADO) =
        Gasto(Dinheiro(valor), LocalDate(2026, 11, 10), "Mercado", novembro, inter, terceiro, situacao = situacao)

    // ---------- RN09: valor positivo ----------

    @Test
    fun `RN09 - gasto e receita com valor positivo sao criados`() {
        assertEquals(Dinheiro(15_000), gasto(15_000).valor)
        val receita = Receita(Dinheiro(500_000), LocalDate(2026, 11, 5), "Salário", novembro, TipoDeReceita.SALARIO)
        assertEquals(TipoDeReceita.SALARIO, receita.tipo)
    }

    @Test
    fun `RN09 - lancamento com valor zero ou negativo e rejeitado`() {
        assertFailsWith<ValorInvalido> { gasto(0) }
        assertFailsWith<ValorInvalido> {
            Receita(Dinheiro(-1), LocalDate(2026, 11, 5), "Extra", novembro, TipoDeReceita.AVULSA)
        }
    }

    // ---------- RN03: gasto de terceiro ----------

    @Test
    fun `RN03 - gasto em nome de terceiro e de terceiro`() {
        assertTrue(gasto(5_000, terceiro = pai).ehDeTerceiro())
        assertFalse(gasto(5_000).ehDeTerceiro())
    }

    // ---------- confirmar previsto (RN06, RN07) ----------

    @Test
    fun `RN06 - confirmar previsto troca o valor e o torna realizado`() {
        val luz = gasto(20_000, situacao = SituacaoDoLancamento.PREVISTO)

        luz.confirmar(Dinheiro(18_750))

        assertEquals(Dinheiro(18_750), luz.valor)
        assertEquals(SituacaoDoLancamento.REALIZADO, luz.situacao)
    }

    @Test
    fun `RN09 - confirmar previsto com valor zero e rejeitado e nada muda`() {
        val luz = gasto(20_000, situacao = SituacaoDoLancamento.PREVISTO)

        assertFailsWith<ValorInvalido> { luz.confirmar(Dinheiro(0)) }
        assertEquals(Dinheiro(20_000), luz.valor)
        assertEquals(SituacaoDoLancamento.PREVISTO, luz.situacao)
    }

    @Test
    fun `confirmar lancamento ja realizado e rejeitado`() {
        assertFailsWith<IllegalStateException> { gasto(20_000).confirmar(Dinheiro(100)) }
    }

    // ---------- RN05: conversão cambial (D1) ----------

    @Test
    fun `RN05 - conversao cambial calcula o valor em reais`() {
        // US$ 12,50 × 5,4321 = R$ 67,90
        assertEquals(Dinheiro(6_790), ConversaoCambial(valorOriginal = 1_250, cotacao = 54_321).emReais())
    }

    @Test
    fun `RN05 - cotacao zero ou negativa e rejeitada`() {
        assertFailsWith<CotacaoInvalida> { ConversaoCambial(valorOriginal = 1_250, cotacao = 0) }
        assertFailsWith<CotacaoInvalida> { ConversaoCambial(valorOriginal = 1_250, cotacao = -1) }
    }

    @Test
    fun `RN05 - valor na moeda zero e rejeitado`() {
        assertFailsWith<ValorInvalido> { ConversaoCambial(valorOriginal = 0, cotacao = 54_321) }
    }

    // ---------- D2 ----------

    @Test
    fun `D2 - lancamentos sao iguais pelo id`() {
        val a = Gasto(Dinheiro(100), LocalDate(2026, 11, 1), "A", novembro, inter, id = "l1")
        val b = Gasto(Dinheiro(999), LocalDate(2026, 11, 2), "B", novembro, inter, id = "l1")
        assertEquals<Lancamento>(a, b)
    }
}
