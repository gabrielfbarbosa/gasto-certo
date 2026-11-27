package br.com.gabriel.gastocerto.ui

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRecurrenceRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRegistryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryReimbursementRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryTransaction
import br.com.gabriel.gastocerto.domain.repository.Repositories
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import br.com.gabriel.gastocerto.usecase.CloseMonthUseCase
import br.com.gabriel.gastocerto.usecase.CorrectEntryUseCase
import br.com.gabriel.gastocerto.usecase.GetStatementUseCase
import br.com.gabriel.gastocerto.usecase.GetSummaryUseCase
import br.com.gabriel.gastocerto.usecase.ManageInvestmentsUseCase
import br.com.gabriel.gastocerto.usecase.ManageRecurrencesUseCase
import br.com.gabriel.gastocerto.usecase.ManageRegistryUseCase
import br.com.gabriel.gastocerto.usecase.RegisterExpenseUseCase
import br.com.gabriel.gastocerto.usecase.RegisterInstallmentPurchaseUseCase
import br.com.gabriel.gastocerto.usecase.RegisterOneOffIncomeUseCase
import br.com.gabriel.gastocerto.usecase.RegisterReimbursementUseCase
import br.com.gabriel.gastocerto.usecase.ReopenMonthUseCase
import br.com.gabriel.gastocerto.usecase.demoLabelsForTests
import br.com.gabriel.gastocerto.usecase.seedUseCase
import kotlinx.datetime.LocalDate

/** Date used by the ViewModel tests. */
val TEST_TODAY = LocalDate(2026, 11, 27)

/** In-memory repositories plus every use case, as Koin would wire them (ViewModel tests). */
class TestApp(val today: LocalDate = TEST_TODAY) {
    private val database = InMemoryDatabase()
    val repositories = Repositories(
        InMemoryEntryRepository(database), InMemoryAccountingMonthRepository(database),
        InMemoryRegistryRepository(database), InMemoryRecurrenceRepository(database),
        InMemoryReimbursementRepository(database), InMemoryTransaction(database),
    )
    private val factory = EntryFactory()
    private val r = repositories
    val registerExpense = RegisterExpenseUseCase(factory, r.accountingMonths, r.entries)
    val registerPurchase = RegisterInstallmentPurchaseUseCase(factory, r.accountingMonths, r.entries)
    val closeMonth = CloseMonthUseCase(r.transaction, r.accountingMonths, r.recurrences, factory, r.entries)
    val reopenMonth = ReopenMonthUseCase(r.accountingMonths)
    val summary = GetSummaryUseCase(r.entries)
    val statement = GetStatementUseCase(r.entries)
    val registry = ManageRegistryUseCase(r.registry)
    val recurrences = ManageRecurrencesUseCase(r.recurrences, factory, r.entries, r.accountingMonths) { today }
    val investments = ManageInvestmentsUseCase(r.registry, factory, r.accountingMonths, r.entries)
    val oneOffIncome = RegisterOneOffIncomeUseCase(factory, r.accountingMonths, r.entries)
    val correctEntry = CorrectEntryUseCase(r.entries, r.accountingMonths)
    val reimbursement = RegisterReimbursementUseCase(r.entries, r.reimbursements)

    /** Loads the demo data (people, payment methods, entries…). */
    suspend fun seed(): TestApp = apply { repositories.seedUseCase(today).execute(demoLabelsForTests) }
}
