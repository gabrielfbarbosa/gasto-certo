package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.ReimbursementExceedsBalance
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReceivableBalanceTest {

    private val gabriel = Person("Gabriel", Role.USER)
    private val father = Person("Father", Role.THIRD_PARTY)
    private val motherInLaw = Person("Mother-in-law", Role.THIRD_PARTY)
    private val card = CreditCard("Nubank", gabriel, Institution("Nubank"), 5, 12)
    private val day = LocalDate(2026, 11, 10)

    private fun expense(amount: Long, thirdParty: Person?) =
        Expense(Money(amount), day, "expense", MonthYear(2026, 11), card, thirdParty)

    private val expenses = listOf(
        expense(30_000, father), expense(20_000, father), expense(10_000, motherInLaw), expense(99_000, null),
    )

    @Test
    fun `RN03 - balance adds the third party's expenses and deducts their reimbursements`() {
        val reimbursements = listOf(Reimbursement(father, Money(15_000), day), Reimbursement(motherInLaw, Money(10_000), day))

        val balance = ReceivableBalance.of(father, expenses, reimbursements)

        assertEquals(father, balance.thirdParty)
        assertEquals(Money(35_000), balance.amount)
    }

    @Test
    fun `RN03 - a reimbursement up to the balance is accepted`() {
        val balance = ReceivableBalance.of(father, expenses, emptyList())
        balance.validateReimbursement(Money(50_000)) // equal to the balance: does not throw
        balance.validateReimbursement(Money(1))
    }

    @Test
    fun `RN03 - a reimbursement larger than the balance is rejected`() {
        val balance = ReceivableBalance.of(motherInLaw, expenses, emptyList())
        val error = assertFailsWith<ReimbursementExceedsBalance> { balance.validateReimbursement(Money(10_001)) }
        assertEquals("RN03", error.rule)
    }

    @Test
    fun `RN03 - a third party with no expenses has zero balance and accepts no reimbursement`() {
        val balance = ReceivableBalance.of(Person("Uncle", Role.THIRD_PARTY), expenses, emptyList())
        assertEquals(Money(0), balance.amount)
        assertFailsWith<ReimbursementExceedsBalance> { balance.validateReimbursement(Money(1)) }
    }

    @Test
    fun `RN09 - reimbursement with zero or negative amount is rejected`() {
        assertFailsWith<InvalidAmount> { Reimbursement(father, Money(0), day) }
        assertFailsWith<InvalidAmount> { Reimbursement(father, Money(-100), day) }
    }
}
