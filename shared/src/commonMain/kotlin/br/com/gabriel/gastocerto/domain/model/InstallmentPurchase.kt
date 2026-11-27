package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.InstallmentsOutOfRange
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * RN02 – credit card purchase split into [installmentCount] installments (2 to 48).
 *
 * **Creator + Information Expert**: [generateInstallments] creates the installments. It has no table of its own:
 * it is rebuilt from the installments, which keep the purchase [id] in `purchaseId` (D5).
 * Every installment is on behalf of the [thirdParty], if any (RN03). Equality by [id] (D2).
 */
class InstallmentPurchase(
    val totalAmount: Money,
    val installmentCount: Int,
    val purchaseDate: LocalDate,
    val description: String,
    val card: CreditCard,
    val thirdParty: Person? = null,
    val id: String = newId(),
) {
    init {
        if (!totalAmount.isPositive()) throw InvalidAmount(totalAmount)
        if (installmentCount !in INSTALLMENT_RANGE) throw InstallmentsOutOfRange(installmentCount)
        // RN09 – each installment needs at least 1 cent (e.g. R$ 0.01 in 2x is invalid)
        if (totalAmount.cents < installmentCount) throw InvalidAmount(Money(totalAmount.cents / installmentCount))
    }

    /**
     * RN02 – generates the installments in consecutive accounting months starting from the 1st one,
     * given by the card policy (RN01). Amount = total ÷ N in cents, with the remainder in the 1st;
     * the installments add up to the total. The date of installment k is the purchase date + (k − 1) months
     * (missing day → last day of the month), for the weekly and yearly summaries (D9).
     */
    fun generateInstallments(): List<Installment> {
        val policy = card.accountingMonthPolicy()
        var accountingMonth = policy.accountingMonthOf(purchaseDate)
        val amounts = totalAmount.splitInto(installmentCount)
        return amounts.mapIndexed { index, amount ->
            if (index > 0) accountingMonth = accountingMonth.next()
            Installment(
                amount = amount,
                date = purchaseDate.plus(index, DateTimeUnit.MONTH),
                description = description,
                accountingMonth = accountingMonth,
                paymentMethod = card,
                purchaseId = id,
                number = index + 1,
                totalInstallments = installmentCount,
                thirdParty = thirdParty,
            )
        }
    }

    override fun equals(other: Any?): Boolean = other is InstallmentPurchase && other.id == id
    override fun hashCode(): Int = id.hashCode()

    companion object {
        /** RN02 – from 2 to 48 installments. */
        val INSTALLMENT_RANGE = 2..48
    }
}
