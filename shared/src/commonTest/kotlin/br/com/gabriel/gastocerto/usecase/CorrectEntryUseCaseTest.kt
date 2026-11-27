package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CorrectEntryUseCaseTest {

    private val database = InMemoryDatabase()
    private val entries = InMemoryEntryRepository(database)
    private val accountingMonths = InMemoryAccountingMonthRepository(database)
    private val useCase = CorrectEntryUseCase(entries, accountingMonths)

    private val inter = CheckingAccount("Inter", Person("Gabriel", Role.USER), Institution("Inter"))
    private val october = MonthYear(2026, 10)
    private val november = MonthYear(2026, 11)

    private fun expense(amount: Long, month: MonthYear = november, day: Int = 10, status: EntryStatus = EntryStatus.ACTUAL, id: String = "e1") =
        Expense(Money(amount), LocalDate(month.year, month.month, day), "Groceries", month, inter, status = status, id = id)

    private suspend fun close(month: MonthYear) = accountingMonths.save(AccountingMonth(month, AccountingMonthStatus.CLOSED))

    @Test
    fun `RF12 - editing an entry in an open month replaces it`() = runTest {
        entries.save(expense(4_590))

        useCase.edit(expense(4_950))

        assertEquals(Money(4_950), entries.findById("e1")?.amount)
    }

    @Test
    fun `RN08 - editing an entry of a closed month is rejected`() = runTest {
        entries.save(expense(4_590))
        close(november)

        assertFailsWith<AccountingMonthClosed> { useCase.edit(expense(4_950)) }
        assertEquals(Money(4_590), entries.findById("e1")?.amount)
    }

    @Test
    fun `RN08 - moving an entry out of a closed month is rejected`() = runTest {
        entries.save(expense(4_590, month = october))
        close(october)

        assertFailsWith<AccountingMonthClosed> { useCase.edit(expense(4_590, month = november)) }
        assertEquals(october, entries.findById("e1")?.accountingMonth)
    }

    @Test
    fun `RF12 - deleting an entry in an open month removes it`() = runTest {
        entries.save(expense(4_590))
        useCase.delete(expense(4_590))
        assertTrue(entries.ofMonth(november).isEmpty())
    }

    @Test
    fun `RN08 - deleting an entry of a closed month is rejected`() = runTest {
        entries.save(expense(4_590))
        close(november)

        assertFailsWith<AccountingMonthClosed> { useCase.delete(expense(4_590)) }
        assertEquals(1, entries.ofMonth(november).size)
    }

    @Test
    fun `RN06 - confirming an expected entry saves the actual amount`() = runTest {
        entries.save(expense(20_000, status = EntryStatus.EXPECTED))

        useCase.confirmExpected(entries.findById("e1")!!, Money(18_437))

        val stored = entries.findById("e1")
        assertEquals(Money(18_437), stored?.amount)
        assertEquals(EntryStatus.ACTUAL, stored?.status)
    }

    @Test
    fun `RN09 and RN08 - confirming with zero or in a closed month is rejected`() = runTest {
        entries.save(expense(20_000, status = EntryStatus.EXPECTED))
        assertFailsWith<InvalidAmount> { useCase.confirmExpected(entries.findById("e1")!!, Money(0)) }

        close(november)
        assertFailsWith<AccountingMonthClosed> { useCase.confirmExpected(entries.findById("e1")!!, Money(18_000)) }
        assertEquals(EntryStatus.EXPECTED, entries.findById("e1")?.status)
    }
}
