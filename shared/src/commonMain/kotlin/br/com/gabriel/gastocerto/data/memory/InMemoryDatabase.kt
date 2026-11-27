package br.com.gabriel.gastocerto.data.memory

import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.model.Reimbursement

/**
 * State shared by the in-memory repositories (one "database" per instance).
 *
 * Stores **copies** of the objects (like a real database): what goes in is copied on save and what
 * comes out is copied on read, so changing a read object does not change the database without `save`.
 * That is why a shallow [snapshot] of the tables is enough for [InMemoryTransaction] to undo writes.
 * Not thread-safe: Web and tests run on a single thread.
 */
class InMemoryDatabase {
    internal var people = mapOf<String, Person>()
    internal var institutions = mapOf<String, Institution>()
    internal var paymentMethods = mapOf<String, PaymentMethod>()
    internal var entries = mapOf<String, Entry>()
    internal var accountingMonths = mapOf<MonthYear, AccountingMonthStatus>()
    internal var recurringBills = mapOf<String, RecurringBill>()
    internal var recurringIncomes = mapOf<String, RecurringIncome>()
    internal var reimbursements = mapOf<String, Reimbursement>()

    internal class Snapshot(
        val people: Map<String, Person>,
        val institutions: Map<String, Institution>,
        val paymentMethods: Map<String, PaymentMethod>,
        val entries: Map<String, Entry>,
        val accountingMonths: Map<MonthYear, AccountingMonthStatus>,
        val recurringBills: Map<String, RecurringBill>,
        val recurringIncomes: Map<String, RecurringIncome>,
        val reimbursements: Map<String, Reimbursement>,
    )

    internal fun snapshot() = Snapshot(
        people, institutions, paymentMethods, entries, accountingMonths, recurringBills, recurringIncomes, reimbursements,
    )

    internal fun restore(snapshot: Snapshot) {
        people = snapshot.people
        institutions = snapshot.institutions
        paymentMethods = snapshot.paymentMethods
        entries = snapshot.entries
        accountingMonths = snapshot.accountingMonths
        recurringBills = snapshot.recurringBills
        recurringIncomes = snapshot.recurringIncomes
        reimbursements = snapshot.reimbursements
    }
}
