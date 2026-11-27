package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryReimbursementRepository
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.ReimbursementExceedsBalance
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegisterReimbursementUseCaseTest {

    private val database = InMemoryDatabase()
    private val entries = InMemoryEntryRepository(database)
    private val reimbursements = InMemoryReimbursementRepository(database)
    private val useCase = RegisterReimbursementUseCase(entries, reimbursements)

    private val father = Person("Father", Role.THIRD_PARTY)
    private val inter = CheckingAccount("Inter", Person("Gabriel", Role.USER), Institution("Inter"))
    private val day = LocalDate(2026, 11, 20)

    private suspend fun givenFatherOwes(amount: Long) =
        entries.save(Expense(Money(amount), LocalDate(2026, 11, 10), "Medicine", MonthYear(2026, 11), inter, father))

    @Test
    fun `RN03 - a reimbursement lowers the receivable balance`() = runTest {
        givenFatherOwes(50_000)

        useCase.execute(father, Money(20_000), day)

        assertEquals(Money(30_000), useCase.receivableBalance(father).amount)
        assertEquals(1, reimbursements.ofThirdParty(father).size)
    }

    @Test
    fun `RN03 - a reimbursement is not income`() = runTest {
        givenFatherOwes(50_000)
        useCase.execute(father, Money(50_000), day)
        assertTrue(entries.ofMonth(MonthYear(2026, 11)).none { it !is Expense })
    }

    @Test
    fun `RN03 - a reimbursement larger than the balance is rejected and nothing is saved`() = runTest {
        givenFatherOwes(50_000)

        assertFailsWith<ReimbursementExceedsBalance> { useCase.execute(father, Money(50_001), day) }
        assertTrue(reimbursements.ofThirdParty(father).isEmpty())
    }

    @Test
    fun `RN09 - a zero reimbursement is rejected`() = runTest {
        givenFatherOwes(50_000)
        assertFailsWith<InvalidAmount> { useCase.execute(father, Money(0), day) }
    }
}
