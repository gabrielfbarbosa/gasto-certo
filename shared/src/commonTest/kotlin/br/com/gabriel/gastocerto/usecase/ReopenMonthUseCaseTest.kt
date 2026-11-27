package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRecurrenceRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryTransaction
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import br.com.gabriel.gastocerto.domain.rule.ExpenseInput
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReopenMonthUseCaseTest {

    private val database = InMemoryDatabase()
    private val accountingMonths = InMemoryAccountingMonthRepository(database)
    private val entries = InMemoryEntryRepository(database)
    private val recurrences = InMemoryRecurrenceRepository(database)
    private val closeMonth = CloseMonthUseCase(InMemoryTransaction(database), accountingMonths, recurrences, EntryFactory(), entries)
    private val reopenMonth = ReopenMonthUseCase(accountingMonths)
    private val registerExpense = RegisterExpenseUseCase(EntryFactory(), accountingMonths, entries)

    private val inter = CheckingAccount("Inter", Person("Gabriel", Role.USER), Institution("Inter"))
    private val november = MonthYear(2026, 11)
    private val groceries = ExpenseInput(inter, Money(4_590), LocalDate(2026, 11, 10), "Groceries")

    @Test
    fun `RN08 - reopening a closed month accepts entries again`() = runTest {
        closeMonth.execute(november)
        assertFailsWith<AccountingMonthClosed> { registerExpense.execute(groceries) }

        val reopened = reopenMonth.execute(november)

        assertEquals(AccountingMonthStatus.OPEN, reopened.status)
        registerExpense.execute(groceries)
        assertEquals(1, entries.ofMonth(november).size)
    }

    @Test
    fun `RN08 - reopening keeps the expected entries already generated for the next month`() = runTest {
        recurrences.saveRecurringBill(RecurringBill("Power", Money(20_000), 15, inter))
        closeMonth.execute(november)

        reopenMonth.execute(november)

        assertEquals(1, entries.ofMonth(november.next()).size)
    }

    @Test
    fun `RN08 - reopening a month that is already open changes nothing`() = runTest {
        assertEquals(AccountingMonthStatus.OPEN, reopenMonth.execute(november).status)
    }
}
