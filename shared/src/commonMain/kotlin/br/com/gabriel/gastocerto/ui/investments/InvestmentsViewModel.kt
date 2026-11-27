package br.com.gabriel.gastocerto.ui.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.rule.RuleViolationException
import br.com.gabriel.gastocerto.ui.today
import br.com.gabriel.gastocerto.usecase.ManageInvestmentsUseCase
import br.com.gabriel.gastocerto.usecase.ManageRegistryUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class InvestmentsUiState(
    val accounts: List<InvestmentAccount> = emptyList(),
    val error: RuleViolationException? = null,    /**
     * Bumped on every reload. Domain entities compare by `id` (D2) and `StateFlow` skips a value equal to the
     * current one, so without it a reload where only a field changed (amount, contribution…) would not reach the screen.
     */
    val revision: Int = 0,
)

sealed interface InvestmentsEvent {
    data class ContributionSet(val account: String) : InvestmentsEvent
    /** RN11 – the sale is income in [month]. */
    data class SaleRegistered(val account: String, val month: MonthYear) : InvestmentsEvent
}

/** UC10 Manage investments (MVVM + UDF): planned contribution (RN11) and sale as income. */
class InvestmentsViewModel(
    private val investments: ManageInvestmentsUseCase,
    private val registry: ManageRegistryUseCase,
    private val today: () -> LocalDate = ::today,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InvestmentsUiState())
    val uiState: StateFlow<InvestmentsUiState> = _uiState.asStateFlow()

    private val _events = Channel<InvestmentsEvent>(Channel.BUFFERED)
    val events: Flow<InvestmentsEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { reload() }
    }

    /** RN11 – sets the planned monthly contribution (`null` or empty removes it; ≤ 0 is rejected). */
    fun setPlannedContribution(account: InvestmentAccount, amount: Long?) = act {
        investments.setPlannedContribution(account, amount?.let(::Money))
        InvestmentsEvent.ContributionSet(account.name)
    }

    /** RN11 – registers a sale as income received today. */
    fun registerSale(account: InvestmentAccount, amount: Long?) = act {
        val income = investments.registerSale(account, Money(amount ?: 0), today())
        InvestmentsEvent.SaleRegistered(account.name, income.accountingMonth)
    }

    private fun act(action: suspend () -> InvestmentsEvent) {
        viewModelScope.launch {
            try {
                val event = action()
                _uiState.update { it.copy(error = null) }
                reload()
                _events.send(event)
            } catch (error: RuleViolationException) {
                _uiState.update { it.copy(error = error) }
            }
        }
    }

    private suspend fun reload() {
        val accounts = registry.activePaymentMethods().filterIsInstance<InvestmentAccount>()
        _uiState.update { it.copy(accounts = accounts, revision = it.revision + 1) }
    }
}
