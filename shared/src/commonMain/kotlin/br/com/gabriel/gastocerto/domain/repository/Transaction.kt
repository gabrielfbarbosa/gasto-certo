package br.com.gabriel.gastocerto.domain.repository

/**
 * Runs a block of writes as **all or nothing**: if the block throws, nothing it saved
 * remains (used when closing the month, UC03). Suspending because Room KMP only has
 * suspending transactions (D4).
 */
interface Transaction {
    suspend fun <T> execute(block: suspend () -> T): T
}
