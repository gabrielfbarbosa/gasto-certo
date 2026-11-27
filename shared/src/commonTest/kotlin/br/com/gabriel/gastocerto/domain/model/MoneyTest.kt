package br.com.gabriel.gastocerto.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoneyTest {

    // ---------- RNF04: exact precision in cents ----------

    @Test
    fun `RNF04 - adds and subtracts in cents with no floating point error`() {
        // 0.10 + 0.20 = 0.30 exactly (as Double it would be 0.30000000000000004)
        assertEquals(Money(30), Money(10) + Money(20))
        assertEquals(Money(-5_000), Money(10_000) - Money(15_000))
    }

    @Test
    fun `RNF04 - one thousand additions of one cent give exactly ten reais`() {
        val total = (1..1_000).fold(Money(0)) { acc, _ -> acc + Money(1) }
        assertEquals(Money(1_000), total)
    }

    // ---------- RN02: split into installments, remainder in the 1st ----------

    @Test
    fun `RN02 - splits 100 reais into 3 installments without losing cents`() {
        val installments = Money(10_000).splitInto(3)

        assertEquals(listOf(Money(3_334), Money(3_333), Money(3_333)), installments)
        assertEquals(Money(10_000), installments.reduce(Money::plus))
    }

    @Test
    fun `RN02 - an even split leaves no remainder`() {
        assertEquals(List(4) { Money(2_500) }, Money(10_000).splitInto(4))
    }

    @Test
    fun `RN02 - splits into 48 installments and the sum stays equal to the total`() {
        val total = Money(123_457)
        val installments = total.splitInto(48)

        assertEquals(48, installments.size)
        assertEquals(total, installments.reduce(Money::plus))
        assertEquals(Money(2_572 + 1), installments.first()) // 123457 = 48 × 2572 + 1
        assertTrue(installments.drop(1).all { it == Money(2_572) })
    }

    @Test
    fun `RN02 - splitting into zero parts is rejected`() {
        assertFailsWith<IllegalArgumentException> { Money(10_000).splitInto(0) }
    }

    // ---------- RN05: foreign currency conversion (D1) ----------

    @Test
    fun `RN05 - converts dollars to reais with a 4 decimal exchange rate`() {
        // US$ 12.50 × 5.4321 = R$ 67.90125 → R$ 67.90
        assertEquals(Money(6_790), Money(1_250).convert(exchangeRate = 54_321))
    }

    @Test
    fun `RN05 - rounds half a cent up - half-up`() {
        // US$ 0.01 × 0.5000 = R$ 0.005 → R$ 0.01
        assertEquals(Money(1), Money(1).convert(exchangeRate = 5_000))
        // US$ 0.01 × 0.4999 = R$ 0.004999 → R$ 0.00
        assertEquals(Money(0), Money(1).convert(exchangeRate = 4_999))
    }

    @Test
    fun `RN05 - exchange rate 1 keeps the amount`() {
        assertEquals(Money(98_765), Money(98_765).convert(exchangeRate = Money.EXCHANGE_RATE_SCALE))
    }

    @Test
    fun `RN05 - zero or negative exchange rate is rejected`() {
        assertFailsWith<IllegalArgumentException> { Money(1_250).convert(exchangeRate = 0) }
        assertFailsWith<IllegalArgumentException> { Money(1_250).convert(exchangeRate = -54_321) }
    }

    // ---------- RN09: positive amount ----------

    @Test
    fun `RN09 - an amount greater than zero is positive`() {
        assertTrue(Money(1).isPositive())
    }

    @Test
    fun `RN09 - zero or negative amount is not positive`() {
        assertFalse(Money(0).isPositive())
        assertFalse(Money(-1).isPositive())
    }
}
