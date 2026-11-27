package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.ExpenseDatePolicy
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.InvalidDay
import br.com.gabriel.gastocerto.domain.rule.InvalidHolder
import br.com.gabriel.gastocerto.domain.rule.StatementClosingPolicy
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class RegistryTest {

    private val gabriel = Person("Gabriel", Role.USER)
    private val spouse = Person("Spouse", Role.HOLDER)
    private val father = Person("Father", Role.THIRD_PARTY)
    private val nubank = Institution("Nubank")

    // ---------- D2: identity ----------

    @Test
    fun `D2 - each registration gets a different id`() {
        assertNotEquals(Person("Ana", Role.HOLDER).id, Person("Ana", Role.HOLDER).id)
        assertNotEquals(Institution("Inter"), Institution("Inter"))
    }

    @Test
    fun `D2 - equality is by id and not by the other fields`() {
        assertEquals(Person("Gabriel", Role.USER, id = "p1"), Person("Gabriel F", Role.HOLDER, id = "p1"))
        assertEquals<PaymentMethod>(
            CheckingAccount("Inter", gabriel, nubank, id = "m1"),
            CreditCard("Inter Black", gabriel, nubank, 5, 12, id = "m1"),
        )
    }

    // ---------- RN10: holder ----------

    @Test
    fun `RN10 - an institution may have several cards of different holders`() {
        val gabrielsCard = CreditCard("Nubank Gabriel", gabriel, nubank, closingDay = 5, dueDay = 12)
        val spousesCard = CreditCard("Nubank Spouse", spouse, nubank, closingDay = 20, dueDay = 27)

        assertEquals(gabrielsCard.institution, spousesCard.institution)
        assertNotEquals(gabrielsCard.holder, spousesCard.holder)
    }

    @Test
    fun `RN10 - a third party cannot hold a payment method`() {
        val error = assertFailsWith<InvalidHolder> { CheckingAccount("Father's account", father, nubank) }
        assertEquals("RN10", error.rule)
    }

    // ---------- RN09: card days ----------

    @Test
    fun `RN09 - card accepts closing and due days between 1 and 31`() {
        val card = CreditCard("Nubank", gabriel, nubank, closingDay = 31, dueDay = 1)
        assertEquals(31, card.closingDay)
        assertEquals(1, card.dueDay)
    }

    @Test
    fun `RN09 - card with closing day outside 1 to 31 is rejected`() {
        assertFailsWith<InvalidDay> { CreditCard("Nubank", gabriel, nubank, closingDay = 0, dueDay = 12) }
        assertFailsWith<InvalidDay> { CreditCard("Nubank", gabriel, nubank, closingDay = 32, dueDay = 12) }
    }

    @Test
    fun `RN09 - card with due day outside 1 to 31 is rejected`() {
        val error = assertFailsWith<InvalidDay> {
            CreditCard("Nubank", gabriel, nubank, closingDay = 5, dueDay = 40)
        }
        assertEquals("RN09", error.rule)
    }

    @Test
    fun `RN09 - a deactivated payment method stays registered`() {
        val account = CheckingAccount("Inter", gabriel, Institution("Inter"))
        account.active = false
        assertFalse(account.active)
    }

    // ---------- RN01: each payment method returns its policy (Strategy) ----------

    @Test
    fun `RN01 - credit card uses the statement closing policy`() {
        val card = CreditCard("Nubank", gabriel, nubank, closingDay = 5, dueDay = 12)
        val policy = card.accountingMonthPolicy()

        assertIs<StatementClosingPolicy>(policy)
        assertEquals(MonthYear(2026, 12), policy.accountingMonthOf(LocalDate(2026, 11, 6)))
    }

    @Test
    fun `RN01 - checking account wallet and brokerage use the month of the expense date`() {
        val methods = listOf(
            CheckingAccount("Inter", gabriel, Institution("Inter")),
            ForeignCurrencyWallet("Wise", gabriel, Institution("Wise"), currency = "USD"),
            InvestmentAccount("Clear", gabriel, Institution("Clear")),
        )
        methods.forEach { assertIs<ExpenseDatePolicy>(it.accountingMonthPolicy()) }
    }

    // ---------- RN11: planned contribution ----------

    @Test
    fun `RN11 - brokerage accepts a positive planned contribution or none`() {
        assertEquals(Money(50_000), InvestmentAccount("Clear", gabriel, Institution("Clear"), Money(50_000)).plannedMonthlyContribution)
        assertEquals(null, InvestmentAccount("Binance", gabriel, Institution("Binance")).plannedMonthlyContribution)
    }

    @Test
    fun `RN11 - zero or negative planned contribution is rejected`() {
        assertFailsWith<InvalidAmount> { InvestmentAccount("Clear", gabriel, Institution("Clear"), Money(0)) }
        assertFailsWith<InvalidAmount> { InvestmentAccount("Clear", gabriel, Institution("Clear"), Money(-100)) }
    }
}
