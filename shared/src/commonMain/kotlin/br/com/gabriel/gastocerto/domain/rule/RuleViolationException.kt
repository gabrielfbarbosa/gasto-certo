package br.com.gabriel.gastocerto.domain.rule

import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear

/**
 * Violation of a business rule. Thrown by the domain or by the [EntryFactory] and caught by the
 * ViewModel, which shows the user a localized text (string resources in English and Portuguese,
 * see `ui/RuleViolationMessages.kt`) built from the exception's data — no crash.
 *
 * [rule] is the ID of the violated business rule (e.g. `"RN02"`), used in tests and logs;
 * [message] is a technical English description.
 */
sealed class RuleViolationException(
    val rule: String,
    message: String,
) : RuntimeException("$rule: $message")

/** RN09 – an entry amount must be greater than zero. */
class InvalidAmount(val amount: Money) :
    RuleViolationException("RN09", "amount must be greater than zero (given: ${amount.cents} cents)")

/** RN05 – a foreign currency expense requires an exchange rate greater than zero. */
class InvalidExchangeRate(val exchangeRate: Long) :
    RuleViolationException("RN05", "exchange rate must be greater than zero (given: $exchangeRate)")

/** RN02 – an installment purchase must have from 2 to 48 installments. */
class InstallmentsOutOfRange(val installmentCount: Int) :
    RuleViolationException("RN02", "installment purchase must have 2 to 48 installments (given: $installmentCount)")

/** RN08 – a closed accounting month accepts no insertion, edit or deletion of entries. */
class AccountingMonthClosed(val month: MonthYear) :
    RuleViolationException("RN08", "month ${month.month}/${month.year} is closed; reopen it to change entries")

/** RN03 – a reimbursement may not exceed the third party's receivable balance. */
class ReimbursementExceedsBalance(val amount: Money, val balance: Money) :
    RuleViolationException(
        "RN03",
        "reimbursement (${amount.cents} cents) exceeds the receivable balance (${balance.cents} cents)",
    )

/** RN09 – a payment method or institution with entries cannot be deleted, only deactivated. */
class PaymentMethodHasEntries(val name: String) :
    RuleViolationException("RN09", "\"$name\" has entries and cannot be deleted; deactivate it instead")

/** Which day an [InvalidDay] refers to. */
enum class DayKind { CLOSING, DUE, PAY }

/** RN09 – closing, due and pay days must be between 1 and 31. */
class InvalidDay(val kind: DayKind, val day: Int) :
    RuleViolationException("RN09", "${kind.name.lowercase()} day must be between 1 and 31 (given: $day)")

/** RN10 – the holder of a payment method is the user or a household holder, never a third party. */
class InvalidHolder(val name: String) :
    RuleViolationException("RN10", "$name is a third party and cannot hold a payment method")
