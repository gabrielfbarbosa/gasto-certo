package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.repository.RecurrenceRepository
import br.com.gabriel.gastocerto.domain.repository.Transaction
import br.com.gabriel.gastocerto.domain.rule.EntryFactory

/**
 * UC03 – Close the month and open the next one (GRASP **Controller**), in the call order of
 * `sequencia-uc03-2-fechamento` and `sequencia-uc03-3-previstos`.
 *
 * Everything runs inside one [Transaction]: if any step fails, nothing is kept.
 */
class CloseMonthUseCase(
    private val transaction: Transaction,
    private val accountingMonths: AccountingMonthRepository,
    private val recurrences: RecurrenceRepository,
    private val factory: EntryFactory,
    private val entries: EntryRepository,
) {
    /** The accounting month of [month] with its status (OPEN/CLOSED), for the screen (RN08). */
    suspend fun accountingMonth(month: MonthYear): AccountingMonth = accountingMonths.findOrCreate(month)

    /**
     * Closes [month] (RN08 – closing a closed month throws `AccountingMonthClosed`), opens the
     * following month and saves its expected entries from recurring bills (RN06), recurring incomes
     * (RN07) and planned contributions (RN11), **without duplicating** the ones already there (A3, D3).
     * Returns the following accounting month.
     */
    suspend fun execute(month: MonthYear): AccountingMonth = transaction.execute {
        val current = accountingMonths.findOrCreate(month)
        current.close()
        accountingMonths.save(current)
        val following = accountingMonths.findOrCreate(month.next())
        val expected = factory.expectedForMonth(
            following.monthYear,
            recurrences.activeRecurringBills(),
            recurrences.activeRecurringIncomes(),
            recurrences.investmentsWithContribution(),
        )
        val existing = entries.ofMonth(following.monthYear)
        entries.saveAll(withoutDuplicates(expected, existing))
        following
    }

    /** A3 / D3 – an expected entry is a duplicate when the month already has an entry with the same `originId`. */
    private fun withoutDuplicates(expected: List<Entry>, existing: List<Entry>): List<Entry> {
        val origins = existing.mapNotNull { it.originId }.toSet()
        return expected.filter { it.originId !in origins }
    }
}
