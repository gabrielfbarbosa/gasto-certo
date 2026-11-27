package br.com.gabriel.gastocerto.domain.model

import kotlinx.datetime.LocalDate

/**
 * RF13 – statement of a [card] for the [accountingMonth] (the due month, RN01). **It is not stored**:
 * it is assembled from the card and the expenses ([of]).
 */
data class Statement(
    val card: CreditCard,
    val accountingMonth: MonthYear,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
    val expenses: List<Expense>,
) {
    /** Sum of the statement's expenses and installments. */
    fun total(): Money = expenses.map { it.amount }.sum()

    companion object {
        /**
         * Assembles the statement of [card] due in [month]. Due date = due day in [month];
         * closing in the same month if `dueDay > closingDay`, otherwise in the previous month (RN01).
         * Missing day → last day of the month. Only expenses of this card in this accounting month count.
         */
        fun of(card: CreditCard, month: MonthYear, expenses: List<Expense>): Statement {
            val closingMonth = if (card.dueDay > card.closingDay) month else month.previous()
            return Statement(
                card = card,
                accountingMonth = month,
                closingDate = closingMonth.atDay(card.closingDay),
                dueDate = month.atDay(card.dueDay),
                expenses = expenses.filter { it.paymentMethod == card && it.accountingMonth == month },
            )
        }
    }
}
