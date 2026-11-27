package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.InstallmentsOutOfRange
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InstallmentPurchaseTest {

    private val gabriel = Person("Gabriel", Role.USER)
    private val father = Person("Father", Role.THIRD_PARTY)
    // closes on day 5 and is due on day 12 (same month)
    private val nubank = CreditCard("Nubank", gabriel, Institution("Nubank"), closingDay = 5, dueDay = 12)

    private fun purchase(amount: Long, count: Int, date: LocalDate = LocalDate(2026, 11, 3), thirdParty: Person? = null) =
        InstallmentPurchase(Money(amount), count, date, "Fridge", nubank, thirdParty)

    @Test
    fun `RN02 - 100 reais in 3x gives installments with the remainder in the first`() {
        val installments = purchase(10_000, 3).generateInstallments()

        assertEquals(listOf(Money(3_334), Money(3_333), Money(3_333)), installments.map { it.amount })
        assertEquals(Money(10_000), installments.map { it.amount }.reduce(Money::plus))
    }

    @Test
    fun `RN02 - installments are labeled k of N`() {
        assertEquals(listOf("1/3", "2/3", "3/3"), purchase(10_000, 3).generateInstallments().map { it.label() })
    }

    @Test
    fun `RN02 - installments in consecutive accounting months from the purchase statement`() {
        // purchase on 11/03 before the closing day (5) → 1st on the November statement
        val months = purchase(10_000, 3).generateInstallments().map { it.accountingMonth }
        assertEquals(listOf(MonthYear(2026, 11), MonthYear(2026, 12), MonthYear(2027, 1)), months)
    }

    @Test
    fun `RN01 and RN02 - a purchase after the closing day starts on the next statement`() {
        val months = purchase(10_000, 2, date = LocalDate(2026, 11, 6)).generateInstallments().map { it.accountingMonth }
        assertEquals(listOf(MonthYear(2026, 12), MonthYear(2027, 1)), months)
    }

    @Test
    fun `RN02 - installment date moves one month per installment with a missing day on the last day`() {
        val dates = purchase(10_000, 3, date = LocalDate(2027, 1, 31)).generateInstallments().map { it.date }
        assertEquals(listOf(LocalDate(2027, 1, 31), LocalDate(2027, 2, 28), LocalDate(2027, 3, 31)), dates)
    }

    @Test
    fun `RN02 and D5 - installments keep the purchase id the card and the description`() {
        val purchase = purchase(10_000, 4)
        val installments = purchase.generateInstallments()

        assertTrue(installments.all { it.purchaseId == purchase.id && it.paymentMethod == nubank && it.description == "Fridge" })
        assertEquals(4, installments.map { it.id }.toSet().size)
    }

    @Test
    fun `RN02 - 48 installments add up to the total`() {
        val installments = purchase(123_457, 48).generateInstallments()
        assertEquals(48, installments.size)
        assertEquals(Money(123_457), installments.map { it.amount }.reduce(Money::plus))
        assertEquals("48/48", installments.last().label())
    }

    @Test
    fun `RN03 - every installment is on behalf of the third party`() {
        assertTrue(purchase(10_000, 3, thirdParty = father).generateInstallments().all { it.thirdParty == father && it.isThirdParty() })
    }

    @Test
    fun `RN02 - 1 or 49 installments is rejected`() {
        assertEquals("RN02", assertFailsWith<InstallmentsOutOfRange> { purchase(10_000, 1) }.rule)
        assertFailsWith<InstallmentsOutOfRange> { purchase(10_000, 49) }
    }

    @Test
    fun `RN09 - purchase with zero total amount is rejected`() {
        assertFailsWith<InvalidAmount> { purchase(0, 3) }
    }

    @Test
    fun `RN09 - purchase leaving an installment with zero cents is rejected`() {
        // R$ 0.01 in 2x would give an installment of R$ 0.00
        assertFailsWith<InvalidAmount> { purchase(1, 2).generateInstallments() }
    }
}
