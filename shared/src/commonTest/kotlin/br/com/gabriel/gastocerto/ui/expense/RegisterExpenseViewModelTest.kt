package br.com.gabriel.gastocerto.ui.expense

import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.rule.InstallmentsOutOfRange
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.InvalidExchangeRate
import br.com.gabriel.gastocerto.ui.TestApp
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterExpenseViewModelTest {
    private lateinit var app: TestApp
    private lateinit var vm: RegisterExpenseViewModel
    private val november = MonthYear(2026, 11)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun start() {
        app = TestApp().seed()
        vm = RegisterExpenseViewModel(app.registerExpense, app.registerPurchase, app.reopenMonth, app.registry) { app.today }
    }

    private fun method(name: String) = vm.uiState.value.paymentMethods.first { it.name == name }

    @Test
    fun `the form lists the active payment methods and the third parties`() = runTest {
        start()
        val state = vm.uiState.value
        assertEquals(7, state.paymentMethods.size)
        assertEquals(listOf("Father"), state.thirdParties.map { it.name })
        assertEquals("C6", state.paymentMethod?.name) // the first credit card
        assertEquals(app.today, state.date)
    }

    @Test
    fun `UC01 - a debit expense is saved in the month of its date and the form is cleared`() = runTest {
        start()
        vm.selectPaymentMethod(method("Inter"))
        vm.setAmount(4_590)
        vm.setDescription("Bakery")

        vm.confirm()

        assertEquals(RegisterExpenseEvent.ExpenseSaved(november), vm.events.first())
        assertTrue(app.repositories.entries.ofMonth(november).any { it.description == "Bakery" && it.amount == Money(4_590) })
        assertEquals("", vm.uiState.value.description)
        assertIs<CheckingAccount>(vm.uiState.value.paymentMethod)
    }

    @Test
    fun `RN01 - a card expense after the closing day goes to the next statement`() = runTest {
        start()
        vm.selectPaymentMethod(method("Nubank"))
        vm.setAmount(10_000)
        vm.setDescription("Shoes")

        vm.confirm()

        assertEquals(RegisterExpenseEvent.ExpenseSaved(MonthYear(2026, 12)), vm.events.first())
    }

    @Test
    fun `RN09 - an expense without amount shows the rule error and saves nothing`() = runTest {
        start()
        val before = app.repositories.entries.ofMonth(november).size
        vm.selectPaymentMethod(method("Inter"))
        vm.setDescription("No amount")

        vm.confirm()

        assertIs<InvalidAmount>(vm.uiState.value.error)
        assertFalse(vm.uiState.value.saving)
        assertEquals(before, app.repositories.entries.ofMonth(november).size)
    }

    @Test
    fun `RN05 - a dollar expense shows the converted amount and is saved in reais`() = runTest {
        start()
        vm.selectPaymentMethod(method("Wise"))
        assertTrue(vm.uiState.value.isForeignCurrency)
        vm.setForeignAmount(1_250)
        vm.setExchangeRate(54_321)
        vm.setDescription("Cloud")
        assertEquals(Money(6_790), vm.uiState.value.convertedAmount)

        vm.confirm()

        vm.events.first()
        assertEquals(Money(6_790), app.repositories.entries.ofMonth(november).first { it.description == "Cloud" }.amount)
    }

    @Test
    fun `RN05 - a foreign expense without exchange rate is rejected`() = runTest {
        start()
        assertIs<ForeignCurrencyWallet>(method("Wise"))
        vm.selectPaymentMethod(method("Wise"))
        vm.setForeignAmount(1_000)
        vm.setDescription("No rate")

        vm.confirm()

        assertIs<InvalidExchangeRate>(vm.uiState.value.error)
    }

    @Test
    fun `UC02 - the preview shows the installments with their statement and confirming saves them`() = runTest {
        start()
        vm.selectPaymentMethod(method("Nubank"))
        vm.setAmount(10_000)
        vm.setDescription("Chair")
        vm.setInstallmentCount(3)

        vm.previewInstallments()

        val preview = vm.uiState.value.preview
        assertEquals(listOf("1/3", "2/3", "3/3"), preview.map { it.label() })
        assertEquals(listOf(Money(3_334), Money(3_333), Money(3_333)), preview.map { it.amount })
        assertEquals(listOf(MonthYear(2026, 12), MonthYear(2027, 1), MonthYear(2027, 2)), preview.map { it.accountingMonth })

        vm.confirm()

        assertEquals(RegisterExpenseEvent.PurchaseSaved(3, MonthYear(2026, 12)), vm.events.first())
        val saved = app.repositories.entries.ofPeriod(LocalDate(2026, 11, 1), LocalDate(2027, 3, 1))
            .filterIsInstance<Installment>().filter { it.description == "Chair" }
        assertEquals(3, saved.size)
    }

    @Test
    fun `RN02 - 49 installments are rejected in the preview and editing clears the preview`() = runTest {
        start()
        vm.selectPaymentMethod(method("Nubank"))
        vm.setAmount(10_000)
        vm.setDescription("Too many")
        vm.setInstallmentCount(49)

        vm.previewInstallments()
        assertIs<InstallmentsOutOfRange>(vm.uiState.value.error)

        vm.setInstallmentCount(2)
        vm.previewInstallments()
        assertEquals(2, vm.uiState.value.preview.size)
        vm.setAmount(20_000)
        assertTrue(vm.uiState.value.preview.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `installments only apply to credit cards`() = runTest {
        start()
        vm.selectPaymentMethod(method("Inter"))
        vm.setInstallmentCount(6)
        assertFalse(vm.uiState.value.isInstallmentPurchase)
        assertIs<CreditCard>(method("C6"))
    }

    @Test
    fun `RN08 - a closed month offers to reopen and reopening saves the expense`() = runTest {
        start()
        app.repositories.accountingMonths.save(AccountingMonth(november, AccountingMonthStatus.CLOSED))
        vm.selectPaymentMethod(method("Inter"))
        vm.setAmount(1_000)
        vm.setDescription("Late entry")

        vm.confirm()
        assertEquals(RegisterExpenseEvent.OfferReopen(november), vm.events.first())
        assertTrue(app.repositories.entries.ofMonth(november).none { it.description == "Late entry" })

        vm.reopenAndConfirm(november)

        assertEquals(RegisterExpenseEvent.ExpenseSaved(november), vm.events.first())
        assertEquals(AccountingMonthStatus.OPEN, app.repositories.accountingMonths.findOrCreate(november).status)
    }

    @Test
    fun `RN03 - an expense on behalf of the father is saved with the third party`() = runTest {
        start()
        val father = vm.uiState.value.thirdParties.single()
        vm.selectPaymentMethod(method("Inter"))
        vm.setAmount(3_000)
        vm.setDescription("Medicine")
        vm.selectThirdParty(father)

        vm.confirm()

        vm.events.first()
        assertTrue(app.repositories.entries.ofThirdParty(father).any { it.description == "Medicine" })
    }
}
