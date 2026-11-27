package br.com.gabriel.gastocerto.ui.app

import br.com.gabriel.gastocerto.data.memory.InMemoryAccountingMonthRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRecurrenceRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRegistryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryReimbursementRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryTransaction
import br.com.gabriel.gastocerto.domain.repository.Repositories
import br.com.gabriel.gastocerto.usecase.demoLabelsForTests
import br.com.gabriel.gastocerto.usecase.seedUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val database = InMemoryDatabase()
    private val repositories = Repositories(
        InMemoryEntryRepository(database), InMemoryAccountingMonthRepository(database),
        InMemoryRegistryRepository(database), InMemoryRecurrenceRepository(database),
        InMemoryReimbursementRepository(database), InMemoryTransaction(database),
    )

    private fun viewModel() = AppViewModel(repositories.seedUseCase(LocalDate(2026, 11, 27))) { demoLabelsForTests }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `on the first run the demo data is loaded the event is sent and the app becomes ready`() = runTest {
        val vm = viewModel()

        assertEquals(AppEvent.DemoDataLoaded, vm.events.first())
        assertTrue(vm.uiState.value.ready)
        assertEquals(3, repositories.registry.people().size)
    }

    @Test
    fun `on later runs no event is sent and the data is not duplicated`() = runTest {
        viewModel().events.first()

        val second = viewModel()

        assertTrue(second.uiState.value.ready)
        assertEquals(3, repositories.registry.people().size)
    }
}
