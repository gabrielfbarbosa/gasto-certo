package br.com.gabriel.gastocerto.data.room

import androidx.room.Room
import br.com.gabriel.gastocerto.domain.repository.Repositories

/** An empty Room database in memory, with the same driver and settings as the app. */
fun inMemoryAppDatabase(): AppDatabase = buildAppDatabase(Room.inMemoryDatabaseBuilder<AppDatabase>())

fun AppDatabase.repositories() = Repositories(
    entries = RoomEntryRepository(this),
    accountingMonths = RoomAccountingMonthRepository(this),
    registry = RoomRegistryRepository(this),
    recurrences = RoomRecurrenceRepository(this),
    reimbursements = RoomReimbursementRepository(this),
    transaction = RoomTransaction(this),
)
