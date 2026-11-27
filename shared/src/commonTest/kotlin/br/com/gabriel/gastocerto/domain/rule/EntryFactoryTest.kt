package br.com.gabriel.gastocerto.domain.rule

import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.CurrencyConversion
import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.model.Role
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EntryFactoryTest {

    private val factory = EntryFactory()
    private val gabriel = Person("Gabriel", Role.USER)
    private val father = Person("Father", Role.THIRD_PARTY)
    private val nubank = CreditCard("Nubank", gabriel, Institution("Nubank"), closingDay = 5, dueDay = 12)
    private val inter = CheckingAccount("Inter", gabriel, Institution("Inter"))
    private val wise = ForeignCurrencyWallet("Wise", gabriel, Institution("Wise"), currency = "USD")
    private val clear = InvestmentAccount("Clear", gabriel, Institution("Clear"), plannedMonthlyContribution = Money(50_000))
    private val november = MonthYear(2026, 11)

    // ---------- single expense (UC01) ----------

    @Test
    fun `RN01 - a card expense after closing goes into the next statement`() {
        val expense = factory.expense(ExpenseInput(nubank, Money(15_000), LocalDate(2026, 11, 6), "Pharmacy"))

        assertEquals(MonthYear(2026, 12), expense.accountingMonth)
        assertEquals(Money(15_000), expense.amount)
        assertEquals(EntryStatus.ACTUAL, expense.status)
        assertNull(expense.conversion)
    }

    @Test
    fun `RN01 - a checking account expense goes into the month of its date`() {
        val expense = factory.expense(ExpenseInput(inter, Money(4_590), LocalDate(2026, 11, 30), "Pix"))
        assertEquals(november, expense.accountingMonth)
    }

    @Test
    fun `RN03 - expense on behalf of a third party`() {
        val expense = factory.expense(ExpenseInput(inter, Money(8_000), LocalDate(2026, 11, 10), "Father's medicine", thirdParty = father))
        assertTrue(expense.isThirdParty())
        assertEquals(father, expense.thirdParty)
    }

    @Test
    fun `RN09 - expense with zero negative or missing amount is rejected`() {
        val date = LocalDate(2026, 11, 10)
        assertFailsWith<InvalidAmount> { factory.expense(ExpenseInput(inter, Money(0), date, "x")) }
        assertFailsWith<InvalidAmount> { factory.expense(ExpenseInput(inter, Money(-500), date, "x")) }
        assertFailsWith<InvalidAmount> { factory.expense(ExpenseInput(inter, null, date, "x")) }
    }

    @Test
    fun `RN05 - a dollar expense is converted to reais by the exchange rate`() {
        val expense = factory.expense(
            ExpenseInput(wise, amount = null, LocalDate(2026, 11, 10), "Subscription", foreignAmount = 1_250, exchangeRate = 54_321),
        )

        assertEquals(Money(6_790), expense.amount)
        assertEquals(CurrencyConversion(1_250, 54_321), expense.conversion)
        assertEquals(november, expense.accountingMonth)
    }

    @Test
    fun `RN05 - foreign currency expense without exchange rate or with zero rate is rejected`() {
        val date = LocalDate(2026, 11, 10)
        assertFailsWith<InvalidExchangeRate> { factory.expense(ExpenseInput(wise, null, date, "x", foreignAmount = 1_250)) }
        assertFailsWith<InvalidExchangeRate> { factory.expense(ExpenseInput(wise, null, date, "x", foreignAmount = 1_250, exchangeRate = 0)) }
    }

    @Test
    fun `RN05 - foreign currency expense without the foreign amount is rejected`() {
        val date = LocalDate(2026, 11, 10)
        assertFailsWith<InvalidAmount> { factory.expense(ExpenseInput(wise, null, date, "x", exchangeRate = 54_321)) }
        assertFailsWith<InvalidAmount> { factory.expense(ExpenseInput(wise, null, date, "x", foreignAmount = 0, exchangeRate = 54_321)) }
    }

    @Test
    fun `RN11 - an actual contribution is an expense in the brokerage on its date`() {
        val contribution = factory.expense(ExpenseInput(clear, Money(30_000), LocalDate(2026, 11, 20), "Contribution"))
        assertEquals(clear, contribution.paymentMethod)
        assertEquals(november, contribution.accountingMonth)
    }

    @Test
    fun `RN11 - contribution with zero amount is rejected`() {
        assertFailsWith<InvalidAmount> { factory.expense(ExpenseInput(clear, Money(0), LocalDate(2026, 11, 20), "Contribution")) }
    }

    // ---------- installment purchase (UC02) ----------

    @Test
    fun `RN02 - a valid installment purchase generates the installments`() {
        val purchase = factory.installmentPurchase(
            PurchaseInput(nubank, Money(10_000), 3, LocalDate(2026, 11, 3), "Fridge", thirdParty = father),
        )
        val installments = purchase.generateInstallments()

        assertEquals(listOf("1/3", "2/3", "3/3"), installments.map { it.label() })
        assertEquals(listOf(MonthYear(2026, 11), MonthYear(2026, 12), MonthYear(2027, 1)), installments.map { it.accountingMonth })
        assertTrue(installments.all { it.thirdParty == father && it.description == "Fridge" })
    }

    @Test
    fun `RN02 - purchase with 1 or 49 installments is rejected`() {
        val date = LocalDate(2026, 11, 3)
        assertFailsWith<InstallmentsOutOfRange> { factory.installmentPurchase(PurchaseInput(nubank, Money(10_000), 1, date, "x")) }
        assertFailsWith<InstallmentsOutOfRange> { factory.installmentPurchase(PurchaseInput(nubank, Money(10_000), 49, date, "x")) }
    }

    @Test
    fun `RN09 - purchase with zero amount or less than 1 cent per installment is rejected`() {
        val date = LocalDate(2026, 11, 3)
        assertFailsWith<InvalidAmount> { factory.installmentPurchase(PurchaseInput(nubank, Money(0), 3, date, "x")) }
        assertFailsWith<InvalidAmount> { factory.installmentPurchase(PurchaseInput(nubank, Money(2), 3, date, "x")) }
    }

    // ---------- expected entries of the month (UC03) ----------

    @Test
    fun `RN06 RN07 and RN11 - the month's expected entries gather active bills salary and contributions`() {
        val power = RecurringBill("Power", Money(20_000), 15, inter)
        val cancelledInternet = RecurringBill("Internet", Money(10_000), 10, inter, active = false)
        val salary = RecurringIncome("Salary", Money(800_000), 5)
        val binance = InvestmentAccount("Binance", gabriel, Institution("Binance")) // no planned contribution
        val deactivatedBrokerage = InvestmentAccount("XP", gabriel, Institution("XP"), Money(10_000), active = false)

        val expected = factory.expectedForMonth(
            november,
            recurringBills = listOf(power, cancelledInternet),
            recurringIncomes = listOf(salary),
            investments = listOf(clear, binance, deactivatedBrokerage),
        )

        assertEquals(listOf(power.id, salary.id, clear.id), expected.map { it.originId })
        assertTrue(expected.all { it.status == EntryStatus.EXPECTED && it.accountingMonth == november })
        assertEquals(2, expected.filterIsInstance<Expense>().size)
        assertEquals(1, expected.filterIsInstance<Income>().size)
    }

    @Test
    fun `RN06 - no recurrences means no expected entries`() {
        assertEquals(emptyList(), factory.expectedForMonth(november, emptyList(), emptyList(), emptyList()))
    }
}
