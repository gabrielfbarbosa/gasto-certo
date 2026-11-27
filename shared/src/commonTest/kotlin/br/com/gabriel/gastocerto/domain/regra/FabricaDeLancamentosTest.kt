package br.com.gabriel.gastocerto.domain.regra

import br.com.gabriel.gastocerto.domain.model.CarteiraMoedaEstrangeira
import br.com.gabriel.gastocerto.domain.model.CartaoDeCredito
import br.com.gabriel.gastocerto.domain.model.ContaCorrente
import br.com.gabriel.gastocerto.domain.model.ContaFixa
import br.com.gabriel.gastocerto.domain.model.ContaInvestimento
import br.com.gabriel.gastocerto.domain.model.ConversaoCambial
import br.com.gabriel.gastocerto.domain.model.Dinheiro
import br.com.gabriel.gastocerto.domain.model.Gasto
import br.com.gabriel.gastocerto.domain.model.Instituicao
import br.com.gabriel.gastocerto.domain.model.MesAno
import br.com.gabriel.gastocerto.domain.model.Papel
import br.com.gabriel.gastocerto.domain.model.Pessoa
import br.com.gabriel.gastocerto.domain.model.Receita
import br.com.gabriel.gastocerto.domain.model.ReceitaRecorrente
import br.com.gabriel.gastocerto.domain.model.SituacaoDoLancamento
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FabricaDeLancamentosTest {

    private val fabrica = FabricaDeLancamentos()
    private val gabriel = Pessoa("Gabriel", Papel.USUARIO)
    private val pai = Pessoa("Pai", Papel.TERCEIRO)
    private val nubank = CartaoDeCredito("Nubank", gabriel, Instituicao("Nubank"), diaFechamento = 5, diaVencimento = 12)
    private val inter = ContaCorrente("Inter", gabriel, Instituicao("Inter"))
    private val wise = CarteiraMoedaEstrangeira("Wise", gabriel, Instituicao("Wise"), moeda = "USD")
    private val clear = ContaInvestimento("Clear", gabriel, Instituicao("Clear"), aportePlanejadoMensal = Dinheiro(50_000))
    private val novembro = MesAno(2026, 11)

    // ---------- gasto à vista (UC01) ----------

    @Test
    fun `RN01 - gasto no cartao depois do fechamento entra na fatura seguinte`() {
        val gasto = fabrica.gasto(DadosDoGasto(nubank, Dinheiro(15_000), LocalDate(2026, 11, 6), "Farmácia"))

        assertEquals(MesAno(2026, 12), gasto.competencia)
        assertEquals(Dinheiro(15_000), gasto.valor)
        assertEquals(SituacaoDoLancamento.REALIZADO, gasto.situacao)
        assertNull(gasto.conversao)
    }

    @Test
    fun `RN01 - gasto na conta corrente entra no mes da data`() {
        val gasto = fabrica.gasto(DadosDoGasto(inter, Dinheiro(4_590), LocalDate(2026, 11, 30), "Pix"))
        assertEquals(novembro, gasto.competencia)
    }

    @Test
    fun `RN03 - gasto em nome de terceiro`() {
        val gasto = fabrica.gasto(DadosDoGasto(inter, Dinheiro(8_000), LocalDate(2026, 11, 10), "Remédio do pai", terceiro = pai))
        assertTrue(gasto.ehDeTerceiro())
        assertEquals(pai, gasto.terceiro)
    }

    @Test
    fun `RN09 - gasto com valor zero negativo ou vazio e rejeitado`() {
        val data = LocalDate(2026, 11, 10)
        assertFailsWith<ValorInvalido> { fabrica.gasto(DadosDoGasto(inter, Dinheiro(0), data, "x")) }
        assertFailsWith<ValorInvalido> { fabrica.gasto(DadosDoGasto(inter, Dinheiro(-500), data, "x")) }
        assertFailsWith<ValorInvalido> { fabrica.gasto(DadosDoGasto(inter, null, data, "x")) }
    }

    @Test
    fun `RN05 - gasto em dolar e convertido para reais pela cotacao`() {
        val gasto = fabrica.gasto(
            DadosDoGasto(wise, valor = null, LocalDate(2026, 11, 10), "Assinatura", valorEmMoeda = 1_250, cotacao = 54_321),
        )

        assertEquals(Dinheiro(6_790), gasto.valor)
        assertEquals(ConversaoCambial(1_250, 54_321), gasto.conversao)
        assertEquals(novembro, gasto.competencia)
    }

    @Test
    fun `RN05 - gasto em moeda estrangeira sem cotacao ou com cotacao zero e rejeitado`() {
        val data = LocalDate(2026, 11, 10)
        assertFailsWith<CotacaoInvalida> { fabrica.gasto(DadosDoGasto(wise, null, data, "x", valorEmMoeda = 1_250)) }
        assertFailsWith<CotacaoInvalida> { fabrica.gasto(DadosDoGasto(wise, null, data, "x", valorEmMoeda = 1_250, cotacao = 0)) }
    }

    @Test
    fun `RN05 - gasto em moeda estrangeira sem valor na moeda e rejeitado`() {
        val data = LocalDate(2026, 11, 10)
        assertFailsWith<ValorInvalido> { fabrica.gasto(DadosDoGasto(wise, null, data, "x", cotacao = 54_321)) }
        assertFailsWith<ValorInvalido> { fabrica.gasto(DadosDoGasto(wise, null, data, "x", valorEmMoeda = 0, cotacao = 54_321)) }
    }

    @Test
    fun `RN11 - aporte realizado e um gasto na corretora na data`() {
        val aporte = fabrica.gasto(DadosDoGasto(clear, Dinheiro(30_000), LocalDate(2026, 11, 20), "Aporte"))
        assertEquals(clear, aporte.meio)
        assertEquals(novembro, aporte.competencia)
    }

    @Test
    fun `RN11 - aporte com valor zero e rejeitado`() {
        assertFailsWith<ValorInvalido> { fabrica.gasto(DadosDoGasto(clear, Dinheiro(0), LocalDate(2026, 11, 20), "Aporte")) }
    }

    // ---------- compra parcelada (UC02) ----------

    @Test
    fun `RN02 - compra parcelada valida gera as parcelas`() {
        val compra = fabrica.compraParcelada(
            DadosDaCompra(nubank, Dinheiro(10_000), 3, LocalDate(2026, 11, 3), "Geladeira", terceiro = pai),
        )
        val parcelas = compra.gerarParcelas()

        assertEquals(listOf("1/3", "2/3", "3/3"), parcelas.map { it.rotulo() })
        assertEquals(listOf(MesAno(2026, 11), MesAno(2026, 12), MesAno(2027, 1)), parcelas.map { it.competencia })
        assertTrue(parcelas.all { it.terceiro == pai && it.descricao == "Geladeira" })
    }

    @Test
    fun `RN02 - compra com 1 ou 49 parcelas e rejeitada`() {
        val data = LocalDate(2026, 11, 3)
        assertFailsWith<ParcelasForaDoLimite> { fabrica.compraParcelada(DadosDaCompra(nubank, Dinheiro(10_000), 1, data, "x")) }
        assertFailsWith<ParcelasForaDoLimite> { fabrica.compraParcelada(DadosDaCompra(nubank, Dinheiro(10_000), 49, data, "x")) }
    }

    @Test
    fun `RN09 - compra com valor zero ou menor que 1 centavo por parcela e rejeitada`() {
        val data = LocalDate(2026, 11, 3)
        assertFailsWith<ValorInvalido> { fabrica.compraParcelada(DadosDaCompra(nubank, Dinheiro(0), 3, data, "x")) }
        assertFailsWith<ValorInvalido> { fabrica.compraParcelada(DadosDaCompra(nubank, Dinheiro(2), 3, data, "x")) }
    }

    // ---------- previstos do mês (UC03) ----------

    @Test
    fun `RN06 RN07 e RN11 - previstos do mes reunem contas fixas salario e aportes ativos`() {
        val luz = ContaFixa("Luz", Dinheiro(20_000), 15, inter)
        val internetCancelada = ContaFixa("Internet", Dinheiro(10_000), 10, inter, ativa = false)
        val salario = ReceitaRecorrente("Salário", Dinheiro(800_000), 5)
        val binance = ContaInvestimento("Binance", gabriel, Instituicao("Binance")) // sem aporte planejado
        val corretoraDesativada = ContaInvestimento("XP", gabriel, Instituicao("XP"), Dinheiro(10_000), ativo = false)

        val previstos = fabrica.previstosDoMes(
            novembro,
            contasFixas = listOf(luz, internetCancelada),
            receitas = listOf(salario),
            investimentos = listOf(clear, binance, corretoraDesativada),
        )

        assertEquals(listOf(luz.id, salario.id, clear.id), previstos.map { it.origemId })
        assertTrue(previstos.all { it.situacao == SituacaoDoLancamento.PREVISTO && it.competencia == novembro })
        assertEquals(2, previstos.filterIsInstance<Gasto>().size)
        assertEquals(1, previstos.filterIsInstance<Receita>().size)
    }

    @Test
    fun `RN06 - sem recorrencias nao ha previstos`() {
        assertEquals(emptyList(), fabrica.previstosDoMes(novembro, emptyList(), emptyList(), emptyList()))
    }
}
