package br.com.gabriel.gastocerto.domain.repository

import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.MonthYear

/** Persistence port for accounting months (RN08). There is one per month. */
interface AccountingMonthRepository {
    /** The accounting month of [month]; if it does not exist yet, it is created OPEN and saved. */
    suspend fun findOrCreate(month: MonthYear): AccountingMonth
    suspend fun save(accountingMonth: AccountingMonth)
}
