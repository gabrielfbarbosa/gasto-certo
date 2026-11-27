package br.com.gabriel.gastocerto.domain.repository

import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Reimbursement

/** Persistence port for reimbursements received from third parties (RN03). */
interface ReimbursementRepository {
    suspend fun save(reimbursement: Reimbursement)
    suspend fun ofThirdParty(thirdParty: Person): List<Reimbursement>
}
