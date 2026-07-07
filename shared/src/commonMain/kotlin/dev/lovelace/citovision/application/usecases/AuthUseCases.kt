package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.errors.AuthError

/**
 * Casos de uso de autenticación. El ViewModel invoca estos use cases, nunca el puerto directamente
 * (ver Skill mvvm-compose-kmp). La validación de formato de campos vive en Presentation.
 */
class SignInWithEmailUseCase(private val authService: AuthService) {
    suspend operator fun invoke(email: String, password: String): Result<AuthUser, AuthError> =
        authService.signInWithEmail(email, password)
}

class SignInWithGoogleUseCase(private val authService: AuthService) {
    suspend operator fun invoke(idToken: String): Result<AuthUser, AuthError> =
        authService.signInWithGoogle(idToken)
}

class SignInAsGuestUseCase(private val authService: AuthService) {
    suspend operator fun invoke(): Result<AuthUser, AuthError> =
        authService.signInAsGuest()
}

class SignOutUseCase(private val authService: AuthService) {
    suspend operator fun invoke(): Result<Unit, AuthError> =
        authService.signOut()
}
