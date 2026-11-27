package br.com.gabriel.gastocerto.domain.rule

import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.CurrencyConversion
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.InstallmentPurchase
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import kotlinx.datetime.LocalDate

/**
 * Data entered to register a single expense (UC01).
 *
 * - Regular payment method: [amount] in reais (required).
 * - Foreign currency wallet (RN05): [foreignAmount] in cents of the currency and [exchangeRate] with 4 decimal
 *   places (D1); [amount] is ignored and computed by the conversion.
 * - [thirdParty]: expense on behalf of someone who will pay it back (RN03).
 * - Brokerage: the expense is an actual contribution (RN11).
 */
data class ExpenseInput(
    val paymentMethod: PaymentMethod,
    val amount: Money?,
    val date: LocalDate,
    val description: String,
    val thirdParty: Person? = null,
    val foreignAmount: Long? = null,
    val exchangeRate: Long? = null,
)

/** Data entered to register an installment purchase on a credit card (UC02). */
data class PurchaseInput(
    val card: CreditCard,
    val totalAmount: Money,
    val installmentCount: Int,
    val purchaseDate: LocalDate,
    val description: String,
    val thirdParty: Person? = null,
)

/**
 * **Factory** – gathers the ways entries are created (single, foreign currency, third party,
 * contribution, installments and the month's expected entries) and the shared validations, so use cases
 * just ask for a ready entry. Throws subclasses of [RuleViolationException].
 */
class EntryFactory {

    /**
     * UC01 – ACTUAL single expense, with the accounting month given by the payment method policy (RN01).
     * RN09: amount > 0 (`InvalidAmount`). RN05: in a foreign currency wallet, rate > 0 (`InvalidExchangeRate`)
     * and foreign amount > 0; the amount in reais comes from the conversion (D1).
     */
    fun expense(input: ExpenseInput): Expense {
        val conversion = if (input.paymentMethod is ForeignCurrencyWallet) conversionOf(input) else null
        val amount = conversion?.toBrl() ?: input.amount ?: Money(0)
        if (!amount.isPositive()) throw InvalidAmount(amount)
        return Expense(
            amount = amount,
            date = input.date,
            description = input.description,
            accountingMonth = input.paymentMethod.accountingMonthPolicy().accountingMonthOf(input.date),
            paymentMethod = input.paymentMethod,
            thirdParty = input.thirdParty,
            conversion = conversion,
        )
    }

    /**
     * UC02 – validates and creates the [InstallmentPurchase] (the installments come from `generateInstallments()`).
     * RN09: total amount > 0 and at least 1 cent per installment (`InvalidAmount`).
     * RN02: from 2 to 48 installments (`InstallmentsOutOfRange`).
     */
    fun installmentPurchase(input: PurchaseInput): InstallmentPurchase {
        if (!input.totalAmount.isPositive()) throw InvalidAmount(input.totalAmount)
        if (input.installmentCount !in InstallmentPurchase.INSTALLMENT_RANGE) {
            throw InstallmentsOutOfRange(input.installmentCount)
        }
        return InstallmentPurchase(
            totalAmount = input.totalAmount,
            installmentCount = input.installmentCount,
            purchaseDate = input.purchaseDate,
            description = input.description,
            card = input.card,
            thirdParty = input.thirdParty,
        )
    }

    /**
     * UC03 – expected entries of the [month] being opened: active recurring bills (RN06), active
     * recurring incomes (RN07) and planned contributions of active brokerages (RN11), each with
     * `originId` (D3). Does not remove duplicates: `CloseMonthUseCase` does that (`withoutDuplicates`).
     */
    fun expectedForMonth(
        month: MonthYear,
        recurringBills: List<RecurringBill>,
        recurringIncomes: List<RecurringIncome>,
        investments: List<InvestmentAccount>,
    ): List<Entry> =
        recurringBills.filter { it.active }.map { it.generateExpected(month) } +
            recurringIncomes.filter { it.active }.map { it.generateExpected(month) } +
            investments.filter { it.active }.mapNotNull { it.generateExpectedContribution(month) }

    /** RN05 – the conversion requires rate > 0 and foreign amount > 0 (checked in [CurrencyConversion]). */
    private fun conversionOf(input: ExpenseInput): CurrencyConversion {
        val rate = input.exchangeRate ?: throw InvalidExchangeRate(0)
        val foreignAmount = input.foreignAmount ?: throw InvalidAmount(Money(0))
        return CurrencyConversion(originalAmount = foreignAmount, exchangeRate = rate)
    }
}
