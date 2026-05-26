package com.uader.ptah.ui.chat

/**
 * Eventos de UI de disparo único (one-shot) que el ViewModel envía a la pantalla.
 *
 * A diferencia de [ChatUiState], estos eventos no representan estado persistente:
 * deben ser consumidos exactamente una vez por la UI (ej: mostrar un Snackbar).
 *
 * Se transportan mediante un [kotlinx.coroutines.channels.Channel] para garantizar
 * que cada evento llegue a la UI aunque el mensaje anterior fuera idéntico.
 */
sealed interface UserEvent {
    /** Solicita mostrar un Snackbar con [message] y un botón "Reintentar". */
    data class ShowError(val message: String) : UserEvent
}
