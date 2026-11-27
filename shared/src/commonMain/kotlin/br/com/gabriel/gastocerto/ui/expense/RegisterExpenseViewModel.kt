package br.com.gabriel.gastocerto.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed
import br.com.gabriel.gastocerto.domain.rule.ExpenseInput
import br.com.gabriel.gastocerto.domain.rule.PurchaseInput
import br.com.gabriel.gastocerto.domain.rule.RuleViolationException
import br.com.gabriel.gastocerto.ui.today
import br.com.gabriel.gastocerto.usecase.ManageRegistryUseCase
import br.com.gabriel.gastocerto.usecase.RegisterExpenseUseCase
import br.com.gabriel.gastocerto.usecase.RegisterInstallmentPurchaseUseCase
import br.com.gabriel.gastocerto.usecase.ReopenMonthUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Everything the expense form draws (UC01 + UC02). */
data class RegisterExpenseUiState(
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val thirdParties: List<Person> = emptyList(),
    val paymentMethod: PaymentMethod? = null,
    /** Amount in cents (total amount when in installments). */
    val amount: Long? = null,
    val date: LocalDate,
    val description: String = "",
    val thirdParty: Person? = null,
    /** 1 = single payment; 2..48 = installment purchase (credit card only, RN02). */
    val installmentCount: Int? = 1,
    /** RN05 – amount in cents of the foreign currency and exchange rate with 4 decimals (D1). */
    val foreignAmount: Long? = null,
    val exchangeRate: Long? = null,
    /** UC02 – the installments shown before confirming (`k/N – amount – statement`). */
    val preview: List<Installment> = emptyList(),
    val error: RuleViolationException? = null,
    val saving: Boolean = false,    /**
     * Bumped on every reload. Domain entities compare by `id` (D2) and `StateFlow` skips a value equal to the
     * current one, so without it a reload where only a field changed (amount, contribution…) would not reach the screen.
     */
    val revision: Int = 0,
) {
    val isForeignCurrency: Boolean get() = paymentMethod is ForeignCurrencyWallet
    val isCreditCard: Boolean get() = paymentMethod is CreditCard
    val isInstallmentPurchase: Boolean get() = isCreditCard && (installmentCount ?: 1) >= 2

    /** RN05 – amount in reais of a foreign currency expense, shown while typing. */
    val convertedAmount: Money?
        get() = if (foreignAmount != null && exchangeRate != null && exchangeRate > 0) {
            Money(foreignAmount).convert(exchangeRate)
        } else null

    val canConfirm: Boolean get() = paymentMethod != null && description.isNotBlank() && !saving
}

sealed interface RegisterExpenseEvent {
    /** Saved; [accountingMonth] is where it counts (the statement month on a card, RN01). */
    data class ExpenseSaved(val accountingMonth: MonthYear) : RegisterExpenseEvent

    /** UC02 saved: [count] installments, the first one in [firstMonth]. */
    data class PurchaseSaved(val count: Int, val firstMonth: MonthYear) : RegisterExpenseEvent

    /** RN08 – the month is closed: offer to reopen it and register anyway. */
    data class OfferReopen(val month: MonthYear) : RegisterExpenseEvent
}

/**
 * UC01 Register expense and UC02 Register installment purchase (MVVM + UDF, see `AppViewModel`).
 * Field changes update the state; [previewInstallments] and [confirm] call the use cases.
 */
