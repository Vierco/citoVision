package dev.lovelace.citovision.presentation.state

import org.jetbrains.compose.resources.StringResource

/**
 * Estado completo e inmutable de la pantalla de login (ver Skill mvvm-compose-kmp, SPEC-0001).
 * El error se expone como recurso de string ya resuelto por el ViewModel; la UI solo lo muestra.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: StringResource? = null,
)
