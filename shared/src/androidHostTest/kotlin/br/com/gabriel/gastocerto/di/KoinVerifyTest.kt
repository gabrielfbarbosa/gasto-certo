package br.com.gabriel.gastocerto.di

import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

/** Koin's static check (JVM only): every constructor dependency of every definition is declared. */
class KoinVerifyTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `the app graph declares every dependency`() {
        // Function0 = the `today` clock of ManageRecurrencesUseCase, which keeps its default value.
        module { includes(appModule, inMemoryDataModule) }.verify(extraTypes = listOf(Function0::class))
    }
}
