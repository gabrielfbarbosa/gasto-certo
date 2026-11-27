package br.com.gabriel.gastocerto.domain.rule

import br.com.gabriel.gastocerto.domain.model.MonthYear
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccountingMonthPolicyTest {

    // Card closing on day 5 and due on day 12: due in the same month as the closing.
    private val card = StatementClosingPolicy(closingDay = 5, dueDay = 12)

    // ---------- RN01: credit card ----------

    @Test
    fun `RN01 - a purchase before closing goes into the statement due in the same month`() {
        assertEquals(MonthYear(2026, 11), card.accountingMonthOf(LocalDate(2026, 11, 3)))
    }

    @Test
    fun `RN01 - a purchase on the closing day itself still goes into the current statement`() {
        assertEquals(MonthYear(2026, 11), card.accountingMonthOf(LocalDate(2026, 11, 5)))
    }

    @Test
    fun `RN01 - a purchase the day after closing goes into the next statement`() {
        assertEquals(MonthYear(2026, 12), card.accountingMonthOf(LocalDate(2026, 11, 6)))
    }

    @Test
    fun `RN01 - due day before closing day is due the month after closing`() {
        // closes on 25, due on 2: purchase on 11/20 closes on 11/25 and is due on 12/02
        val policy = StatementClosingPolicy(closingDay = 25, dueDay = 2)
        assertEquals(MonthYear(2026, 12), policy.accountingMonthOf(LocalDate(2026, 11, 20)))
        // purchase on 11/26 closes on 12/25 and is due on 01/02
        assertEquals(MonthYear(2027, 1), policy.accountingMonthOf(LocalDate(2026, 11, 26)))
    }

    @Test
    fun `RN01 - due day equal to closing day is due the following month`() {
        val policy = StatementClosingPolicy(closingDay = 10, dueDay = 10)
        assertEquals(MonthYear(2026, 12), policy.accountingMonthOf(LocalDate(2026, 11, 8)))
    }

    @Test
    fun `RN01 - a purchase after the December closing goes to January of the next year`() {
        assertEquals(MonthYear(2027, 1), card.accountingMonthOf(LocalDate(2026, 12, 20)))
    }

    @Test
    fun `RN01 - closing day 31 in a 30-day month closes on the last day`() {
        // closes on 31 (in November: 11/30), due on day 8 of the next month
        val policy = StatementClosingPolicy(closingDay = 31, dueDay = 8)
        assertEquals(MonthYear(2026, 12), policy.accountingMonthOf(LocalDate(2026, 11, 30)))
        assertEquals(MonthYear(2027, 1), policy.accountingMonthOf(LocalDate(2026, 12, 1)))
    }

    @Test
    fun `RN01 - closing day 30 in February closes on day 28`() {
        val policy = StatementClosingPolicy(closingDay = 30, dueDay = 7)
        assertEquals(MonthYear(2027, 3), policy.accountingMonthOf(LocalDate(2027, 2, 28)))
        assertEquals(MonthYear(2027, 4), policy.accountingMonthOf(LocalDate(2027, 3, 1)))
    }

    @Test
    fun `RN01 - closing or due day outside 1 to 31 is rejected`() {
        assertFailsWith<IllegalArgumentException> { StatementClosingPolicy(0, 12) }
        assertFailsWith<IllegalArgumentException> { StatementClosingPolicy(5, 32) }
    }

    // ---------- RN01: other payment methods ----------

    @Test
    fun `RN01 - checking account uses the month of the expense date`() {
        val policy = ExpenseDatePolicy()
        assertEquals(MonthYear(2026, 11), policy.accountingMonthOf(LocalDate(2026, 11, 30)))
        assertEquals(MonthYear(2026, 12), policy.accountingMonthOf(LocalDate(2026, 12, 1)))
    }
}
