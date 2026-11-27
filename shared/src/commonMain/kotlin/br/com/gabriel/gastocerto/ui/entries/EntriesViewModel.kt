package br.com.gabriel.gastocerto.ui.entries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.rule.RuleViolationException
import br.com.gabriel.gastocerto.ui.monthYear
import br.com.gabriel.gastocerto.ui.today
import br.com.gabriel.gastocerto.usecase.CorrectEntryUseCase
import br.com.gabriel.gastocerto.usecase.RegisterOneOffIncomeUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** New one-off income form (UC08). */
data class IncomeForm(val amount: Long? = null, val date: LocalDate, val description: String = "")

data class EntriesUiState(
    val month: MonthYear,
    val entries: List<Entry> = emptyList(),
    val incomeForm: IncomeForm,
    val error: RuleViolationException? = null,    /**
     * Bumped on every reload. Domain entities compare by `id` (D2) and `StateFlow` skips a value equal to the
     * current one, so without it a reload where only a field changed (amount, contribution…) would not reach the screen.
     */
    val revision: Int = 0,
)

sealed interface EntriesEvent {
    data class IncomeSaved(val month: MonthYear) : EntriesEvent
    data class Corrected(val description: String) : EntriesEvent
    data class Deleted(val description: String) : EntriesEvent
}

/**
 * UC08 Register one-off income and UC09 Correct an entry (MVVM + UDF). RN08: a closed month accepts no
 * insertion, edit, confirmation or deletion — the use cases throw `AccountingMonthClosed`.
 */
class EntriesViewModel(
    private val oneOffIncome: RegisterOneOffIncomeUseCase,
    private val correctEntry: CorrectEntryUseCase,
    today: () -> LocalDate = ::today,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        EntriesUiState(month = today().monthYear(), incomeForm = IncomeForm(date = today())),
    )
    val uiState: StateFlow<EntriesUiState> = _uiState.asStateFlow()

    private val _events = Channel<EntriesEvent>(Channel.BUFFERED)
    val events: Flow<EntriesEvent> = _events.receiveAsFlow()

    init {
        load(_uiState.value.month)
    }

    fun load(month: MonthYear) {
        _uiState.update { it.copy(month = month, error = null) }
        viewModelScope.launch { reload() }
    }

    fun updateIncomeForm(change: IncomeForm.() -> IncomeForm) =
        _uiState.update { it.copy(incomeForm = it.incomeForm.change(), error = null) }

    /** UC08 – ACTUAL income in the month of its date (RN09 amount > 0, RN08 open month). */
    fun registerIncome() {
        val form = _uiState.value.incomeForm
        if (form.description.isBlank()) return
        act {
            val income = oneOffIncome.execute(Money(form.amount ?: 0), form.date, form.description.trim())
            _uiState.update { it.copy(incomeForm = IncomeForm(date = form.date)) }
            EntriesEvent.IncomeSaved(income.accountingMonth)
        }
    }

    /** UC09 – checks an EXPECTED entry with its real amount (RN06, RN07, RN11). */
    fun confirmExpected(entry: Entry, actualAmount: Long?) = act {
        correctEntry.confirmExpected(entry, Money(actualAmount ?: 0))
        EntriesEvent.Corrected(entry.description)
    }

    /** UC09 – fixes the amount and description (same id). A foreign expense whose amount changes loses its conversion. */
    fun edit(entry: Entry, amount: Long?, description: String) = act {
        correctEntry.edit(entry.withChanges(Money(amount ?: 0), description.trim().ifEmpty { entry.description }))
        EntriesEvent.Corrected(description.trim().ifEmpty { entry.description })
    }

    fun delete(entry: Entry) = act {
        correctEntry.delete(entry)
        EntriesEvent.Deleted(entry.description)
    }

    private fun act(action: suspend () -> EntriesEvent) {
        viewModelScope.launch {
            try {
                val event = action()
                _uiState.update { it.copy(error = null) }
                reload()
                _events.send(event)
            } catch (error: RuleViolationException) {
                _uiState.update { it.copy(error = error) }
                reload()
            }
        }
    }

    private suspend fun reload() {
        val month = _uiState.value.month
        val entries = correctEntry.entriesOf(month)
        _uiState.update { if (it.month == month) it.copy(entries = entries, revision = it.revision + 1) else it }
    }
}

/** A copy of the entry with another amount and description; the constructor re-checks RN09 (amount > 0). */
internal fun Entry.withChanges(amount: Money, description: String): Entry = when (this) {
    is Installment -> Installment(
        amount, date, description, accountingMonth, paymentMethod as CreditCard,
        purchaseId, number, totalInstallments, thirdParty, id,
    )
    is Expense -> Expense(
        amount, date, description, accountingMonth, paymentMethod, thirdParty,
        conversion.takeIf { amount == this.amount }, status, originId, id,
    )
    is Income -> Income(amount, date, description, accountingMonth, type, status, originId, id)
}
