package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.ReembolsoMaiorQueSaldo
import br.com.gabriel.gastocerto.domain.regra.ValorInvalido
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SaldoAReceberTest {

    private val gabriel = Pessoa("Gabriel", Papel.USUARIO)
    private val pai = Pessoa("Pai", Papel.TERCEIRO)
    private val sogra = Pessoa("Sogra", Papel.TERCEIRO)
    private val cartao = CartaoDeCredito("Nubank", gabriel, Instituicao("Nubank"), 5, 12)
    private val dia = LocalDate(2026, 11, 10)

    private fun gasto(valor: Long, terceiro: Pessoa?) =
        Gasto(Dinheiro(valor), dia, "gasto", MesAno(2026, 11), cartao, terceiro)

    private val gastos = listOf(gasto(30_000, pai), gasto(20_000, pai), gasto(10_000, sogra), gasto(99_000, null))

    @Test
    fun `RN03 - saldo soma os gastos do terceiro e desconta os reembolsos dele`() {
        val reembolsos = listOf(Reembolso(pai, Dinheiro(15_000), dia), Reembolso(sogra, Dinheiro(10_000), dia))

        val saldo = SaldoAReceber.de(pai, gastos, reembolsos)

        assertEquals(pai, saldo.terceiro)
        assertEquals(Dinheiro(35_000), saldo.valor)
    }

    @Test
    fun `RN03 - reembolso ate o saldo e aceito`() {
        val saldo = SaldoAReceber.de(pai, gastos, emptyList())
        saldo.validarReembolso(Dinheiro(50_000)) // igual ao saldo: não lança
        saldo.validarReembolso(Dinheiro(1))
    }

    @Test
    fun `RN03 - reembolso maior que o saldo e rejeitado`() {
        val saldo = SaldoAReceber.de(sogra, gastos, emptyList())
        val erro = assertFailsWith<ReembolsoMaiorQueSaldo> { saldo.validarReembolso(Dinheiro(10_001)) }
        assertEquals("RN03", erro.regra)
    }

    @Test
    fun `RN03 - terceiro sem gastos tem saldo zero e nao aceita reembolso`() {
        val saldo = SaldoAReceber.de(Pessoa("Tio", Papel.TERCEIRO), gastos, emptyList())
        assertEquals(Dinheiro(0), saldo.valor)
        assertFailsWith<ReembolsoMaiorQueSaldo> { saldo.validarReembolso(Dinheiro(1)) }
    }

    @Test
    fun `RN09 - reembolso com valor zero ou negativo e rejeitado`() {
        assertFailsWith<ValorInvalido> { Reembolso(pai, Dinheiro(0), dia) }
        assertFailsWith<ValorInvalido> { Reembolso(pai, Dinheiro(-100), dia) }
    }
}
