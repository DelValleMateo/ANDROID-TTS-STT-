package com.uader.ptah.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uader.ptah.ui.theme.PtahSpacing
import com.uader.ptah.data.tts.TtsState
import com.uader.ptah.ui.theme.screenHorizontalPadding
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(context))

    val messages = viewModel.messages
    val uiState = viewModel.uiState
    val inputText = viewModel.inputText
    val inputOrigin = viewModel.inputOrigin
    val sttState by viewModel.sttState.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher para solicitar permiso de micrófono en tiempo de ejecución.
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onMicClicked(permissionGranted = true)
        }
        // Si denegado: el ViewModel emitirá ShowSttError via el observer de SttState.PermissionDenied.
    }

    // Manejo de eventos de un solo disparo desde el ViewModel.
    LaunchedEffect(Unit) {
        viewModel.userEvents.collectLatest { event ->
            when (event) {
                is UserEvent.ShowError -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Reintentar",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.retryLastQuery()
                    }
                }

                is UserEvent.ShowSttError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }

                is UserEvent.RequestMicPermission -> {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }

                is UserEvent.ShowTtsError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    // Auto-scroll al último mensaje.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PTAH - Asistente Normativo",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        ChatContent(
            paddingValues = innerPadding,
            messages = messages,
            uiState = uiState,
            sttState = sttState,
            ttsState = ttsState,
            input = inputText,
            inputOrigin = inputOrigin,
            onInputChange = viewModel::onTextChanged,
            onSend = viewModel::onSendClicked,
            onMicClicked = {
                val permissionGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                viewModel.onMicClicked(permissionGranted)

                if (!permissionGranted) {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onCancelStt = { viewModel.onMicClicked(permissionGranted = true) },
            onSpeak = viewModel::onSpeakClicked,
            onStopSpeaking = viewModel::onStopSpeakingClicked,
            listState = listState
        )
    }
}

@Composable
private fun ChatContent(
    paddingValues: PaddingValues,
    messages: List<ChatMessage>,
    uiState: ChatUiState,
    sttState: SttState,
    ttsState: TtsState,
    input: String,
    inputOrigin: InputOrigin,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClicked: () -> Unit,
    onCancelStt: () -> Unit,
    onSpeak: (ChatMessage) -> Unit,
    onStopSpeaking: () -> Unit,
    listState: LazyListState
) {
    val isLoading = uiState is ChatUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .screenHorizontalPadding()
            .padding(vertical = PtahSpacing.screenVertical)
    ) {
        // Lista de mensajes.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (messages.isEmpty()) {
                EmptyHistoryPlaceholder()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        MessageBubble(
                            message = msg,
                            ttsState = ttsState,
                            onSpeak = { onSpeak(msg) },
                            onStop = onStopSpeaking
                        )
                    }
                }
            }
        }

        // Estado del chat (latencia, carga, error).
        StatusRow(uiState)

        // Banner "Escuchando..." — aparece solo cuando el mic está activo.
        // Usa slideInVertically + fade para una transición más fluida.
        SttStatusBanner(
            sttState = sttState,
            onCancel = onCancelStt
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Fila de entrada: campo de texto + botón mic + botón enviar.
        InputRow(
            value = input,
            inputOrigin = inputOrigin,
            onValueChange = onInputChange,
            onSend = onSend,
            onMicClicked = onMicClicked,
            sendEnabled = input.isNotBlank() && !isLoading,
            sttState = sttState
        )
    }
}

// ─── Banner de estado STT ─────────────────────────────────────────────────────

