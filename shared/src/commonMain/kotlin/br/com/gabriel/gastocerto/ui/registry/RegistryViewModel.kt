package br.com.gabriel.gastocerto.ui.registry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.RuleViolationException
import br.com.gabriel.gastocerto.usecase.ManageRegistryUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The four kinds of payment method the form can create (RF03). */
enum class PaymentMethodType { CREDIT_CARD, CHECKING_ACCOUNT, FOREIGN_CURRENCY_WALLET, INVESTMENT_ACCOUNT }

/** New payment method form; only the fields of the chosen [type] are used. */
data class PaymentMethodForm(
    val type: PaymentMethodType = PaymentMethodType.CREDIT_CARD,
    val name: String = "",
    val holder: Person? = null,
    val institution: Institution? = null,
    val closingDay: Int? = null,
    val dueDay: Int? = null,
    val currency: String = "USD",
    /** RN11 – optional planned monthly contribution in cents. */
    val plannedContribution: Long? = null,
) {
    val isComplete: Boolean get() = name.isNotBlank() && holder != null && institution != null
}

/** Everything the registry screen draws (UC06). */
data class RegistryUiState(
    val people: List<Person> = emptyList(),
    val institutions: List<Institution> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val personName: String = "",
    val personRole: Role = Role.THIRD_PARTY,
    val institutionName: String = "",
    val form: PaymentMethodForm = PaymentMethodForm(),
    val error: RuleViolationException? = null,    /**
     * Bumped on every reload. Domain entities compare by `id` (D2) and `StateFlow` skips a value equal to the
     * current one, so without it a reload where only a field changed (amount, contribution…) would not reach the screen.
     */
    val revision: Int = 0,
) {
    /** RN10 – a payment method is held by the user or a household holder, never a third party. */
    val holders: List<Person> get() = people.filter { it.role != Role.THIRD_PARTY }
}

sealed interface RegistryEvent {
    data class Saved(val name: String) : RegistryEvent
    data class Deactivated(val name: String) : RegistryEvent
    data class Deleted(val name: String) : RegistryEvent
}

/** UC06 Manage people, institutions and payment methods (MVVM + UDF). */
class RegistryViewModel(private val registry: ManageRegistryUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(RegistryUiState())
    val uiState: StateFlow<RegistryUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegistryEvent>(Channel.BUFFERED)
    val events: Flow<RegistryEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { reload() }
    }

    fun setPersonName(name: String) = _uiState.update { it.copy(personName = name, error = null) }
    fun setPersonRole(role: Role) = _uiState.update { it.copy(personRole = role, error = null) }
    fun setInstitutionName(name: String) = _uiState.update { it.copy(institutionName = name, error = null) }
    fun updateForm(change: PaymentMethodForm.() -> PaymentMethodForm) =
        _uiState.update { it.copy(form = it.form.change(), error = null) }

    fun addPerson() {
        val state = _uiState.value
        if (state.personName.isBlank()) return
        act {
            registry.savePerson(Person(state.personName.trim(), state.personRole))
            _uiState.update { it.copy(personName = "") }
            RegistryEvent.Saved(state.personName.trim())
        }
    }

    fun addInstitution() {
        val name = _uiState.value.institutionName.trim()
        if (name.isEmpty()) return
        act {
            registry.saveInstitution(Institution(name))
            _uiState.update { it.copy(institutionName = "") }
            RegistryEvent.Saved(name)
        }
    }

    /** Creates the payment method; the domain checks RN09 (days 1–31, contribution > 0) and RN10 (holder). */
    fun addPaymentMethod() {
        val form = _uiState.value.form
        val holder = form.holder ?: return
        val institution = form.institution ?: return
        if (form.name.isBlank()) return
        act {
            val name = form.name.trim()
            val paymentMethod = when (form.type) {
                PaymentMethodType.CREDIT_CARD -> CreditCard(name, holder, institution, form.closingDay ?: 0, form.dueDay ?: 0)
                PaymentMethodType.CHECKING_ACCOUNT -> CheckingAccount(name, holder, institution)
                PaymentMethodType.FOREIGN_CURRENCY_WALLET ->
                    ForeignCurrencyWallet(name, holder, institution, form.currency.trim().uppercase().ifEmpty { "USD" })
                PaymentMethodType.INVESTMENT_ACCOUNT ->
                    InvestmentAccount(name, holder, institution, form.plannedContribution?.let(::Money))
            }
            registry.savePaymentMethod(paymentMethod)
            _uiState.update { it.copy(form = PaymentMethodForm(type = form.type)) }
            RegistryEvent.Saved(name)
        }
    }

    /** RN09 – hides the payment method from new entries but keeps its history. */
    fun deactivate(paymentMethod: PaymentMethod) = act {
        registry.deactivatePaymentMethod(paymentMethod)
        RegistryEvent.Deactivated(paymentMethod.name)
    }

    /** RN09 – only a payment method without entries can be deleted (`PaymentMethodHasEntries`). */
    fun delete(paymentMethod: PaymentMethod) = act {
        registry.deletePaymentMethod(paymentMethod)
        RegistryEvent.Deleted(paymentMethod.name)
    }

    private fun act(action: suspend () -> RegistryEvent) {
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
        val people = registry.people()
        val institutions = registry.institutions()
        val methods = registry.activePaymentMethods()
        _uiState.update { state ->
            state.copy(
                people = people, institutions = institutions, paymentMethods = methods, revision = state.revision + 1,
                form = state.form.copy(
                    holder = state.form.holder?.takeIf { it in people } ?: people.firstOrNull { it.role == Role.USER },
                    institution = state.form.institution?.takeIf { it in institutions } ?: institutions.firstOrNull(),
                ),
            )
        }
    }
}
