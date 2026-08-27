package com.uader.ptah.ui.chat

sealed interface UserEvent {
    /** Error de red o de API. Muestra un Snackbar con botón "Reintentar". */
    data class ShowError(val message: String) : UserEvent

    /** Error específico del reconocimiento de voz. Muestra un Snackbar informativo. */
    data class ShowSttError(val message: String) : UserEvent

    /** La UI debe lanzar el diálogo de solicitud de permiso de micrófono. */
    data object RequestMicPermission : UserEvent
}
