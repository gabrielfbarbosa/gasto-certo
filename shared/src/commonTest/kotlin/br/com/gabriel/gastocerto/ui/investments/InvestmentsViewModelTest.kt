package br.com.gabriel.gastocerto.ui.investments

import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
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

@OptIn(ExperimentalCoroutinesApi::class)
class InvestmentsViewModelTest {
    private lateinit var app: TestApp
    private lateinit var vm: InvestmentsViewModel

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun start() {
        app = TestApp().seed()
        vm = InvestmentsViewModel(app.investments, app.registry) { app.today }
    }

    private fun account(name: String) = vm.uiState.value.accounts.first { it.name == name }

    @Test
    fun `RN11 - lists the brokerages and changes or removes the planned contribution`() = runTest {
        start()
        assertEquals(listOf("Binance", "Clear"), vm.uiState.value.accounts.map { it.name })

        vm.setPlannedContribution(account("Binance"), 20_000)
        assertEquals(InvestmentsEvent.ContributionSet("Binance"), vm.events.first())
        assertEquals(Money(20_000), account("Binance").plannedMonthlyContribution)

        vm.setPlannedContribution(account("Clear"), null)
        vm.events.first()
        assertNull(account("Clear").plannedMonthlyContribution)
    }

    @Test
    fun `RN11 - a sale is income of type investment sale and zero is rejected`() = runTest {
        start()
        vm.registerSale(account("Clear"), 0)
        assertIs<InvalidAmount>(vm.uiState.value.error)

        vm.registerSale(account("Clear"), 250_000)
        assertEquals(InvestmentsEvent.SaleRegistered("Clear", MonthYear(2026, 11)), vm.events.first())
        val sale = app.repositories.entries.ofMonth(MonthYear(2026, 11)).filterIsInstance<Income>()
            .single { it.type == IncomeType.INVESTMENT_SALE }
        assertEquals(Money(250_000), sale.amount)
    }
}
