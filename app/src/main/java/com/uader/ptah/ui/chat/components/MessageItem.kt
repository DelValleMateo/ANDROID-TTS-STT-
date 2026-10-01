package com.uader.ptah.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uader.ptah.data.tts.TtsState
import com.uader.ptah.ui.chat.ChatMessage
import com.uader.ptah.ui.chat.InputOrigin

@Composable
fun MessageItem(
    message: ChatMessage,
    ttsState: TtsState,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.author == ChatMessage.Author.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (isUser) {
            UserMessageBubble(message)
        } else {
            SystemMessage(
                message = message,
                ttsState = ttsState,
                onSpeak = onSpeak,
                onStop = onStop
            )
        }
    }
}

@Composable
private fun UserMessageBubble(message: ChatMessage) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 4.dp
        ),
        modifier = Modifier.widthIn(max = 280.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge
            )

            // Origen por voz
            if (message.origin == InputOrigin.VOICE) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics {
                        contentDescription = "Enviado por voz"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "por voz",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemMessage(
    message: ChatMessage,
    ttsState: TtsState,
    onSpeak: () -> Unit,
    onStop: () -> Unit
) {
    val isSpeaking = ttsState is TtsState.Speaking && ttsState.messageId == message.id
    val isGenerating = ttsState is TtsState.Generating && ttsState.messageId == message.id
    val isActive = isSpeaking || isGenerating

    Column(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        // Controles de audio
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = if (isActive) onStop else onSpeak,
                enabled = ttsState !is TtsState.Initializing,
                modifier = Modifier.semantics {
                    contentDescription = if (isActive) "Detener audio" else "Escuchar respuesta"
                }
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isActive) "Detener" else "Escuchar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = if (isGenerating) "Generando voz natural" else "Reproduciendo audio"
                        }
                ) {
                    AudioVisualizerDot()
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isGenerating) "Generando..." else "Reproduciendo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioVisualizerDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
    )
}
