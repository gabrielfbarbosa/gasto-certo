package br.com.gabriel.gastocerto.data.room

import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import br.com.gabriel.gastocerto.usecase.CloseMonthUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** UC03 over the real Room database: the transaction is all or nothing (D4). */
class RoomCloseMonthTest {
    private val database = inMemoryAppDatabase()
    private val r = database.repositories()

    private val gabriel = Person("Gabriel", Role.USER)
    private val interInst = Institution("Inter")
    private val inter = CheckingAccount("Inter", gabriel, interInst)
    private val november = MonthYear(2026, 11)
    private val december = MonthYear(2026, 12)

    private fun useCase(entries: EntryRepository = r.entries) =
        CloseMonthUseCase(r.transaction, r.accountingMonths, r.recurrences, EntryFactory(), entries)

    private suspend fun givenRecurrences() {
        r.registry.savePerson(gabriel)
        r.registry.saveInstitution(interInst)
        r.registry.savePaymentMethod(inter)
        r.recurrences.saveRecurringBill(RecurringBill("Power", Money(20_000), dueDay = 15, paymentMethod = inter))
        r.recurrences.saveRecurringIncome(RecurringIncome("Salary", Money(800_000), payDay = 5))
    }

    @AfterTest
    fun close() = database.close()

    @Test
    fun `UC03 - closing with Room stores the closed month and the next month expected entries`() = runTest {
        givenRecurrences()

        useCase().execute(november)

        assertEquals(AccountingMonthStatus.CLOSED, r.accountingMonths.findOrCreate(november).status)
        assertEquals(AccountingMonthStatus.OPEN, r.accountingMonths.findOrCreate(december).status)
        assertEquals(setOf("Power", "Salary"), r.entries.ofMonth(december).map { it.description }.toSet())
    }

    @Test
    fun `UC03 - a failure midway rolls the Room transaction back`() = runTest {
        givenRecurrences()
        val failingEntries = object : EntryRepository by r.entries {
            override suspend fun saveAll(entries: List<Entry>) {
                r.entries.save(entries.first()) // a write that must be undone too
                error("disk full")
            }
        }

        assertFailsWith<IllegalStateException> { useCase(failingEntries).execute(november) }

        assertEquals(AccountingMonthStatus.OPEN, r.accountingMonths.findOrCreate(november).status)
        assertTrue(r.entries.ofMonth(december).isEmpty())
    }
}
