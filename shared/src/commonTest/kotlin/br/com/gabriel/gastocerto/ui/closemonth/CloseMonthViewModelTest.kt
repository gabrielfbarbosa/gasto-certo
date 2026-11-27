package br.com.gabriel.gastocerto.ui.closemonth

import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.EntryStatus
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CloseMonthViewModelTest {
    private lateinit var app: TestApp
    private lateinit var vm: CloseMonthViewModel
    private val october = MonthYear(2026, 10)
    private val november = MonthYear(2026, 11)
    private val december = MonthYear(2026, 12)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun start() {
        app = TestApp().seed()
        vm = CloseMonthViewModel(app.summary, app.closeMonth, app.reopenMonth, app.correctEntry) { app.today }
    }

    @Test
    fun `UC03 - opens on the current month with its summary and the expected entries to check`() = runTest {
        start()
        val state = vm.uiState.value
        assertEquals(november, state.month)
        assertFalse(state.isClosed)
        assertEquals(app.summary.ofMonth(november), state.summary)
        assertEquals(5, state.expected.size)
        assertTrue(state.expected.all { it.status == EntryStatus.EXPECTED })
    }

    @Test
    fun `UC03 - closing closes the month and opens the next one with its expected entries`() = runTest {
        start()
        vm.close()

        val event = assertIs<CloseMonthEvent.Closed>(vm.events.first())
        assertEquals(december, event.next)
        assertEquals(5, event.expectedCount)
        assertTrue(vm.uiState.value.isClosed)
        assertEquals(AccountingMonthStatus.CLOSED, app.repositories.accountingMonths.findOrCreate(november).status)
    }

    @Test
    fun `RN08 - an already closed month shows the rule error and reopening opens it again`() = runTest {
        start()
        vm.load(october)
        assertTrue(vm.uiState.value.isClosed)

        vm.close()
        assertIs<AccountingMonthClosed>(vm.uiState.value.error)

        vm.reopen()
        assertEquals(CloseMonthEvent.Reopened(october), vm.events.first())
        assertFalse(vm.uiState.value.isClosed)
    }

    @Test
    fun `RN06 - checking an expected bill with the real amount makes it actual`() = runTest {
        start()
        val power = vm.uiState.value.expected.first { it.description == "Power" }

        vm.confirmExpected(power, 17_350)

        assertEquals(CloseMonthEvent.Confirmed("Power"), vm.events.first())
        assertEquals(4, vm.uiState.value.expected.size)
        val stored = app.repositories.entries.findById(power.id)!!
        assertEquals(EntryStatus.ACTUAL, stored.status)
        assertEquals(Money(17_350), stored.amount)
    }

    @Test
    fun `RN09 - checking with zero is rejected and the entry stays expected`() = runTest {
        start()
        val water = vm.uiState.value.expected.first { it.description == "Water" }

        vm.confirmExpected(water, null)

        assertIs<InvalidAmount>(vm.uiState.value.error)
        assertEquals(EntryStatus.EXPECTED, app.repositories.entries.findById(water.id)!!.status)
    }
}
