package br.com.gabriel.gastocerto.ui.reimbursement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.ReceivableBalance
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.RuleViolationException
import br.com.gabriel.gastocerto.ui.today
import br.com.gabriel.gastocerto.usecase.ManageRegistryUseCase
import br.com.gabriel.gastocerto.usecase.RegisterReimbursementUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class ReimbursementUiState(
    /** RN03 – what each third party owes (expenses on their behalf − reimbursements). */
    val balances: List<ReceivableBalance> = emptyList(),
    val thirdParty: Person? = null,
    val amount: Long? = null,
    val date: LocalDate,
    val error: RuleViolationException? = null,
) {
    val selectedBalance: ReceivableBalance? get() = balances.firstOrNull { it.thirdParty == thirdParty }
}

sealed interface ReimbursementEvent {
    data class Registered(val thirdParty: String, val remaining: Money) : ReimbursementEvent
}

/** UC11 Register reimbursement (MVVM + UDF). RN03: it lowers the balance, is not income and cannot exceed it. */
class ReimbursementViewModel(
    private val reimbursements: RegisterReimbursementUseCase,
    private val registry: ManageRegistryUseCase,
    today: () -> LocalDate = ::today,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReimbursementUiState(date = today()))
    val uiState: StateFlow<ReimbursementUiState> = _uiState.asStateFlow()

    private val _events = Channel<ReimbursementEvent>(Channel.BUFFERED)
    val events: Flow<ReimbursementEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { reload() }
    }

    fun selectThirdParty(person: Person) = _uiState.update { it.copy(thirdParty = person, error = null) }
    fun setAmount(amount: Long?) = _uiState.update { it.copy(amount = amount, error = null) }
    fun setDate(date: LocalDate) = _uiState.update { it.copy(date = date, error = null) }

    fun register() {
        val state = _uiState.value
        val person = state.thirdParty ?: return
        viewModelScope.launch {
            try {
                reimbursements.execute(person, Money(state.amount ?: 0), state.date)
                reload()
                val remaining = _uiState.value.selectedBalance?.amount ?: Money(0)
                _uiState.update { it.copy(amount = null, error = null) }
                _events.send(ReimbursementEvent.Registered(person.name, remaining))
            } catch (error: RuleViolationException) {
                _uiState.update { it.copy(error = error) }
            }
        }
    }

    private suspend fun reload() {
        val balances = registry.people().filter { it.role == Role.THIRD_PARTY }.map { reimbursements.receivableBalance(it) }
        _uiState.update { state ->
            state.copy(balances = balances, thirdParty = state.thirdParty?.takeIf { p -> balances.any { it.thirdParty == p } }
                ?: balances.firstOrNull()?.thirdParty)
        }
    }
}