class RegisterExpenseViewModel(
    private val registerExpense: RegisterExpenseUseCase,
    private val registerPurchase: RegisterInstallmentPurchaseUseCase,
    private val reopenMonth: ReopenMonthUseCase,
    private val registry: ManageRegistryUseCase,
    today: () -> LocalDate = ::today,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterExpenseUiState(date = today()))
    val uiState: StateFlow<RegisterExpenseUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterExpenseEvent>(Channel.BUFFERED)
    val events: Flow<RegisterExpenseEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { reload() }
    }

    /** Reloads the payment methods and third parties (after the registry changes); starts on the first card. */
    suspend fun reload() {
        val methods = registry.activePaymentMethods()
        val thirdParties = registry.people().filter { it.role == Role.THIRD_PARTY }
        _uiState.update { state ->
            state.copy(
                paymentMethods = methods,
                revision = state.revision + 1,
                thirdParties = thirdParties,
                paymentMethod = state.paymentMethod?.takeIf { it in methods }
                    ?: methods.firstOrNull { it is CreditCard } ?: methods.firstOrNull(),
            )
        }
    }

    fun selectPaymentMethod(paymentMethod: PaymentMethod) = edit { copy(paymentMethod = paymentMethod) }
    fun setAmount(amount: Long?) = edit { copy(amount = amount) }
    fun setDate(date: LocalDate) = edit { copy(date = date) }
    fun setDescription(description: String) = edit { copy(description = description) }
    fun selectThirdParty(thirdParty: Person?) = edit { copy(thirdParty = thirdParty) }
    fun setInstallmentCount(count: Int?) = edit { copy(installmentCount = count) }
    fun setForeignAmount(amount: Long?) = edit { copy(foreignAmount = amount) }
    fun setExchangeRate(rate: Long?) = edit { copy(exchangeRate = rate) }

    /** Any field change invalidates the preview and clears the last error. */
    private fun edit(change: RegisterExpenseUiState.() -> RegisterExpenseUiState) =
        _uiState.update { it.change().copy(preview = emptyList(), error = null) }

    /** UC02 steps 1–4 – shows the installments with their statement month, without saving. */
    fun previewInstallments() {
        val input = purchaseInput() ?: return
        viewModelScope.launch {
            try {
                val installments = registerPurchase.preview(input)
                _uiState.update { it.copy(preview = installments, error = null) }
            } catch (error: RuleViolationException) {
                _uiState.update { it.copy(preview = emptyList(), error = error) }
            }
        }
    }

    /** UC01 / UC02 – validates and saves. A closed month (RN08) becomes an offer to reopen it. */
    fun confirm() {
        val state = _uiState.value
        if (!state.canConfirm) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val event = save(_uiState.value)
                clearForm()
                _events.send(event)
            } catch (closed: AccountingMonthClosed) {
                _uiState.update { it.copy(saving = false) }
                _events.send(RegisterExpenseEvent.OfferReopen(closed.month))
            } catch (error: RuleViolationException) {
                _uiState.update { it.copy(saving = false, error = error) }
            }
        }
    }

    /** UC03 A2 / RN08 – the user chose to reopen [month]; then the same entry is confirmed again. */
    fun reopenAndConfirm(month: MonthYear) {
        viewModelScope.launch {
            reopenMonth.execute(month)
            confirm()
        }
    }

    private suspend fun save(state: RegisterExpenseUiState): RegisterExpenseEvent {
        val purchase = purchaseInput(state)
        if (purchase != null) {
            val saved = registerPurchase.execute(purchase)
            val installments = saved.generateInstallments()
            return RegisterExpenseEvent.PurchaseSaved(installments.size, installments.first().accountingMonth)
        }
        val expense = registerExpense.execute(
            ExpenseInput(
                paymentMethod = checkNotNull(state.paymentMethod),
                amount = state.amount?.let(::Money),
                date = state.date,
                description = state.description.trim(),
                thirdParty = state.thirdParty,
                foreignAmount = state.foreignAmount.takeIf { state.isForeignCurrency },
                exchangeRate = state.exchangeRate.takeIf { state.isForeignCurrency },
            ),
        )
        return RegisterExpenseEvent.ExpenseSaved(expense.accountingMonth)
    }

    private fun purchaseInput(state: RegisterExpenseUiState = _uiState.value): PurchaseInput? {
        val card = state.paymentMethod as? CreditCard ?: return null
        if (!state.isInstallmentPurchase) return null
        return PurchaseInput(
            card = card,
            totalAmount = Money(state.amount ?: 0),
            installmentCount = state.installmentCount ?: 0,
            purchaseDate = state.date,
            description = state.description.trim(),
            thirdParty = state.thirdParty,
        )
    }

    /** After saving: keeps the payment method and date (several expenses in a row), clears the rest. */
    private fun clearForm() = _uiState.update {
        it.copy(
            amount = null, description = "", thirdParty = null, installmentCount = 1,
            foreignAmount = null, exchangeRate = null, preview = emptyList(), error = null, saving = false,
        )
    }
}
