# Contrato de API - Sprint 1
**Endpoint:** `/busqueda/semantica`
**Método:** `POST`
**Request:** `{"consulta": "string"}`
**Response:** Devuelve JSON con "estado", "mensaje" y array de "resultados".

## Integración y Arquitectura

En el Sprint 4 la app realiza una llamada HTTP real al Mock usando Retrofit y corrutinas de Kotlin. El endpoint configurado es `http://10.0.2.2:3000/busqueda/semantica`, donde `10.0.2.2` es la IP especial que usa el emulador Android para acceder al `localhost` de la computadora donde corre `mock_server/app_mock.py`.

La capa de red está separada en `PtahApiService`, `RetrofitProvider` y los modelos de red definidos en `PtahModels.kt`. La app envía un JSON con el campo `consulta` y recibe la respuesta del Mock con `estado`, `mensaje` y `resultados`. El `PtahRepositoryImpl` es responsable de ejecutar la llamada, validar que el estado recibido sea `exito` y mapear cada resultado del contrato (`id_articulo`, `titulo`, `contenido`, `score_relevancia`) al modelo de dominio `NormativeArticle`.

El `ChatViewModel` coordina el flujo de pantalla: toma el texto ingresado por el usuario, lo agrega como mensaje de chat, cambia el estado a `Loading`, llama al Repository desde `viewModelScope` y luego actualiza el estado a `Success` o `Error`. La UI no llama directamente a Retrofit; solo observa los mensajes y el `ChatUiState` expuestos por el ViewModel.

La pantalla `ChatScreen` usa una `LazyColumn` para renderizar dinámicamente el historial. Los mensajes del usuario y del sistema se muestran como burbujas diferenciadas por alineación y color. Cuando llegan artículos normativos, la respuesta del sistema se agrega al chat y la fila de estado muestra la cantidad de resultados.

La latencia se mide en el `ChatViewModel` desde el momento previo al envío al Repository hasta que vuelve la respuesta del Mock o el error. El tiempo se calcula con `SystemClock.elapsedRealtime()`, se publica en el estado de UI y se registra en Logcat con los tags `ChatViewModel` y `PtahRepository` para poder demostrar el ida y vuelta durante la presentación.
