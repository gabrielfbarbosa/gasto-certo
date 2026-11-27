package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.data.memory.InMemoryDatabase
import br.com.gabriel.gastocerto.data.memory.InMemoryEntryRepository
import br.com.gabriel.gastocerto.data.memory.InMemoryRegistryRepository
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.domain.rule.PaymentMethodHasEntries
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ManageRegistryUseCaseTest {

    private val database = InMemoryDatabase()
    private val registry = InMemoryRegistryRepository(database)
    private val entries = InMemoryEntryRepository(database)
    private val useCase = ManageRegistryUseCase(registry)

    private val gabriel = Person("Gabriel", Role.USER)
    private val nubankInst = Institution("Nubank")
    private val nubank = CreditCard("Nubank", gabriel, nubankInst, 5, 12)
    private val inter = CheckingAccount("Inter", gabriel, Institution("Inter"))

    @Test
    fun `UC06 - people institutions and payment methods are saved`() = runTest {
        useCase.savePerson(gabriel)
        useCase.saveInstitution(nubankInst)
        useCase.savePaymentMethod(nubank)

        assertEquals(listOf(gabriel), registry.people())
        assertEquals(listOf(nubankInst), registry.institutions())
        assertEquals(listOf<Any>(nubank), registry.activePaymentMethods())
    }

    @Test
    fun `RN09 - a deactivated payment method leaves the active list but keeps its entries`() = runTest {
        useCase.savePaymentMethod(inter)
        entries.save(Expense(Money(1_000), LocalDate(2026, 11, 10), "x", MonthYear(2026, 11), inter))

        useCase.deactivatePaymentMethod(inter)

        assertTrue(registry.activePaymentMethods().isEmpty())
        assertEquals(1, entries.ofMonth(MonthYear(2026, 11)).size)
    }

    @Test
    fun `RN09 - a payment method without entries can be deleted`() = runTest {
        useCase.savePaymentMethod(nubank)
        useCase.deletePaymentMethod(nubank)
        assertTrue(registry.activePaymentMethods().isEmpty())
    }

    @Test
    fun `RN09 - deleting a payment method with entries is rejected`() = runTest {
        useCase.savePaymentMethod(inter)
        entries.save(Expense(Money(1_000), LocalDate(2026, 11, 10), "x", MonthYear(2026, 11), inter))

        val error = assertFailsWith<PaymentMethodHasEntries> { useCase.deletePaymentMethod(inter) }

        assertEquals("Inter", error.name)
        assertEquals(listOf<Any>(inter), registry.activePaymentMethods())
    }
}
