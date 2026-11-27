package br.com.gabriel.gastocerto.data.memory

import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository

class InMemoryAccountingMonthRepository(private val database: InMemoryDatabase) : AccountingMonthRepository {

    override suspend fun findOrCreate(month: MonthYear): AccountingMonth {
        val status = database.accountingMonths[month] ?: AccountingMonthStatus.OPEN.also {
            database.accountingMonths = database.accountingMonths + (month to it)
        }
        return AccountingMonth(month, status)
    }

    override suspend fun save(accountingMonth: AccountingMonth) {
        database.accountingMonths = database.accountingMonths + (accountingMonth.monthYear to accountingMonth.status)
    }
}
