package br.com.gabriel.gastocerto.data.room

import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.CurrencyConversion
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.model.Reimbursement
import br.com.gabriel.gastocerto.domain.model.Role
import kotlinx.datetime.LocalDate

// Entity ⇄ domain mappers. Rebuilding a domain object goes through its constructor,
// so the rules in `init` (RN09, RN10…) are checked again on every read.

internal fun MonthYear.toKey(): Int = year * 100 + month

internal fun Int.toMonthYear(): MonthYear = MonthYear(this / 100, this % 100)

internal fun LocalDate.toColumn(): Long = toEpochDays()

internal fun Long.toLocalDate(): LocalDate = LocalDate.fromEpochDays(this)

/** A column that must be filled for this kind of row (e.g. `closingDay` of a CREDIT_CARD). */
private fun <T : Any> T?.required(column: String): T = checkNotNull(this) { "column $column is empty" }

// ---------- Person and Institution ----------

fun Person.toEntity() = PersonEntity(id = id, name = name, role = role.name)

fun PersonEntity.toDomain() = Person(name = name, role = Role.valueOf(role), id = id)

fun Institution.toEntity() = InstitutionEntity(id = id, name = name)

fun InstitutionEntity.toDomain() = Institution(name = name, id = id)

// ---------- PaymentMethod ----------

fun PaymentMethod.toEntity(): PaymentMethodEntity {
    val row = PaymentMethodEntity(
        id = id, kind = "", name = name, holderId = holder.id, institutionId = institution.id, active = active,
    )
    return when (this) {
        is CreditCard -> row.copy(kind = PaymentMethodKind.CREDIT_CARD.name, closingDay = closingDay, dueDay = dueDay)
        is CheckingAccount -> row.copy(kind = PaymentMethodKind.CHECKING_ACCOUNT.name)
        is ForeignCurrencyWallet -> row.copy(kind = PaymentMethodKind.FOREIGN_CURRENCY_WALLET.name, currency = currency)
        is InvestmentAccount -> row.copy(
            kind = PaymentMethodKind.INVESTMENT_ACCOUNT.name,
            plannedContributionCents = plannedMonthlyContribution?.cents,
        )
    }
}

fun PaymentMethodEntity.toDomain(holder: Person, institution: Institution): PaymentMethod =
    when (PaymentMethodKind.valueOf(kind)) {
        PaymentMethodKind.CREDIT_CARD -> CreditCard(
            name, holder, institution, closingDay.required("closingDay"), dueDay.required("dueDay"), active, id,
        )
        PaymentMethodKind.CHECKING_ACCOUNT -> CheckingAccount(name, holder, institution, active, id)
        PaymentMethodKind.FOREIGN_CURRENCY_WALLET ->
            ForeignCurrencyWallet(name, holder, institution, currency.required("currency"), active, id)
        PaymentMethodKind.INVESTMENT_ACCOUNT ->
            InvestmentAccount(name, holder, institution, plannedContributionCents?.let(::Money), active, id)
    }

// ---------- Entry ----------

fun Entry.toEntity(): EntryEntity {
    val row = EntryEntity(
        id = id, kind = "", amountCents = amount.cents, date = date.toColumn(), description = description,
        accountingMonth = accountingMonth.toKey(), status = status.name, originId = originId,
    )
    return when (this) {
        is Installment -> row.copy(
            kind = EntryKind.INSTALLMENT.name, paymentMethodId = paymentMethod.id, thirdPartyId = thirdParty?.id,
            purchaseId = purchaseId, installmentNumber = number, totalInstallments = totalInstallments,
        )
        is Expense -> row.copy(
            kind = EntryKind.EXPENSE.name, paymentMethodId = paymentMethod.id, thirdPartyId = thirdParty?.id,
            originalAmount = conversion?.originalAmount, exchangeRate = conversion?.exchangeRate,
        )
        is Income -> row.copy(kind = EntryKind.INCOME.name, incomeType = type.name)
    }
}

/** [paymentMethod] and [thirdParty] are the rows this one points to, already rebuilt. */
fun EntryEntity.toDomain(paymentMethod: PaymentMethod?, thirdParty: Person?): Entry {
    val amount = Money(amountCents)
    val date = date.toLocalDate()
    val month = accountingMonth.toMonthYear()
    val status = EntryStatus.valueOf(status)
    return when (EntryKind.valueOf(kind)) {
        EntryKind.INSTALLMENT -> Installment(
            amount, date, description, month, paymentMethod.required("paymentMethodId") as CreditCard,
            purchaseId.required("purchaseId"), installmentNumber.required("installmentNumber"),
            totalInstallments.required("totalInstallments"), thirdParty, id,
        )
        EntryKind.EXPENSE -> Expense(
            amount, date, description, month, paymentMethod.required("paymentMethodId"), thirdParty,
            originalAmount?.let { CurrencyConversion(it, exchangeRate.required("exchangeRate")) },
            status, originId, id,
        )
        EntryKind.INCOME -> Income(
            amount, date, description, month, IncomeType.valueOf(incomeType.required("incomeType")), status, originId, id,
        )
    }
}

// ---------- AccountingMonth ----------

fun AccountingMonth.toEntity() = AccountingMonthEntity(month = monthYear.toKey(), status = status.name)

fun AccountingMonthEntity.toDomain() = AccountingMonth(month.toMonthYear(), AccountingMonthStatus.valueOf(status))

// ---------- Recurrences and reimbursements ----------

fun RecurringBill.toEntity() = RecurringBillEntity(
    id = id, description = description, expectedAmountCents = expectedAmount.cents, dueDay = dueDay,
    paymentMethodId = paymentMethod.id, active = active,
)

fun RecurringBillEntity.toDomain(paymentMethod: PaymentMethod) =
    RecurringBill(description, Money(expectedAmountCents), dueDay, paymentMethod, active, id)

fun RecurringIncome.toEntity() = RecurringIncomeEntity(
    id = id, description = description, amountCents = amount.cents, payDay = payDay, active = active,
)

fun RecurringIncomeEntity.toDomain() = RecurringIncome(description, Money(amountCents), payDay, active, id)

fun Reimbursement.toEntity() = ReimbursementEntity(
    id = id, thirdPartyId = thirdParty.id, amountCents = amount.cents, date = date.toColumn(),
)

fun ReimbursementEntity.toDomain(thirdParty: Person) = Reimbursement(thirdParty, Money(amountCents), date.toLocalDate(), id)
