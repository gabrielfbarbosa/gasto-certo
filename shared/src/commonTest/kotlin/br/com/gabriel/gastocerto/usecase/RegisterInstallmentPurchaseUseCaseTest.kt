package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import br.com.gabriel.gastocerto.domain.rule.InstallmentsOutOfRange
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.PurchaseInput
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegisterInstallmentPurchaseUseCaseTest {

    private val database = InMemoryDatabase()
    private val accountingMonths = InMemoryAccountingMonthRepository(database)
    private val entries = InMemoryEntryRepository(database)
    private val useCase = RegisterInstallmentPurchaseUseCase(EntryFactory(), accountingMonths, entries)

    private val gabriel = Person("Gabriel", Role.USER)
    private val father = Person("Father", Role.THIRD_PARTY)
    // closes on day 5, due on day 12 (same month)
    private val nubank = CreditCard("Nubank", gabriel, Institution("Nubank"), closingDay = 5, dueDay = 12)
    private val november = MonthYear(2026, 11)
    private val december = MonthYear(2026, 12)
    private val january = MonthYear(2027, 1)

    private fun input(total: Long = 10_000, count: Int = 3, date: LocalDate = LocalDate(2026, 11, 3), thirdParty: Person? = null) =
        PurchaseInput(nubank, Money(total), count, date, "Fridge", thirdParty)

    private suspend fun savedInstallments() =
        listOf(november, december, january).flatMap { entries.ofMonth(it) }.filterIsInstance<Installment>()

    // ---------- Part 1: preview ----------

    @Test
    fun `UC02 - preview shows k of N amount and statement without saving`() = runTest {
        val preview = useCase.preview(input())

        assertEquals(listOf("1/3", "2/3", "3/3"), preview.map { it.label() })
        assertEquals(listOf(Money(3_334), Money(3_333), Money(3_333)), preview.map { it.amount })
        assertEquals(listOf(november, december, january), preview.map { it.accountingMonth })
        assertTrue(savedInstallments().isEmpty())
    }

    @Test
    fun `UC02 and RN02 - preview with 1 or 49 installments is rejected`() = runTest {
        assertFailsWith<InstallmentsOutOfRange> { useCase.preview(input(count = 1)) }
        assertFailsWith<InstallmentsOutOfRange> { useCase.preview(input(count = 49)) }
    }

    @Test
    fun `UC02 and RN09 - preview with zero amount is rejected`() = runTest {
        assertFailsWith<InvalidAmount> { useCase.preview(input(total = 0)) }
    }

    // ---------- Part 2: confirmation ----------

    @Test
    fun `UC02 - confirming saves every installment with the purchase id`() = runTest {
        val purchase = useCase.execute(input(thirdParty = father))

        val saved = savedInstallments()
        assertEquals(listOf("1/3", "2/3", "3/3"), saved.map { it.label() })
        assertTrue(saved.all { it.purchaseId == purchase.id && it.thirdParty == father })
        assertEquals(Money(10_000), saved.map { it.amount }.reduce(Money::plus))
    }

    @Test
    fun `RN08 - confirming when the 1st installment month is closed is rejected and nothing is saved`() = runTest {
        accountingMonths.save(AccountingMonth(november, AccountingMonthStatus.CLOSED))

        val error = assertFailsWith<AccountingMonthClosed> { useCase.execute(input()) }

        assertEquals(november, error.month)
        assertTrue(savedInstallments().isEmpty())
    }

    @Test
    fun `RN01 and RN08 - a purchase after closing starts in the next statement and ignores the closed month`() = runTest {
        accountingMonths.save(AccountingMonth(november, AccountingMonthStatus.CLOSED))

        useCase.execute(input(count = 2, date = LocalDate(2026, 11, 6)))

        assertEquals(listOf(december, january), savedInstallments().map { it.accountingMonth })
    }

    @Test
    fun `UC02 and RN02 - confirming an invalid purchase saves nothing`() = runTest {
        assertFailsWith<InstallmentsOutOfRange> { useCase.execute(input(count = 49)) }
        assertTrue(savedInstallments().isEmpty())
    }
}
