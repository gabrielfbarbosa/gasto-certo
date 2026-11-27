package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Statement
import br.com.gabriel.gastocerto.domain.repository.EntryRepository

/**
 * UC05 – Card statement (GRASP **Controller**, RF13). The statement is not stored: it is assembled by
 * [Statement.of] from the card and the expenses of its due month (RN01, RN02).
 */
class GetStatementUseCase(private val entries: EntryRepository) {

    suspend fun execute(card: CreditCard, month: MonthYear): Statement =
        Statement.of(card, month, entries.ofMonth(month).filterIsInstance<Expense>())
}
