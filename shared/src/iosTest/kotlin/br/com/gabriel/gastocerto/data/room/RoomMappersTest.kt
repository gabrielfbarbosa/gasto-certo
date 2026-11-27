package br.com.gabriel.gastocerto.data.room

import br.com.gabriel.gastocerto.domain.model.AccountingMonth
import br.com.gabriel.gastocerto.domain.model.AccountingMonthStatus
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.CurrencyConversion
import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.InstallmentPurchase
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.model.Reimbursement
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/** Entity ⇄ domain round trips: every field comes back, including the subclass-specific ones. */
class RoomMappersTest {
    private val gabriel = Person("Gabriel", Role.HOLDER)
    private val father = Person("Father", Role.THIRD_PARTY)
    private val nubankInst = Institution("Nubank")
    private val nubank = CreditCard("Nubank", gabriel, nubankInst, closingDay = 5, dueDay = 12)
    private val inter = CheckingAccount("Inter", gabriel, Institution("Inter"), active = false)
    private val november = MonthYear(2026, 11)

    @Test
    fun `month and date columns keep their values`() {
        assertEquals(202611, november.toKey())
        assertEquals(november, 202611.toMonthYear())
        assertEquals(MonthYear(2027, 1), MonthYear(2027, 1).toKey().toMonthYear())
        assertEquals(LocalDate(2028, 2, 29), LocalDate(2028, 2, 29).toColumn().toLocalDate())
    }

    @Test
    fun `person and institution round trip`() {
        val person = gabriel.toEntity().toDomain()
        assertEquals(gabriel, person)
        assertEquals("Gabriel", person.name)
        assertEquals(Role.HOLDER, person.role)
        assertEquals("Nubank", nubankInst.toEntity().toDomain().name)
    }

    @Test
    fun `the four kinds of payment method round trip with their own fields`() {
        val wise = ForeignCurrencyWallet("Wise", gabriel, Institution("Wise"), "USD")
        val clear = InvestmentAccount("Clear", gabriel, Institution("Clear"), Money(50_000))
        val binance = InvestmentAccount("Binance", gabriel, Institution("Binance"))

        val card = assertIs<CreditCard>(nubank.toEntity().toDomain(gabriel, nubankInst))
        assertEquals(5, card.closingDay)
        assertEquals(12, card.dueDay)
        assertEquals(false, inter.toEntity().toDomain(gabriel, inter.institution).active)
        assertEquals("USD", assertIs<ForeignCurrencyWallet>(wise.toEntity().toDomain(gabriel, wise.institution)).currency)
        assertEquals(Money(50_000), assertIs<InvestmentAccount>(clear.toEntity().toDomain(gabriel, clear.institution)).plannedMonthlyContribution)
        assertEquals(null, assertIs<InvestmentAccount>(binance.toEntity().toDomain(gabriel, binance.institution)).plannedMonthlyContribution)
    }

    @Test
    fun `RN02 - an installment comes back with its label and purchase`() {
        val installment = InstallmentPurchase(Money(10_000), 3, LocalDate(2026, 11, 3), "TV", nubank, father)
            .generateInstallments()[1]

        val read = assertIs<Installment>(installment.toEntity().toDomain(nubank, father))

        assertEquals(installment.id, read.id)
        assertEquals("2/3", read.label())
        assertEquals(installment.purchaseId, read.purchaseId)
        assertEquals(installment.amount, read.amount)
        assertEquals(installment.date, read.date)
        assertEquals(installment.accountingMonth, read.accountingMonth)
        assertEquals(father, read.thirdParty)
    }

    @Test
    fun `RN05 - an expense keeps its conversion status and origin`() {
        val expense = Expense(
            Money(6_790), LocalDate(2026, 11, 10), "App", november, inter,
            conversion = CurrencyConversion(1_250, 54_321), status = EntryStatus.EXPECTED, originId = "bill-1",
        )

        val read = assertIs<Expense>(expense.toEntity().toDomain(inter, null))

        assertEquals(CurrencyConversion(1_250, 54_321), read.conversion)
        assertEquals(EntryStatus.EXPECTED, read.status)
        assertEquals("bill-1", read.originId)
        assertEquals(null, read.thirdParty)
        assertEquals("App", read.description)
    }

    @Test
    fun `an income comes back with its type`() {
        val income = Income(Money(800_000), LocalDate(2026, 11, 5), "Salary", november, IncomeType.SALARY)

        val read = assertIs<Income>(income.toEntity().toDomain(null, null))

        assertEquals(IncomeType.SALARY, read.type)
        assertEquals(EntryStatus.ACTUAL, read.status)
        assertEquals(Money(800_000), read.amount)
    }

    @Test
    fun `accounting month recurrences and reimbursement round trip`() {
        val closed = AccountingMonth(november, AccountingMonthStatus.CLOSED).toEntity().toDomain()
        assertEquals(november, closed.monthYear)
        assertEquals(AccountingMonthStatus.CLOSED, closed.status)

        val bill = RecurringBill("Power", Money(20_000), 31, inter, active = false).let { it.toEntity().toDomain(inter) }
        assertEquals(31, bill.dueDay)
        assertEquals(false, bill.active)
        assertEquals(Money(20_000), bill.expectedAmount)

        val salary = RecurringIncome("Salary", Money(800_000), 5).toEntity().toDomain()
        assertEquals(5, salary.payDay)

        val reimbursement = Reimbursement(father, Money(5_000), LocalDate(2026, 11, 20))
        val read = reimbursement.toEntity().toDomain(father)
        assertEquals(reimbursement.id, read.id)
        assertEquals(LocalDate(2026, 11, 20), read.date)
    }

    @Test
    fun `RN09 - a corrupted row is rejected by the domain rules when read`() {
        val row = Income(Money(1), LocalDate(2026, 11, 5), "x", november, IncomeType.ONE_OFF).toEntity().copy(amountCents = 0)

        assertFailsWith<InvalidAmount> { row.toDomain(null, null) }
    }
}