@Composable
private fun SttStatusBanner(
    sttState: SttState,
    onCancel: () -> Unit
) {
    val isVisible = sttState is SttState.Listening || sttState is SttState.Processing

    AnimatedVisibility(
        visible = isVisible,
        // Desliza desde abajo + fade al aparecer.
        enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
        // Desliza hacia abajo + fade al desaparecer.
        exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                // TalkBack anuncia cambios en este banner automáticamente.
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = if (sttState is SttState.Listening) {
                        "Escuchando. Hablá ahora."
                    } else {
                        "Procesando voz. Esperá un momento."
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Indicador pulsante cuando está escuchando.
                if (sttState is SttState.Listening) {
                    PulsingDot()
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Escuchando...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Procesando voz...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Botón cancelar — solo disponible mientras escucha, no mientras procesa.
            if (sttState is SttState.Listening) {
                TextButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar reconocimiento de voz",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Cancelar",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/** Punto rojo que pulsa para indicar que el micrófono está capturando audio. */
@Composable
private fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .scale(scale)
            .background(
                color = MaterialTheme.colorScheme.error,
                shape = CircleShape
            )
    )
}

// ─── Composables de historial ─────────────────────────────────────────────────

@Composable
private fun EmptyHistoryPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Aun no hay mensajes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Escribí una consulta o usá el micrófono 🎙️",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Burbuja de mensaje del historial.
 *
 * Para mensajes del **usuario** enviados por voz ([InputOrigin.VOICE]) se muestra
 * un badge 🎙️ debajo del texto, con `contentDescription` para TalkBack.
 * Los mensajes del sistema nunca muestran badge de origen.
 */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    ttsState: TtsState,
    onSpeak: () -> Unit,
    onStop: () -> Unit
) {
    val isUser = message.author == ChatMessage.Author.USER
    val bg = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bg,
            contentColor = fg,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = message.text)

                if (!isUser) {
                    val isSpeaking = ttsState is TtsState.Speaking &&
                        ttsState.messageId == message.id
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = if (isSpeaking) onStop else onSpeak,
                        enabled = ttsState !is TtsState.Initializing,
                        modifier = Modifier.semantics {
                            contentDescription = if (isSpeaking) {
                                "Detener audio de esta respuesta"
                            } else {
                                "Escuchar esta respuesta"
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isSpeaking) "Detener" else "Escuchar")
                    }
                    AnimatedVisibility(visible = isSpeaking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = "Reproduciendo respuesta"
                            }
                        ) {
                            PulsingDot()
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Reproduciendo...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Badge de origen: solo en mensajes del usuario enviados por voz.
                if (isUser && message.origin == InputOrigin.VOICE) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics {
                            contentDescription = "Enviado por voz"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null, // descrito por el Row padre
                            tint = fg.copy(alpha = 0.6f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "por voz",
                            style = MaterialTheme.typography.labelSmall,
                            color = fg.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// ─── Status y campo de entrada ────────────────────────────────────────────────

@Composable
private fun StatusRow(state: ChatUiState) {
    when (state) {
        ChatUiState.Idle -> Unit
        ChatUiState.Loading -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .height(20.dp)
                    .width(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text("Cargando...", style = MaterialTheme.typography.bodySmall)
        }

        is ChatUiState.Success -> Text(
            text = "Respuesta recibida. Latencia: ${state.latencyMs} ms.",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(8.dp)
        )

        is ChatUiState.Error -> Text(
            text = state.latencyMs?.let { "Error: ${state.message}. Latencia: ${it} ms." }
                ?: "Error: ${state.message}",
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(8.dp)
        )
    }
}

@Composable
private fun InputRow(
    value: String,
    inputOrigin: InputOrigin,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClicked: () -> Unit,
    sendEnabled: Boolean,
    sttState: SttState
) {
    val isListening = sttState is SttState.Listening
    val isProcessing = sttState is SttState.Processing
    val isVoiceInput = inputOrigin == InputOrigin.VOICE

    // Placeholder dinámico según el origen del texto actual.
    val placeholder = if (isVoiceInput && value.isNotBlank()) {
        "Texto reconocido por voz — podés editarlo"
    } else {
        "Escribí tu consulta..."
    }

    // El borde del campo usa secondaryContainer cuando el texto vino de voz,
    // para dar un sutil feedback visual de origen.
    val fieldColors = if (isVoiceInput && value.isNotBlank()) {
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Campo de texto.
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = if (isVoiceInput && value.isNotBlank()) {
                        "Campo de texto con voz reconocida. Podés editarlo antes de enviar."
                    } else {
                        "Campo de texto. Escribi tu consulta normativa aqui."
                    }
                },
            placeholder = { Text(placeholder) },
            label = {
                Text(
                    if (isVoiceInput && value.isNotBlank()) "Consulta (por voz)" else "Consulta"
                )
            },
            singleLine = true,
            colors = fieldColors
        )

        Spacer(Modifier.width(8.dp))

        // Botón de micrófono.
        IconButton(
            onClick = onMicClicked,
            enabled = !isProcessing,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = when {
                        isListening -> "Micrófono activo. Tocá para cancelar."
                        isProcessing -> "Procesando voz. Esperá un momento."
                        else -> "Activar reconocimiento de voz."
                    }
                }
        ) {
            when {
                isProcessing -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                isListening -> Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                else -> Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Botón enviar.
        Button(
            onClick = onSend,
            enabled = sendEnabled,
            modifier = Modifier.semantics {
                contentDescription = if (sendEnabled) {
                    "Enviar consulta al asistente"
                } else {
                    "Boton Enviar deshabilitado. Escribi una consulta primero."
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Enviar")
        }
    }
}
