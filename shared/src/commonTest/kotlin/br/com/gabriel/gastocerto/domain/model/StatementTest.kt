package br.com.gabriel.gastocerto.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StatementTest {

    private val gabriel = Person("Gabriel", Role.USER)
    // closes on 5, due on 12 (same month)
    private val nubank = CreditCard("Nubank", gabriel, Institution("Nubank"), closingDay = 5, dueDay = 12)
    // closes on 25, due on 2 (next month)
    private val c6 = CreditCard("C6", gabriel, Institution("C6"), closingDay = 25, dueDay = 2)
    private val november = MonthYear(2026, 11)

    private fun expense(amount: Long, card: CreditCard, month: MonthYear) =
        Expense(Money(amount), LocalDate(2026, 11, 1), "expense", month, card)

    @Test
    fun `RF13 - a statement adds only the card's expenses in the accounting month`() {
        val expenses = listOf(
            expense(10_000, nubank, november),
            expense(5_050, nubank, november),
            expense(7_000, nubank, MonthYear(2026, 12)),
            expense(9_000, c6, november),
        )

        val statement = Statement.of(nubank, november, expenses)

        assertEquals(2, statement.expenses.size)
        assertEquals(Money(15_050), statement.total())
    }

    @Test
    fun `RF13 and RN01 - due day after closing day closes and is due in the same month`() {
        val statement = Statement.of(nubank, november, emptyList())
        assertEquals(LocalDate(2026, 11, 5), statement.closingDate)
        assertEquals(LocalDate(2026, 11, 12), statement.dueDate)
    }

    @Test
    fun `RF13 and RN01 - due day before closing day closes in the previous month`() {
        val statement = Statement.of(c6, MonthYear(2027, 1), emptyList())
        assertEquals(LocalDate(2026, 12, 25), statement.closingDate)
        assertEquals(LocalDate(2027, 1, 2), statement.dueDate)
    }

    @Test
    fun `RF13 - statement with installments of an installment purchase`() {
        val installments = InstallmentPurchase(Money(10_000), 3, LocalDate(2026, 11, 3), "TV", nubank).generateInstallments()

        val december = Statement.of(nubank, MonthYear(2026, 12), installments)

        assertEquals(listOf("2/3"), december.expenses.filterIsInstance<Installment>().map { it.label() })
        assertEquals(Money(3_333), december.total())
    }

    @Test
    fun `RF13 - a statement with no expenses has zero total`() {
        assertEquals(Money(0), Statement.of(nubank, november, emptyList()).total())
    }
}
