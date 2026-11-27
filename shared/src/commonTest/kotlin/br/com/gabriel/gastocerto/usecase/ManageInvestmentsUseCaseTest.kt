package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRecurrenceRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRegistryRepository
import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ManageInvestmentsUseCaseTest {

    private val database = InMemoryDatabase()
    private val registry = InMemoryRegistryRepository(database)
    private val entries = InMemoryEntryRepository(database)
    private val accountingMonths = InMemoryAccountingMonthRepository(database)
    private val recurrences = InMemoryRecurrenceRepository(database)
    private val useCase = ManageInvestmentsUseCase(registry, EntryFactory(), accountingMonths, entries)

    private val clear = InvestmentAccount("Clear", Person("Gabriel", Role.USER), Institution("Clear"))
    private val november = MonthYear(2026, 11)

    @Test
    fun `RN11 - setting a planned contribution makes the brokerage generate expected contributions`() = runTest {
        registry.savePaymentMethod(clear)

        val updated = useCase.setPlannedContribution(clear, Money(50_000))

        assertEquals(clear.id, updated.id)
        assertEquals(listOf(Money(50_000)), recurrences.investmentsWithContribution().map { it.plannedMonthlyContribution })
    }

    @Test
    fun `RN11 - removing the planned contribution stops the expected contributions`() = runTest {
        useCase.setPlannedContribution(clear, Money(50_000))
        useCase.setPlannedContribution(clear, null)
        assertTrue(recurrences.investmentsWithContribution().isEmpty())
    }

    @Test
    fun `RN11 - a zero planned contribution is rejected`() = runTest {
        assertFailsWith<InvalidAmount> { useCase.setPlannedContribution(clear, Money(0)) }
    }

    @Test
    fun `RN11 - an investment sale is income on the date received`() = runTest {
        val sale = useCase.registerSale(clear, Money(250_000), LocalDate(2026, 11, 18))

        assertEquals(IncomeType.INVESTMENT_SALE, sale.type)
        assertEquals(november, sale.accountingMonth)
        assertEquals(listOf(sale.id), entries.ofMonth(november).map { it.id })
    }

    @Test
    fun `RN11 and RN08 - a zero sale or a sale in a closed month is rejected`() = runTest {
        assertFailsWith<InvalidAmount> { useCase.registerSale(clear, Money(0), LocalDate(2026, 11, 18)) }
        accountingMonths.save(AccountingMonth(november, AccountingMonthStatus.CLOSED))
        assertFailsWith<AccountingMonthClosed> { useCase.registerSale(clear, Money(1_000), LocalDate(2026, 11, 18)) }
        assertTrue(entries.ofMonth(november).isEmpty())
    }
}
