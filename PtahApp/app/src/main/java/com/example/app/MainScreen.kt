package com.example.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen() {
    var pantalla by remember { mutableStateOf("Chat") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pantalla == "Chat",
                    onClick = { pantalla = "Chat" },
                    icon = { Text("💬") },
                    label = { Text("Chat") }
                )
                NavigationBarItem(
                    selected = pantalla == "Historial",
                    onClick = { pantalla = "Historial" },
                    icon = { Text("🕘") },
                    label = { Text("Historial") }
                )
                NavigationBarItem(
                    selected = pantalla == "Guardados",
                    onClick = { pantalla = "Guardados" },
                    icon = { Text("🔖") },
                    label = { Text("Guardados") }
                )
                NavigationBarItem(
                    selected = pantalla == "Perfil",
                    onClick = { pantalla = "Perfil" },
                    icon = { Text("👤") },
                    label = { Text("Perfil") }
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            Text(
                text = pantalla,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (pantalla) {
                "Chat" -> ChatScreen()
                "Historial" -> HistorialScreen()
                "Guardados" -> GuardadosScreen()
                "Perfil" -> PerfilScreen()
            }
        }
    }
}

// --------------------------------------------------------
// AQUÍ ESTÁ TU TAREA DEL SPRINT 2 (UI ESTÁTICA DEL CHAT)
// --------------------------------------------------------
@Composable
fun ChatScreen() {
    // Estado local de la caja de texto (lo que el usuario está escribiendo)
    var inputText by remember { mutableStateOf("") }

    // Lista vacía para simular el historial (cumple el requisito del Sprint)
    val mensajes = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Área para el historial de mensajes (ocupa todo el espacio disponible arriba)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(mensajes) { index, texto ->
                ChatBubble(
                    texto = texto,
                    esUsuario = index % 2 == 0 // Alterna el lado visualmente
                )
            }
        }

        // 2. Caja de entrada de texto y 3. Botón de enviar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje...") },
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        mensajes.add(inputText) // Agrega el mensaje a la interfaz
                        inputText = ""          // Limpia la caja de texto
                    }
                }
            ) {
                Text("Enviar")
            }
        }
    }
}

@Composable
fun ChatBubble(texto: String, esUsuario: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (esUsuario) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .widthIn(max = 250.dp),
            colors = CardDefaults.cardColors(
                // Le damos colores distintos dependiendo de si es el usuario o la IA
                containerColor = if (esUsuario) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = texto,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun HistorialScreen() {
    val historial = listOf("Licencias por maternidad", "Vacaciones", "Permisos especiales")
    LazyColumn {
        itemsIndexed(historial) { _, item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text(item, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun GuardadosScreen() {
    val guardados = listOf("Artículo 45", "Artículo 78", "Artículo 102")
    LazyColumn {
        itemsIndexed(guardados) { _, item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text(item, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun PerfilScreen() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("👤", style = MaterialTheme.typography.displayMedium)
        Text("Usuario")
        Text("usuario@ptah.gob.ar")
    }
}