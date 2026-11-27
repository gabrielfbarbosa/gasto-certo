package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.repository.RegistryRepository
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import kotlinx.datetime.LocalDate

/**
 * UC10 – Manage investments (GRASP **Controller**, RF17, RF18). An actual contribution is a regular
 * expense in the brokerage (UC01, `RegisterExpenseUseCase`).
 */
class ManageInvestmentsUseCase(
    private val registry: RegistryRepository,
    private val factory: EntryFactory,
    private val accountingMonths: AccountingMonthRepository,
    private val entries: EntryRepository,
) {
    /**
     * RN11 – sets (or removes, with `null`) the planned monthly contribution of [account]; it generates
     * the EXPECTED contribution when the next month is opened. A planned amount ≤ 0 throws `InvalidAmount`.
     */
    suspend fun setPlannedContribution(account: InvestmentAccount, amount: Money?): InvestmentAccount {
        val updated = InvestmentAccount(account.name, account.holder, account.institution, amount, account.active, account.id)
        registry.savePaymentMethod(updated)
        return updated
    }

    /** RN11 – an investment sale is income on the date received; RN08 – its month must be open. */
    suspend fun registerSale(account: InvestmentAccount, amount: Money, date: LocalDate): Income {
        val sale = factory.income(amount, date, "Investment sale – ${account.name}", IncomeType.INVESTMENT_SALE)
        accountingMonths.findOrCreate(sale.accountingMonth).ensureOpen()
        entries.save(sale)
        return sale
    }
}
