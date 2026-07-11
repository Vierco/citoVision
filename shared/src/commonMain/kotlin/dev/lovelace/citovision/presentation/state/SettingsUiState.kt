package dev.lovelace.citovision.presentation.state

import dev.lovelace.citovision.application.usecases.SessionStatus

/**
 * Estado de la pantalla de Ajustes. El tipo de sesión decide qué opción se muestra:
 * cuenta → cerrar sesión; invitado → iniciar sesión con cuenta. `NONE` no muestra ninguna.
 * `email` y `avatarUrl` identifican al usuario de cuenta en la cabecera: si hay `avatarUrl`
 * (login con Google) se muestra su foto; si no, un círculo con la inicial del correo.
 */
data class SettingsUiState(
    val sessionStatus: SessionStatus = SessionStatus.NONE,
    val email: String? = null,
    val avatarUrl: String? = null,
)
