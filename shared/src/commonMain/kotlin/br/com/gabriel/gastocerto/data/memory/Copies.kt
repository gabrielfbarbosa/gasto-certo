package br.com.gabriel.gastocerto.data.memory

import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome

// Copies of the mutable domain objects, so the in-memory database behaves like a real one
// (the same role as Room's Entity ⇄ domain mappers). Same id, same values.

internal fun PaymentMethod.copy(): PaymentMethod = when (this) {
    is CreditCard -> CreditCard(name, holder, institution, closingDay, dueDay, active, id)
    is CheckingAccount -> CheckingAccount(name, holder, institution, active, id)
    is ForeignCurrencyWallet -> ForeignCurrencyWallet(name, holder, institution, currency, active, id)
    is InvestmentAccount -> InvestmentAccount(name, holder, institution, plannedMonthlyContribution, active, id)
}

internal fun Entry.copy(): Entry = when (this) {
    is Installment -> Installment(
        amount, date, description, accountingMonth, paymentMethod as CreditCard,
        purchaseId, number, totalInstallments, thirdParty, id,
    )
    is Expense -> Expense(amount, date, description, accountingMonth, paymentMethod, thirdParty, conversion, status, originId, id)
    is Income -> Income(amount, date, description, accountingMonth, type, status, originId, id)
}

internal fun RecurringBill.copy() = RecurringBill(description, expectedAmount, dueDay, paymentMethod, active, id)

internal fun RecurringIncome.copy() = RecurringIncome(description, amount, payDay, active, id)
