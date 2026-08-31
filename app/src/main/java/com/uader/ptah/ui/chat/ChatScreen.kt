package com.uader.ptah.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uader.ptah.ui.chat.components.ChatHeader
import com.uader.ptah.ui.chat.components.DynamicInputArea
import com.uader.ptah.ui.chat.components.EmptyState
import com.uader.ptah.ui.chat.components.MessageList
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(context))

    val messages = viewModel.messages
    val conversationState = viewModel.conversationState
    val inputText = viewModel.inputText
    val inputOrigin = viewModel.inputOrigin
    val sttState by viewModel.sttState.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onMicClicked(permissionGranted = true)
    }

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

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ChatHeader(
                autoSpeakEnabled = viewModel.autoSpeakEnabled,
                onToggleAutoSpeak = { viewModel.toggleAutoSpeak() }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    EmptyState(
                        onExampleClicked = { example ->
                            viewModel.onTextChanged(example)
                            viewModel.onSendClicked()
                        }
                    )
                } else {
                    MessageList(
                        messages = messages,
                        listState = listState,
                        ttsState = ttsState,
                        onSpeak = { viewModel.onSpeakClicked(it) },
                        onStopSpeaking = { viewModel.onStopSpeakingClicked() }
                    )
                }
            }

            DynamicInputArea(
                value = inputText,
                inputOrigin = inputOrigin,
                sttState = sttState,
                isLoading = conversationState is ConversationState.Consulting,
                onValueChange = viewModel::onTextChanged,
                onSend = viewModel::onSendClicked,
                onMicClicked = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    viewModel.onMicClicked(granted)
                    if (!granted) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onCancelStt = { viewModel.onMicClicked(permissionGranted = true) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
