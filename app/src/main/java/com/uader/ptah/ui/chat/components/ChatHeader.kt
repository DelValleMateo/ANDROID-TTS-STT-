package com.uader.ptah.ui.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uader.ptah.ui.theme.PtahMotion

@Composable
fun ChatHeader(
    autoSpeakEnabled: Boolean,
    onToggleAutoSpeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo / Title
        Text(
            text = "PTAH",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        // Auto-Speak Toggle Pill
        val pillColor by animateColorAsState(
            targetValue = if (autoSpeakEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            animationSpec = PtahMotion.colorTransition,
            label = "autoSpeakColor"
        )
        
        val contentColor by animateColorAsState(
            targetValue = if (autoSpeakEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = PtahMotion.colorTransition,
            label = "autoSpeakContentColor"
        )

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(pillColor)
                .clickable { onToggleAutoSpeak() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .semantics {
                    contentDescription = if (autoSpeakEnabled) "Voz automática activada. Toca para desactivar." else "Voz automática desactivada. Toca para activar."
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (autoSpeakEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (autoSpeakEnabled) "Voz Auto" else "Silencio",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}
