package dev.lovelace.citovision.composition.di

import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.infrastructure.repositories.SessionRepositoryImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Implementaciones de puertos comunes a todas las plataformas. El `DataStore<Preferences>` que
 * consume [SessionRepositoryImpl] lo aporta cada [platformModule] (la ruta es específica de plataforma).
 */
val infrastructureModule = module {
    singleOf(::SessionRepositoryImpl) bind SessionRepository::class
}
