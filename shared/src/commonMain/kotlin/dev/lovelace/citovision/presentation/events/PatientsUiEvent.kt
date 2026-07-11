package dev.lovelace.citovision.presentation.events

import dev.lovelace.citovision.domain.entities.Analysis

/** Acciones de la pestaña Pacientes, resueltas en PatientsViewModel.onEvent() (SPEC-0005). */
sealed interface PatientsUiEvent {
    data class QueryChanged(
        val code: String,
    ) : PatientsUiEvent

    data object Search : PatientsUiEvent

    data object NewSearch : PatientsUiEvent

    data class ShowDetail(
        val analysis: Analysis,
    ) : PatientsUiEvent

    data object DismissDetail : PatientsUiEvent

    data object DismissNoResults : PatientsUiEvent

    data object DismissRequiresAccount : PatientsUiEvent

    data object DismissError : PatientsUiEvent
}
