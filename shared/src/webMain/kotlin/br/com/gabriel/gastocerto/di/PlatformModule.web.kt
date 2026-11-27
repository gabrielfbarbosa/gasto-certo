package br.com.gabriel.gastocerto.di

import org.koin.core.module.Module

/** Web (JS and Wasm): Room does not run in the browser, so the repositories live in memory (replaceable persistence). */
actual val platformModule: Module = inMemoryDataModule
