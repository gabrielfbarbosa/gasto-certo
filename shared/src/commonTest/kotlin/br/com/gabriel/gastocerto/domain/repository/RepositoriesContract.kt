package br.com.gabriel.gastocerto.domain.repository

import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.CurrencyConversion
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.InstallmentPurchase
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.model.Reimbursement
import br.com.gabriel.gastocerto.domain.model.Role
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The 5 repository ports + the transaction, wired to the same database. */
class Repositories(
    val entries: EntryRepository,
    val accountingMonths: AccountingMonthRepository,
    val registry: RegistryRepository,
    val recurrences: RecurrenceRepository,
    val reimbursements: ReimbursementRepository,
    val transaction: Transaction,
)

/**
 * Contract every repository implementation meets (in memory now, Room in 8.16):
 * saving with the same id replaces, queries filter correctly, what is read is a copy
 * and the [Transaction] undoes everything when the block fails.
 */
abstract class RepositoriesContract {

    /** An empty database for each test. */
    abstract fun newRepositories(): Repositories

    private val gabriel = Person("Gabriel", Role.USER)
    private val father = Person("Father", Role.THIRD_PARTY)
    private val nubankInst = Institution("Nubank")
    private val nubank = CreditCard("Nubank", gabriel, nubankInst, closingDay = 5, dueDay = 12)
    private val inter = CheckingAccount("Inter", gabriel, Institution("Inter"))
    private val november = MonthYear(2026, 11)
    private val december = MonthYear(2026, 12)

    private fun expense(amount: Long, date: LocalDate, month: MonthYear = november, thirdParty: Person? = null) =
        Expense(Money(amount), date, "expense", month, inter, thirdParty)

    // ---------- EntryRepository ----------

    @Test
    fun `entries of the month filter by accounting month`() = runTest {
        val r = newRepositories()
        val inNovember = expense(1_000, LocalDate(2026, 11, 30))
        val inDecember = expense(2_000, LocalDate(2026, 11, 30), month = december)
        r.entries.saveAll(listOf(inNovember, inDecember))

        assertEquals(listOf<Entry>(inNovember), r.entries.ofMonth(november))
        assertEquals(listOf<Entry>(inDecember), r.entries.ofMonth(december))
    }

    @Test
    fun `entries of the period filter by date with both ends included`() = runTest {
        val r = newRepositories()
        val monday = expense(1, LocalDate(2026, 11, 23))
        val sunday = expense(2, LocalDate(2026, 11, 29))
        val after = expense(3, LocalDate(2026, 11, 30))
        r.entries.saveAll(listOf(after, sunday, monday))

        assertEquals(
            listOf<Entry>(monday, sunday),
            r.entries.ofPeriod(LocalDate(2026, 11, 23), LocalDate(2026, 11, 29)),
        )
    }

    @Test
    fun `saving with the same id replaces and delete removes`() = runTest {
        val r = newRepositories()
        val expected = Expense(Money(20_000), LocalDate(2026, 11, 15), "Power", november, inter, status = EntryStatus.EXPECTED)
        r.entries.save(expected)

        expected.confirm(Money(18_000))
        r.entries.save(expected)
        val read = r.entries.ofMonth(november).single()
        assertEquals(Money(18_000), read.amount)
        assertEquals(EntryStatus.ACTUAL, read.status)

        r.entries.delete(read)
        assertTrue(r.entries.ofMonth(november).isEmpty())
    }

    @Test
    fun `changing a read entry does not change the database without saving`() = runTest {
        val r = newRepositories()
        r.entries.save(Expense(Money(20_000), LocalDate(2026, 11, 15), "Power", november, inter, status = EntryStatus.EXPECTED))

        r.entries.ofMonth(november).single().confirm(Money(1))

        assertEquals(Money(20_000), r.entries.ofMonth(november).single().amount)
    }

    @Test
    fun `installments incomes and conversion come back with every field`() = runTest {
        val r = newRepositories()
        val installments = InstallmentPurchase(Money(10_000), 2, LocalDate(2026, 11, 3), "TV", nubank, father).generateInstallments()
        val salary = Income(Money(800_000), LocalDate(2026, 11, 5), "Salary", november, IncomeType.SALARY,
            EntryStatus.EXPECTED, originId = "rec-1")
        val dollar = Expense(Money(6_790), LocalDate(2026, 11, 10), "App", november, inter, conversion = CurrencyConversion(1_250, 54_321))
        r.entries.saveAll(installments + salary + dollar)

        val read = r.entries.ofMonth(november)
        val installment = assertIs<Installment>(read.first { it.id == installments[0].id })
        assertEquals("1/2", installment.label())
        assertEquals(installments[0].purchaseId, installment.purchaseId)
        assertEquals(father, installment.thirdParty)
        val income = assertIs<Income>(read.first { it.id == salary.id })
        assertEquals(IncomeType.SALARY, income.type)
        assertEquals("rec-1", income.originId)
        assertEquals(EntryStatus.EXPECTED, income.status)
        assertEquals(CurrencyConversion(1_250, 54_321), assertIs<Expense>(read.first { it.id == dollar.id }).conversion)
    }

