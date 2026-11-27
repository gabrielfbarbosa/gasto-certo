package br.com.gabriel.gastocerto.data.room

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.model.Reimbursement
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.repository.RecurrenceRepository
import br.com.gabriel.gastocerto.domain.repository.RegistryRepository
import br.com.gabriel.gastocerto.domain.repository.ReimbursementRepository
import br.com.gabriel.gastocerto.domain.repository.Transaction
import kotlinx.datetime.LocalDate

// Room implementations of the domain ports (Android and iOS). They meet the same contract
// as the in-memory ones (RepositoriesContract). Rows a record points to (payment method,
// person, institution) must be saved first: the foreign keys reject dangling references.

class RoomEntryRepository(private val database: AppDatabase) : EntryRepository {
    private val dao = database.entryDao()

    override suspend fun save(entry: Entry) = dao.upsert(entry.toEntity())

    override suspend fun saveAll(entries: List<Entry>) = dao.upsertAll(entries.map { it.toEntity() })

    override suspend fun delete(entry: Entry) = dao.delete(entry.id)

    override suspend fun findById(id: String): Entry? = dao.findById(id)?.let { toDomain(listOf(it)).single() }

    override suspend fun ofMonth(month: MonthYear): List<Entry> = toDomain(dao.ofMonth(month.toKey()))

    override suspend fun ofPeriod(start: LocalDate, end: LocalDate): List<Entry> =
        toDomain(dao.ofPeriod(start.toColumn(), end.toColumn()))

    override suspend fun ofThirdParty(thirdParty: Person): List<Expense> =
        toDomain(dao.ofThirdParty(thirdParty.id)).filterIsInstance<Expense>()

    private suspend fun toDomain(rows: List<EntryEntity>): List<Entry> {
        if (rows.isEmpty()) return emptyList()
        val registry = database.registrySnapshot()
        return rows.map(registry::entry)
    }
}

class RoomAccountingMonthRepository(database: AppDatabase) : AccountingMonthRepository {
    private val dao = database.accountingMonthDao()

    override suspend fun findOrCreate(month: MonthYear): AccountingMonth =
        dao.find(month.toKey())?.toDomain() ?: AccountingMonth(month).also { save(it) }

    override suspend fun save(accountingMonth: AccountingMonth) = dao.upsert(accountingMonth.toEntity())
}

class RoomRegistryRepository(private val database: AppDatabase) : RegistryRepository {

    override suspend fun people(): List<Person> = database.personDao().all().map { it.toDomain() }

    override suspend fun institutions(): List<Institution> = database.institutionDao().all().map { it.toDomain() }

    override suspend fun activePaymentMethods(): List<PaymentMethod> {
        val registry = database.registrySnapshot()
        return database.paymentMethodDao().active().map { registry.paymentMethod(it.id) }
    }

    override suspend fun savePerson(person: Person) = database.personDao().upsert(person.toEntity())

    override suspend fun saveInstitution(institution: Institution) =
        database.institutionDao().upsert(institution.toEntity())

    override suspend fun savePaymentMethod(paymentMethod: PaymentMethod) =
        database.paymentMethodDao().upsert(paymentMethod.toEntity())

    override suspend fun deletePaymentMethod(paymentMethod: PaymentMethod) =
        database.paymentMethodDao().delete(paymentMethod.id)

    override suspend fun hasEntries(paymentMethod: PaymentMethod): Boolean =
        database.entryDao().existsForPaymentMethod(paymentMethod.id) ||
            database.recurrenceDao().existsBillFor(paymentMethod.id)
}

class RoomRecurrenceRepository(private val database: AppDatabase) : RecurrenceRepository {
    private val dao = database.recurrenceDao()

    override suspend fun activeRecurringBills(): List<RecurringBill> {
        val rows = dao.activeBills()
        if (rows.isEmpty()) return emptyList()
        val registry = database.registrySnapshot()
        return rows.map { it.toDomain(registry.paymentMethod(it.paymentMethodId)) }
    }

    override suspend fun activeRecurringIncomes(): List<RecurringIncome> = dao.activeIncomes().map { it.toDomain() }

    override suspend fun investmentsWithContribution(): List<InvestmentAccount> {
        val rows = database.paymentMethodDao().investmentsWithContribution()
        if (rows.isEmpty()) return emptyList()
        val registry = database.registrySnapshot()
        return rows.map { registry.paymentMethod(it.id) as InvestmentAccount }
    }

    override suspend fun saveRecurringBill(bill: RecurringBill) = dao.upsertBill(bill.toEntity())

    override suspend fun saveRecurringIncome(income: RecurringIncome) = dao.upsertIncome(income.toEntity())
}

class RoomReimbursementRepository(private val database: AppDatabase) : ReimbursementRepository {
    private val dao = database.reimbursementDao()

    override suspend fun save(reimbursement: Reimbursement) = dao.upsert(reimbursement.toEntity())

    override suspend fun ofThirdParty(thirdParty: Person): List<Reimbursement> {
        val rows = dao.ofThirdParty(thirdParty.id)
        if (rows.isEmpty()) return emptyList()
        val registry = database.registrySnapshot()
        return rows.map { it.toDomain(registry.person(it.thirdPartyId)) }
    }
}

/**
 * All or nothing with Room (D4): the block runs inside a write transaction on the writer connection;
 * DAO calls made inside it reuse that connection, and an exception rolls everything back.
 */
class RoomTransaction(private val database: AppDatabase) : Transaction {
    override suspend fun <T> execute(block: suspend () -> T): T =
        database.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}
