package br.com.gabriel.gastocerto.data.room

import br.com.gabriel.gastocerto.domain.repository.Repositories
import br.com.gabriel.gastocerto.domain.repository.RepositoriesContract
import kotlin.test.AfterTest

/**
 * The Room repositories meet the same contract as the in-memory ones (integration test with the
 * real bundled SQLite). Runs on the iOS simulator: Android host tests have no device and no
 * native SQLite library, so Room is not exercised there.
 */
class RoomRepositoriesTest : RepositoriesContract() {
    private val opened = mutableListOf<AppDatabase>()

    override fun newRepositories(): Repositories = inMemoryAppDatabase().also { opened += it }.repositories()

    @AfterTest
    fun closeDatabases() = opened.forEach { it.close() }
}
