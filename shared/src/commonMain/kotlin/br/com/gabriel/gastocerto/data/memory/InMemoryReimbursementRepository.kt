package br.com.gabriel.gastocerto.data.memory

import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Reimbursement
import br.com.gabriel.gastocerto.domain.repository.ReimbursementRepository

class InMemoryReimbursementRepository(private val database: InMemoryDatabase) : ReimbursementRepository {

    override suspend fun save(reimbursement: Reimbursement) {
        database.reimbursements = database.reimbursements + (reimbursement.id to reimbursement)
    }

    override suspend fun ofThirdParty(thirdParty: Person): List<Reimbursement> =
        database.reimbursements.values.filter { it.thirdParty == thirdParty }.sortedBy { it.date }
}
