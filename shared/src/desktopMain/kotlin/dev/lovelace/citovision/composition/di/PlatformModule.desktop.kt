package dev.lovelace.citovision.composition.di

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.infrastructure.auth.StubAuthService
import dev.lovelace.citovision.infrastructure.auth.StubGoogleSignInLauncher
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::StubAuthService) bind AuthService::class
    singleOf(::StubGoogleSignInLauncher) bind GoogleSignInLauncher::class
}
