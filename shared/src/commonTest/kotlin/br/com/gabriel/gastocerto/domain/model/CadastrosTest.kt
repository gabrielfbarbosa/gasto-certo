package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.CompetenciaPorDataDoGasto
import br.com.gabriel.gastocerto.domain.regra.CompetenciaPorFechamentoDeFatura
import br.com.gabriel.gastocerto.domain.regra.DiaInvalido
import br.com.gabriel.gastocerto.domain.regra.TitularInvalido
import br.com.gabriel.gastocerto.domain.regra.ValorInvalido
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class CadastrosTest {

    private val gabriel = Pessoa("Gabriel", Papel.USUARIO)
    private val esposa = Pessoa("Esposa", Papel.TITULAR)
    private val pai = Pessoa("Pai", Papel.TERCEIRO)
    private val nubank = Instituicao("Nubank")

    // ---------- D2: identidade ----------

    @Test
    fun `D2 - cada cadastro recebe um id diferente`() {
        assertNotEquals(Pessoa("Ana", Papel.TITULAR).id, Pessoa("Ana", Papel.TITULAR).id)
        assertNotEquals(Instituicao("Inter"), Instituicao("Inter"))
    }

    @Test
    fun `D2 - igualdade e pelo id e nao pelos outros campos`() {
        assertEquals(Pessoa("Gabriel", Papel.USUARIO, id = "p1"), Pessoa("Gabriel F", Papel.TITULAR, id = "p1"))
        assertEquals<MeioDePagamento>(
            ContaCorrente("Inter", gabriel, nubank, id = "m1"),
            CartaoDeCredito("Inter Black", gabriel, nubank, 5, 12, id = "m1"),
        )
    }

    // ---------- RN10: titularidade ----------

    @Test
    fun `RN10 - uma instituicao pode ter varios cartoes de titulares diferentes`() {
        val doGabriel = CartaoDeCredito("Nubank Gabriel", gabriel, nubank, diaFechamento = 5, diaVencimento = 12)
        val daEsposa = CartaoDeCredito("Nubank Esposa", esposa, nubank, diaFechamento = 20, diaVencimento = 27)

        assertEquals(doGabriel.instituicao, daEsposa.instituicao)
        assertNotEquals(doGabriel.titular, daEsposa.titular)
    }

    @Test
    fun `RN10 - terceiro reembolsante nao pode ser titular de meio de pagamento`() {
        val erro = assertFailsWith<TitularInvalido> { ContaCorrente("Conta do pai", pai, nubank) }
        assertEquals("RN10", erro.regra)
    }

    // ---------- RN09: dias do cartão ----------

    @Test
    fun `RN09 - cartao aceita dias de fechamento e vencimento entre 1 e 31`() {
        val cartao = CartaoDeCredito("Nubank", gabriel, nubank, diaFechamento = 31, diaVencimento = 1)
        assertEquals(31, cartao.diaFechamento)
        assertEquals(1, cartao.diaVencimento)
    }

    @Test
    fun `RN09 - cartao com dia de fechamento fora de 1 a 31 e rejeitado`() {
        assertFailsWith<DiaInvalido> { CartaoDeCredito("Nubank", gabriel, nubank, diaFechamento = 0, diaVencimento = 12) }
        assertFailsWith<DiaInvalido> { CartaoDeCredito("Nubank", gabriel, nubank, diaFechamento = 32, diaVencimento = 12) }
    }

    @Test
    fun `RN09 - cartao com dia de vencimento fora de 1 a 31 e rejeitado`() {
        val erro = assertFailsWith<DiaInvalido> {
            CartaoDeCredito("Nubank", gabriel, nubank, diaFechamento = 5, diaVencimento = 40)
        }
        assertEquals("RN09", erro.regra)
    }

    @Test
    fun `RN09 - meio desativado continua cadastrado`() {
        val conta = ContaCorrente("Inter", gabriel, Instituicao("Inter"))
        conta.ativo = false
        assertFalse(conta.ativo)
    }

    // ---------- RN01: cada meio devolve a sua política (Strategy) ----------

    @Test
    fun `RN01 - cartao de credito usa a politica de fechamento de fatura`() {
        val cartao = CartaoDeCredito("Nubank", gabriel, nubank, diaFechamento = 5, diaVencimento = 12)
        val politica = cartao.politicaDeCompetencia()

        assertIs<CompetenciaPorFechamentoDeFatura>(politica)
        assertEquals(MesAno(2026, 12), politica.competenciaDe(LocalDate(2026, 11, 6)))
    }

    @Test
    fun `RN01 - conta carteira e corretora usam o mes da data do gasto`() {
        val meios = listOf(
            ContaCorrente("Inter", gabriel, Instituicao("Inter")),
            CarteiraMoedaEstrangeira("Wise", gabriel, Instituicao("Wise"), moeda = "USD"),
            ContaInvestimento("Clear", gabriel, Instituicao("Clear")),
        )
        meios.forEach { assertIs<CompetenciaPorDataDoGasto>(it.politicaDeCompetencia()) }
    }

    // ---------- RN11: aporte planejado ----------

    @Test
    fun `RN11 - corretora aceita aporte planejado positivo ou nenhum`() {
        assertEquals(Dinheiro(50_000), ContaInvestimento("Clear", gabriel, Instituicao("Clear"), Dinheiro(50_000)).aportePlanejadoMensal)
        assertEquals(null, ContaInvestimento("Binance", gabriel, Instituicao("Binance")).aportePlanejadoMensal)
    }

    @Test
    fun `RN11 - aporte planejado zero ou negativo e rejeitado`() {
        assertFailsWith<ValorInvalido> { ContaInvestimento("Clear", gabriel, Instituicao("Clear"), Dinheiro(0)) }
        assertFailsWith<ValorInvalido> { ContaInvestimento("Clear", gabriel, Instituicao("Clear"), Dinheiro(-100)) }
    }
}
