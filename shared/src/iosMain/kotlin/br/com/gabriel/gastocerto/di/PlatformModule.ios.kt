package br.com.gabriel.gastocerto.di

import br.com.gabriel.gastocerto.data.room.appDatabaseBuilder
import org.koin.core.module.Module

/** iOS: Room, database file in the app's Documents folder. */
actual val platformModule: Module = roomDataModule { appDatabaseBuilder() }