    @Test
    fun `third-party expenses bring only the expenses on their behalf`() = runTest {
        val r = newRepositories()
        val fathers = expense(5_000, LocalDate(2026, 11, 10), thirdParty = father)
        r.entries.saveAll(listOf(fathers, expense(7_000, LocalDate(2026, 11, 11))))

        assertEquals(listOf<Expense>(fathers), r.entries.ofThirdParty(father))
    }

    // ---------- AccountingMonthRepository ----------

    @Test
    fun `findOrCreate creates an open month and save stores the status`() = runTest {
        val r = newRepositories()
        val month = r.accountingMonths.findOrCreate(november)
        assertEquals(AccountingMonthStatus.OPEN, month.status)

        month.close()
        assertEquals(AccountingMonthStatus.OPEN, r.accountingMonths.findOrCreate(november).status) // not saved
        r.accountingMonths.save(month)
        assertEquals(AccountingMonthStatus.CLOSED, r.accountingMonths.findOrCreate(november).status)
    }

    // ---------- RegistryRepository ----------

    @Test
    fun `registry records are saved and only active payment methods are listed`() = runTest {
        val r = newRepositories()
        r.registry.savePerson(gabriel)
        r.registry.saveInstitution(nubankInst)
        r.registry.savePaymentMethod(nubank)
        r.registry.savePaymentMethod(inter)

        inter.active = false
        r.registry.savePaymentMethod(inter)

        assertEquals(listOf(gabriel), r.registry.people())
        assertEquals(listOf(nubankInst), r.registry.institutions())
        assertEquals(listOf<Any>(nubank), r.registry.activePaymentMethods())
        inter.active = true
    }

    @Test
    fun `RN09 - hasEntries tells whether the payment method has expenses`() = runTest {
        val r = newRepositories()
        r.registry.savePaymentMethod(inter)
        r.registry.savePaymentMethod(nubank)
        r.entries.save(expense(1_000, LocalDate(2026, 11, 10)))

        assertTrue(r.registry.hasEntries(inter))
        assertFalse(r.registry.hasEntries(nubank))
    }

    // ---------- RecurrenceRepository ----------

    @Test
    fun `active recurrences and brokerages with a planned contribution`() = runTest {
        val r = newRepositories()
        val power = RecurringBill("Power", Money(20_000), 15, inter)
        val internet = RecurringBill("Internet", Money(10_000), 10, inter, active = false)
        val salary = RecurringIncome("Salary", Money(800_000), 5)
        val clear = InvestmentAccount("Clear", gabriel, Institution("Clear"), Money(50_000))
        val binance = InvestmentAccount("Binance", gabriel, Institution("Binance"))
        r.recurrences.saveRecurringBill(power)
        r.recurrences.saveRecurringBill(internet)
        r.recurrences.saveRecurringIncome(salary)
        r.registry.savePaymentMethod(clear)
        r.registry.savePaymentMethod(binance)

        assertEquals(listOf(power), r.recurrences.activeRecurringBills())
        assertEquals(listOf(salary), r.recurrences.activeRecurringIncomes())
        assertEquals(listOf(clear), r.recurrences.investmentsWithContribution())
    }

    // ---------- ReimbursementRepository ----------

    @Test
    fun `reimbursements are listed by third party`() = runTest {
        val r = newRepositories()
        val fathers = Reimbursement(father, Money(5_000), LocalDate(2026, 11, 20))
        r.reimbursements.save(fathers)
        r.reimbursements.save(Reimbursement(Person("Mother-in-law", Role.THIRD_PARTY), Money(1_000), LocalDate(2026, 11, 21)))

        assertEquals(listOf(fathers), r.reimbursements.ofThirdParty(father))
    }

    // ---------- Transaction ----------

    @Test
    fun `the transaction saves everything when the block finishes`() = runTest {
        val r = newRepositories()
        val result = r.transaction.execute {
            r.entries.save(expense(1_000, LocalDate(2026, 11, 10)))
            r.accountingMonths.save(AccountingMonth(november, AccountingMonthStatus.CLOSED))
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(1, r.entries.ofMonth(november).size)
        assertEquals(AccountingMonthStatus.CLOSED, r.accountingMonths.findOrCreate(november).status)
    }

    @Test
    fun `the transaction undoes everything when the block fails midway`() = runTest {
        val r = newRepositories()
        r.entries.save(expense(1_000, LocalDate(2026, 11, 1)))

        assertFailsWith<IllegalStateException> {
            r.transaction.execute {
                r.accountingMonths.save(AccountingMonth(november, AccountingMonthStatus.CLOSED))
                r.entries.save(expense(2_000, LocalDate(2026, 11, 2)))
                error("failure in the middle of closing")
            }
        }

        assertEquals(listOf(Money(1_000)), r.entries.ofMonth(november).map { it.amount })
        assertEquals(AccountingMonthStatus.OPEN, r.accountingMonths.findOrCreate(november).status)
    }
}
