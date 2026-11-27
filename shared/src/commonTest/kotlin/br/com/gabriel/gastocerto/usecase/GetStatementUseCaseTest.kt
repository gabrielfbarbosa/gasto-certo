package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import br.com.gabriel.gastocerto.domain.rule.ExpenseInput
import br.com.gabriel.gastocerto.domain.rule.PurchaseInput
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GetStatementUseCaseTest {

    private val database = InMemoryDatabase()
    private val entries = InMemoryEntryRepository(database)
    private val accountingMonths = InMemoryAccountingMonthRepository(database)
    private val registerExpense = RegisterExpenseUseCase(EntryFactory(), accountingMonths, entries)
    private val registerPurchase = RegisterInstallmentPurchaseUseCase(EntryFactory(), accountingMonths, entries)
    private val useCase = GetStatementUseCase(entries)

    private val gabriel = Person("Gabriel", Role.USER)
    private val nubank = CreditCard("Nubank", gabriel, Institution("Nubank"), closingDay = 5, dueDay = 12)
    private val c6 = CreditCard("C6", gabriel, Institution("C6"), closingDay = 5, dueDay = 12)
    private val december = MonthYear(2026, 12)

    @Test
    fun `RF13 - the statement brings the card's expenses and installments of the due month`() = runTest {
        registerExpense.execute(ExpenseInput(nubank, Money(15_000), LocalDate(2026, 11, 6), "Pharmacy")) // → Dec
        registerExpense.execute(ExpenseInput(c6, Money(9_000), LocalDate(2026, 11, 6), "Other card")) // → Dec, other card
        registerPurchase.execute(PurchaseInput(nubank, Money(10_000), 3, LocalDate(2026, 11, 3), "TV")) // 1/3 Nov, 2/3 Dec

        val statement = useCase.execute(nubank, december)

        assertEquals(LocalDate(2026, 12, 5), statement.closingDate)
        assertEquals(LocalDate(2026, 12, 12), statement.dueDate)
        assertEquals(listOf("2/3"), statement.expenses.filterIsInstance<Installment>().map { it.label() })
        assertEquals(Money(15_000 + 3_333), statement.total())
    }

    @Test
    fun `RF13 - a card with no expenses in the month has a zero statement`() = runTest {
        assertEquals(Money(0), useCase.execute(nubank, december).total())
    }
}
