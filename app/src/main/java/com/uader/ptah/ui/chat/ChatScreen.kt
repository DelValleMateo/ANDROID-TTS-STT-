package com.uader.ptah.ui.chat

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uader.ptah.ui.theme.PtahSpacing
import com.uader.ptah.ui.theme.screenHorizontalPadding

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory())
) {
    val messages = viewModel.messages
    val uiState = viewModel.uiState
    val inputText = viewModel.inputText
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // FIX: usar el canal de eventos en lugar de LaunchedEffect(uiState).
    // Problema del código anterior: si dos errores consecutivos tenían el MISMO
    // mensaje, LaunchedEffect no se re-ejecutaba porque su "key" (uiState) no
    // cambiaba de forma estructural suficiente. El canal garantiza entrega por
    // cada evento emitido, independientemente del contenido.
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
            }
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
            input = inputText,
            onInputChange = viewModel::onTextChanged,
            onSend = viewModel::onSendClicked,
            listState = listState
        )
    }
}

@Composable
private fun ChatContent(
    paddingValues: PaddingValues,
    messages: List<ChatMessage>,
    uiState: ChatUiState,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val isLoading = uiState is ChatUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            // Uso del modifier centralizado: reemplaza padding(horizontal=16, vertical=12)
            // hardcodeados. Si se necesita cambiar el margen global, se toca Spacing.kt.
            .screenHorizontalPadding()
            .padding(vertical = PtahSpacing.screenVertical)
    ) {
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
                        MessageBubble(msg)
                    }
                }
            }
        }

        StatusRow(uiState)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        InputRow(
            value = input,
            onValueChange = onInputChange,
            onSend = onSend,
            sendEnabled = input.isNotBlank() && !isLoading
        )
    }
}

@Composable
private fun EmptyHistoryPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Aún no hay mensajes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Escribí una consulta y tocá Enviar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.author == ChatMessage.Author.USER
    val bg = if (isUser) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
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
            Text(
                text = message.text,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

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
            text = if (state.results.isEmpty()) {
                "Consulta finalizada sin resultados. Latencia: ${state.latencyMs} ms."
            } else {
                "Consulta finalizada: ${state.results.size} resultado(s). Latencia: ${state.latencyMs} ms."
            },
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
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    sendEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                // ACCESIBILIDAD: TalkBack leerá esto como descripción del campo.
                // Al combinar semantics{} con el placeholder visual, los usuarios
                // videntes ven el placeholder y TalkBack anuncia la descripción.
                .semantics {
                    contentDescription = "Campo de texto. Escribí tu consulta normativa aquí."
                },
            placeholder = { Text("Escribí tu consulta...") },
            label = { Text("Consulta") },   // Label visible siempre: mejora accesibilidad visual
            singleLine = true,
            enabled = true
        )
        Spacer(Modifier.width(PtahSpacing.itemGap))
        Button(
            onClick = onSend,
            enabled = sendEnabled,
            // ACCESIBILIDAD: describe la acción exacta del botón, no solo su etiqueta.
            // TalkBack anunciará: "Enviar consulta. Botón."
            modifier = Modifier.semantics {
                contentDescription = if (sendEnabled) {
                    "Enviar consulta al asistente"
                } else {
                    "Botón Enviar deshabilitado. Escribí una consulta primero."
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
