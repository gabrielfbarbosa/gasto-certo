package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.ReceivableBalance
import br.com.gabriel.gastocerto.domain.model.Reimbursement
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.repository.ReimbursementRepository
import kotlinx.datetime.LocalDate

/**
 * UC11 – Register a reimbursement from a third party (GRASP **Controller**, RF19). RN03 – the
 * reimbursement lowers the receivable balance, is **not income** and may not exceed the balance.
 */
class RegisterReimbursementUseCase(
    private val entries: EntryRepository,
    private val reimbursements: ReimbursementRepository,
) {
    /** RF19 – how much [thirdParty] still owes. */
    suspend fun receivableBalance(thirdParty: Person): ReceivableBalance =
        ReceivableBalance.of(thirdParty, entries.ofThirdParty(thirdParty), reimbursements.ofThirdParty(thirdParty))

    /** RN09 amount > 0 (`InvalidAmount`); RN03 amount ≤ balance (`ReimbursementExceedsBalance`). */
    suspend fun execute(thirdParty: Person, amount: Money, date: LocalDate): Reimbursement {
        val reimbursement = Reimbursement(thirdParty, amount, date)
        receivableBalance(thirdParty).validateReimbursement(amount)
        reimbursements.save(reimbursement)
        return reimbursement
    }
}
