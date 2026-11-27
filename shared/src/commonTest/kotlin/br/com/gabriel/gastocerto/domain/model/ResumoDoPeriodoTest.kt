package br.com.gabriel.gastocerto.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ResumoDoPeriodoTest {

    private val gabriel = Pessoa("Gabriel", Papel.USUARIO)
    private val esposa = Pessoa("Esposa", Papel.TITULAR)
    private val pai = Pessoa("Pai", Papel.TERCEIRO)
    private val nubankInst = Instituicao("Nubank")
    private val nubankGabriel = CartaoDeCredito("Nubank Gabriel", gabriel, nubankInst, 5, 12)
    private val nubankEsposa = CartaoDeCredito("Nubank Esposa", esposa, nubankInst, 20, 27)
    private val inter = ContaCorrente("Inter", gabriel, Instituicao("Inter"))
    private val novembro = MesAno(2026, 11)
    private val dia = LocalDate(2026, 11, 10)

    private fun gasto(valor: Long, meio: MeioDePagamento, terceiro: Pessoa? = null) =
        Gasto(Dinheiro(valor), dia, "gasto", novembro, meio, terceiro)

    private fun receita(valor: Long) = Receita(Dinheiro(valor), dia, "receita", novembro, TipoDeReceita.SALARIO)

    // Receitas 8.000; gastos 3.000 (sendo 500 do pai) → sobra = 8.000 − (3.000 − 500) = 5.500
    private val lancamentos = listOf(
        receita(800_000),
        gasto(100_000, nubankGabriel),
        gasto(50_000, nubankGabriel, terceiro = pai),
        gasto(120_000, nubankEsposa),
        gasto(30_000, inter),
    )

    @Test
    fun `RN04 - sobra desconta so os gastos proprios`() {
        val resumo = ResumoDoPeriodo.de(lancamentos)

        assertEquals(Dinheiro(800_000), resumo.receitas)
        assertEquals(Dinheiro(300_000), resumo.gastoTotal)
        assertEquals(Dinheiro(50_000), resumo.gastosDeTerceiros)
        assertEquals(Dinheiro(550_000), resumo.sobra())
    }

    @Test
    fun `RN04 - gastos maiores que receitas dao sobra negativa`() {
        val resumo = ResumoDoPeriodo.de(listOf(receita(10_000), gasto(25_000, inter)))
        assertEquals(Dinheiro(-15_000), resumo.sobra())
    }

    @Test
    fun `RN04 - periodo sem lancamentos tem tudo zerado`() {
        val resumo = ResumoDoPeriodo.de(emptyList())
        assertEquals(Dinheiro(0), resumo.sobra())
        assertEquals(emptyMap(), resumo.porMeio)
    }

    @Test
    fun `RN03 - gasto de terceiro entra no total do meio e no gasto total`() {
        val resumo = ResumoDoPeriodo.de(lancamentos)
        assertEquals(Dinheiro(150_000), resumo.porMeio[nubankGabriel])
    }

    @Test
    fun `RN10 - gasto total soma todos os titulares e abre por titular e por meio`() {
        val resumo = ResumoDoPeriodo.de(lancamentos)

        assertEquals(mapOf(gabriel to Dinheiro(180_000), esposa to Dinheiro(120_000)), resumo.porTitular)
        assertEquals(3, resumo.porMeio.size)
        assertEquals(Dinheiro(30_000), resumo.porMeio[inter])
    }

    @Test
    fun `RN10 - filtro por titular considera so os gastos dos meios dele`() {
        val resumo = ResumoDoPeriodo.de(lancamentos, titular = esposa)

        assertEquals(Dinheiro(120_000), resumo.gastoTotal)
        assertEquals(Dinheiro(0), resumo.gastosDeTerceiros)
        assertEquals(setOf(nubankEsposa), resumo.porMeio.keys)
        assertEquals(Dinheiro(800_000), resumo.receitas) // receitas não têm titular
    }

    @Test
    fun `RN10 - titular sem meios no periodo tem gasto zero`() {
        val outro = Pessoa("Outro", Papel.TITULAR)
        assertEquals(Dinheiro(0), ResumoDoPeriodo.de(lancamentos, titular = outro).gastoTotal)
    }
}
