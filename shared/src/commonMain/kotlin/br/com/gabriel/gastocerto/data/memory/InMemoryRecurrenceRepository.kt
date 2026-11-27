package br.com.gabriel.gastocerto.data.memory

import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.repository.RecurrenceRepository

class InMemoryRecurrenceRepository(private val database: InMemoryDatabase) : RecurrenceRepository {

    override suspend fun activeRecurringBills(): List<RecurringBill> =
        database.recurringBills.values.filter { it.active }.sortedBy { it.dueDay }.map { it.copy() }

    override suspend fun activeRecurringIncomes(): List<RecurringIncome> =
        database.recurringIncomes.values.filter { it.active }.sortedBy { it.payDay }.map { it.copy() }

    override suspend fun investmentsWithContribution(): List<InvestmentAccount> =
        database.paymentMethods.values.filterIsInstance<InvestmentAccount>()
            .filter { it.active && it.plannedMonthlyContribution != null }
            .sortedBy { it.name }
            .map { it.copy() as InvestmentAccount }

    override suspend fun saveRecurringBill(bill: RecurringBill) {
        database.recurringBills = database.recurringBills + (bill.id to bill.copy())
    }

    override suspend fun saveRecurringIncome(income: RecurringIncome) {
        database.recurringIncomes = database.recurringIncomes + (income.id to income.copy())
    }
}
