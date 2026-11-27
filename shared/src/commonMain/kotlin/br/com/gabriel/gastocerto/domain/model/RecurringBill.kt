package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.DayKind
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.InvalidDay

/**
 * RN06 – monthly recurring bill (water, power, internet, groceries) paid with a [paymentMethod].
 *
 * **Creator**: [generateExpected] creates each month's EXPECTED expense. [expectedAmount] > 0 (RN09)
 * and [dueDay] between 1 and 31 (RN09). Only active bills generate expected entries. Equality by [id] (D2).
 */
class RecurringBill(
    val description: String,
    val expectedAmount: Money,
    val dueDay: Int,
    val paymentMethod: PaymentMethod,
    var active: Boolean = true,
    val id: String = newId(),
) {
    init {
        if (!expectedAmount.isPositive()) throw InvalidAmount(expectedAmount)
        if (dueDay !in 1..31) throw InvalidDay(DayKind.DUE, dueDay)
    }

    /**
     * RN06 – EXPECTED expense for [month] on the due day (missing day → last day of the month),
     * with `originId` = [id] so it is not generated twice (D3). The amount is adjusted on confirmation.
     */
    fun generateExpected(month: MonthYear): Expense = Expense(
        amount = expectedAmount,
        date = month.atDay(dueDay),
        description = description,
        accountingMonth = month,
        paymentMethod = paymentMethod,
        status = EntryStatus.EXPECTED,
        originId = id,
    )

    override fun equals(other: Any?): Boolean = other is RecurringBill && other.id == id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "RecurringBill($description, day $dueDay)"
}
