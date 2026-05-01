package com.uader.ptah.ui.chat

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf

class ChatViewModel : ViewModel() {
    // Lista observable para que la interfaz se actualice automáticamente
    private val _messages = mutableStateListOf<String>()
    val messages: List<String> = _messages

    fun sendMessage(text: String) {
        // 1. Agregamos lo que dice el usuario
        _messages.add("Usuario: $text")

        // TODO: Acá después conectaremos Retrofit para consultar al motor de búsqueda
    }
}