package br.com.gabriel.gastocerto.ui.reimbursement

import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.rule.ReimbursementExceedsBalance
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

@OptIn(ExperimentalCoroutinesApi::class)
class ReimbursementViewModelTest {
    private lateinit var app: TestApp
    private lateinit var vm: ReimbursementViewModel

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun start() {
        app = TestApp().seed()
        vm = ReimbursementViewModel(app.reimbursement, app.registry) { app.today }
    }

    @Test
    fun `RN03 - shows the father balance and a reimbursement lowers it`() = runTest {
        start()
        val balance = vm.uiState.value.selectedBalance!!
        assertEquals("Father", balance.thirdParty.name)
        assertEquals(Money(127_000), balance.amount)

        vm.setAmount(27_000)
        vm.register()

        assertEquals(ReimbursementEvent.Registered("Father", Money(100_000)), vm.events.first())
        assertEquals(Money(100_000), vm.uiState.value.selectedBalance!!.amount)
    }

    @Test
    fun `RN03 - a reimbursement greater than the balance is rejected`() = runTest {
        start()
        vm.setAmount(200_000)
        vm.register()
        val error = assertIs<ReimbursementExceedsBalance>(vm.uiState.value.error)
        assertEquals(Money(127_000), error.balance)
    }
}
