package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.DayKind
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.InvalidDay

/**
 * RN07 – monthly income on a fixed day (salary).
 *
 * **Creator**: [generateExpected] creates each month's EXPECTED income. [amount] > 0 (RN09) and
 * [payDay] between 1 and 31 (RN09). Only active incomes generate expected entries. Equality by [id] (D2).
 */
class RecurringIncome(
    val description: String,
    val amount: Money,
    val payDay: Int,
    var active: Boolean = true,
    val id: String = newId(),
) {
    init {
        if (!amount.isPositive()) throw InvalidAmount(amount)
        if (payDay !in 1..31) throw InvalidDay(DayKind.PAY, payDay)
    }

    /**
     * RN07 – EXPECTED income (type SALARY) for [month] on the pay day (missing day →
     * last day of the month), with `originId` = [id] (D3).
     */
    fun generateExpected(month: MonthYear): Income = Income(
        amount = amount,
        date = month.atDay(payDay),
        description = description,
        accountingMonth = month,
        type = IncomeType.SALARY,
        status = EntryStatus.EXPECTED,
        originId = id,
    )

    override fun equals(other: Any?): Boolean = other is RecurringIncome && other.id == id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "RecurringIncome($description, day $payDay)"
}
