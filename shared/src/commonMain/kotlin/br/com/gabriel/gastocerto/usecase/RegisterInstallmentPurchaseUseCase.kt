package br.com.gabriel.gastocerto.usecase

import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.InstallmentPurchase
import br.com.gabriel.gastocerto.domain.repository.AccountingMonthRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.domain.rule.EntryFactory
import br.com.gabriel.gastocerto.domain.rule.PurchaseInput

/**
 * UC02 – Register installment purchase (GRASP **Controller**), in the call order of
 * `sequencia-uc02-1-previa` and `sequencia-uc02-2-confirmacao`.
 */
class RegisterInstallmentPurchaseUseCase(
    private val factory: EntryFactory,
    private val accountingMonths: AccountingMonthRepository,
    private val entries: EntryRepository,
) {
    /**
     * Part 1 – preview `k/N – amount – statement` before confirming. Nothing is saved.
     * The factory validates (`InvalidAmount`, `InstallmentsOutOfRange`) and the purchase generates the
     * installments (RN01 accounting month of the 1st, RN02 split with the remainder in the 1st).
     */
    suspend fun preview(input: PurchaseInput): List<Installment> =
        factory.installmentPurchase(input).generateInstallments()

    /**
     * Part 2 – repeats Part 1, ensures the accounting month of the **1st installment** is open (RN08,
     * `AccountingMonthClosed`) and saves all installments (each one keeps `purchaseId`, D5).
     */
    suspend fun execute(input: PurchaseInput): InstallmentPurchase {
        val purchase = factory.installmentPurchase(input)
        val installments = purchase.generateInstallments()
        accountingMonths.findOrCreate(installments.first().accountingMonth).ensureOpen()
        entries.saveAll(installments)
        return purchase
    }
}
