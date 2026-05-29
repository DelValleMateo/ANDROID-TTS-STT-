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
        executeQuery(clean)
    }

    fun retryLastQuery() {
        lastQuery?.let {
            Log.d(TAG, "Reintentando ultima consulta: $it")
            executeQuery(it)
        }
    }

    private fun executeQuery(query: String) {
        lastQuery = query
        uiState = ChatUiState.Loading

        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            Log.d(TAG, "Inicio de consulta a Groq: $query")

            repository.ask(query)
                .onSuccess { response ->
                    val latencyMs = SystemClock.elapsedRealtime() - startedAt
                    Log.d(TAG, "Fin de consulta a Groq. Latencia: ${latencyMs}ms")

                    _messages.add(ChatMessage(ChatMessage.Author.SYSTEM, response.answer))
                    uiState = ChatUiState.Success(latencyMs)
                }
                .onFailure { throwable ->
                    handleFailure(throwable, startedAt)
                }
        }
    }

    private fun handleFailure(throwable: Throwable, startedAt: Long) {
        val latencyMs = SystemClock.elapsedRealtime() - startedAt
        val message = when (throwable) {
            is ApiException -> throwable.message ?: "El servicio respondio con un error. Intenta nuevamente."
            is IOException -> throwable.message ?: "No se pudo conectar con el servidor. Verifica tu conexion."
            else -> throwable.message ?: "Fallo inesperado. Intenta nuevamente."
        }

        Log.e(TAG, "Error en consulta a Groq tras ${latencyMs}ms: $message", throwable)
        uiState = ChatUiState.Error(message, latencyMs)

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
