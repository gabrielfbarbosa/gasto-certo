package br.com.gabriel.gastocerto.di

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
import br.com.gabriel.gastocerto.usecase.SeedDemoDataUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Dependency Injection (Koin): the use cases only know the repository interfaces; [platformModule]
 * supplies Room (Android/iOS) or memory (Web). Use cases are `factory` (no state); the database
 * and repositories are `single` in the platform module. ViewModels join this module with the UI.
 */
val appModule = module {
    factoryOf(::EntryFactory)
    factoryOf(::RegisterExpenseUseCase)
    factoryOf(::RegisterInstallmentPurchaseUseCase)
    factoryOf(::CloseMonthUseCase)
    factoryOf(::ReopenMonthUseCase)
    factoryOf(::GetSummaryUseCase)
    factoryOf(::GetStatementUseCase)
    factoryOf(::ManageRegistryUseCase)
    // Explicit constructor: the `today` clock keeps its default value.
    factory { ManageRecurrencesUseCase(get(), get(), get(), get()) }
    factoryOf(::ManageInvestmentsUseCase)
    factoryOf(::RegisterOneOffIncomeUseCase)
    factoryOf(::CorrectEntryUseCase)
    factoryOf(::RegisterReimbursementUseCase)
    factory { SeedDemoDataUseCase(get(), get(), get(), get(), get(), get(), get()) }
}
