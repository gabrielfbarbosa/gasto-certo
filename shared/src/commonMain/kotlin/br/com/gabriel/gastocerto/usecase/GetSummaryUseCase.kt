package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.PeriodSummary
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * UC04 – Period summary (GRASP **Controller**, RF14, RF15). The totals are computed by
 * [PeriodSummary.of] (RN03, RN04, RN10); this class only picks the entries of the period.
 */
class GetSummaryUseCase(private val entries: EntryRepository) {

    /** RF14 – the month by **accounting month** (card expenses count in the statement month, RN01); optional [holder] filter (RN10). */
    suspend fun ofMonth(month: MonthYear, holder: Person? = null): PeriodSummary =
        PeriodSummary.of(entries.ofMonth(month), holder)

    /** RF15 – the week (**Monday to Sunday**) that contains [date], by entry date; optional [holder] filter (RN10). */
    suspend fun ofWeek(date: LocalDate, holder: Person? = null): PeriodSummary {
        val monday = date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        val sunday = monday.plus(6, DateTimeUnit.DAY)
        return PeriodSummary.of(entries.ofPeriod(monday, sunday), holder)
    }

    /** RF15 – the calendar [year] (January 1 to December 31), by entry date; optional [holder] filter (RN10). */
    suspend fun ofYear(year: Int, holder: Person? = null): PeriodSummary =
        PeriodSummary.of(entries.ofPeriod(LocalDate(year, 1, 1), LocalDate(year, 12, 31)), holder)
}
