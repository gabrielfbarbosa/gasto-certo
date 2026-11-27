package br.com.gabriel.gastocerto.ui.recurrences

import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.rule.DayKind
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.InvalidDay
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecurrencesViewModelTest {
    private lateinit var app: TestApp
    private lateinit var vm: RecurrencesViewModel

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun start() {
        app = TestApp().seed()
        vm = RecurrencesViewModel(app.recurrences, app.registry)
    }

    @Test
    fun `UC07 - lists the active bills and incomes`() = runTest {
        start()
        assertEquals(listOf("Power", "Water", "Internet"), vm.uiState.value.bills.map { it.description })
        assertEquals(listOf("Salary"), vm.uiState.value.incomes.map { it.description })
    }

    @Test
    fun `RN06 - a new bill due later this month generates its expected expense now`() = runTest {
        start()
        vm.updateBillForm { copy(description = "Gym", amount = 9_990, day = 30) }
        vm.addBill()

        assertEquals(RecurrencesEvent.Saved("Gym"), vm.events.first())
        val gym = app.repositories.entries.ofMonth(MonthYear(2026, 11)).single { it.description == "Gym" }
        assertEquals(EntryStatus.EXPECTED, gym.status)
        assertEquals(Money(9_990), gym.amount)
    }

    @Test
    fun `RN09 - a bill without amount or with day 0 is rejected`() = runTest {
        start()
        vm.updateBillForm { copy(description = "Bad", amount = null, day = 10) }
        vm.addBill()
        assertIs<InvalidAmount>(vm.uiState.value.error)

        vm.updateBillForm { copy(amount = 1_000, day = 0) }
        vm.addBill()
        val error = assertIs<InvalidDay>(vm.uiState.value.error)
        assertEquals(DayKind.DUE, error.kind)
        assertEquals(0, error.day)
    }

    @Test
    fun `RN07 - a salary is saved and stopping a bill removes it from the active list`() = runTest {
        start()
        vm.updateIncomeForm { copy(description = "Bonus", amount = 100_000, day = 20) }
        vm.addIncome()
        vm.events.first()
        assertTrue(vm.uiState.value.incomes.any { it.description == "Bonus" })

        vm.deactivateBill(vm.uiState.value.bills.first { it.description == "Water" })
        assertEquals(RecurrencesEvent.Deactivated("Water"), vm.events.first())
        assertTrue(vm.uiState.value.bills.none { it.description == "Water" })
    }
}
