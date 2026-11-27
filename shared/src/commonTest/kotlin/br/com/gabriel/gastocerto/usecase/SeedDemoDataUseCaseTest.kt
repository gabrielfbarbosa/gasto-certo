package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRecurrenceRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRegistryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryReimbursementRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryTransaction
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.repository.Repositories
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val demoLabelsForTests = DemoLabels(
    user = "Gabriel", spouse = "Wife", father = "Father", spouseCard = "Nubank (wife)", power = "Power",
    water = "Water", internet = "Internet", salary = "Salary", groceries = "Groceries", pharmacy = "Pharmacy",
    restaurant = "Restaurant", clothes = "Clothes", appSubscription = "App", television = "TV", notebook = "Notebook",
)

/** The seed wired to any repositories (memory here, Room in iosTest), with a fixed date. */
fun Repositories.seedUseCase(today: LocalDate): SeedDemoDataUseCase {
    val factory = EntryFactory()
    return SeedDemoDataUseCase(
        registry = registry,
        manageRegistry = ManageRegistryUseCase(registry),
        manageRecurrences = ManageRecurrencesUseCase(recurrences, factory, entries, accountingMonths) { today },
        manageInvestments = ManageInvestmentsUseCase(registry, factory, accountingMonths, entries),
        registerExpense = RegisterExpenseUseCase(factory, accountingMonths, entries),
        registerPurchase = RegisterInstallmentPurchaseUseCase(factory, accountingMonths, entries),
        registerReimbursement = RegisterReimbursementUseCase(entries, reimbursements),
        closeMonth = CloseMonthUseCase(transaction, accountingMonths, recurrences, factory, entries),
        today = { today },
    )
}

/** Checks of the loaded demo data, shared by the memory and Room tests. */
suspend fun Repositories.assertDemoData() {
    val people = registry.people()
    assertEquals(3, people.size)
    val father = people.single { it.role == Role.THIRD_PARTY }
    assertEquals(6, registry.institutions().size)
    assertEquals(7, registry.activePaymentMethods().size)
    assertEquals(listOf(Money(50_000)), recurrences.investmentsWithContribution().map { it.plannedMonthlyContribution })
    assertEquals(3, recurrences.activeRecurringBills().size)
    assertEquals(1, recurrences.activeRecurringIncomes().size)
    // RN03: pharmacy + 3 notebook installments on behalf of the father, and one reimbursement.
    assertEquals(4, entries.ofThirdParty(father).size)
    assertEquals(1, reimbursements.ofThirdParty(father).size)
    // UC03: the previous month is closed and this month has the expected salary, 3 bills and the contribution.
    val november = MonthYear(2026, 11)
    assertEquals(AccountingMonthStatus.CLOSED, accountingMonths.findOrCreate(november.previous()).status)
    val expected = entries.ofMonth(november).filter { it.status == EntryStatus.EXPECTED }
    assertEquals(setOf("Salary", "Power", "Water", "Internet", "Planned contribution – Clear"), expected.map { it.description }.toSet())
    // RN02: TV in 6 installments over the year.
    val year = entries.ofPeriod(LocalDate(2026, 1, 1), LocalDate(2027, 12, 31))
    assertEquals(6, year.filterIsInstance<Installment>().count { it.description == "TV" })
}

class SeedDemoDataUseCaseTest {
    private val database = InMemoryDatabase()
    private val repositories = Repositories(
        InMemoryEntryRepository(database), InMemoryAccountingMonthRepository(database),
        InMemoryRegistryRepository(database), InMemoryRecurrenceRepository(database),
        InMemoryReimbursementRepository(database), InMemoryTransaction(database),
    )
    private val seed = repositories.seedUseCase(LocalDate(2026, 11, 27))

    @Test
    fun `the demo data is loaded through the use cases on an empty database`() = runTest {
        assertTrue(seed.execute(demoLabelsForTests))
        repositories.assertDemoData()
    }

    @Test
    fun `the seed is idempotent - a second run changes nothing`() = runTest {
        seed.execute(demoLabelsForTests)
        val before = repositories.entries.ofPeriod(LocalDate(2026, 1, 1), LocalDate(2027, 12, 31)).size

        assertFalse(seed.execute(demoLabelsForTests))

        assertEquals(before, repositories.entries.ofPeriod(LocalDate(2026, 1, 1), LocalDate(2027, 12, 31)).size)
        repositories.assertDemoData()
    }

    @Test
    fun `on the first day of the month every date stays within today`() = runTest {
        assertTrue(repositories.seedUseCase(LocalDate(2026, 12, 1)).execute(demoLabelsForTests))
        val december = repositories.entries.ofPeriod(LocalDate(2026, 12, 2), LocalDate(2027, 12, 31))
        assertTrue(december.none { it.description in setOf("Groceries", "Pharmacy", "Restaurant", "Clothes", "App") })
    }
}
