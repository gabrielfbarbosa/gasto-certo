package br.com.gabriel.gastocerto.domain.model

/**
 * Totals of a period (week, month or year) – RF14, RF15.
 *
 * - [totalExpenses] adds up every expense, including those made on behalf of third parties (RN03).
 * - [thirdPartyExpenses]: the part of the total that will be paid back; deducted from the [surplus] (RN03, RN04).
 * - [byPaymentMethod] and [byHolder]: total expenses broken down by payment method and by its holder (RN10).
 * - Reimbursements are left out: they are not income (RN03).
 */
data class PeriodSummary(
    val income: Money,
    val totalExpenses: Money,
    val thirdPartyExpenses: Money,
    val byPaymentMethod: Map<PaymentMethod, Money>,
    val byHolder: Map<Person, Money>,
) {
    /** RN04 – surplus = income − (total expenses − third-party expenses). May be negative. */
    fun surplus(): Money = income - (totalExpenses - thirdPartyExpenses)

    companion object {
        /**
         * Builds the summary from the period's [entries] (EXPECTED and ACTUAL: expected entries count
         * at their expected amount until confirmed – D11).
         *
         * RN10 – with a [holder], only expenses of that holder's payment methods count; incomes have no
         * holder and all of them stay (they belong to the household – D10).
         */
        fun of(entries: List<Entry>, holder: Person? = null): PeriodSummary {
            val expenses = entries.filterIsInstance<Expense>()
                .filter { holder == null || it.paymentMethod.holder == holder }
            val incomes = entries.filterIsInstance<Income>()
            return PeriodSummary(
                income = incomes.map { it.amount }.sum(),
                totalExpenses = expenses.map { it.amount }.sum(),
                thirdPartyExpenses = expenses.filter { it.isThirdParty() }.map { it.amount }.sum(),
                byPaymentMethod = expenses.groupBy { it.paymentMethod }
                    .mapValues { (_, ofMethod) -> ofMethod.map { it.amount }.sum() },
                byHolder = expenses.groupBy { it.paymentMethod.holder }
                    .mapValues { (_, ofHolder) -> ofHolder.map { it.amount }.sum() },
            )
        }
    }
}
