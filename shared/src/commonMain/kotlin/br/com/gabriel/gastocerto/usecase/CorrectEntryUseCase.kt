package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository

/**
 * UC09 – Correct an entry (GRASP **Controller**, RF12). RN08 – a CLOSED accounting month accepts no
 * edit, deletion or confirmation: both the month the entry was in and the month it goes to must be open.
 */
class CorrectEntryUseCase(
    private val entries: EntryRepository,
    private val accountingMonths: AccountingMonthRepository,
) {
    /** Saves the edited [entry] (same `id`). Throws `AccountingMonthClosed` if either month is closed. */
    suspend fun edit(entry: Entry) {
        entries.findById(entry.id)?.let { ensureOpen(it.accountingMonth) }
        ensureOpen(entry.accountingMonth)
        entries.save(entry)
    }

    /** Deletes [entry]. Throws `AccountingMonthClosed` if its month is closed. */
    suspend fun delete(entry: Entry) {
        val stored = entries.findById(entry.id) ?: entry
        ensureOpen(stored.accountingMonth)
        entries.delete(stored)
    }

    /**
     * RN06/RN07/RN11 – confirms an EXPECTED entry with the month's [actualAmount] (RN09: > 0),
     * making it ACTUAL. Throws `AccountingMonthClosed` if its month is closed.
     */
    suspend fun confirmExpected(entry: Entry, actualAmount: Money): Entry {
        ensureOpen(entry.accountingMonth)
        entry.confirm(actualAmount)
        entries.save(entry)
        return entry
    }

    private suspend fun ensureOpen(month: MonthYear) = accountingMonths.findOrCreate(month).ensureOpen()
}
