package br.com.gabriel.gastocerto.di

import androidx.room.Room
import br.com.gabriel.gastocerto.data.room.AppDatabase
import br.com.gabriel.gastocerto.data.room.RoomEntryRepository
import br.com.gabriel.gastocerto.domain.repository.EntryRepository
import br.com.gabriel.gastocerto.usecase.CloseMonthUseCase
import br.com.gabriel.gastocerto.usecase.GetSummaryUseCase
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertIs

/** The mobile graph: the app module over the Room module (in-memory database for the test). */
class RoomKoinGraphTest {
    @Test
    fun `the use cases resolve over the Room repositories`() {
        val app = koinApplication {
            modules(appModule, roomDataModule { Room.inMemoryDatabaseBuilder<AppDatabase>() })
        }
        assertIs<RoomEntryRepository>(app.koin.get<EntryRepository>())
        app.koin.get<CloseMonthUseCase>()
        app.koin.get<GetSummaryUseCase>()
        app.koin.get<AppDatabase>().close()
        app.close()
    }
}
