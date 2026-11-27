package br.com.gabriel.gastocerto.data.room

import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Person

/**
 * People, institutions and payment methods read once, so the rows that point to them
 * (entries, recurring bills, reimbursements) can be rebuilt as domain objects.
 * A household has few registry records, so reading them all is cheap.
 */
internal class RegistrySnapshot(
    val people: Map<String, Person>,
    val paymentMethods: Map<String, PaymentMethod>,
) {
    fun person(id: String): Person = checkNotNull(people[id]) { "person $id not found" }

    fun paymentMethod(id: String): PaymentMethod = checkNotNull(paymentMethods[id]) { "payment method $id not found" }

    fun entry(row: EntryEntity): Entry =
        row.toDomain(row.paymentMethodId?.let(::paymentMethod), row.thirdPartyId?.let(::person))
}

internal suspend fun AppDatabase.registrySnapshot(): RegistrySnapshot {
    val people = personDao().all().map { it.toDomain() }.associateBy { it.id }
    val institutions = institutionDao().all().map { it.toDomain() }.associateBy { it.id }
    val paymentMethods = paymentMethodDao().all().associate { row ->
        row.id to row.toDomain(
            holder = checkNotNull(people[row.holderId]) { "person ${row.holderId} not found" },
            institution = checkNotNull(institutions[row.institutionId]) { "institution ${row.institutionId} not found" },
        )
    }
    return RegistrySnapshot(people, paymentMethods)
}

