package br.com.gabriel.gastocerto.ui.registry

import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.DayKind
import br.com.gabriel.gastocerto.domain.rule.InvalidDay
import br.com.gabriel.gastocerto.domain.rule.PaymentMethodHasEntries
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
class RegistryViewModelTest {
    private lateinit var app: TestApp
    private lateinit var vm: RegistryViewModel

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun start() {
        app = TestApp().seed()
        vm = RegistryViewModel(app.registry)
    }

    @Test
    fun `UC06 - lists the registry and offers only household holders`() = runTest {
        start()
        val state = vm.uiState.value
        assertEquals(3, state.people.size)
        assertEquals(6, state.institutions.size)
        assertEquals(7, state.paymentMethods.size)
        assertTrue(state.holders.none { it.role == Role.THIRD_PARTY })
        assertEquals(Role.USER, state.form.holder?.role)
    }

    @Test
    fun `UC06 - adds a person and an institution`() = runTest {
        start()
        vm.setPersonName("Mother-in-law")
        vm.setPersonRole(Role.THIRD_PARTY)
        vm.addPerson()
        assertEquals(RegistryEvent.Saved("Mother-in-law"), vm.events.first())

        vm.setInstitutionName("Itaú")
        vm.addInstitution()
        assertEquals(RegistryEvent.Saved("Itaú"), vm.events.first())

        assertTrue(vm.uiState.value.people.any { it.name == "Mother-in-law" && it.role == Role.THIRD_PARTY })
        assertTrue(vm.uiState.value.institutions.any { it.name == "Itaú" })
        assertEquals("", vm.uiState.value.personName)
    }

    @Test
    fun `RN09 - a credit card with valid days is saved and an invalid closing day is rejected`() = runTest {
        start()
        vm.updateForm { copy(type = PaymentMethodType.CREDIT_CARD, name = "Inter card", closingDay = 32, dueDay = 10) }
        vm.addPaymentMethod()
        val error = assertIs<InvalidDay>(vm.uiState.value.error)
        assertEquals(DayKind.CLOSING, error.kind)
        assertEquals(32, error.day)

        vm.updateForm { copy(closingDay = 30) }
        vm.addPaymentMethod()
        assertEquals(RegistryEvent.Saved("Inter card"), vm.events.first())
        val card = vm.uiState.value.paymentMethods.first { it.name == "Inter card" }
        assertIs<CreditCard>(card)
        assertEquals(30, card.closingDay)
    }

    @Test
    fun `RN11 - a brokerage keeps its planned contribution`() = runTest {
        start()
        vm.updateForm { copy(type = PaymentMethodType.INVESTMENT_ACCOUNT, name = "XP", plannedContribution = 30_000) }
        vm.addPaymentMethod()
        vm.events.first()
        val xp = assertIs<InvestmentAccount>(vm.uiState.value.paymentMethods.first { it.name == "XP" })
        assertEquals(Money(30_000), xp.plannedMonthlyContribution)
    }

    @Test
    fun `RN09 - a payment method with entries cannot be deleted but can be deactivated`() = runTest {
        start()
        val inter = vm.uiState.value.paymentMethods.first { it.name == "Inter" }

        vm.delete(inter)
        assertIs<PaymentMethodHasEntries>(vm.uiState.value.error)

        vm.deactivate(inter)
        assertEquals(RegistryEvent.Deactivated("Inter"), vm.events.first())
        assertTrue(vm.uiState.value.paymentMethods.none { it.name == "Inter" })
    }

    @Test
    fun `RN09 - a payment method without entries is deleted`() = runTest {
        start()
        val binance = vm.uiState.value.paymentMethods.first { it.name == "Binance" }
        vm.delete(binance)
        assertEquals(RegistryEvent.Deleted("Binance"), vm.events.first())
        assertEquals(6, vm.uiState.value.paymentMethods.size)
    }
}
