package br.com.gabriel.gastocerto.ui

import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock

// Display formats. The app tracks a Brazilian household, so amounts are always in reais
// (`R$ 1.234,56`) and dates are `dd/MM/yyyy`, whatever the device language.

/** [value] with [decimals] implied decimal places, Brazilian separators: `123456, 2` → `1.234,56`. */
fun formatFixed(value: Long, decimals: Int): String {
    val absolute = if (value < 0) -value else value
    var scale = 1L
    repeat(decimals) { scale *= 10 }
    val whole = (absolute / scale).toString().reversed().chunked(3).joinToString(".").reversed()
    val fraction = if (decimals == 0) "" else "," + (absolute % scale).toString().padStart(decimals, '0')
    return (if (value < 0) "-" else "") + whole + fraction
}

/** Brazilian real format (`R$ 1.234,56`; negative `-R$ 150,00`). */
fun Money.toBrl(): String = if (cents < 0) "-R$ " + formatFixed(-cents, 2) else "R$ " + formatFixed(cents, 2)

/** `27/11/2026`. */
fun LocalDate.toDisplay(): String =
    "${day.toString().padStart(2, '0')}/${month.number.toString().padStart(2, '0')}/$year"

/** `11/2026`. */
fun MonthYear.toDisplay(): String = "${month.toString().padStart(2, '0')}/$year"

/** The digits typed in an amount field as a number with implied decimals (`"1.234,5"` → `12345`); no digit → `null`. */
fun digitsToValue(text: String, maxDigits: Int = 13): Long? {
    val digits = text.filter(Char::isDigit)
    if (digits.isEmpty()) return null
    return digits.trimStart('0').take(maxDigits).ifEmpty { "0" }.toLong()
}

/** Today on the device. */
fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/** The month of a date. */
fun LocalDate.monthYear(): MonthYear = MonthYear(year, month.number)
