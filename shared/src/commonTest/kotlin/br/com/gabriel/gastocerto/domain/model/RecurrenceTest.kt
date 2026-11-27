package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.InvalidDay
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RecurrenceTest {

    private val gabriel = Person("Gabriel", Role.USER)
    private val inter = CheckingAccount("Inter", gabriel, Institution("Inter"))
    private val november = MonthYear(2026, 11)

    // ---------- RN06: recurring bills ----------

    @Test
    fun `RN06 - a recurring bill generates an expected expense on the due day`() {
        val power = RecurringBill("Power", Money(20_000), dueDay = 15, paymentMethod = inter)

        val expected = power.generateExpected(november)

        assertEquals(LocalDate(2026, 11, 15), expected.date)
        assertEquals(november, expected.accountingMonth)
        assertEquals(Money(20_000), expected.amount)
        assertEquals(EntryStatus.EXPECTED, expected.status)
        assertEquals(inter, expected.paymentMethod)
        assertEquals("Power", expected.description)
    }

    @Test
    fun `RN06 - a due day missing in the month becomes the last day of the month`() {
        val rent = RecurringBill("Rent", Money(150_000), dueDay = 31, paymentMethod = inter)
        assertEquals(LocalDate(2026, 11, 30), rent.generateExpected(november).date)
        assertEquals(LocalDate(2027, 2, 28), rent.generateExpected(MonthYear(2027, 2)).date)
    }

    @Test
    fun `RN06 and D3 - the expected entry keeps its originating recurring bill`() {
        val power = RecurringBill("Power", Money(20_000), 15, inter)
        assertEquals(power.id, power.generateExpected(november).originId)
        assertEquals(power.id, power.generateExpected(november.next()).originId)
    }

    @Test
    fun `RN06 - a confirmed expected entry takes the month's actual amount`() {
        val power = RecurringBill("Power", Money(20_000), 15, inter).generateExpected(november)
        power.confirm(Money(18_437))
        assertEquals(Money(18_437), power.amount)
        assertEquals(EntryStatus.ACTUAL, power.status)
    }

    @Test
    fun `RN06 and RN09 - recurring bill with zero amount or invalid day is rejected`() {
        assertFailsWith<InvalidAmount> { RecurringBill("Power", Money(0), 15, inter) }
        assertFailsWith<InvalidDay> { RecurringBill("Power", Money(20_000), 0, inter) }
        assertFailsWith<InvalidDay> { RecurringBill("Power", Money(20_000), 32, inter) }
    }

    // ---------- RN07: salary ----------

    @Test
    fun `RN07 - salary generates an expected income on the pay day`() {
        val salary = RecurringIncome("Salary", Money(800_000), payDay = 5)

        val expected = salary.generateExpected(november)

        assertEquals(LocalDate(2026, 11, 5), expected.date)
        assertEquals(november, expected.accountingMonth)
        assertEquals(IncomeType.SALARY, expected.type)
        assertEquals(EntryStatus.EXPECTED, expected.status)
        assertEquals(salary.id, expected.originId)
    }

    @Test
    fun `RN07 - pay day 30 in February becomes the last day`() {
        val salary = RecurringIncome("Salary", Money(800_000), payDay = 30)
        assertEquals(LocalDate(2028, 2, 29), salary.generateExpected(MonthYear(2028, 2)).date)
    }

    @Test
    fun `RN07 and RN09 - salary with zero amount or invalid day is rejected`() {
        assertFailsWith<InvalidAmount> { RecurringIncome("Salary", Money(0), 5) }
        assertFailsWith<InvalidDay> { RecurringIncome("Salary", Money(800_000), 40) }
    }

    // ---------- RN11: planned contribution ----------

    @Test
    fun `RN11 - a brokerage with a planned contribution generates an expected contribution in the month`() {
        val clear = InvestmentAccount("Clear", gabriel, Institution("Clear"), plannedMonthlyContribution = Money(50_000))

        val contribution = assertNotNull(clear.generateExpectedContribution(november))

        assertEquals(Money(50_000), contribution.amount)
        assertEquals(LocalDate(2026, 11, 1), contribution.date)
        assertEquals(november, contribution.accountingMonth)
        assertEquals(clear, contribution.paymentMethod)
        assertEquals(EntryStatus.EXPECTED, contribution.status)
        assertEquals(clear.id, contribution.originId)
    }

    @Test
    fun `RN11 - a brokerage without a planned contribution generates no expected entry`() {
        assertNull(InvestmentAccount("Binance", gabriel, Institution("Binance")).generateExpectedContribution(november))
    }
}
