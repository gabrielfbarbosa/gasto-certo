package br.com.gabriel.gastocerto.data.room

import br.com.gabriel.gastocerto.usecase.assertDemoData
import br.com.gabriel.gastocerto.usecase.demoLabelsForTests
import br.com.gabriel.gastocerto.usecase.seedUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 8.18 over Room: the seed saves in an order the foreign keys accept, and stays idempotent. */
class RoomSeedTest {
    private val database = inMemoryAppDatabase()
    private val repositories = database.repositories()

    @AfterTest
    fun close() = database.close()

    @Test
    fun `the demo data loads into Room and a second run does nothing`() = runTest {
        val seed = repositories.seedUseCase(LocalDate(2026, 11, 27))
        assertTrue(seed.execute(demoLabelsForTests))
        assertFalse(seed.execute(demoLabelsForTests))
        repositories.assertDemoData()
    }
}
