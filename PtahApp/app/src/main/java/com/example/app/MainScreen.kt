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

@Composable
fun ChatScreen() {
    val mensajes = listOf(
        "Hola",
        "¿Qué dice el reglamento sobre licencias?",
        "El reglamento establece 90 días de licencia por maternidad.",
        "¿Se puede extender?",
        "Sí, con justificación médica."
    )

    LazyColumn {
        itemsIndexed(mensajes) { index, texto ->
            ChatBubble(
                texto = texto,
                esUsuario = index % 2 == 0
            )
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
                .widthIn(max = 250.dp)
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
    val historial = listOf(
        "Licencias por maternidad",
        "Vacaciones",
        "Permisos especiales"
    )

    LazyColumn {
        itemsIndexed(historial) { _, item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(item, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun GuardadosScreen() {
    val guardados = listOf(
        "Artículo 45",
        "Artículo 78",
        "Artículo 102"
    )

    LazyColumn {
        itemsIndexed(guardados) { _, item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(item, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun PerfilScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("👤", style = MaterialTheme.typography.displayMedium)
        Text("Usuario")
        Text("usuario@ptah.gob.ar")
    }
}