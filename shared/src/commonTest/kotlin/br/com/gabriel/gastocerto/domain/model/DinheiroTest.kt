package br.com.gabriel.gastocerto.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DinheiroTest {

    // ---------- RNF04: precisão exata em centavos ----------

    @Test
    fun `RNF04 - soma e subtrai em centavos sem erro de ponto flutuante`() {
        // 0,10 + 0,20 = 0,30 exato (em Double daria 0,30000000000000004)
        assertEquals(Dinheiro(30), Dinheiro(10) + Dinheiro(20))
        assertEquals(Dinheiro(-5_000), Dinheiro(10_000) - Dinheiro(15_000))
    }

    @Test
    fun `RNF04 - mil somas de um centavo resultam em exatamente dez reais`() {
        val total = (1..1_000).fold(Dinheiro(0)) { acc, _ -> acc + Dinheiro(1) }
        assertEquals(Dinheiro(1_000), total)
    }

    // ---------- RN02: divisão em parcelas, resto na 1ª ----------

    @Test
    fun `RN02 - divide 100 reais em 3 parcelas sem perder centavos`() {
        val parcelas = Dinheiro(10_000).dividirEm(3)

        assertEquals(listOf(Dinheiro(3_334), Dinheiro(3_333), Dinheiro(3_333)), parcelas)
        assertEquals(Dinheiro(10_000), parcelas.reduce(Dinheiro::plus))
    }

    @Test
    fun `RN02 - divisao exata nao deixa resto`() {
        assertEquals(List(4) { Dinheiro(2_500) }, Dinheiro(10_000).dividirEm(4))
    }

    @Test
    fun `RN02 - divide em 48 parcelas e a soma continua igual ao total`() {
        val total = Dinheiro(123_457)
        val parcelas = total.dividirEm(48)

        assertEquals(48, parcelas.size)
        assertEquals(total, parcelas.reduce(Dinheiro::plus))
        assertEquals(Dinheiro(2_572 + 1), parcelas.first()) // 123457 = 48 × 2572 + 1
        assertTrue(parcelas.drop(1).all { it == Dinheiro(2_572) })
    }

    @Test
    fun `RN02 - dividir em zero partes e rejeitado`() {
        assertFailsWith<IllegalArgumentException> { Dinheiro(10_000).dividirEm(0) }
    }

    // ---------- RN05: conversão de moeda estrangeira (D1) ----------

    @Test
    fun `RN05 - converte dolares para reais pela cotacao com 4 casas`() {
        // US$ 12,50 × 5,4321 = R$ 67,90125 → R$ 67,90
        assertEquals(Dinheiro(6_790), Dinheiro(1_250).converter(cotacao = 54_321))
    }

    @Test
    fun `RN05 - arredonda meio centavo para cima - half-up`() {
        // US$ 0,01 × 0,5000 = R$ 0,005 → R$ 0,01
        assertEquals(Dinheiro(1), Dinheiro(1).converter(cotacao = 5_000))
        // US$ 0,01 × 0,4999 = R$ 0,004999 → R$ 0,00
        assertEquals(Dinheiro(0), Dinheiro(1).converter(cotacao = 4_999))
    }

    @Test
    fun `RN05 - cotacao 1 mantem o valor`() {
        assertEquals(Dinheiro(98_765), Dinheiro(98_765).converter(cotacao = Dinheiro.ESCALA_DA_COTACAO))
    }

    @Test
    fun `RN05 - cotacao zero ou negativa e rejeitada`() {
        assertFailsWith<IllegalArgumentException> { Dinheiro(1_250).converter(cotacao = 0) }
        assertFailsWith<IllegalArgumentException> { Dinheiro(1_250).converter(cotacao = -54_321) }
    }

    // ---------- RN09: valor positivo ----------

    @Test
    fun `RN09 - valor maior que zero e positivo`() {
        assertTrue(Dinheiro(1).ehPositivo())
    }

    @Test
    fun `RN09 - valor zero ou negativo nao e positivo`() {
        assertFalse(Dinheiro(0).ehPositivo())
        assertFalse(Dinheiro(-1).ehPositivo())
    }
}
