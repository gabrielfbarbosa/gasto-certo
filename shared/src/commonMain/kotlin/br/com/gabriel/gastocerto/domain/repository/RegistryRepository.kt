package br.com.gabriel.gastocerto.domain.repository

import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Person

/** Persistence port for the registry: people, institutions and payment methods (UC06). */
interface RegistryRepository {
    suspend fun people(): List<Person>
    suspend fun institutions(): List<Institution>
    suspend fun activePaymentMethods(): List<PaymentMethod>
    suspend fun savePerson(person: Person)
    suspend fun saveInstitution(institution: Institution)
    suspend fun savePaymentMethod(paymentMethod: PaymentMethod)

    /** Removes a payment method. Callers check [hasEntries] first (RN09). */
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethod)

    /** RN09 – a payment method with entries cannot be deleted, only deactivated. */
    suspend fun hasEntries(paymentMethod: PaymentMethod): Boolean
}
