package br.com.gabriel.gastocerto.ui.closemonth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.PeriodSummary
import br.com.gabriel.gastocerto.domain.rule.RuleViolationException
import br.com.gabriel.gastocerto.ui.monthYear
import br.com.gabriel.gastocerto.ui.today
import br.com.gabriel.gastocerto.usecase.CloseMonthUseCase
import br.com.gabriel.gastocerto.usecase.CorrectEntryUseCase
import br.com.gabriel.gastocerto.usecase.GetSummaryUseCase
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

/** Everything the close-month screen draws (UC03). */
data class CloseMonthUiState(
    val month: MonthYear,
    val status: AccountingMonthStatus = AccountingMonthStatus.OPEN,
    val summary: PeriodSummary? = null,
    /** EXPECTED entries of the month still to be checked (RN06, RN07, RN11). */
    val expected: List<Entry> = emptyList(),
    val error: RuleViolationException? = null,
    val busy: Boolean = true,
) {
    val isClosed: Boolean get() = status == AccountingMonthStatus.CLOSED
}

sealed interface CloseMonthEvent {
    /** UC03 done: [month] closed and [next] opened with [expectedCount] expected entries. */
    data class Closed(val month: MonthYear, val next: MonthYear, val expectedCount: Int) : CloseMonthEvent

    /** UC03 A2: [month] reopened for corrections (RN08). */
    data class Reopened(val month: MonthYear) : CloseMonthEvent

    /** An expected entry was checked and is now ACTUAL. */
    data class Confirmed(val description: String) : CloseMonthEvent
}

/**
 * UC03 Close the month (MVVM + UDF): the month summary (sequence `uc03-1-resumo`), the expected entries
 * to check, [close] (`uc03-2-fechamento`) and [reopen] (A2).
 */
class CloseMonthViewModel(
    private val summaries: GetSummaryUseCase,
    private val closeMonth: CloseMonthUseCase,
    private val reopenMonth: ReopenMonthUseCase,
    private val correctEntry: CorrectEntryUseCase,
    today: () -> LocalDate = ::today,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CloseMonthUiState(month = today().monthYear()))
    val uiState: StateFlow<CloseMonthUiState> = _uiState.asStateFlow()

    private val _events = Channel<CloseMonthEvent>(Channel.BUFFERED)
    val events: Flow<CloseMonthEvent> = _events.receiveAsFlow()

    init {
        load(_uiState.value.month)
    }

    /** UC03 steps 1–3 – status, summary and expected entries of [month]. */
    fun load(month: MonthYear) {
        _uiState.update { it.copy(month = month, busy = true, error = null) }
        viewModelScope.launch { refresh(month) }
    }

    /** UC03 steps 4–8 – closes the month and opens the next one with its expected entries (one transaction). */
    fun close() = act { month ->
        val next = closeMonth.execute(month)
        val expectedCount = correctEntry.entriesOf(next.monthYear).count { it.status == EntryStatus.EXPECTED }
        CloseMonthEvent.Closed(month, next.monthYear, expectedCount)
    }

    /** UC03 A2 – reopens the month (RN08: explicit; the next month's expected entries stay). */
    fun reopen() = act { month ->
        reopenMonth.execute(month)
        CloseMonthEvent.Reopened(month)
    }

    /** Checks an expected entry against the real amount of the month (RN09: > 0). */
    fun confirmExpected(entry: Entry, actualAmount: Long?) = act {
        correctEntry.confirmExpected(entry, Money(actualAmount ?: 0))
        CloseMonthEvent.Confirmed(entry.description)
    }

    private fun act(action: suspend (MonthYear) -> CloseMonthEvent) {
        val month = _uiState.value.month
        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                _events.send(action(month))
                refresh(month)
            } catch (error: RuleViolationException) {
                _uiState.update { it.copy(busy = false, error = error) }
                refresh(month, keepError = true)
            }
        }
    }

    private suspend fun refresh(month: MonthYear, keepError: Boolean = false) {
        val accountingMonth = closeMonth.accountingMonth(month)
        val summary = summaries.ofMonth(month)
        val expected = correctEntry.entriesOf(month).filter { it.status == EntryStatus.EXPECTED }
        _uiState.update {
            if (it.month != month) it
            else it.copy(
                status = accountingMonth.status, summary = summary, expected = expected, busy = false,
                error = if (keepError) it.error else null,
            )
        }
    }
}
