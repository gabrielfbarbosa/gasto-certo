package br.com.gabriel.gastocerto.di

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRecurrenceRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRegistryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryReimbursementRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryTransaction
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.repository.RecurrenceRepository
import br.com.gabriel.gastocerto.domain.repository.RegistryRepository
import br.com.gabriel.gastocerto.domain.repository.ReimbursementRepository
import br.com.gabriel.gastocerto.domain.repository.Transaction
import org.koin.dsl.module

/** In-memory repositories sharing one [InMemoryDatabase] (Web and tests). */
val inMemoryDataModule = module {
    single { InMemoryDatabase() }
    single<EntryRepository> { InMemoryEntryRepository(get()) }
    single<AccountingMonthRepository> { InMemoryAccountingMonthRepository(get()) }
    single<RegistryRepository> { InMemoryRegistryRepository(get()) }
    single<RecurrenceRepository> { InMemoryRecurrenceRepository(get()) }
    single<ReimbursementRepository> { InMemoryReimbursementRepository(get()) }
    single<Transaction> { InMemoryTransaction(get()) }
}
