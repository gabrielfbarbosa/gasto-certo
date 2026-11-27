package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GetSummaryUseCaseTest {

    private val entries = InMemoryEntryRepository(InMemoryDatabase())
    private val useCase = GetSummaryUseCase(entries)

    private val gabriel = Person("Gabriel", Role.USER)
    private val spouse = Person("Spouse", Role.HOLDER)
    private val father = Person("Father", Role.THIRD_PARTY)
    private val inter = CheckingAccount("Inter", gabriel, Institution("Inter"))
    private val spousesCard = CreditCard("Nubank Spouse", spouse, Institution("Nubank"), 5, 12)
    private val november = MonthYear(2026, 11)
    private val december = MonthYear(2026, 12)

    private fun expense(amount: Long, date: LocalDate, month: MonthYear, method: PaymentMethod = inter, thirdParty: Person? = null) =
        Expense(Money(amount), date, "expense", month, method, thirdParty)

    private fun income(amount: Long, date: LocalDate, month: MonthYear) =
        Income(Money(amount), date, "income", month, IncomeType.ONE_OFF)

    @Test
    fun `RF14 and RN04 - month summary uses the accounting month and deducts third-party expenses`() = runTest {
        entries.saveAll(
            listOf(
                income(800_000, LocalDate(2026, 11, 5), november),
                expense(100_000, LocalDate(2026, 11, 10), november),
                expense(30_000, LocalDate(2026, 11, 12), november, thirdParty = father),
                // card purchase dated November but in the December statement (RN01)
                expense(50_000, LocalDate(2026, 11, 20), december, spousesCard),
            ),
        )

        val summary = useCase.ofMonth(november)

        assertEquals(Money(130_000), summary.totalExpenses)
        assertEquals(Money(30_000), summary.thirdPartyExpenses)
        assertEquals(Money(700_000), summary.surplus())
    }

    @Test
    fun `RN10 - month summary filtered by holder`() = runTest {
        entries.saveAll(
            listOf(expense(100_000, LocalDate(2026, 12, 1), december), expense(50_000, LocalDate(2026, 11, 20), december, spousesCard)),
        )

        assertEquals(Money(50_000), useCase.ofMonth(december, holder = spouse).totalExpenses)
        assertEquals(Money(150_000), useCase.ofMonth(december).totalExpenses)
    }

    @Test
    fun `RF15 - week summary goes from Monday to Sunday by entry date`() = runTest {
        entries.saveAll(
            listOf(
                expense(1, LocalDate(2026, 11, 22), november), // Sunday before
                expense(10, LocalDate(2026, 11, 23), november), // Monday
                expense(100, LocalDate(2026, 11, 29), november), // Sunday
                expense(1_000, LocalDate(2026, 11, 30), november), // next Monday
                income(5_000, LocalDate(2026, 11, 26), november), // Thursday
            ),
        )

        val week = useCase.ofWeek(LocalDate(2026, 11, 26))

        assertEquals(Money(110), week.totalExpenses)
        assertEquals(Money(5_000), week.income)
    }

    @Test
    fun `RF15 - a Monday and a Sunday belong to the same week`() = runTest {
        entries.save(expense(10, LocalDate(2026, 11, 23), november))

        assertEquals(Money(10), useCase.ofWeek(LocalDate(2026, 11, 23)).totalExpenses)
        assertEquals(Money(10), useCase.ofWeek(LocalDate(2026, 11, 29)).totalExpenses)
        assertEquals(Money(0), useCase.ofWeek(LocalDate(2026, 11, 30)).totalExpenses)
    }

    @Test
    fun `RF15 - year summary goes from January 1 to December 31 by entry date`() = runTest {
        entries.saveAll(
            listOf(
                expense(10, LocalDate(2026, 1, 1), MonthYear(2026, 1)),
                expense(100, LocalDate(2026, 12, 31), MonthYear(2027, 1)),
                expense(1_000, LocalDate(2027, 1, 1), MonthYear(2027, 1)),
            ),
        )

        assertEquals(Money(110), useCase.ofYear(2026).totalExpenses)
    }

    @Test
    fun `RF14 - a period with no entries has zero totals`() = runTest {
        assertEquals(Money(0), useCase.ofMonth(november).surplus())
        assertEquals(Money(0), useCase.ofYear(2030).totalExpenses)
    }
}
