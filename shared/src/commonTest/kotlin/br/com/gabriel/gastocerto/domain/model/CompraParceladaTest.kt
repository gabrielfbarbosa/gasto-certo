package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.ParcelasForaDoLimite
import br.com.gabriel.gastocerto.domain.regra.ValorInvalido
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompraParceladaTest {

    private val gabriel = Pessoa("Gabriel", Papel.USUARIO)
    private val pai = Pessoa("Pai", Papel.TERCEIRO)
    // fecha dia 5 e vence dia 12 (mesmo mês)
    private val nubank = CartaoDeCredito("Nubank", gabriel, Instituicao("Nubank"), diaFechamento = 5, diaVencimento = 12)

    private fun compra(valor: Long, quantidade: Int, data: LocalDate = LocalDate(2026, 11, 3), terceiro: Pessoa? = null) =
        CompraParcelada(Dinheiro(valor), quantidade, data, "Geladeira", nubank, terceiro)

    @Test
    fun `RN02 - 100 reais em 3x gera parcelas com o resto na primeira`() {
        val parcelas = compra(10_000, 3).gerarParcelas()

        assertEquals(listOf(Dinheiro(3_334), Dinheiro(3_333), Dinheiro(3_333)), parcelas.map { it.valor })
        assertEquals(Dinheiro(10_000), parcelas.map { it.valor }.reduce(Dinheiro::plus))
    }

    @Test
    fun `RN02 - parcelas sao rotuladas k de N`() {
        assertEquals(listOf("1/3", "2/3", "3/3"), compra(10_000, 3).gerarParcelas().map { it.rotulo() })
    }

    @Test
    fun `RN02 - parcelas em competencias consecutivas a partir da fatura da compra`() {
        // compra 03/11 antes do fechamento (5) → 1ª na fatura de novembro
        val competencias = compra(10_000, 3).gerarParcelas().map { it.competencia }
        assertEquals(listOf(MesAno(2026, 11), MesAno(2026, 12), MesAno(2027, 1)), competencias)
    }

    @Test
    fun `RN01 e RN02 - compra depois do fechamento comeca na fatura seguinte`() {
        val competencias = compra(10_000, 2, data = LocalDate(2026, 11, 6)).gerarParcelas().map { it.competencia }
        assertEquals(listOf(MesAno(2026, 12), MesAno(2027, 1)), competencias)
    }

    @Test
    fun `RN02 - data da parcela avanca um mes por parcela com dia inexistente no ultimo dia`() {
        val datas = compra(10_000, 3, data = LocalDate(2027, 1, 31)).gerarParcelas().map { it.data }
        assertEquals(listOf(LocalDate(2027, 1, 31), LocalDate(2027, 2, 28), LocalDate(2027, 3, 31)), datas)
    }

    @Test
    fun `RN02 e D5 - parcelas guardam o id da compra o cartao e a descricao`() {
        val compra = compra(10_000, 4)
        val parcelas = compra.gerarParcelas()

        assertTrue(parcelas.all { it.compraId == compra.id && it.meio == nubank && it.descricao == "Geladeira" })
        assertEquals(4, parcelas.map { it.id }.toSet().size)
    }

    @Test
    fun `RN02 - 48 parcelas somam o total`() {
        val parcelas = compra(123_457, 48).gerarParcelas()
        assertEquals(48, parcelas.size)
        assertEquals(Dinheiro(123_457), parcelas.map { it.valor }.reduce(Dinheiro::plus))
        assertEquals("48/48", parcelas.last().rotulo())
    }

    @Test
    fun `RN03 - todas as parcelas ficam em nome do terceiro`() {
        assertTrue(compra(10_000, 3, terceiro = pai).gerarParcelas().all { it.terceiro == pai && it.ehDeTerceiro() })
    }

    @Test
    fun `RN02 - 1 ou 49 parcelas e rejeitado`() {
        assertEquals("RN02", assertFailsWith<ParcelasForaDoLimite> { compra(10_000, 1) }.regra)
        assertFailsWith<ParcelasForaDoLimite> { compra(10_000, 49) }
    }

    @Test
    fun `RN09 - compra com valor total zero e rejeitada`() {
        assertFailsWith<ValorInvalido> { compra(0, 3) }
    }

    @Test
    fun `RN09 - parcela que ficaria com zero centavos e rejeitada`() {
        // R$ 0,01 em 2x daria uma parcela de R$ 0,00
        assertFailsWith<ValorInvalido> { compra(1, 2).gerarParcelas() }
    }
}
