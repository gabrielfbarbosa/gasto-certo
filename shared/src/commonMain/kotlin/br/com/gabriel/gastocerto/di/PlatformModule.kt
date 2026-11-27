package br.com.gabriel.gastocerto.di

import org.koin.core.module.Module

/** Repositories and transaction of each platform: Room on Android and iOS, memory on the Web. */
expect val platformModule: Module
