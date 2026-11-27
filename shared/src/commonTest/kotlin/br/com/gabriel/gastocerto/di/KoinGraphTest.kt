package br.com.gabriel.gastocerto.di

import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
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
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/** Every use case resolves from [appModule] over the in-memory repositories (the Web graph). */
class KoinGraphTest {

    private fun koin(): Koin = koinApplication { modules(appModule, inMemoryDataModule) }.koin

    @Test
    fun `every use case resolves from the app module`() {
        val koin = koin()
        koin.get<RegisterExpenseUseCase>()
        koin.get<RegisterInstallmentPurchaseUseCase>()
        koin.get<CloseMonthUseCase>()
        koin.get<ReopenMonthUseCase>()
        koin.get<GetSummaryUseCase>()
        koin.get<GetStatementUseCase>()
        koin.get<ManageRegistryUseCase>()
        koin.get<ManageRecurrencesUseCase>()
        koin.get<ManageInvestmentsUseCase>()
        koin.get<RegisterOneOffIncomeUseCase>()
        koin.get<CorrectEntryUseCase>()
        koin.get<RegisterReimbursementUseCase>()
    }

    @Test
    fun `repositories are single and use cases are factory`() {
        val koin = koin()
        assertIs<InMemoryEntryRepository>(koin.get<EntryRepository>())
        assertSame(koin.get<EntryRepository>(), koin.get<EntryRepository>())
        assertNotSame(koin.get<GetSummaryUseCase>(), koin.get<GetSummaryUseCase>())
    }
}
