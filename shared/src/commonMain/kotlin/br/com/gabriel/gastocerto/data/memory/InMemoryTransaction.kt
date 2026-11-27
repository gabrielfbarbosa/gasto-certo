package br.com.gabriel.gastocerto.data.memory

import br.com.gabriel.gastocerto.domain.repository.Transaction

/** All or nothing in memory: takes a snapshot of the [database] and restores it if the block throws. */
class InMemoryTransaction(private val database: InMemoryDatabase) : Transaction {

    override suspend fun <T> execute(block: suspend () -> T): T {
        val snapshot = database.snapshot()
        try {
            return block()
        } catch (error: Throwable) {
            database.restore(snapshot)
            throw error
        }
    }
}
