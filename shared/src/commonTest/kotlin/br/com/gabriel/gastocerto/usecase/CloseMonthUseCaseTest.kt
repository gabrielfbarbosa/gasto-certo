package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRecurrenceRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRegistryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryTransaction
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CloseMonthUseCaseTest {

    private val database = InMemoryDatabase()
    private val accountingMonths = InMemoryAccountingMonthRepository(database)
    private val entries = InMemoryEntryRepository(database)
    private val recurrences = InMemoryRecurrenceRepository(database)
    private val registry = InMemoryRegistryRepository(database)

    private fun useCase(entryRepository: EntryRepository = entries) = CloseMonthUseCase(
        InMemoryTransaction(database), accountingMonths, recurrences, EntryFactory(), entryRepository,
    )

    private val gabriel = Person("Gabriel", Role.USER)
    private val inter = CheckingAccount("Inter", gabriel, Institution("Inter"))
    private val power = RecurringBill("Power", Money(20_000), dueDay = 15, paymentMethod = inter)
    private val salary = RecurringIncome("Salary", Money(800_000), payDay = 5)
    private val clear = InvestmentAccount("Clear", gabriel, Institution("Clear"), plannedMonthlyContribution = Money(50_000))
    private val november = MonthYear(2026, 11)
    private val december = MonthYear(2026, 12)

    private suspend fun givenRecurrences() {
        recurrences.saveRecurringBill(power)
        recurrences.saveRecurringIncome(salary)
        registry.savePaymentMethod(clear)
    }

    @Test
    fun `UC03 - closing the month closes it and opens the next one with the expected entries`() = runTest {
        givenRecurrences()

        val following = useCase().execute(november)

        assertEquals(december, following.monthYear)
        assertEquals(AccountingMonthStatus.OPEN, following.status)
        assertEquals(AccountingMonthStatus.CLOSED, accountingMonths.findOrCreate(november).status)
        val expected = entries.ofMonth(december)
        assertEquals(setOf(power.id, salary.id, clear.id), expected.map { it.originId }.toSet())
        assertTrue(expected.all { it.status == EntryStatus.EXPECTED })
    }

    @Test
    fun `RN08 - closing a month that is already closed is rejected and nothing is generated`() = runTest {
        givenRecurrences()
        useCase().execute(november)
        entries.ofMonth(december).forEach { entries.delete(it) }

        assertFailsWith<AccountingMonthClosed> { useCase().execute(november) }
        assertTrue(entries.ofMonth(december).isEmpty())
    }

    @Test
    fun `A3 - closing twice after reopening does not duplicate the expected entries`() = runTest {
        givenRecurrences()
        useCase().execute(november)
        ReopenMonthUseCase(accountingMonths).execute(november)

        useCase().execute(november)

        assertEquals(3, entries.ofMonth(december).size)
    }

    @Test
    fun `A3 - an expected entry already confirmed is not generated again`() = runTest {
        givenRecurrences()
        useCase().execute(november)
        val confirmedPower = entries.ofMonth(december).first { it.originId == power.id }
        confirmedPower.confirm(Money(18_000))
        entries.save(confirmedPower)
        ReopenMonthUseCase(accountingMonths).execute(november)

        useCase().execute(november)

        val powerEntries = entries.ofMonth(december).filter { it.originId == power.id }
        assertEquals(listOf(Money(18_000)), powerEntries.map { it.amount })
    }

    @Test
    fun `UC03 - a failure midway undoes everything - the month stays open and nothing is saved`() = runTest {
        givenRecurrences()
        val failingEntries = object : EntryRepository by entries {
            override suspend fun saveAll(entries: List<Entry>) = error("disk full")
        }

        assertFailsWith<IllegalStateException> { useCase(failingEntries).execute(november) }

        assertEquals(AccountingMonthStatus.OPEN, accountingMonths.findOrCreate(november).status)
        assertTrue(entries.ofMonth(december).isEmpty())
    }

    @Test
    fun `UC03 - with no recurrences the next month opens empty`() = runTest {
        val following = useCase().execute(november)
        assertEquals(december, following.monthYear)
        assertTrue(entries.ofMonth(december).isEmpty())
    }
}
