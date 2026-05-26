package com.uader.ptah.ui.chat

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uader.ptah.data.PtahRepository
import com.uader.ptah.data.network.ApiException
import com.uader.ptah.di.ServiceLocator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.IOException

class ChatViewModel(
    private val repository: PtahRepository
) : ViewModel() {

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    var uiState by mutableStateOf<ChatUiState>(ChatUiState.Idle)
        private set

    var inputText by mutableStateOf("")
        private set

    private var lastQuery: String? = null

    /**
     * Canal de eventos de UI de "disparo único" (one-shot).
     * Usar [userEvents] en la UI para colectar.
     *
     * Razón: LaunchedEffect(uiState) NO dispara si el error tiene el mismo
     * mensaje que el anterior (la key no cambia). Un Channel garantiza entrega
     * individual para CADA error, independientemente del contenido.
     */
    private val _userEvents = Channel<UserEvent>(capacity = Channel.BUFFERED)
    val userEvents = _userEvents.receiveAsFlow()

    fun onTextChanged(text: String) {
        inputText = text
    }

    fun onSendClicked() {
        val clean = inputText.trim()
        if (clean.isEmpty()) return
        if (uiState is ChatUiState.Loading) return

        _messages.add(ChatMessage(ChatMessage.Author.USER, clean))
        inputText = ""
        executeSearch(clean)
    }

    fun retryLastQuery() {
        lastQuery?.let { 
            Log.d(TAG, "Reintentando última consulta: $it")
            executeSearch(it) 
        }
    }

    private fun executeSearch(query: String) {
        lastQuery = query
        uiState = ChatUiState.Loading

        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            Log.d(TAG, "Inicio de consulta al Mock: $query")

            try {
                repository.searchRegulations(query)
                    .onSuccess { results ->
                        val latencyMs = SystemClock.elapsedRealtime() - startedAt
                        Log.d(TAG, "Fin de consulta al Mock. Latencia: ${latencyMs}ms")

                        val reply = if (results.isEmpty()) {
                            "Sin resultados.\nLatencia: ${latencyMs} ms"
                        } else {
                            results.joinToString(separator = "\n\n") { article ->
                                "${article.title}\n${article.content}"
                            }
                        }
                        _messages.add(ChatMessage(ChatMessage.Author.SYSTEM, reply))
                        uiState = ChatUiState.Success(results, latencyMs)
                    }
                    .onFailure { throwable ->
                        handleFailure(throwable, startedAt)
                    }
            } catch (e: Exception) {
                // Captura de excepciones que podrían ocurrir fuera del runCatching del repo
                handleFailure(e, startedAt)
            }
        }
    }

    private fun handleFailure(throwable: Throwable, startedAt: Long) {
        val latencyMs = SystemClock.elapsedRealtime() - startedAt
        val message = when (throwable) {
            is ApiException -> throwable.message ?: "Error de servidor"
            is IOException -> "Error de conexión: Revisa tu internet"
            else -> throwable.message ?: "Error inesperado"
        }

        Log.e(TAG, "Error en consulta al Mock tras ${latencyMs}ms: $message", throwable)

        // Actualizar el estado visual (banner de error en StatusRow)
        uiState = ChatUiState.Error(message, latencyMs)

        // Emitir evento de Snackbar por el canal: garantiza que la UI lo reciba
        // incluso si el mensaje anterior era idéntico (fix al bug de LaunchedEffect).
        viewModelScope.launch {
            _userEvents.send(UserEvent.ShowError(message))
        }
    }

    class Factory(
        private val repository: PtahRepository = ServiceLocator.ptahRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ChatViewModel(repository) as T
        }
    }

    private companion object {
        const val TAG = "ChatViewModel"
    }
}
