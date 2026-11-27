package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import br.com.gabriel.gastocerto.domain.rule.ExpenseInput
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.InvalidExchangeRate
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegisterExpenseUseCaseTest {

    private val database = InMemoryDatabase()
    private val accountingMonths = InMemoryAccountingMonthRepository(database)
    private val entries = InMemoryEntryRepository(database)
    private val useCase = RegisterExpenseUseCase(EntryFactory(), accountingMonths, entries)

    private val gabriel = Person("Gabriel", Role.USER)
    private val nubank = CreditCard("Nubank", gabriel, Institution("Nubank"), closingDay = 5, dueDay = 12)
    private val inter = CheckingAccount("Inter", gabriel, Institution("Inter"))
    private val wise = ForeignCurrencyWallet("Wise", gabriel, Institution("Wise"), currency = "USD")
    private val november = MonthYear(2026, 11)
    private val december = MonthYear(2026, 12)

    @Test
    fun `UC01 - a valid expense is saved in the accounting month of its payment method`() = runTest {
        val expense = useCase.execute(ExpenseInput(inter, Money(4_590), LocalDate(2026, 11, 10), "Groceries"))

        assertEquals(november, expense.accountingMonth)
        assertEquals(listOf(expense.id), entries.ofMonth(november).map { it.id })
    }

    @Test
    fun `UC01 and RN01 - a card expense after closing is saved in the next statement month`() = runTest {
        val expense = useCase.execute(ExpenseInput(nubank, Money(15_000), LocalDate(2026, 11, 6), "Pharmacy"))

        assertEquals(december, expense.accountingMonth)
        assertTrue(entries.ofMonth(november).isEmpty())
        assertEquals(1, entries.ofMonth(december).size)
    }

    @Test
    fun `UC01 and RN05 - a foreign currency expense is saved in reais`() = runTest {
        val expense = useCase.execute(
            ExpenseInput(wise, amount = null, LocalDate(2026, 11, 10), "App", foreignAmount = 1_250, exchangeRate = 54_321),
        )
        assertEquals(Money(6_790), entries.ofMonth(november).single().amount)
        assertEquals(expense.id, entries.ofMonth(november).single().id)
    }

    @Test
    fun `RN08 - an expense in a closed month is rejected and nothing is saved`() = runTest {
        accountingMonths.save(AccountingMonth(november, AccountingMonthStatus.CLOSED))

        val error = assertFailsWith<AccountingMonthClosed> {
            useCase.execute(ExpenseInput(inter, Money(4_590), LocalDate(2026, 11, 10), "Groceries"))
        }

        assertEquals(november, error.month)
        assertTrue(entries.ofMonth(november).isEmpty())
    }

    @Test
    fun `RN08 - a closed month does not block a card expense that falls in the next statement`() = runTest {
        accountingMonths.save(AccountingMonth(november, AccountingMonthStatus.CLOSED))

        useCase.execute(ExpenseInput(nubank, Money(15_000), LocalDate(2026, 11, 6), "Pharmacy"))

        assertEquals(1, entries.ofMonth(december).size)
    }

    @Test
    fun `RN09 and RN05 - invalid amount or exchange rate is rejected and nothing is saved`() = runTest {
        assertFailsWith<InvalidAmount> { useCase.execute(ExpenseInput(inter, Money(0), LocalDate(2026, 11, 10), "x")) }
        assertFailsWith<InvalidExchangeRate> {
            useCase.execute(ExpenseInput(wise, null, LocalDate(2026, 11, 10), "x", foreignAmount = 1_250, exchangeRate = 0))
        }
        assertTrue(entries.ofMonth(november).isEmpty())
    }
}
