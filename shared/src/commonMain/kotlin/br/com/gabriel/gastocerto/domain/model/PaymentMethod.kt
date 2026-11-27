package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.AccountingMonthPolicy
import br.com.gabriel.gastocerto.domain.rule.DayKind
import br.com.gabriel.gastocerto.domain.rule.ExpenseDatePolicy
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.InvalidDay
import br.com.gabriel.gastocerto.domain.rule.InvalidHolder
import br.com.gabriel.gastocerto.domain.rule.StatementClosingPolicy

/**
 * Where an expense is paid from (RF03). Each kind returns its own [AccountingMonthPolicy] (Strategy, RN01),
 * so use cases need no `when(paymentMethod)`.
 *
 * RN10 – every payment method has a [holder] (the user or a household holder, never a third party);
 * an institution may have several payment methods of different holders.
 * RN09 – a payment method with entries is not deleted, only deactivated ([active] = `false`).
 * Equality by [id] (D2).
 */
sealed class PaymentMethod(
    val name: String,
    val holder: Person,
    val institution: Institution,
    var active: Boolean,
    val id: String,
) {
    init {
        if (holder.role == Role.THIRD_PARTY) throw InvalidHolder(holder.name)
    }

    abstract fun accountingMonthPolicy(): AccountingMonthPolicy

    final override fun equals(other: Any?): Boolean = other is PaymentMethod && other.id == id
    final override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "${this::class.simpleName}($name, ${holder.name})"
}

/** Credit card: the accounting month follows the statement closing and due days (RN01); days from 1 to 31 (RN09). */
class CreditCard(
    name: String,
    holder: Person,
    institution: Institution,
    val closingDay: Int,
    val dueDay: Int,
    active: Boolean = true,
    id: String = newId(),
) : PaymentMethod(name, holder, institution, active, id) {

    init {
        if (closingDay !in 1..31) throw InvalidDay(DayKind.CLOSING, closingDay)
        if (dueDay !in 1..31) throw InvalidDay(DayKind.DUE, dueDay)
    }

    override fun accountingMonthPolicy(): AccountingMonthPolicy = StatementClosingPolicy(closingDay, dueDay)
}

/** Checking account (debit and Pix): accounting month = month of the expense date (RN01). */
class CheckingAccount(
    name: String,
    holder: Person,
    institution: Institution,
    active: Boolean = true,
    id: String = newId(),
) : PaymentMethod(name, holder, institution, active, id) {
    override fun accountingMonthPolicy(): AccountingMonthPolicy = ExpenseDatePolicy()
}

/** Foreign currency wallet (e.g. Wise in dollars); its expenses require an exchange rate (RN05). Month of the expense date. */
class ForeignCurrencyWallet(
    name: String,
    holder: Person,
    institution: Institution,
    val currency: String,
    active: Boolean = true,
    id: String = newId(),
) : PaymentMethod(name, holder, institution, active, id) {
    override fun accountingMonthPolicy(): AccountingMonthPolicy = ExpenseDatePolicy()
}

/**
 * Brokerage account (RN11). The [plannedMonthlyContribution], when given, must be greater than zero
 * and generates the EXPECTED contribution of each month ([generateExpectedContribution]).
 * Accounting month = month of the expense date.
 */
class InvestmentAccount(
    name: String,
    holder: Person,
    institution: Institution,
    val plannedMonthlyContribution: Money? = null,
    active: Boolean = true,
    id: String = newId(),
) : PaymentMethod(name, holder, institution, active, id) {

    init {
        if (plannedMonthlyContribution != null && !plannedMonthlyContribution.isPositive()) {
            throw InvalidAmount(plannedMonthlyContribution)
        }
    }

    override fun accountingMonthPolicy(): AccountingMonthPolicy = ExpenseDatePolicy()

    /**
     * RN11 – EXPECTED contribution for [month] (an expense in this brokerage) on day 1, with `originId` = [id] (D3).
     * Returns `null` when there is no planned contribution. The amount is adjusted on confirmation.
     * The description is the brokerage name (data, not UI text); the screen adds "Planned contribution" in the user's language.
     */
    fun generateExpectedContribution(month: MonthYear): Expense? = plannedMonthlyContribution?.let { amount ->
        Expense(
            amount = amount,
            date = month.atDay(1),
            description = name,
            accountingMonth = month,
            paymentMethod = this,
            status = EntryStatus.EXPECTED,
            originId = id,
        )
    }
}
