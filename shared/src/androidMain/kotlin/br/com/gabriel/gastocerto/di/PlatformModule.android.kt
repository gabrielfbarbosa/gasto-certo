package br.com.gabriel.gastocerto.di

import android.content.Context
import br.com.gabriel.gastocerto.data.room.appDatabaseBuilder
import org.koin.core.module.Module

/** Android: Room. The `Context` comes from Koin's Compose `KoinApplication` (androidContext). */
actual val platformModule: Module = roomDataModule { appDatabaseBuilder(get<Context>()) }
