package br.com.gabriel.gastocerto.ui.summary

import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.PeriodSummary
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.ui.TestApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {
    private lateinit var app: TestApp
    private lateinit var vm: SummaryViewModel
    private val november = MonthYear(2026, 11)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun start() {
        app = TestApp().seed()
        vm = SummaryViewModel(app.summary, app.statement, app.registry, app.reimbursement) { app.today }
    }

    @Test
    fun `UC04 - opens on the current month with the same totals as the use case`() = runTest {
        start()
        val state = vm.uiState.value
        assertEquals(Period.Month(november), state.period)
        assertFalse(state.loading)
        assertEquals(app.summary.ofMonth(november), state.summary)
        assertEquals(listOf("Gabriel", "Wife"), state.holders.map { it.name }.sorted())
    }

    @Test
    fun `RN04 - the surplus deducts only the household expenses`() = runTest {
        start()
        val summary: PeriodSummary = vm.uiState.value.summary!!
        assertEquals(summary.income - (summary.totalExpenses - summary.thirdPartyExpenses), summary.surplus())
        assertTrue(summary.thirdPartyExpenses.isPositive())
    }

    @Test
    fun `RN10 - filtering by holder keeps only the expenses of that holder`() = runTest {
        start()
        val wife = vm.uiState.value.holders.first { it.role == Role.HOLDER }
        // RN01: her card expense of 11/07 is after the closing day (5), so it is in the December statement.
        vm.selectPeriod(Period.Month(MonthYear(2026, 12)))

        vm.filterHolder(wife)

        val summary = vm.uiState.value.summary!!
        assertEquals(setOf(wife), summary.byHolder.keys)
        assertEquals(Money(20_000), summary.totalExpenses)
        assertTrue(vm.uiState.value.statements.all { it.card.holder == wife })
    }

    @Test
    fun `RF15 - week and year periods and moving between periods`() = runTest {
        start()
        vm.selectPeriodKind(Period::weekOf)
        assertEquals(Period.Week(LocalDate(2026, 11, 23)), vm.uiState.value.period)
        assertEquals(app.summary.ofWeek(app.today), vm.uiState.value.summary)
        assertTrue(vm.uiState.value.statements.isEmpty())

        vm.selectPeriod(vm.uiState.value.period.next())
        assertEquals(Period.Week(LocalDate(2026, 11, 30)), vm.uiState.value.period)

        vm.selectPeriodKind(Period::yearOf)
        assertEquals(app.summary.ofYear(2026), vm.uiState.value.summary)
        vm.selectPeriod(vm.uiState.value.period.previous())
        assertEquals(Period.Year(2025), vm.uiState.value.period)
    }

    @Test
    fun `RN03 - the receivable balance of the father is shown`() = runTest {
        start()
        val father = app.registry.people().first { it.role == Role.THIRD_PARTY }
        assertEquals(listOf(app.reimbursement.receivableBalance(father)), vm.uiState.value.receivables)
    }

    @Test
    fun `UC05 - the month has one statement per active credit card`() = runTest {
        start()
        vm.selectPeriod(Period.Month(MonthYear(2026, 12)))
        val statements = vm.uiState.value.statements
        assertEquals(setOf("C6", "Nubank", "Nubank (wife)"), statements.map { it.card.name }.toSet())
        assertEquals(statements.first { it.card.name == "Nubank" }, app.statement.execute(statements.first { it.card.name == "Nubank" }.card, MonthYear(2026, 12)))
    }
}
