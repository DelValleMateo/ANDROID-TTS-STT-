package com.uader.ptah.ui.chat

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
            repository.searchRegulations(clean)
                .onSuccess { results ->
                    val reply = if (results.isEmpty()) {
                        "Sin resultados."
                    } else {
                        results.joinToString(separator = "\n\n") { article ->
                            "• ${article.title}\n${article.content}"
                        }
                    }
                    _messages.add(ChatMessage(ChatMessage.Author.SYSTEM, reply))
                    uiState = ChatUiState.Success(results)
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Error desconocido"
                    _messages.add(
                        ChatMessage(ChatMessage.Author.SYSTEM, "Error: $message")
                    )
                    uiState = ChatUiState.Error(message)
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
}
