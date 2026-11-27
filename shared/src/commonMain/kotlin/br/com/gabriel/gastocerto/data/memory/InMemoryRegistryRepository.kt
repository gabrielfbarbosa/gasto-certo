package br.com.gabriel.gastocerto.data.memory

import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.repository.RegistryRepository

class InMemoryRegistryRepository(private val database: InMemoryDatabase) : RegistryRepository {

    override suspend fun people(): List<Person> = database.people.values.sortedBy { it.name }

    override suspend fun institutions(): List<Institution> = database.institutions.values.sortedBy { it.name }

    override suspend fun activePaymentMethods(): List<PaymentMethod> =
        database.paymentMethods.values.filter { it.active }.sortedBy { it.name }.map { it.copy() }

    override suspend fun savePerson(person: Person) {
        database.people = database.people + (person.id to person)
    }

    override suspend fun saveInstitution(institution: Institution) {
        database.institutions = database.institutions + (institution.id to institution)
    }

    override suspend fun savePaymentMethod(paymentMethod: PaymentMethod) {
        database.paymentMethods = database.paymentMethods + (paymentMethod.id to paymentMethod.copy())
    }

    override suspend fun deletePaymentMethod(paymentMethod: PaymentMethod) {
        database.paymentMethods = database.paymentMethods - paymentMethod.id
    }

    override suspend fun hasEntries(paymentMethod: PaymentMethod): Boolean =
        database.entries.values.any { it is Expense && it.paymentMethod == paymentMethod } ||
            database.recurringBills.values.any { it.paymentMethod == paymentMethod }
}
