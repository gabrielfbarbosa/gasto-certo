package br.com.gabriel.gastocerto.domain.repository

import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import kotlinx.datetime.LocalDate

/**
 * Persistence port for entries (expenses, installments, incomes). Implemented with Room
 * (Android/iOS) and in memory (Web and tests). Saving with the same `id` replaces (D2).
 * What is read is a copy: changing a read object only counts after [save].
 */
interface EntryRepository {
    suspend fun save(entry: Entry)
    suspend fun saveAll(entries: List<Entry>)
    suspend fun delete(entry: Entry)

    /** Entries whose accounting month is [month] (RN01). */
    suspend fun ofMonth(month: MonthYear): List<Entry>

    /** Entries dated between [start] and [end], inclusive (week and year – RF15). */
    suspend fun ofPeriod(start: LocalDate, end: LocalDate): List<Entry>

    /** Expenses made on behalf of [thirdParty] (RN03). */
    suspend fun ofThirdParty(thirdParty: Person): List<Expense>
}
