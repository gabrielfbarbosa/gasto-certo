package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.repository.RecurrenceRepository
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * UC07 – Manage recurring bills and salary (GRASP **Controller**, RF09, RF10).
 *
 * [today] is injected so tests control the date; by default it is the device's current date.
 */
class ManageRecurrencesUseCase(
    private val recurrences: RecurrenceRepository,
    private val factory: EntryFactory,
    private val entries: EntryRepository,
    private val accountingMonths: AccountingMonthRepository,
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) {
    /**
     * Saves the bill. RN06 – when an active bill is registered, the EXPECTED expense is also generated
     * in the **current month** if that month is open and the due date has not passed yet, without
     * duplicating one that already exists with the same `originId` (D3).
     */
    suspend fun saveRecurringBill(bill: RecurringBill) {
        recurrences.saveRecurringBill(bill)
        if (!bill.active) return
        val date = today()
        val month = MonthYear(date.year, date.month.number)
        if (accountingMonths.findOrCreate(month).status != AccountingMonthStatus.OPEN) return
        val expected = factory.expectedForMonth(month, listOf(bill), emptyList(), emptyList())
            .filter { it.date >= date }
        val existingOrigins = entries.ofMonth(month).mapNotNull { it.originId }.toSet()
        entries.saveAll(expected.filter { it.originId !in existingOrigins })
    }

    /** RN07 – saves the salary; its EXPECTED income is generated when the next month is opened (UC03). */
    suspend fun saveRecurringIncome(income: RecurringIncome) = recurrences.saveRecurringIncome(income)
}
