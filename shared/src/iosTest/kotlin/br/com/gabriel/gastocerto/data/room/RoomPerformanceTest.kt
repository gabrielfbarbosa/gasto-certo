package br.com.gabriel.gastocerto.data.room

import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.usecase.GetSummaryUseCase
import br.com.gabriel.gastocerto.usecase.givenThousandEntries
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** RNF05 over the real SQLite (Room): 1,000 entries, summary of the month and of the year in at most 1 second. */
class RoomPerformanceTest {
    private val database = inMemoryAppDatabase()
    private val repositories = database.repositories()

    @AfterTest
    fun close() = database.close()

    @Test
    fun `RNF05 - the Room summary over 1000 entries takes at most 1 second`() = runTest {
        repositories.givenThousandEntries()
        val summary = GetSummaryUseCase(repositories.entries)

        val mark = TimeSource.Monotonic.markNow()
        summary.ofMonth(MonthYear(2026, 6))
        summary.ofYear(2026)
        val elapsed = mark.elapsedNow()

        assertTrue(elapsed <= 1.seconds, "summary took $elapsed")
    }
}
