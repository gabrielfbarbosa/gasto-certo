package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.ReimbursementExceedsBalance

/** RN03 – how much a [thirdParty] still owes: expenses on their behalf − reimbursements received. */
data class ReceivableBalance(val thirdParty: Person, val amount: Money) {

    /** RN03 – throws [ReimbursementExceedsBalance] when a reimbursement of [amount] exceeds the balance. */
    fun validateReimbursement(amount: Money) {
        if (amount.cents > this.amount.cents) throw ReimbursementExceedsBalance(amount, this.amount)
    }

    companion object {
        /** Computes the balance of [thirdParty]; expenses and reimbursements of other people are ignored. */
        fun of(thirdParty: Person, expenses: List<Expense>, reimbursements: List<Reimbursement>): ReceivableBalance {
            val owed = expenses.filter { it.thirdParty == thirdParty }.map { it.amount }.sum()
            val paidBack = reimbursements.filter { it.thirdParty == thirdParty }.map { it.amount }.sum()
            return ReceivableBalance(thirdParty, owed - paidBack)
        }
    }
}
