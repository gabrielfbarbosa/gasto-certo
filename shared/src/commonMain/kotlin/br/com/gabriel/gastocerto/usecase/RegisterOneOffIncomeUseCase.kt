package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import kotlinx.datetime.LocalDate

/**
 * UC08 – Register one-off income (GRASP **Controller**, RF11). RN07 – it counts in the week, month and
 * year of the date received; RN09 – amount > 0; RN08 – the month must be open.
 */
class RegisterOneOffIncomeUseCase(
    private val factory: EntryFactory,
    private val accountingMonths: AccountingMonthRepository,
    private val entries: EntryRepository,
) {
    suspend fun execute(amount: Money, date: LocalDate, description: String): Income {
        val income = factory.income(amount, date, description, IncomeType.ONE_OFF)
        accountingMonths.findOrCreate(income.accountingMonth).ensureOpen()
        entries.save(income)
        return income
    }
}
