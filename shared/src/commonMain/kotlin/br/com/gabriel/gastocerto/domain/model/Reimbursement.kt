package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import kotlinx.datetime.LocalDate

/**
 * RN03 – amount paid back by a [thirdParty]. It lowers the receivable balance and **is not income**.
 * [amount] > 0 (RN09); may not exceed the balance ([ReceivableBalance.validateReimbursement]). Equality by [id] (D2).
 */
class Reimbursement(
    val thirdParty: Person,
    val amount: Money,
    val date: LocalDate,
    val id: String = newId(),
) {
    init {
        if (!amount.isPositive()) throw InvalidAmount(amount)
    }

    override fun equals(other: Any?): Boolean = other is Reimbursement && other.id == id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Reimbursement(${thirdParty.name}, ${amount.cents}, $date)"
}
