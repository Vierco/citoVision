package dev.lovelace.citovision.presentation.events

/** Acciones de la pantalla de Ajustes. */
sealed interface SettingsUiEvent {
    /** Ir a la pantalla de login para iniciar sesión con una cuenta (p. ej. desde sesión de invitado). */
    data object Login : SettingsUiEvent

    /** Cerrar la sesión actual (cuenta o invitado) y volver al login (SPEC-0001 RF-6). */
    data object SignOut : SettingsUiEvent

    /** Borrar todos los análisis locales de la base de datos. */
    data object ClearLocalAnalyses : SettingsUiEvent

    /** Cerrar el aviso de confirmación tras borrar los análisis locales. */
    data object DismissClearedConfirmation : SettingsUiEvent

    /** Volver a la pantalla anterior. */
    data object NavigateBack : SettingsUiEvent
}
