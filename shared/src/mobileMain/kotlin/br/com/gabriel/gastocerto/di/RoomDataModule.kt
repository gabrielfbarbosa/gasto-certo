package br.com.gabriel.gastocerto.di

import androidx.room.RoomDatabase
import br.com.gabriel.gastocerto.data.room.AppDatabase
import br.com.gabriel.gastocerto.data.room.RoomAccountingMonthRepository
import br.com.gabriel.gastocerto.data.room.RoomEntryRepository
import br.com.gabriel.gastocerto.data.room.RoomRecurrenceRepository
import br.com.gabriel.gastocerto.data.room.RoomRegistryRepository
import br.com.gabriel.gastocerto.data.room.RoomReimbursementRepository
import br.com.gabriel.gastocerto.data.room.RoomTransaction
import br.com.gabriel.gastocerto.data.room.buildAppDatabase
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.repository.RecurrenceRepository
import br.com.gabriel.gastocerto.domain.repository.RegistryRepository
import br.com.gabriel.gastocerto.domain.repository.ReimbursementRepository
import br.com.gabriel.gastocerto.domain.repository.Transaction
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

/** Room repositories over a single [AppDatabase]; [databaseBuilder] is the platform builder. */
fun roomDataModule(databaseBuilder: Scope.() -> RoomDatabase.Builder<AppDatabase>): Module = module {
    single { buildAppDatabase(databaseBuilder()) }
    single<EntryRepository> { RoomEntryRepository(get()) }
    single<AccountingMonthRepository> { RoomAccountingMonthRepository(get()) }
    single<RegistryRepository> { RoomRegistryRepository(get()) }
    single<RecurrenceRepository> { RoomRecurrenceRepository(get()) }
    single<ReimbursementRepository> { RoomReimbursementRepository(get()) }
    single<Transaction> { RoomTransaction(get()) }
}
