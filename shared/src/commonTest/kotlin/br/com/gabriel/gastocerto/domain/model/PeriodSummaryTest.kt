package br.com.gabriel.gastocerto.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PeriodSummaryTest {

    private val gabriel = Person("Gabriel", Role.USER)
    private val spouse = Person("Spouse", Role.HOLDER)
    private val father = Person("Father", Role.THIRD_PARTY)
    private val nubankInst = Institution("Nubank")
    private val gabrielsNubank = CreditCard("Nubank Gabriel", gabriel, nubankInst, 5, 12)
    private val spousesNubank = CreditCard("Nubank Spouse", spouse, nubankInst, 20, 27)
    private val inter = CheckingAccount("Inter", gabriel, Institution("Inter"))
    private val november = MonthYear(2026, 11)
    private val day = LocalDate(2026, 11, 10)

    private fun expense(amount: Long, method: PaymentMethod, thirdParty: Person? = null) =
        Expense(Money(amount), day, "expense", november, method, thirdParty)

    private fun income(amount: Long) = Income(Money(amount), day, "income", november, IncomeType.SALARY)

    // Income 8,000; expenses 3,000 (500 of them for the father) → surplus = 8,000 − (3,000 − 500) = 5,500
    private val entries = listOf(
        income(800_000),
        expense(100_000, gabrielsNubank),
        expense(50_000, gabrielsNubank, thirdParty = father),
        expense(120_000, spousesNubank),
        expense(30_000, inter),
    )

    @Test
    fun `RN04 - surplus deducts only the household's own expenses`() {
        val summary = PeriodSummary.of(entries)

        assertEquals(Money(800_000), summary.income)
        assertEquals(Money(300_000), summary.totalExpenses)
        assertEquals(Money(50_000), summary.thirdPartyExpenses)
        assertEquals(Money(550_000), summary.surplus())
    }

    @Test
    fun `RN04 - expenses larger than income give a negative surplus`() {
        val summary = PeriodSummary.of(listOf(income(10_000), expense(25_000, inter)))
        assertEquals(Money(-15_000), summary.surplus())
    }

    @Test
    fun `RN04 - a period with no entries is all zero`() {
        val summary = PeriodSummary.of(emptyList())
        assertEquals(Money(0), summary.surplus())
        assertEquals(emptyMap(), summary.byPaymentMethod)
    }

    @Test
    fun `RN03 - a third-party expense counts toward the payment method and the total`() {
        val summary = PeriodSummary.of(entries)
        assertEquals(Money(150_000), summary.byPaymentMethod[gabrielsNubank])
    }

    @Test
    fun `RN10 - total expenses add up every holder and break down by holder and payment method`() {
        val summary = PeriodSummary.of(entries)

        assertEquals(mapOf(gabriel to Money(180_000), spouse to Money(120_000)), summary.byHolder)
        assertEquals(3, summary.byPaymentMethod.size)
        assertEquals(Money(30_000), summary.byPaymentMethod[inter])
    }

    @Test
    fun `RN10 - holder filter considers only expenses of that holder's payment methods`() {
        val summary = PeriodSummary.of(entries, holder = spouse)

        assertEquals(Money(120_000), summary.totalExpenses)
        assertEquals(Money(0), summary.thirdPartyExpenses)
        assertEquals(setOf(spousesNubank), summary.byPaymentMethod.keys)
        assertEquals(Money(800_000), summary.income) // income has no holder
    }

    @Test
    fun `RN10 - a holder with no payment methods in the period has zero expenses`() {
        val other = Person("Other", Role.HOLDER)
        assertEquals(Money(0), PeriodSummary.of(entries, holder = other).totalExpenses)
    }
}
