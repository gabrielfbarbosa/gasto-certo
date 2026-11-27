package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository

/**
 * UC03 A2 – Reopen a month (GRASP **Controller**). RN08: reopening is explicit and does not delete the
 * expected entries already generated for the following month.
 */
class ReopenMonthUseCase(private val accountingMonths: AccountingMonthRepository) {

    suspend fun execute(month: MonthYear): AccountingMonth {
        val accountingMonth = accountingMonths.findOrCreate(month)
        accountingMonth.reopen()
        accountingMonths.save(accountingMonth)
        return accountingMonth
    }
}
