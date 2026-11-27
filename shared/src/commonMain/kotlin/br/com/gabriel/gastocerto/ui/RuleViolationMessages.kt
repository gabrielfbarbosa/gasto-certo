package br.com.gabriel.gastocerto.ui

import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed
import br.com.gabriel.gastocerto.domain.rule.DayKind
import br.com.gabriel.gastocerto.domain.rule.InstallmentsOutOfRange
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.InvalidDay
import br.com.gabriel.gastocerto.domain.rule.InvalidExchangeRate
import br.com.gabriel.gastocerto.domain.rule.InvalidHolder
import br.com.gabriel.gastocerto.domain.rule.PaymentMethodHasEntries
import br.com.gabriel.gastocerto.domain.rule.ReimbursementExceedsBalance
import br.com.gabriel.gastocerto.domain.rule.RuleViolationException
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.day_kind_closing
import gastocerto.shared.generated.resources.day_kind_due
import gastocerto.shared.generated.resources.day_kind_pay
import gastocerto.shared.generated.resources.rule_accounting_month_closed
import gastocerto.shared.generated.resources.rule_installments_out_of_range
import gastocerto.shared.generated.resources.rule_invalid_amount
import gastocerto.shared.generated.resources.rule_invalid_day
import gastocerto.shared.generated.resources.rule_invalid_exchange_rate
import gastocerto.shared.generated.resources.rule_invalid_holder
import gastocerto.shared.generated.resources.rule_payment_method_has_entries
import gastocerto.shared.generated.resources.rule_reimbursement_exceeds_balance
import org.jetbrains.compose.resources.getString

/**
 * Text shown to the user for a business rule violation, in the device language
 * (`composeResources/values` = English, `values-pt-rBR` = Brazilian Portuguese).
 * The domain only carries the data (RNF07); the wording lives in the string resources.
 * The `when` is exhaustive over the sealed class: a new exception does not compile without its message.
 */
suspend fun RuleViolationException.userMessage(): String = when (this) {
    is InvalidAmount -> getString(Res.string.rule_invalid_amount)
    is InvalidExchangeRate -> getString(Res.string.rule_invalid_exchange_rate)
    is InstallmentsOutOfRange -> getString(Res.string.rule_installments_out_of_range, installmentCount)
    is AccountingMonthClosed ->
        getString(Res.string.rule_accounting_month_closed, month.month.toString().padStart(2, '0'), month.year)
    is ReimbursementExceedsBalance ->
        getString(Res.string.rule_reimbursement_exceeds_balance, amount.toBrl(), balance.toBrl())
    is PaymentMethodHasEntries -> getString(Res.string.rule_payment_method_has_entries, name)
    is InvalidDay -> getString(Res.string.rule_invalid_day, getString(kind.label()), day)
    is InvalidHolder -> getString(Res.string.rule_invalid_holder, name)
}

private fun DayKind.label() = when (this) {
    DayKind.CLOSING -> Res.string.day_kind_closing
    DayKind.DUE -> Res.string.day_kind_due
    DayKind.PAY -> Res.string.day_kind_pay
}
