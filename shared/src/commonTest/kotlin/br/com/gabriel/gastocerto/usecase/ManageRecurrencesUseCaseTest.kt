package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRecurrenceRepository
import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import br.com.gabriel.gastocerto.domain.rule.InvalidDay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ManageRecurrencesUseCaseTest {

    private val database = InMemoryDatabase()
    private val recurrences = InMemoryRecurrenceRepository(database)
    private val entries = InMemoryEntryRepository(database)
    private val accountingMonths = InMemoryAccountingMonthRepository(database)
    private val today = LocalDate(2026, 11, 10)
    private val useCase = ManageRecurrencesUseCase(recurrences, EntryFactory(), entries, accountingMonths, today = { today })

    private val inter = CheckingAccount("Inter", Person("Gabriel", Role.USER), Institution("Inter"))
    private val november = MonthYear(2026, 11)

    @Test
    fun `RN06 - registering a bill due later this month also generates this month's expected expense`() = runTest {
        val power = RecurringBill("Power", Money(20_000), dueDay = 15, paymentMethod = inter)

        useCase.saveRecurringBill(power)

        assertEquals(listOf(power), recurrences.activeRecurringBills())
        val expected = entries.ofMonth(november).single()
        assertEquals(power.id, expected.originId)
        assertEquals(LocalDate(2026, 11, 15), expected.date)
        assertEquals(EntryStatus.EXPECTED, expected.status)
    }

    @Test
    fun `RN06 - a bill whose due day already passed this month generates nothing now`() = runTest {
        useCase.saveRecurringBill(RecurringBill("Water", Money(9_000), dueDay = 5, paymentMethod = inter))
        assertTrue(entries.ofMonth(november).isEmpty())
    }

    @Test
    fun `RN06 - a bill due today is still generated`() = runTest {
        useCase.saveRecurringBill(RecurringBill("Internet", Money(10_000), dueDay = 10, paymentMethod = inter))
        assertEquals(1, entries.ofMonth(november).size)
    }

    @Test
    fun `RN06 and D3 - saving the same bill again does not duplicate the expected expense`() = runTest {
        val power = RecurringBill("Power", Money(20_000), 15, inter)
        useCase.saveRecurringBill(power)
        useCase.saveRecurringBill(power)
        assertEquals(1, entries.ofMonth(november).size)
    }

    @Test
    fun `RN06 and RN08 - nothing is generated when the current month is closed`() = runTest {
        accountingMonths.save(AccountingMonth(november, AccountingMonthStatus.CLOSED))
        useCase.saveRecurringBill(RecurringBill("Power", Money(20_000), 15, inter))
        assertTrue(entries.ofMonth(november).isEmpty())
    }

    @Test
    fun `RN06 - an inactive bill is saved but generates nothing`() = runTest {
        useCase.saveRecurringBill(RecurringBill("Gym", Money(9_900), 20, inter, active = false))
        assertTrue(entries.ofMonth(november).isEmpty())
    }

    @Test
    fun `RN07 - salary is saved and its expected income waits for the next month`() = runTest {
        val salary = RecurringIncome("Salary", Money(800_000), payDay = 25)
        useCase.saveRecurringIncome(salary)
        assertEquals(listOf(salary), recurrences.activeRecurringIncomes())
        assertTrue(entries.ofMonth(november).isEmpty())
    }

    @Test
    fun `RN09 - a recurring bill with an invalid due day is rejected before saving`() = runTest {
        assertFailsWith<InvalidDay> { useCase.saveRecurringBill(RecurringBill("Power", Money(20_000), 40, inter)) }
        assertTrue(recurrences.activeRecurringBills().isEmpty())
    }
}
