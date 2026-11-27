package br.com.gabriel.gastocerto.ui.recurrences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.rule.RuleViolationException
import br.com.gabriel.gastocerto.usecase.ManageRecurrencesUseCase
import br.com.gabriel.gastocerto.usecase.ManageRegistryUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A new recurring bill (RN06) or income (RN07): description, amount in cents and day of the month. */
data class RecurrenceForm(
    val description: String = "",
    val amount: Long? = null,
    val day: Int? = null,
    /** Bills only. */
    val paymentMethod: PaymentMethod? = null,
)

data class RecurrencesUiState(
    val bills: List<RecurringBill> = emptyList(),
    val incomes: List<RecurringIncome> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val billForm: RecurrenceForm = RecurrenceForm(),
    val incomeForm: RecurrenceForm = RecurrenceForm(),
    val error: RuleViolationException? = null,    /**
     * Bumped on every reload. Domain entities compare by `id` (D2) and `StateFlow` skips a value equal to the
     * current one, so without it a reload where only a field changed (amount, contribution…) would not reach the screen.
     */
    val revision: Int = 0,
)

sealed interface RecurrencesEvent {
    data class Saved(val description: String) : RecurrencesEvent
    data class Deactivated(val description: String) : RecurrencesEvent
}

/** UC07 Manage recurring bills and salary (MVVM + UDF). */
class RecurrencesViewModel(
    private val recurrences: ManageRecurrencesUseCase,
    private val registry: ManageRegistryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecurrencesUiState())
    val uiState: StateFlow<RecurrencesUiState> = _uiState.asStateFlow()

    private val _events = Channel<RecurrencesEvent>(Channel.BUFFERED)
    val events: Flow<RecurrencesEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { reload() }
    }

    fun updateBillForm(change: RecurrenceForm.() -> RecurrenceForm) =
        _uiState.update { it.copy(billForm = it.billForm.change(), error = null) }

    fun updateIncomeForm(change: RecurrenceForm.() -> RecurrenceForm) =
        _uiState.update { it.copy(incomeForm = it.incomeForm.change(), error = null) }

    /** RN06 – saves the bill; if the current month is open and the due day has not passed, its expected expense is created. */
    fun addBill() {
        val form = _uiState.value.billForm
        val paymentMethod = form.paymentMethod ?: return
        if (form.description.isBlank()) return
        act {
            recurrences.saveRecurringBill(
                RecurringBill(form.description.trim(), Money(form.amount ?: 0), form.day ?: 0, paymentMethod),
            )
            _uiState.update { it.copy(billForm = RecurrenceForm(paymentMethod = paymentMethod)) }
            RecurrencesEvent.Saved(form.description.trim())
        }
    }

    /** RN07 – saves the salary; its expected income comes when the next month is opened. */
    fun addIncome() {
        val form = _uiState.value.incomeForm
        if (form.description.isBlank()) return
        act {
            recurrences.saveRecurringIncome(RecurringIncome(form.description.trim(), Money(form.amount ?: 0), form.day ?: 0))
            _uiState.update { it.copy(incomeForm = RecurrenceForm()) }
            RecurrencesEvent.Saved(form.description.trim())
        }
    }

    /** Stops generating expected entries from this bill (the ones already generated stay). */
    fun deactivateBill(bill: RecurringBill) = act {
        bill.active = false
        recurrences.saveRecurringBill(bill)
        RecurrencesEvent.Deactivated(bill.description)
    }

    fun deactivateIncome(income: RecurringIncome) = act {
        income.active = false
        recurrences.saveRecurringIncome(income)
        RecurrencesEvent.Deactivated(income.description)
    }

    private fun act(action: suspend () -> RecurrencesEvent) {
        viewModelScope.launch {
            try {
                val event = action()
                reload()
                _events.send(event)
            } catch (error: RuleViolationException) {
                _uiState.update { it.copy(error = error) }
            }
        }
    }

    private suspend fun reload() {
        val methods = registry.activePaymentMethods()
        val bills = recurrences.recurringBills()
        val incomes = recurrences.recurringIncomes()
        _uiState.update { state ->
            state.copy(
                bills = bills, incomes = incomes, paymentMethods = methods, revision = state.revision + 1,
                billForm = state.billForm.copy(
                    paymentMethod = state.billForm.paymentMethod?.takeIf { it in methods } ?: methods.firstOrNull(),
                ),
            )
        }
    }
}
