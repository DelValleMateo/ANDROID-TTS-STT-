package com.uader.ptah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uader.ptah.ui.chat.ChatViewModel
import com.uader.ptah.ui.theme.PTAHTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PTAHTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // 1. Instanciamos el ViewModel que armaste
                    val viewModel: ChatViewModel = viewModel()

                    // 2. Llamamos a nuestra pantalla de prueba
                    PruebaChatScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PruebaChatScreen(viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        // Un botón que ejecuta tu función sendMessage
        Button(onClick = { viewModel.sendMessage("¡Hola, testeando la arquitectura!") }) {
            Text("Simular mensaje")
        }

        // Recorremos la lista de tu ViewModel y la mostramos en pantalla
        viewModel.messages.forEach { mensaje ->
            Text(text = mensaje, modifier = Modifier.padding(top = 8.dp))
        }
    }
}