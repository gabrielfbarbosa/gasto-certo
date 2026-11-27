package br.com.gabriel.gastocerto.data.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

// @Upsert inserts or updates the row with the same id (D2) without deleting it,
// so foreign keys pointing to it stay valid. `rowid` breaks ties in insertion order.

@Dao
interface PersonDao {
    @Upsert
    suspend fun upsert(person: PersonEntity)

    @Query("SELECT * FROM person ORDER BY name, rowid")
    suspend fun all(): List<PersonEntity>
}

@Dao
interface InstitutionDao {
    @Upsert
    suspend fun upsert(institution: InstitutionEntity)

    @Query("SELECT * FROM institution ORDER BY name, rowid")
    suspend fun all(): List<InstitutionEntity>
}

@Dao
interface PaymentMethodDao {
    @Upsert
    suspend fun upsert(paymentMethod: PaymentMethodEntity)

    @Query("DELETE FROM payment_method WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM payment_method")
    suspend fun all(): List<PaymentMethodEntity>

    @Query("SELECT * FROM payment_method WHERE active = 1 ORDER BY name, rowid")
    suspend fun active(): List<PaymentMethodEntity>

    @Query(
        "SELECT * FROM payment_method WHERE kind = 'INVESTMENT_ACCOUNT' AND active = 1 " +
            "AND plannedContributionCents IS NOT NULL ORDER BY name, rowid",
    )
    suspend fun investmentsWithContribution(): List<PaymentMethodEntity>
}

@Dao
interface EntryDao {
    @Upsert
    suspend fun upsert(entry: EntryEntity)

    @Upsert
    suspend fun upsertAll(entries: List<EntryEntity>)

    @Query("DELETE FROM entry WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM entry WHERE id = :id")
    suspend fun findById(id: String): EntryEntity?

    @Query("SELECT * FROM entry WHERE accountingMonth = :month ORDER BY date, rowid")
    suspend fun ofMonth(month: Int): List<EntryEntity>

    @Query("SELECT * FROM entry WHERE date BETWEEN :start AND :end ORDER BY date, rowid")
    suspend fun ofPeriod(start: Long, end: Long): List<EntryEntity>

    @Query("SELECT * FROM entry WHERE thirdPartyId = :thirdPartyId AND kind != 'INCOME' ORDER BY date, rowid")
    suspend fun ofThirdParty(thirdPartyId: String): List<EntryEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM entry WHERE paymentMethodId = :paymentMethodId)")
    suspend fun existsForPaymentMethod(paymentMethodId: String): Boolean
}

@Dao
interface AccountingMonthDao {
    @Upsert
    suspend fun upsert(accountingMonth: AccountingMonthEntity)

    @Query("SELECT * FROM accounting_month WHERE month = :month")
    suspend fun find(month: Int): AccountingMonthEntity?
}

@Dao
interface RecurrenceDao {
    @Upsert
    suspend fun upsertBill(bill: RecurringBillEntity)

    @Upsert
    suspend fun upsertIncome(income: RecurringIncomeEntity)

    @Query("SELECT * FROM recurring_bill WHERE active = 1 ORDER BY dueDay, rowid")
    suspend fun activeBills(): List<RecurringBillEntity>

    @Query("SELECT * FROM recurring_income WHERE active = 1 ORDER BY payDay, rowid")
    suspend fun activeIncomes(): List<RecurringIncomeEntity>
}

@Dao
interface ReimbursementDao {
    @Upsert
    suspend fun upsert(reimbursement: ReimbursementEntity)

    @Query("SELECT * FROM reimbursement WHERE thirdPartyId = :thirdPartyId ORDER BY date, rowid")
    suspend fun ofThirdParty(thirdPartyId: String): List<ReimbursementEntity>
}
