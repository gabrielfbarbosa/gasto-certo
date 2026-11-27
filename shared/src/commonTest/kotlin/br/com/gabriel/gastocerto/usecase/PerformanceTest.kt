package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.repository.Repositories
import br.com.gabriel.gastocerto.ui.TestApp
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** 1,000 entries spread over a year (2026): the RNF05 load. Registry rows are saved first (Room foreign keys). */
suspend fun Repositories.givenThousandEntries(): Int {
    val user = Person("Gabriel", Role.USER)
    val father = Person("Father", Role.THIRD_PARTY)
    val bank = Institution("Bank")
    val card = CreditCard("Card", user, bank, closingDay = 5, dueDay = 12)
    val account = CheckingAccount("Account", user, bank)
    registry.savePerson(user)
    registry.savePerson(father)
    registry.saveInstitution(bank)
    registry.savePaymentMethod(card)
    registry.savePaymentMethod(account)
    val entries = List<Entry>(1_000) { i ->
        val date = LocalDate(2026, i % 12 + 1, i % 28 + 1)
        val month = MonthYear(2026, i % 12 + 1)
        when (i % 10) {
            0 -> Income(Money(100_000L + i), date, "Income $i", month, IncomeType.ONE_OFF)
            1 -> Expense(Money(1_000L + i), date, "Father $i", month, account, thirdParty = father)
            else -> Expense(Money(1_000L + i), date, "Expense $i", month, if (i % 2 == 0) card else account)
        }
    }
    this.entries.saveAll(entries)
    return entries.size
}

/** RNF05 – with 1,000 entries, the summary of a month, a week and the year is ready in at most 1 second. */
class PerformanceTest {

    @Test
    fun `RNF05 - the summary over 1000 entries takes at most 1 second`() = runTest {
        val app = TestApp()
        assertEquals(1_000, app.repositories.givenThousandEntries())

        val mark = TimeSource.Monotonic.markNow()
        val month = app.summary.ofMonth(MonthYear(2026, 6))
        app.summary.ofWeek(LocalDate(2026, 6, 10))
        val year = app.summary.ofYear(2026)
        val elapsed = mark.elapsedNow()

        assertTrue(elapsed <= 1.seconds, "summary took $elapsed")
        assertTrue(month.totalExpenses.isPositive())
        assertEquals(100, (0 until 1_000).count { it % 10 == 0 }) // incomes in the load
        assertTrue(year.income.isPositive())
    }
}
