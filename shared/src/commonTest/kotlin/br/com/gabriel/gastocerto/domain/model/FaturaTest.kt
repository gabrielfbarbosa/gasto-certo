package br.com.gabriel.gastocerto.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FaturaTest {

    private val gabriel = Pessoa("Gabriel", Papel.USUARIO)
    private val nubankInst = Instituicao("Nubank")
    // fecha 5, vence 12 (mesmo mês)
    private val nubank = CartaoDeCredito("Nubank", gabriel, nubankInst, diaFechamento = 5, diaVencimento = 12)
    // fecha 25, vence 2 (mês seguinte)
    private val c6 = CartaoDeCredito("C6", gabriel, Instituicao("C6"), diaFechamento = 25, diaVencimento = 2)
    private val novembro = MesAno(2026, 11)

    private fun gasto(valor: Long, cartao: CartaoDeCredito, competencia: MesAno) =
        Gasto(Dinheiro(valor), LocalDate(2026, 11, 1), "gasto", competencia, cartao)

    @Test
    fun `RF13 - fatura soma so os gastos do cartao na competencia`() {
        val gastos = listOf(
            gasto(10_000, nubank, novembro),
            gasto(5_050, nubank, novembro),
            gasto(7_000, nubank, MesAno(2026, 12)),
            gasto(9_000, c6, novembro),
        )

        val fatura = Fatura.de(nubank, novembro, gastos)

        assertEquals(2, fatura.gastos.size)
        assertEquals(Dinheiro(15_050), fatura.total())
    }

    @Test
    fun `RF13 e RN01 - vencimento depois do fechamento fecha e vence no mesmo mes`() {
        val fatura = Fatura.de(nubank, novembro, emptyList())
        assertEquals(LocalDate(2026, 11, 5), fatura.dataFechamento)
        assertEquals(LocalDate(2026, 11, 12), fatura.dataVencimento)
    }

    @Test
    fun `RF13 e RN01 - vencimento antes do fechamento fecha no mes anterior`() {
        val fatura = Fatura.de(c6, MesAno(2027, 1), emptyList())
        assertEquals(LocalDate(2026, 12, 25), fatura.dataFechamento)
        assertEquals(LocalDate(2027, 1, 2), fatura.dataVencimento)
    }

    @Test
    fun `RF13 - fatura com parcelas da compra parcelada`() {
        val parcelas = CompraParcelada(Dinheiro(10_000), 3, LocalDate(2026, 11, 3), "TV", nubank).gerarParcelas()

        val dezembro = Fatura.de(nubank, MesAno(2026, 12), parcelas)

        assertEquals(listOf("2/3"), dezembro.gastos.filterIsInstance<Parcela>().map { it.rotulo() })
        assertEquals(Dinheiro(3_333), dezembro.total())
    }

    @Test
    fun `RF13 - fatura sem gastos tem total zero`() {
        assertEquals(Dinheiro(0), Fatura.de(nubank, novembro, emptyList()).total())
    }
}
