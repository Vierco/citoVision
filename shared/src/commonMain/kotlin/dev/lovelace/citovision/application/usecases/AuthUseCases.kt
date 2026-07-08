package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.core.result.fold
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

/**
 * Orquesta el login con Google: resuelve el `idToken` con el flujo nativo ([GoogleSignInLauncher])
 * y lo canjea en Firebase ([AuthService.signInWithGoogle]). Un fallo del flujo nativo (cancelado,
 * no soportado) se propaga tal cual sin llamar al backend.
 */
class SignInWithGoogleUseCase(
    private val authService: AuthService,
    private val googleSignInLauncher: GoogleSignInLauncher,
) {
    suspend operator fun invoke(): Result<AuthUser, AuthError> =
        googleSignInLauncher.requestIdToken().fold(
            onSuccess = { idToken -> authService.signInWithGoogle(idToken) },
            onFailure = { error -> Result.Failure(error) },
        )
}

class SignInAsGuestUseCase(private val authService: AuthService) {
    suspend operator fun invoke(): Result<AuthUser, AuthError> =
        authService.signInAsGuest()
}

class SignOutUseCase(private val authService: AuthService) {
    suspend operator fun invoke(): Result<Unit, AuthError> =
        authService.signOut()
}

class SendPasswordResetUseCase(private val authService: AuthService) {
    suspend operator fun invoke(email: String): Result<Unit, AuthError> =
        authService.sendPasswordReset(email)
}
