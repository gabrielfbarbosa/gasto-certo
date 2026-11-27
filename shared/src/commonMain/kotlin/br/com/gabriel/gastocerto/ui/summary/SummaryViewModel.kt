package br.com.gabriel.gastocerto.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.PeriodSummary
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.ReceivableBalance
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.model.Statement
import br.com.gabriel.gastocerto.ui.monthYear
import br.com.gabriel.gastocerto.ui.today
import br.com.gabriel.gastocerto.usecase.GetStatementUseCase
import br.com.gabriel.gastocerto.usecase.GetSummaryUseCase
import br.com.gabriel.gastocerto.usecase.ManageRegistryUseCase
import br.com.gabriel.gastocerto.usecase.RegisterReimbursementUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** The period of the summary (RF14 month by accounting month; RF15 week and year by date). */
sealed interface Period {
    data class Month(val month: MonthYear) : Period
    /** The Monday-to-Sunday week that starts on [monday]. */
    data class Week(val monday: LocalDate) : Period {
        val sunday: LocalDate get() = monday.plus(DatePeriod(days = 6))
    }
    data class Year(val year: Int) : Period

    fun previous(): Period = when (this) {
        is Month -> Month(month.previous())
        is Week -> Week(monday.minus(DatePeriod(days = 7)))
        is Year -> Year(year - 1)
    }

    fun next(): Period = when (this) {
        is Month -> Month(month.next())
        is Week -> Week(monday.plus(DatePeriod(days = 7)))
        is Year -> Year(year + 1)
    }

    companion object {
        fun monthOf(date: LocalDate): Period = Month(date.monthYear())
        fun weekOf(date: LocalDate): Period = Week(date.minus(DatePeriod(days = date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal)))
        fun yearOf(date: LocalDate): Period = Year(date.year)
    }
}

/** Everything the summary screen draws (UC04 + UC05). */
data class SummaryUiState(
    val period: Period,
    /** People who hold payment methods (user and household holders, RN10). */
    val holders: List<Person> = emptyList(),
    val holder: Person? = null,
    val summary: PeriodSummary? = null,
    /** RN03 – what each third party still owes (only those with a balance). */
    val receivables: List<ReceivableBalance> = emptyList(),
    /** UC05 – statements of the credit cards for the selected month (month period only). */
    val statements: List<Statement> = emptyList(),
    val loading: Boolean = true,
)

/** UC04 Period summary and UC05 Statement (MVVM + UDF). Only reads: no events. */
class SummaryViewModel(
    private val summaries: GetSummaryUseCase,
    private val statements: GetStatementUseCase,
    private val registry: ManageRegistryUseCase,
    private val reimbursements: RegisterReimbursementUseCase,
    today: () -> LocalDate = ::today,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SummaryUiState(period = Period.monthOf(today())))
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    private val referenceDate = today()

    init {
        refresh()
    }

    /** Switches between month, week and year, or moves to another period of the same kind. */
    fun selectPeriod(period: Period) {
        _uiState.update { it.copy(period = period) }
        refresh()
    }

    /** Month / week / year of today (the period tabs). */
    fun selectPeriodKind(kind: (LocalDate) -> Period) = selectPeriod(kind(referenceDate))

    /** RN10 – only the expenses of this holder's payment methods (`null` = the whole household). */
    fun filterHolder(holder: Person?) {
        _uiState.update { it.copy(holder = holder) }
        refresh()
    }

    /** Reloads everything for the current period and filter (also called when the tab is shown again). */
    fun refresh() {
        viewModelScope.launch {
            val state = _uiState.value
            val holders = registry.people().filter { it.role != Role.THIRD_PARTY }
            val holder = state.holder?.takeIf { it in holders }
            val summary = when (val period = state.period) {
                is Period.Month -> summaries.ofMonth(period.month, holder)
                is Period.Week -> summaries.ofWeek(period.monday, holder)
                is Period.Year -> summaries.ofYear(period.year, holder)
            }
            val receivables = registry.people().filter { it.role == Role.THIRD_PARTY }
                .map { reimbursements.receivableBalance(it) }
                .filter { it.amount.isPositive() }
            val cardStatements = (state.period as? Period.Month)?.let { month ->
                registry.activePaymentMethods().filterIsInstance<CreditCard>()
                    .filter { holder == null || it.holder == holder }
                    .map { statements.execute(it, month.month) }
            }.orEmpty()
            _uiState.update {
                it.copy(
                    holders = holders, holder = holder, summary = summary,
                    receivables = receivables, statements = cardStatements, loading = false,
                )
            }
        }
    }
}
