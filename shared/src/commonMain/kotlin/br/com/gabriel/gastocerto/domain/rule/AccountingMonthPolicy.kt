package br.com.gabriel.gastocerto.domain.rule

import br.com.gabriel.gastocerto.domain.model.MonthYear
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/**
 * RN01 – **Strategy**: decides the accounting month of an expense from its date.
 *
 * Each payment method returns its policy in `accountingMonthPolicy()`; use cases only call
 * [accountingMonthOf], with no `when(paymentMethod)`. A new kind of payment method is a new policy.
 */
interface AccountingMonthPolicy {
    fun accountingMonthOf(date: LocalDate): MonthYear
}

/**
 * RN01 – credit card. A purchase made **up to** the closing day goes into the current statement;
 * **after** it, into the next one. The accounting month is the statement's **due month**:
 * if [dueDay] > [closingDay], it is due in the same month as the closing; otherwise, in the next month.
 *
 * A closing day that does not exist in the month (e.g. 31 in November) becomes the month's last day ([MonthYear.atDay]).
 */
class StatementClosingPolicy(
    private val closingDay: Int,
    private val dueDay: Int,
) : AccountingMonthPolicy {

    init {
        require(closingDay in 1..31) { "closing day must be between 1 and 31, but was $closingDay" }
        require(dueDay in 1..31) { "due day must be between 1 and 31, but was $dueDay" }
    }

    override fun accountingMonthOf(date: LocalDate): MonthYear {
        val purchaseMonth = MonthYear(date.year, date.month.number)
        val closingMonth = if (date <= purchaseMonth.atDay(closingDay)) purchaseMonth else purchaseMonth.next()
        return if (dueDay > closingDay) closingMonth else closingMonth.next()
    }
}

/** RN01 – checking account, debit, Pix, foreign wallet and brokerage: the month of the expense date. */
class ExpenseDatePolicy : AccountingMonthPolicy {
    override fun accountingMonthOf(date: LocalDate): MonthYear = MonthYear(date.year, date.month.number)
}
