package br.com.gabriel.gastocerto.data.memory

import br.com.gabriel.gastocerto.domain.repository.Repositories
import br.com.gabriel.gastocerto.domain.repository.RepositoriesContract

/** The in-memory repositories (Web and tests) meet the contract. */
class InMemoryRepositoriesTest : RepositoriesContract() {
    override fun newRepositories(): Repositories {
        val database = InMemoryDatabase()
        return Repositories(
            entries = InMemoryEntryRepository(database),
            accountingMonths = InMemoryAccountingMonthRepository(database),
            registry = InMemoryRegistryRepository(database),
            recurrences = InMemoryRecurrenceRepository(database),
            reimbursements = InMemoryReimbursementRepository(database),
            transaction = InMemoryTransaction(database),
        )
    }
}
