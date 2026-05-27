package com.uader.ptah.ui.chat

sealed interface UserEvent {
    data class ShowError(val message: String) : UserEvent
}
