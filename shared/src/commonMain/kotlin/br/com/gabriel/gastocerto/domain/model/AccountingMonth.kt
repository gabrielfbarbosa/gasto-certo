package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed

/** Status of an accounting month: OPEN accepts entries; CLOSED does not (RN08). */
enum class AccountingMonthStatus { OPEN, CLOSED }

/**
 * RN08 – one month of the household books. A CLOSED month accepts no insertion, edit or deletion
 * of entries; reopening is explicit ([reopen]). Equality by [monthYear] (one per month).
 */
class AccountingMonth(
    val monthYear: MonthYear,
    status: AccountingMonthStatus = AccountingMonthStatus.OPEN,
) {
    var status: AccountingMonthStatus = status
        private set

    /** RN08 – closes the month. Closing a month that is already closed throws [AccountingMonthClosed]. */
    fun close() {
        ensureOpen()
        status = AccountingMonthStatus.CLOSED
    }

    /** RN08 – reopens the month for corrections. Reopening an open month changes nothing. */
    fun reopen() {
        status = AccountingMonthStatus.OPEN
    }

    /** RN08 – throws [AccountingMonthClosed] when the month is closed; called before saving entries. */
    fun ensureOpen() {
        if (status == AccountingMonthStatus.CLOSED) throw AccountingMonthClosed(monthYear)
    }

    override fun equals(other: Any?): Boolean = other is AccountingMonth && other.monthYear == monthYear
    override fun hashCode(): Int = monthYear.hashCode()
    override fun toString(): String = "AccountingMonth(${monthYear.month}/${monthYear.year}, $status)"
}
