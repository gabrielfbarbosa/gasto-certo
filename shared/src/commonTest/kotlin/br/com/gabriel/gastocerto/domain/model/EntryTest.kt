package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.InvalidExchangeRate
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntryTest {

    private val gabriel = Person("Gabriel", Role.USER)
    private val father = Person("Father", Role.THIRD_PARTY)
    private val inter = CheckingAccount("Inter", gabriel, Institution("Inter"))
    private val november = MonthYear(2026, 11)

    private fun expense(amount: Long, thirdParty: Person? = null, status: EntryStatus = EntryStatus.ACTUAL) =
        Expense(Money(amount), LocalDate(2026, 11, 10), "Groceries", november, inter, thirdParty, status = status)

    // ---------- RN09: positive amount ----------

    @Test
    fun `RN09 - expense and income with a positive amount are created`() {
        assertEquals(Money(15_000), expense(15_000).amount)
        val income = Income(Money(500_000), LocalDate(2026, 11, 5), "Salary", november, IncomeType.SALARY)
        assertEquals(IncomeType.SALARY, income.type)
    }

    @Test
    fun `RN09 - entry with zero or negative amount is rejected`() {
        assertFailsWith<InvalidAmount> { expense(0) }
        assertFailsWith<InvalidAmount> {
            Income(Money(-1), LocalDate(2026, 11, 5), "Extra", november, IncomeType.ONE_OFF)
        }
    }

    // ---------- RN03: third-party expense ----------

    @Test
    fun `RN03 - an expense on behalf of a third party is a third-party expense`() {
        assertTrue(expense(5_000, thirdParty = father).isThirdParty())
        assertFalse(expense(5_000).isThirdParty())
    }

    // ---------- confirming expected entries (RN06, RN07) ----------

    @Test
    fun `RN06 - confirming an expected entry changes the amount and makes it actual`() {
        val power = expense(20_000, status = EntryStatus.EXPECTED)

        power.confirm(Money(18_750))

        assertEquals(Money(18_750), power.amount)
        assertEquals(EntryStatus.ACTUAL, power.status)
    }

    @Test
    fun `RN09 - confirming an expected entry with zero is rejected and nothing changes`() {
        val power = expense(20_000, status = EntryStatus.EXPECTED)

        assertFailsWith<InvalidAmount> { power.confirm(Money(0)) }
        assertEquals(Money(20_000), power.amount)
        assertEquals(EntryStatus.EXPECTED, power.status)
    }

    @Test
    fun `confirming an entry that is already actual is rejected`() {
        assertFailsWith<IllegalStateException> { expense(20_000).confirm(Money(100)) }
    }

    // ---------- RN05: currency conversion (D1) ----------

    @Test
    fun `RN05 - currency conversion computes the amount in reais`() {
        // US$ 12.50 × 5.4321 = R$ 67.90
        assertEquals(Money(6_790), CurrencyConversion(originalAmount = 1_250, exchangeRate = 54_321).toBrl())
    }

    @Test
    fun `RN05 - zero or negative exchange rate is rejected`() {
        assertFailsWith<InvalidExchangeRate> { CurrencyConversion(originalAmount = 1_250, exchangeRate = 0) }
        assertFailsWith<InvalidExchangeRate> { CurrencyConversion(originalAmount = 1_250, exchangeRate = -1) }
    }

    @Test
    fun `RN05 - zero amount in the foreign currency is rejected`() {
        assertFailsWith<InvalidAmount> { CurrencyConversion(originalAmount = 0, exchangeRate = 54_321) }
    }

    // ---------- D2 ----------

    @Test
    fun `D2 - entries are equal by id`() {
        val a = Expense(Money(100), LocalDate(2026, 11, 1), "A", november, inter, id = "l1")
        val b = Expense(Money(999), LocalDate(2026, 11, 2), "B", november, inter, id = "l1")
        assertEquals<Entry>(a, b)
    }
}
