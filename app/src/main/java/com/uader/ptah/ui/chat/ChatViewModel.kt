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
import com.uader.ptah.di.ServiceLocator
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: PtahRepository
) : ViewModel() {

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    var uiState by mutableStateOf<ChatUiState>(ChatUiState.Idle)
        private set

    var inputText by mutableStateOf("")
        private set

    fun onTextChanged(text: String) {
        inputText = text
    }

    fun onSendClicked() {
        val clean = inputText.trim()
        if (clean.isEmpty()) return
        if (uiState is ChatUiState.Loading) return

        _messages.add(ChatMessage(ChatMessage.Author.USER, clean))
        inputText = ""
        uiState = ChatUiState.Loading

        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            Log.d(TAG, "Inicio de consulta al Mock")

            repository.searchRegulations(clean)
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
                    val latencyMs = SystemClock.elapsedRealtime() - startedAt
                    val message = throwable.message ?: "Error desconocido"
                    Log.e(TAG, "Error en consulta al Mock tras ${latencyMs}ms", throwable)
                    _messages.add(
                        ChatMessage(ChatMessage.Author.SYSTEM, "Error: $message")
                    )
                    uiState = ChatUiState.Error(message, latencyMs)
                }
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
