package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.RecurringBill
import br.com.gabriel.gastocerto.domain.model.RecurringIncome
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.repository.RegistryRepository
import br.com.gabriel.gastocerto.domain.rule.ExpenseInput
import br.com.gabriel.gastocerto.domain.rule.PurchaseInput
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Names and descriptions of the demo data, in the user's language (the UI reads them from the
 * string resources). Institution names (Nubank, Inter…) are brands and are not translated.
 */
data class DemoLabels(
    val user: String,
    val spouse: String,
    val father: String,
    val spouseCard: String,
    val power: String,
    val water: String,
    val internet: String,
    val salary: String,
    val groceries: String,
    val pharmacy: String,
    val restaurant: String,
    val clothes: String,
    val appSubscription: String,
    val television: String,
    val notebook: String,
)

/**
 * Demo data loaded on the first run (8.18): the payment methods of the original spreadsheet, recurring
 * bills, salary, expenses on behalf of the father (`[PAI]`, RN03), installment purchases (RN02), a dollar
 * expense (RN05) and a reimbursement. Everything goes **through the use cases**, so every rule applies.
 *
 * The previous month is closed through UC03, so the current month starts with its expected entries.
 * Idempotent: when the registry already has people, nothing is done. Dates are relative to [today],
 * so the current month always has data.
 */
class SeedDemoDataUseCase(
    private val registry: RegistryRepository,
    private val manageRegistry: ManageRegistryUseCase,
    private val manageRecurrences: ManageRecurrencesUseCase,
    private val manageInvestments: ManageInvestmentsUseCase,
    private val registerExpense: RegisterExpenseUseCase,
    private val registerPurchase: RegisterInstallmentPurchaseUseCase,
    private val registerReimbursement: RegisterReimbursementUseCase,
    private val closeMonth: CloseMonthUseCase,
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) {
    /** Loads the demo data; returns `false` when the database already had data. */
    suspend fun execute(labels: DemoLabels): Boolean {
        if (registry.people().isNotEmpty()) return false
        val date = today()
        val month = MonthYear(date.year, date.month.number)
        /** Day [day] of the current month, never after today. */
        fun day(day: Int): LocalDate = month.atDay(minOf(day, date.day))

        val user = Person(labels.user, Role.USER)
        val spouse = Person(labels.spouse, Role.HOLDER)
        val father = Person(labels.father, Role.THIRD_PARTY)
        listOf(user, spouse, father).forEach { manageRegistry.savePerson(it) }

        val nubank = Institution("Nubank")
        val inter = Institution("Inter")
        val c6 = Institution("C6")
        val clear = Institution("Clear")
        val wise = Institution("Wise")
        val binance = Institution("Binance")
        listOf(nubank, inter, c6, clear, wise, binance).forEach { manageRegistry.saveInstitution(it) }

        val nubankCard = CreditCard("Nubank", user, nubank, closingDay = 5, dueDay = 12)
        val spouseCard = CreditCard(labels.spouseCard, spouse, nubank, closingDay = 5, dueDay = 12)
        val c6Card = CreditCard("C6", user, c6, closingDay = 25, dueDay = 3)
        val interAccount = CheckingAccount("Inter", user, inter)
        val wiseWallet = ForeignCurrencyWallet("Wise", user, wise, currency = "USD")
        val clearAccount = InvestmentAccount("Clear", user, clear)
        val binanceAccount = InvestmentAccount("Binance", user, binance)
        listOf(nubankCard, spouseCard, c6Card, interAccount, wiseWallet, clearAccount, binanceAccount)
            .forEach { manageRegistry.savePaymentMethod(it) }
        manageInvestments.setPlannedContribution(clearAccount, Money(50_000))

        manageRecurrences.saveRecurringBill(RecurringBill(labels.power, Money(18_000), dueDay = 10, paymentMethod = interAccount))
        manageRecurrences.saveRecurringBill(RecurringBill(labels.water, Money(9_000), dueDay = 15, paymentMethod = interAccount))
        manageRecurrences.saveRecurringBill(RecurringBill(labels.internet, Money(12_000), dueDay = 20, paymentMethod = nubankCard))
        manageRecurrences.saveRecurringIncome(RecurringIncome(labels.salary, Money(800_000), payDay = 5))

        registerExpense.execute(ExpenseInput(nubankCard, Money(35_000), day(3), labels.groceries))
        registerExpense.execute(ExpenseInput(nubankCard, Money(12_000), day(4), labels.pharmacy, thirdParty = father))
        registerExpense.execute(ExpenseInput(interAccount, Money(8_000), day(6), labels.restaurant))
        registerExpense.execute(ExpenseInput(spouseCard, Money(20_000), day(7), labels.clothes))
        registerExpense.execute(
            ExpenseInput(wiseWallet, amount = null, day(8), labels.appSubscription, foreignAmount = 1_250, exchangeRate = 54_321),
        )
        registerPurchase.execute(PurchaseInput(nubankCard, Money(240_000), 6, date.minus(DatePeriod(months = 1)), labels.television))
        registerPurchase.execute(PurchaseInput(c6Card, Money(120_000), 3, day(2), labels.notebook, thirdParty = father))
        registerReimbursement.execute(father, Money(5_000), date)
        // As in real use: last month was closed (UC03), which opened this month with its EXPECTED
        // salary, recurring bills and planned contribution (RN06, RN07, RN11) to be checked.
        // Last, so nothing above is registered in a closed month (RN08).
        closeMonth.execute(month.previous())
        return true
    }
}
