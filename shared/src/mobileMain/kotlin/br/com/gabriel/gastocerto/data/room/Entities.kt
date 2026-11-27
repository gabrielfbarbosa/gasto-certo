package br.com.gabriel.gastocerto.data.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Table rows. The domain never sees these classes (RNF07): Mappers.kt converts both ways.
// Conventions: money in cents (`Long`, RNF04); dates as epoch days (`Long`, sortable);
// accounting month as `year * 100 + month` (`Int`, e.g. 202611); enums stored by **name**,
// so reordering an enum does not corrupt stored data. Foreign keys keep references valid.

@Entity(tableName = "person")
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String,
)

@Entity(tableName = "institution")
data class InstitutionEntity(
    @PrimaryKey val id: String,
    val name: String,
)

/** Kind of payment method: one table for the 4 subclasses, with the columns each one needs. */
enum class PaymentMethodKind { CREDIT_CARD, CHECKING_ACCOUNT, FOREIGN_CURRENCY_WALLET, INVESTMENT_ACCOUNT }

@Entity(
    tableName = "payment_method",
    foreignKeys = [
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["holderId"]),
        ForeignKey(entity = InstitutionEntity::class, parentColumns = ["id"], childColumns = ["institutionId"]),
    ],
    indices = [Index("holderId"), Index("institutionId")],
)
data class PaymentMethodEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val name: String,
    val holderId: String,
    val institutionId: String,
    val active: Boolean,
    /** CREDIT_CARD only. */
    val closingDay: Int? = null,
    /** CREDIT_CARD only. */
    val dueDay: Int? = null,
    /** FOREIGN_CURRENCY_WALLET only. */
    val currency: String? = null,
    /** INVESTMENT_ACCOUNT only, optional (RN11). */
    val plannedContributionCents: Long? = null,
)

/** Kind of entry: one table for expenses, installments and incomes. */
enum class EntryKind { EXPENSE, INSTALLMENT, INCOME }

@Entity(
    tableName = "entry",
    foreignKeys = [
        ForeignKey(entity = PaymentMethodEntity::class, parentColumns = ["id"], childColumns = ["paymentMethodId"]),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["thirdPartyId"]),
    ],
    indices = [Index("accountingMonth"), Index("date"), Index("paymentMethodId"), Index("thirdPartyId")],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val amountCents: Long,
    val date: Long,
    val description: String,
    val accountingMonth: Int,
    val status: String,
    val originId: String? = null,
    /** EXPENSE and INSTALLMENT. */
    val paymentMethodId: String? = null,
    /** EXPENSE and INSTALLMENT, when made on behalf of a third party (RN03). */
    val thirdPartyId: String? = null,
    /** EXPENSE in a foreign currency (RN05, D1). */
    val originalAmount: Long? = null,
    val exchangeRate: Long? = null,
    /** INSTALLMENT only: the purchase is rebuilt from its installments (D5). */
    val purchaseId: String? = null,
    val installmentNumber: Int? = null,
    val totalInstallments: Int? = null,
    /** INCOME only. */
    val incomeType: String? = null,
)

@Entity(tableName = "accounting_month")
data class AccountingMonthEntity(
    @PrimaryKey val month: Int,
    val status: String,
)

@Entity(
    tableName = "recurring_bill",
    foreignKeys = [ForeignKey(entity = PaymentMethodEntity::class, parentColumns = ["id"], childColumns = ["paymentMethodId"])],
    indices = [Index("paymentMethodId")],
)
data class RecurringBillEntity(
    @PrimaryKey val id: String,
    val description: String,
    val expectedAmountCents: Long,
    val dueDay: Int,
    val paymentMethodId: String,
    val active: Boolean,
)

@Entity(tableName = "recurring_income")
data class RecurringIncomeEntity(
    @PrimaryKey val id: String,
    val description: String,
    val amountCents: Long,
    val payDay: Int,
    val active: Boolean,
)

@Entity(
    tableName = "reimbursement",
    foreignKeys = [ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["thirdPartyId"])],
    indices = [Index("thirdPartyId")],
)
data class ReimbursementEntity(
    @PrimaryKey val id: String,
    val thirdPartyId: String,
    val amountCents: Long,
    val date: Long,
)
