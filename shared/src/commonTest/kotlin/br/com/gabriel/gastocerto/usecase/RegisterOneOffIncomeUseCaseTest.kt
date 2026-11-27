package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegisterOneOffIncomeUseCaseTest {

    private val database = InMemoryDatabase()
    private val accountingMonths = InMemoryAccountingMonthRepository(database)
    private val entries = InMemoryEntryRepository(database)
    private val useCase = RegisterOneOffIncomeUseCase(EntryFactory(), accountingMonths, entries)
    private val november = MonthYear(2026, 11)

    @Test
    fun `RN07 - one-off income is saved in the month of the date received`() = runTest {
        val income = useCase.execute(Money(120_000), LocalDate(2026, 11, 28), "Freelance")

        assertEquals(IncomeType.ONE_OFF, income.type)
        assertEquals(listOf(income.id), entries.ofMonth(november).map { it.id })
    }

    @Test
    fun `RN09 - one-off income with zero amount is rejected`() = runTest {
        assertFailsWith<InvalidAmount> { useCase.execute(Money(0), LocalDate(2026, 11, 28), "Freelance") }
        assertTrue(entries.ofMonth(november).isEmpty())
    }

    @Test
    fun `RN08 - one-off income in a closed month is rejected`() = runTest {
        accountingMonths.save(AccountingMonth(november, AccountingMonthStatus.CLOSED))
        assertFailsWith<AccountingMonthClosed> { useCase.execute(Money(120_000), LocalDate(2026, 11, 28), "Freelance") }
        assertTrue(entries.ofMonth(november).isEmpty())
    }
}
