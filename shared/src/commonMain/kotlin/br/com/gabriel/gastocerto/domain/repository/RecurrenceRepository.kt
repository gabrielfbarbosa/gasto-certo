package br.com.gabriel.gastocerto.domain.repository

import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome

/** Persistence port for the recurrences that generate expected entries (RN06, RN07, RN11). */
interface RecurrenceRepository {
    suspend fun activeRecurringBills(): List<RecurringBill>
    suspend fun activeRecurringIncomes(): List<RecurringIncome>

    /** Active brokerages with a planned monthly contribution (RN11). */
    suspend fun investmentsWithContribution(): List<InvestmentAccount>
    suspend fun saveRecurringBill(bill: RecurringBill)
    suspend fun saveRecurringIncome(income: RecurringIncome)
}
