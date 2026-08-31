package com.uader.ptah.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uader.ptah.data.tts.TtsState
import com.uader.ptah.ui.chat.ChatMessage

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    listState: LazyListState,
    ttsState: TtsState,
    onSpeak: (ChatMessage) -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            MessageItem(
                message = msg,
                ttsState = ttsState,
                onSpeak = { onSpeak(msg) },
                onStop = onStopSpeaking
            )
        }
    }
}
