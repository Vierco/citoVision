package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lovelace.citovision.application.usecases.DeleteAllAnalysesUseCase
import dev.lovelace.citovision.application.usecases.ObserveCurrentUserUseCase
import dev.lovelace.citovision.application.usecases.ObserveSessionStatusUseCase
import dev.lovelace.citovision.application.usecases.SignOutUseCase
import dev.lovelace.citovision.presentation.events.SettingsUiEvent
import dev.lovelace.citovision.presentation.navigation.NavigationEvent
import dev.lovelace.citovision.presentation.state.SettingsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Gestiona las acciones de sesión de la pantalla de Ajustes y expone el tipo de sesión activa para
 * mostrar la opción correcta (cuenta → cerrar sesión; invitado → iniciar sesión con cuenta).
 * Ambas acciones navegan a `LoginRoute`; el cierre de sesión limpia el flag de invitado y la cuenta
 * Firebase vía [SignOutUseCase].
 */
class SettingsViewModel(
    private val signOut: SignOutUseCase,
    private val deleteAllAnalyses: DeleteAllAnalysesUseCase,
    observeSessionStatus: ObserveSessionStatusUseCase,
    observeCurrentUser: ObserveCurrentUserUseCase,
) : ViewModel() {
    private val clearedConfirmation = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> =
        combine(
            observeSessionStatus(),
            observeCurrentUser(),
            clearedConfirmation,
        ) { status, user, cleared ->
            SettingsUiState(
                sessionStatus = status,
                email = user?.email,
                avatarUrl = user?.photoUrl,
                clearedConfirmationVisible = cleared,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SettingsUiState(),
        )

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.Login ->
                viewModelScope.launch {
                    _navigationEvents.send(NavigationEvent.ToLogin)
                }

            SettingsUiEvent.SignOut ->
                viewModelScope.launch {
                    signOut()
                    _navigationEvents.send(NavigationEvent.ToLogin)
                }

            SettingsUiEvent.ClearLocalAnalyses ->
                viewModelScope.launch {
                    deleteAllAnalyses()
                    clearedConfirmation.value = true
                }

            SettingsUiEvent.DismissClearedConfirmation ->
                clearedConfirmation.value = false

            SettingsUiEvent.NavigateBack ->
                viewModelScope.launch {
                    _navigationEvents.send(NavigationEvent.Back)
                }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
