package br.com.gabriel.gastocerto.domain.rule

import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleViolationExceptionTest {

    @Test
    fun `each exception reports the violated business rule`() {
        val expected = mapOf(
            InvalidAmount(Money(0)) to "RN09",
            InvalidExchangeRate(0) to "RN05",
            InstallmentsOutOfRange(49) to "RN02",
            AccountingMonthClosed(MonthYear(2026, 11)) to "RN08",
            ReimbursementExceedsBalance(Money(5_000), Money(3_000)) to "RN03",
            PaymentMethodHasEntries("Nubank") to "RN09",
            InvalidDay(DayKind.CLOSING, 0) to "RN09",
            InvalidHolder("Father") to "RN10",
        )
        expected.forEach { (exception, rule) -> assertEquals(rule, exception.rule) }
    }

    @Test
    fun `each exception keeps the data the user message needs`() {
        assertEquals(49, InstallmentsOutOfRange(49).installmentCount)
        assertEquals(MonthYear(2026, 11), AccountingMonthClosed(MonthYear(2026, 11)).month)
        assertEquals("Nubank", PaymentMethodHasEntries("Nubank").name)
        assertEquals(Money(3_000), ReimbursementExceedsBalance(Money(5_000), Money(3_000)).balance)
        assertEquals(DayKind.DUE, InvalidDay(DayKind.DUE, 40).kind)
    }

    @Test
    fun `all of them can be caught as RuleViolationException`() {
        val caught = runCatching { throw AccountingMonthClosed(MonthYear(2026, 11)) }.exceptionOrNull()
        assertIs<RuleViolationException>(caught)
        assertEquals("RN08: month 11/2026 is closed; reopen it to change entries", caught.message)
    }
}
