package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.AccountingMonthClosed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccountingMonthTest {

    private val november = MonthYear(2026, 11)

    @Test
    fun `RN08 - a new accounting month starts open and accepts entries`() {
        val month = AccountingMonth(november)
        assertEquals(AccountingMonthStatus.OPEN, month.status)
        month.ensureOpen() // does not throw
    }

    @Test
    fun `RN08 - closing makes the month closed`() {
        val month = AccountingMonth(november)
        month.close()
        assertEquals(AccountingMonthStatus.CLOSED, month.status)
    }

    @Test
    fun `RN08 - a closed month accepts no entries`() {
        val month = AccountingMonth(november, AccountingMonthStatus.CLOSED)
        val error = assertFailsWith<AccountingMonthClosed> { month.ensureOpen() }
        assertEquals("RN08", error.rule)
    }

    @Test
    fun `RN08 - closing a month that is already closed is rejected`() {
        val month = AccountingMonth(november)
        month.close()
        assertFailsWith<AccountingMonthClosed> { month.close() }
    }

    @Test
    fun `RN08 - reopening accepts entries again`() {
        val month = AccountingMonth(november, AccountingMonthStatus.CLOSED)
        month.reopen()
        assertEquals(AccountingMonthStatus.OPEN, month.status)
        month.ensureOpen()
    }

    @Test
    fun `there is one accounting month per month - equality by month`() {
        assertEquals(AccountingMonth(november), AccountingMonth(MonthYear(2026, 11), AccountingMonthStatus.CLOSED))
    }
}
