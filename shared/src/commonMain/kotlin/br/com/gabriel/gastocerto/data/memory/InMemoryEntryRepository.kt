package br.com.gabriel.gastocerto.data.memory

import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import kotlinx.datetime.LocalDate

class InMemoryEntryRepository(private val database: InMemoryDatabase) : EntryRepository {

    override suspend fun save(entry: Entry) {
        database.entries = database.entries + (entry.id to entry.copy())
    }

    override suspend fun saveAll(entries: List<Entry>) {
        database.entries = database.entries + entries.associate { it.id to it.copy() }
    }

    override suspend fun delete(entry: Entry) {
        database.entries = database.entries - entry.id
    }

    override suspend fun ofMonth(month: MonthYear): List<Entry> =
        read { it.accountingMonth == month }

    override suspend fun ofPeriod(start: LocalDate, end: LocalDate): List<Entry> =
        read { it.date in start..end }

    override suspend fun ofThirdParty(thirdParty: Person): List<Expense> =
        read { it is Expense && it.thirdParty == thirdParty }.filterIsInstance<Expense>()

    private fun read(filter: (Entry) -> Boolean): List<Entry> =
        database.entries.values.filter(filter).sortedBy { it.date }.map { it.copy() }
}
