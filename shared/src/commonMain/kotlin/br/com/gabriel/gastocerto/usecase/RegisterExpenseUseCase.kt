package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import br.com.gabriel.gastocerto.domain.rule.ExpenseInput

/**
 * UC01 – Register expense (GRASP **Controller**). Coordinates the use case and leaves the rules
 * to the domain: the [EntryFactory] validates and creates the expense (RN05, RN09, RN11) with the
 * accounting month given by the payment method (RN01); the accounting month must be open (RN08).
 */
class RegisterExpenseUseCase(
    private val factory: EntryFactory,
    private val accountingMonths: AccountingMonthRepository,
    private val entries: EntryRepository,
) {
    /**
     * Validates, checks the accounting month and saves the expense, returning it so the screen can
     * show which month/statement it went into. Throws a `RuleViolationException`
     * (`InvalidAmount`, `InvalidExchangeRate`, `AccountingMonthClosed`) and saves nothing on failure.
     */
    suspend fun execute(input: ExpenseInput): Expense {
        val expense = factory.expense(input)
        accountingMonths.findOrCreate(expense.accountingMonth).ensureOpen()
        entries.save(expense)
        return expense
    }
}
