package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.Institution
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.repository.RegistryRepository
import br.com.gabriel.gastocerto.domain.rule.PaymentMethodHasEntries

/**
 * UC06 – Manage people, institutions and payment methods (GRASP **Controller**, RF01–RF04).
 * The entities validate themselves on creation (RN09 days, RN10 holder); this class persists them and
 * applies RN09: a payment method with entries is never deleted, only deactivated.
 */
class ManageRegistryUseCase(private val registry: RegistryRepository) {

    /** Registered people, by name (screens pick holders and third parties from here). */
    suspend fun people(): List<Person> = registry.people()

    suspend fun institutions(): List<Institution> = registry.institutions()

    /** Payment methods that accept new entries (RN09: deactivated ones are hidden). */
    suspend fun activePaymentMethods(): List<PaymentMethod> = registry.activePaymentMethods()

    suspend fun savePerson(person: Person) = registry.savePerson(person)

    suspend fun saveInstitution(institution: Institution) = registry.saveInstitution(institution)

    suspend fun savePaymentMethod(paymentMethod: PaymentMethod) = registry.savePaymentMethod(paymentMethod)

    /** RN09 – keeps the payment method and its history, but hides it from new entries. */
    suspend fun deactivatePaymentMethod(paymentMethod: PaymentMethod) {
        paymentMethod.active = false
        registry.savePaymentMethod(paymentMethod)
    }

    /** RN09 – deletes only a payment method without entries; otherwise throws [PaymentMethodHasEntries]. */
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethod) {
        if (registry.hasEntries(paymentMethod)) throw PaymentMethodHasEntries(paymentMethod.name)
        registry.deletePaymentMethod(paymentMethod)
    }
}
