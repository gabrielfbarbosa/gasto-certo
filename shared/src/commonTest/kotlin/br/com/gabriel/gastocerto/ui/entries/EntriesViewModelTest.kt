package br.com.gabriel.gastocerto.ui.entries

import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.ui.TestApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EntriesViewModelTest {
    private lateinit var app: TestApp
    private lateinit var vm: EntriesViewModel
    private val november = MonthYear(2026, 11)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun start() {
        app = TestApp().seed()
        vm = EntriesViewModel(app.oneOffIncome, app.correctEntry) { app.today }
    }

    @Test
    fun `UC09 - lists the entries of the current month`() = runTest {
        start()
        assertEquals(app.repositories.entries.ofMonth(november), vm.uiState.value.entries)
    }

    @Test
    fun `UC08 - a one-off income is saved as actual in the month of its date`() = runTest {
        start()
        vm.updateIncomeForm { copy(amount = 150_000, description = "Freelance") }
        vm.registerIncome()

        assertEquals(EntriesEvent.IncomeSaved(november), vm.events.first())
        val income = assertIs<Income>(vm.uiState.value.entries.single { it.description == "Freelance" })
        assertEquals(IncomeType.ONE_OFF, income.type)
        assertEquals(EntryStatus.ACTUAL, income.status)
    }

    @Test
    fun `RN09 - a one-off income without amount is rejected`() = runTest {
        start()
        vm.updateIncomeForm { copy(amount = null, description = "Nothing") }
        vm.registerIncome()
        assertIs<InvalidAmount>(vm.uiState.value.error)
    }

    @Test
    fun `UC09 - editing changes amount and description and keeps the id`() = runTest {
        start()
        val groceries = vm.uiState.value.entries.first { it.description == "Groceries" }

        vm.edit(groceries, 36_000, "Supermarket")

        assertEquals(EntriesEvent.Corrected("Supermarket"), vm.events.first())
        val stored = app.repositories.entries.findById(groceries.id)!!
        assertEquals("Supermarket", stored.description)
        assertEquals(Money(36_000), stored.amount)
        // Regression: same id, new amount – the screen state must show the new value.
        assertEquals(Money(36_000), vm.uiState.value.entries.single { it.id == groceries.id }.amount)
    }

    @Test
    fun `RN05 - changing the amount of a dollar expense drops its conversion`() = runTest {
        start()
        val app2 = vm.uiState.value.entries.filterIsInstance<Expense>().first { it.conversion != null }

        vm.edit(app2, 7_000, "")

        vm.events.first()
        val stored = assertIs<Expense>(app.repositories.entries.findById(app2.id))
        assertNull(stored.conversion)
        assertEquals(app2.description, stored.description)
    }

    @Test
    fun `UC09 - checking an expected entry and deleting another`() = runTest {
        start()
        val salary = vm.uiState.value.entries.first { it.description == "Salary" }
        vm.confirmExpected(salary, 820_000)
        vm.events.first()
        assertEquals(EntryStatus.ACTUAL, app.repositories.entries.findById(salary.id)!!.status)

        val restaurant = vm.uiState.value.entries.first { it.description == "Restaurant" }
        vm.delete(restaurant)
        assertEquals(EntriesEvent.Deleted("Restaurant"), vm.events.first())
        assertTrue(vm.uiState.value.entries.none { it.id == restaurant.id })
    }

    @Test
    fun `RN08 - entries of a closed month cannot be corrected`() = runTest {
        start()
        app.repositories.accountingMonths.save(AccountingMonth(november, AccountingMonthStatus.CLOSED))
        val groceries = vm.uiState.value.entries.first { it.description == "Groceries" }

        vm.delete(groceries)

        assertIs<AccountingMonthClosed>(vm.uiState.value.error)
        assertTrue(vm.uiState.value.entries.any { it.id == groceries.id })
    }
}
