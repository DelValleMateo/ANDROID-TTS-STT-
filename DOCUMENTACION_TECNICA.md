# Documentacion tecnica - Proyecto PTAH Android

## 1. Descripcion general del proyecto

PTAH - Cliente Movil Android es una aplicacion Android escrita en Kotlin que permite realizar consultas en lenguaje natural sobre reglamentacion institucional mediante una interfaz conversacional textual.

La app funciona como cliente movil del Proyecto PTAH, un sistema orientado a busqueda semantica de reglamentacion. El problema que busca resolver es facilitar que una persona consulte informacion normativa sin tener que navegar manualmente documentos extensos o conocer de antemano palabras clave exactas.

Para la entrega parcial de junio/julio se entrega un prototipo funcional centrado en:

- Interaccion textual.
- Pantalla tipo chatbot.
- Comunicacion con una API.
- Manejo de estados de carga, respuesta y error.
- Arquitectura base organizada para poder evolucionar el proyecto.

Actualmente Groq API cumple un rol temporal: se usa para validar el flujo conversacional textual de consulta-respuesta mientras se prepara o conecta el motor semantico real de PTAH. No debe interpretarse como reemplazo definitivo del backend semantico institucional, sino como una integracion provisoria para demostrar el comportamiento completo del cliente movil.

## 2. Alcance de la entrega parcial junio/julio

Esta etapa incluye:

- Prototipo funcional con interaccion textual.
- Interfaz tipo chatbot.
- Comunicacion con API externa.
- Manejo de carga, respuesta y error.
- Arquitectura base MVVM.
- Medicion basica de latencia.
- Documentacion tecnica para defender el diseno.

Queda para etapas posteriores:

- Reconocimiento de voz STT.
- Sintesis de voz TTS.
- Evaluacion completa de usabilidad, eficacia y eficiencia.
- Conexion definitiva con el motor semantico real de PTAH si todavia no esta disponible.
- Pruebas avanzadas y validacion experimental completa.

## 3. Tecnologias utilizadas

- Kotlin: lenguaje principal del proyecto Android. Permite escribir codigo conciso, seguro y compatible con las APIs modernas de Android.
- Android SDK: conjunto de herramientas y APIs necesarias para compilar y ejecutar la app en dispositivos Android.
- Jetpack Compose: framework declarativo para construir la interfaz. En este proyecto se usa para la pantalla de chat, el campo de texto, boton, historial, carga y errores.
- Material 3: sistema visual usado por Compose para componentes como `Scaffold`, `TopAppBar`, `OutlinedTextField`, `Button`, `Snackbar` y `CircularProgressIndicator`.
- MVVM: patron de arquitectura aplicado separando UI, estado/logica de pantalla y acceso a datos.
- ViewModel: `ChatViewModel` conserva y administra estado de pantalla, historial de mensajes, llamadas asincronas y errores.
- Estado Compose: el proyecto usa `mutableStateOf` y `mutableStateListOf`, no `StateFlow` para el estado principal. Estos estados disparan recomposicion automatica en Compose.
- Channel y Flow: `Channel<UserEvent>` se expone con `receiveAsFlow()` para eventos de una sola vez, como mostrar un Snackbar de error.
- Coroutines: `viewModelScope.launch` ejecuta la consulta a la API sin bloquear la UI.
- Retrofit: cliente HTTP usado para definir y ejecutar la llamada a Groq API.
- Gson: `converter-gson` convierte JSON de request/response entre Kotlin y la API. Tambien se usan anotaciones `@SerializedName`.
- OkHttp: cliente HTTP usado por Retrofit, con interceptores para API key, latencia, logging y errores.
- BuildConfig: mecanismo de Gradle para exponer constantes al codigo Kotlin, como `GROQ_API_KEY`.
- local.properties: archivo local no versionado donde se configura la API key sin subirla al repositorio.
- Groq API: API temporal compatible con OpenAI Chat Completions usada para generar una respuesta textual y validar el flujo conversacional.

## 4. Estructura general del proyecto

Estructura principal real:

```text
ANDROID-TTS-STT-/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradle/
    libs.versions.toml
    wrapper/
  app/
    build.gradle.kts
    proguard-rules.pro
    src/
      main/
        AndroidManifest.xml
        java/com/uader/ptah/
          MainActivity.kt
          data/
            PtahApiService.kt
            PtahModels.kt
            PtahRepository.kt
            network/
              RetrofitProvider.kt
          di/
            ServiceLocator.kt
          ui/
            chat/
              ChatScreen.kt
              ChatUiState.kt
              ChatViewModel.kt
              UserEvent.kt
            theme/
              Color.kt
              Spacing.kt
              Theme.kt
              Type.kt
        res/
          values/
          xml/
          drawable/
          mipmap-*/
      test/
      androidTest/
  mock_server/
    app_mock.py
    test_endpoint.py
  PtahApp/
  README.md
  CONTRATO_API.md
  GEMINI.md
```

El modulo real de la aplicacion es `:app`. Esto se confirma en `settings.gradle.kts`, que contiene `include(":app")`.

`PtahApp/` parece ser una copia o prototipo antiguo, con paquete `com.example.app`. No forma parte del build principal porque no esta incluido en el `settings.gradle.kts` de la raiz. No se borra, pero para la presentacion se debe explicar que el modulo activo es `:app`.

`mock_server/` contiene un servidor de prueba previo para el contrato semantico local. Actualmente el flujo principal del modulo `:app` consulta Groq API; el mock queda como material historico o de referencia.

## 5. Arquitectura utilizada

La arquitectura aplicada es MVVM con una capa de datos separada.

Flujo principal:

```text
ChatScreen
    ↓
ChatViewModel
    ↓
PtahRepository
    ↓
GroqApiService / RetrofitProvider
    ↓
Groq API
```

Responsabilidades:

- UI: `ChatScreen` muestra la pantalla, captura texto, renderiza mensajes, carga y errores.
- ViewModel: `ChatViewModel` maneja estado, historial, input, loading, errores, reintento y latencia.
- Repository: `PtahRepositoryImpl` centraliza la logica de consulta, arma el request para Groq y limpia la respuesta recibida.
- Network/API Service: `GroqApiService` define el endpoint HTTP y `RetrofitProvider` configura Retrofit/OkHttp.
- DTOs/Models: `PtahModels.kt` contiene los modelos para request y response de Groq API y la respuesta interna `QueryResponse`.
- DI manual: `ServiceLocator` centraliza la creacion del repositorio y del servicio de red.

Esta separacion ayuda porque:

- Ordena el codigo por responsabilidad.
- Evita que la UI conozca detalles de Retrofit o JSON.
- Permite reemplazar Groq API por el backend real PTAH cambiando principalmente la capa de datos/red.
- Facilita explicar, mantener y probar cada parte de forma aislada.

## 6. Flujo completo de una consulta

Paso a paso:

1. El usuario escribe una consulta en el `OutlinedTextField` de `ChatScreen`.
2. Cada cambio de texto llama a `viewModel.onTextChanged`.
3. La UI habilita el boton Enviar solo si el texto no esta vacio y no hay una consulta en curso.
4. Al presionar Enviar, la UI llama a `viewModel.onSendClicked`.
5. El ViewModel recorta espacios y evita enviar texto vacio.
6. El ViewModel agrega un `ChatMessage` del usuario al historial.
7. El ViewModel limpia el input.
8. El ViewModel cambia `uiState` a `ChatUiState.Loading`.
9. El ViewModel inicia la medicion de latencia con `SystemClock.elapsedRealtime()`.
10. El ViewModel llama a `repository.ask(query)` dentro de `viewModelScope.launch`.
11. El Repository arma un `GroqChatCompletionRequest` con el modelo y un mensaje `user` con la consulta del usuario.
12. El Repository llama a `GroqApiService.createChatCompletion`.
13. Retrofit usa `RetrofitProvider` para enviar la request a Groq API.
14. `ApiKeyInterceptor` agrega la API key en el header `Authorization: Bearer`.
15. Groq API devuelve una respuesta JSON.
16. El Repository extrae el texto desde `choices[0].message.content`.
17. El Repository devuelve `QueryResponse(answer)`.
18. El ViewModel calcula la latencia final.
19. El ViewModel agrega un `ChatMessage` del sistema al historial.
20. El ViewModel cambia el estado a `ChatUiState.Success`.
21. Compose recompone la pantalla y muestra la respuesta.
22. Si ocurre un error, el ViewModel cambia a `ChatUiState.Error` y emite un `UserEvent.ShowError` para mostrar Snackbar.

Diagrama simple:

```text
Usuario
  escribe consulta
      ↓
ChatScreen
  input + boton enviar
      ↓
ChatViewModel
  historial + loading + latencia
      ↓
PtahRepository
  request Groq + parsing de respuesta
      ↓
GroqApiService / RetrofitProvider
  HTTP + API key + errores
      ↓
Groq API
  respuesta textual
      ↓
ChatViewModel
  agrega respuesta o error
      ↓
ChatScreen
  muestra mensaje, latencia o error
```

## 7. Explicacion de archivos principales

### `settings.gradle.kts`

Responsabilidad: define la configuracion global de resolucion de plugins/repositorios y los modulos incluidos.

Punto importante: incluye solamente `:app`, por lo tanto ese es el modulo real de la aplicacion.

### `build.gradle.kts`

Responsabilidad: build script raiz. Declara plugins comunes usando alias del version catalog, pero sin aplicarlos directamente en la raiz.

Se relaciona con `gradle/libs.versions.toml`, donde se definen versiones y coordenadas de dependencias.

### `gradle/libs.versions.toml`

Responsabilidad: centraliza versiones de Android Gradle Plugin, Kotlin, Compose BOM, lifecycle, tests y plugins.

Ayuda a que las versiones esten en un solo lugar y el `build.gradle.kts` sea mas legible.

### `app/build.gradle.kts`

Responsabilidad: configura el modulo Android `:app`.

Contiene:

- Plugin Android application.
- Plugin Kotlin Compose.
- `namespace = "com.uader.ptah"`.
- `applicationId = "com.uader.ptah"`.
- `compileSdk`, `minSdk` y `targetSdk`.
- `buildFeatures { compose = true; buildConfig = true }`.
- Lectura de `GROQ_API_KEY` desde `local.properties`.
- Exposicion de `BuildConfig.GROQ_API_KEY`.
- Dependencias de Compose, Material 3, ViewModel, Retrofit, Gson y logging de OkHttp.

No hardcodea una clave real. Solo genera una constante a partir del valor local.

### `app/src/main/AndroidManifest.xml`

Responsabilidad: manifiesto Android de la app.

Contiene:

- Permiso `android.permission.INTERNET`, necesario para llamar a Groq API.
- Declaracion de `MainActivity` como activity principal.
- Intent filter `MAIN` y `LAUNCHER`.
- Tema `Theme.PTAH`.

### `app/src/main/java/com/uader/ptah/MainActivity.kt`

Responsabilidad: punto de entrada de la UI.

Clases/funciones:

- `MainActivity : ComponentActivity`.
- `onCreate`.
- `enableEdgeToEdge`.
- `setContent`.

Relacion: aplica `PTAHTheme` y muestra `ChatScreen`.

### `app/src/main/java/com/uader/ptah/ui/chat/ChatScreen.kt`

Responsabilidad: pantalla principal tipo chatbot.

Contiene:

- `ChatScreen`: Composable principal.
- `ChatContent`: layout general de la pantalla.
- `EmptyHistoryPlaceholder`: mensaje cuando no hay historial.
- `MessageBubble`: burbuja visual diferenciando usuario y sistema.
- `StatusRow`: muestra loading, exito con latencia o error.
- `InputRow`: campo de consulta y boton Enviar.

Relacion: observa estados del `ChatViewModel` y llama a sus metodos. No llama a Retrofit ni conoce detalles de la API.

### `app/src/main/java/com/uader/ptah/ui/chat/ChatViewModel.kt`

Responsabilidad: manejar la logica de pantalla.

Contiene:

- Lista de mensajes `_messages`.
- Estado `uiState`.
- Texto actual `inputText`.
- Ultima consulta `lastQuery` para reintento.
- Canal `_userEvents` para Snackbar.
- `onTextChanged`.
- `onSendClicked`.
- `retryLastQuery`.
- `executeQuery`.
- `handleFailure`.
- `Factory` para crear el ViewModel con el repositorio.

Relacion: recibe eventos desde `ChatScreen`, llama a `PtahRepository`, calcula latencia y actualiza la UI.

### `app/src/main/java/com/uader/ptah/ui/chat/ChatUiState.kt`

Responsabilidad: definir estados y mensajes del chat.

Contiene:

- `ChatUiState.Idle`.
- `ChatUiState.Loading`.
- `ChatUiState.Success(latencyMs)`.
- `ChatUiState.Error(message, latencyMs)`.
- `ChatMessage`.
- `ChatMessage.Author.USER`.
- `ChatMessage.Author.SYSTEM`.

Relacion: el ViewModel publica estos estados y la UI los renderiza.

### `app/src/main/java/com/uader/ptah/ui/chat/UserEvent.kt`

Responsabilidad: representar eventos de una sola vez.

Contiene:

- `UserEvent.ShowError(message)`.

Relacion: el ViewModel lo emite por un `Channel`; la UI lo consume para mostrar Snackbar con opcion de reintentar.

### `app/src/main/java/com/uader/ptah/data/PtahRepository.kt`

Responsabilidad: capa de datos/repositorio.

Contiene:

- Interfaz `PtahRepository`.
- Implementacion `PtahRepositoryImpl`.
- Metodo `ask(query: String): Result<QueryResponse>`.
- Request Chat Completions para Groq API.

Relacion: recibe la consulta desde el ViewModel, arma el `GroqChatCompletionRequest`, llama al API service y devuelve texto limpio.

### `app/src/main/java/com/uader/ptah/data/PtahApiService.kt`

Responsabilidad: contrato HTTP de Retrofit.

Contiene:

- Interfaz `GroqApiService`.
- Metodo suspend `createChatCompletion`.
- Endpoint relativo `openai/v1/chat/completions`.

Relacion: usado por `PtahRepositoryImpl` para enviar consultas a Groq API.

### `app/src/main/java/com/uader/ptah/data/network/RetrofitProvider.kt`

Responsabilidad: configurar red.

Contiene:

- `RetrofitProvider`.
- `BASE_URL = "https://api.groq.com/"`.
- Cliente `OkHttpClient`.
- `ApiKeyInterceptor`.
- `LatencyInterceptor`.
- `ErrorInterceptor`.
- `ApiException`.

Relacion: crea `googleAiApiService`, agrega API key, mide latencia de red, registra logs seguros y convierte errores HTTP en mensajes controlados.

### `app/src/main/java/com/uader/ptah/data/PtahModels.kt`

Responsabilidad: modelos/DTOs.

Contiene:

- `QueryRequest`: modelo simple de entrada interna, actualmente no es central en el flujo final.
- `QueryResponse`: respuesta interna que contiene `answer`.
- `GroqChatCompletionRequest`.
- `GroqMessage`.
- `GroqChatCompletionResponse`.
- `GroqChoice`.

Relacion: el Repository usa estos modelos para armar la request y parsear la response.

### `app/src/main/java/com/uader/ptah/di/ServiceLocator.kt`

Responsabilidad: inyeccion de dependencias manual minima.

Contiene:

- `ServiceLocator`.
- `ptahRepository`.

Relacion: crea `PtahRepositoryImpl` con `RetrofitProvider.googleAiApiService`. El ViewModel lo usa a traves de su `Factory`.

### `app/src/main/java/com/uader/ptah/ui/theme/*`

Responsabilidad: configuracion visual.

Archivos:

- `Theme.kt`: define `PTAHTheme` y aplica Material Theme.
- `Color.kt`: colores base.
- `Type.kt`: tipografias.
- `Spacing.kt`: tokens de espaciado y extensiones como `screenHorizontalPadding`.

Relacion: la UI usa estos elementos para mantener coherencia visual.

### `README.md`

Responsabilidad: documentacion breve del proyecto, alcance, arquitectura, configuracion de API key y ejecucion.

### `.gitignore`

Responsabilidad: evitar subir archivos generados o sensibles.

Incluye:

- `.gradle`.
- `build`.
- `local.properties`.
- `.env`.
- `secrets.properties`.
- keystores.

### `local.properties`

Responsabilidad: configuracion local de la maquina.

Debe contener:

```properties
GROQ_API_KEY=TU_API_KEY_ACA
```

No se debe subir a GitHub. No se debe pegar una clave real en documentacion.

### `CONTRATO_API.md` y `GEMINI.md`

Responsabilidad: documentacion historica o auxiliar del desarrollo. `CONTRATO_API.md` describe el contrato anterior del mock local. `GEMINI.md` contiene notas previas del sprint de manejo de errores.

No representan por si solos el flujo final actual, que en el modulo `:app` usa Groq API.

## 8. Modelos y estados de la aplicacion

`ChatMessage` representa un mensaje dentro del historial. Tiene:

- `author`: indica si el mensaje lo envio el usuario o el sistema.
- `text`: contenido textual del mensaje.

`ChatMessage.Author.USER` se usa para mensajes del usuario. La UI los alinea a la derecha y usa color de contenedor primario.

`ChatMessage.Author.SYSTEM` se usa para respuestas del asistente. La UI los alinea a la izquierda y usa color de superficie variante.

El historial se guarda en el ViewModel mediante:

```kotlin
private val _messages = mutableStateListOf<ChatMessage>()
val messages: List<ChatMessage> = _messages
```

Esto permite que Compose reaccione cuando se agregan mensajes.

`ChatUiState` representa el estado de la pantalla:

- `Idle`: estado inicial, sin operacion en curso.
- `Loading`: hay una consulta en curso.
- `Success(latencyMs)`: llego respuesta correctamente y se conoce la latencia.
- `Error(message, latencyMs)`: ocurrio un error y se conoce, si corresponde, cuanto tardo.

`QueryResponse` representa la respuesta interna final que necesita la UI: un texto `answer`.

`GroqChatCompletionRequest` representa el JSON enviado a Groq API:

- `model`: `llama-3.1-8b-instant`.
- `messages`: lista de mensajes enviados a la API, con `role = "user"` y `content` tomado del input del usuario.

`GroqChatCompletionResponse` representa el JSON devuelto por Groq API. El texto se extrae desde:

```text
choices[0].message.content
```

Si no hay texto o viene vacio, se devuelve un error controlado.

## 9. Comunicacion con Groq API

La base URL configurada es:

```text
https://api.groq.com/
```

El endpoint relativo definido en Retrofit es:

```text
openai/v1/chat/completions
```

El modelo se configura en `PtahRepositoryImpl` como:

```text
model = "llama-3.1-8b-instant"
```

Retrofit se configura en `RetrofitProvider` con:

- `GsonConverterFactory`.
- `OkHttpClient`.
- Timeouts de conexion, lectura y escritura.
- Interceptor de API key.
- Interceptor de latencia.
- Interceptor de errores.
- Logging HTTP en debug con header `Authorization` redactado.

La API key se envia como header:

```text
Authorization: Bearer <clave local>
```

No se muestra ni se hardcodea en el codigo fuente. En logs solo se muestra si esta configurada y su longitud.

La request se arma en `PtahRepositoryImpl` con:

- Modelo `llama-3.1-8b-instant`.
- Mensaje `user` con la consulta textual del usuario.

Si Groq API devuelve respuesta vacia, el Repository lanza un error controlado: "No se obtuvo una respuesta valida."

Significado de errores:

- 400: solicitud invalida; puede indicar un body mal formado o parametro incorrecto.
- 401: no autorizado; suele indicar API key invalida.
- 403: acceso prohibido; puede indicar restricciones o permisos de API key.
- 429: limite de uso alcanzado; hay que esperar o revisar cuotas.
- 500 a 599: error del servidor de Groq API.

La clave no se hardcodea porque seria inseguro, dificil de rotar y riesgoso si el repositorio se comparte. Para prototipo se usa `local.properties`; para produccion conviene que la app consulte un backend propio.

## 10. Configuracion de API key

En la raiz del proyecto, al mismo nivel que `settings.gradle.kts`, `build.gradle.kts` y `app/`, debe existir `local.properties`.

Ejemplo:

```properties
GROQ_API_KEY=TU_API_KEY_ACA
```

Funcionamiento:

1. Gradle lee `rootProject.file("local.properties")`.
2. Busca exactamente la propiedad `GROQ_API_KEY`.
3. Recorta espacios y evita usar valores vacios.
4. Expone el valor mediante `BuildConfig.GROQ_API_KEY`.
5. El codigo Kotlin lee `BuildConfig.GROQ_API_KEY`.
6. `ApiKeyInterceptor` valida si esta configurada.
7. Si falta, la app muestra un error controlado.

`local.properties` no se sube a GitHub. Esta incluido en `.gitignore`.

No se debe pegar una clave real en:

- Codigo Kotlin.
- Gradle.
- README.
- Documentacion tecnica.
- Capturas de pantalla.
- Issues o commits.

## 11. Manejo de errores

Errores controlados:

- API key no configurada: `ApiKeyInterceptor` detecta key vacia y lanza `ApiException`.
- Sin conexion: `ErrorInterceptor` captura `IOException` y muestra mensaje de conexion.
- Timeout: `SocketTimeoutException` se transforma en mensaje de tiempo de espera.
- Error 400: solicitud invalida.
- Error 401: API key invalida o no autorizada.
- Error 403: permisos o restricciones de la API key.
- Error 429: limite de uso o cuota alcanzada.
- Error 500 a 599: error del servidor de Groq API.
- Respuesta vacia: el Repository detecta que no hay texto util.
- Error inesperado: el ViewModel usa un mensaje generico si no reconoce el caso.

Visualizacion en UI:

- `ChatUiState.Error` muestra una fila de error en pantalla.
- `UserEvent.ShowError` muestra un Snackbar con accion "Reintentar".
- La app no se cierra porque los errores se capturan y se convierten en estado de UI.

Esto es importante para la presentacion porque demuestra robustez minima: si falla la API, el usuario recibe feedback entendible y puede reintentar.

## 12. Latencia basica

Se mide el tiempo que tarda una consulta desde que el ViewModel inicia la operacion hasta que recibe exito o error.

En `ChatViewModel`:

- Se toma tiempo inicial con `SystemClock.elapsedRealtime()`.
- Se llama al Repository.
- Al volver la respuesta o el error, se calcula la diferencia en milisegundos.
- Se registra en Logcat.
- Se muestra en la UI dentro de `ChatUiState.Success` o `ChatUiState.Error`.

En `LatencyInterceptor` tambien se registra una latencia de red con tag `PtahLatency`.

La latencia sirve para la entrega parcial como metrica minima: permite decir cuanto tarda aproximadamente el ida y vuelta de una consulta. No es una evaluacion experimental completa ni una medicion estadistica formal.

## 13. Interfaz de usuario

La pantalla principal es `ChatScreen`.

Componentes:

- Barra superior: `TopAppBar` con titulo "PTAH - Asistente Normativo".
- Historial de mensajes: `LazyColumn` que muestra mensajes en orden.
- Mensajes del usuario: burbujas alineadas a la derecha.
- Mensajes del asistente: burbujas alineadas a la izquierda.
- Placeholder inicial: aparece cuando todavia no hay mensajes.
- Campo de consulta: `OutlinedTextField` con label "Consulta".
- Boton Enviar: `Button` que llama al ViewModel.
- Indicador de carga: `CircularProgressIndicator` y texto "Cargando...".
- Mensaje de exito: muestra "Respuesta recibida" y latencia.
- Mensaje de error: muestra el error y latencia si existe.
- Snackbar: aparece cuando ocurre error y ofrece "Reintentar".

El boton Enviar se deshabilita si:

- El input esta vacio.
- Hay una consulta en estado `Loading`.

El estilo visual es simple, academico y profesional. Usa Material 3 y el tema `PTAHTheme`.

## 14. Dependencias y configuracion Gradle

Plugins principales:

- `com.android.application`: permite compilar el modulo Android.
- `org.jetbrains.kotlin.plugin.compose`: habilita Kotlin con Compose.

Configuracion relevante:

- `namespace = "com.uader.ptah"`.
- `applicationId = "com.uader.ptah"`.
- `minSdk = 24`.
- `targetSdk = 36`.
- `compileSdk` configurado en el modulo.
- `buildFeatures.compose = true`.
- `buildFeatures.buildConfig = true`.

Dependencias principales:

- `androidx.core:core-ktx`: extensiones Kotlin para Android.
- `androidx.lifecycle:lifecycle-runtime-ktx`: integracion lifecycle con Kotlin.
- `androidx.activity:activity-compose`: soporte Compose dentro de Activity.
- Compose BOM: alinea versiones Compose.
- Compose UI, graphics y tooling.
- Material 3.
- `androidx.lifecycle:lifecycle-viewmodel-compose`: integracion ViewModel con Compose.
- Retrofit.
- Gson converter.
- OkHttp logging interceptor.

BuildConfig:

- `GROQ_API_KEY`: se genera desde `local.properties`.

La configuracion de Retrofit no esta en Gradle, sino en `RetrofitProvider.kt`.

## 15. Como ejecutar el proyecto

Pasos:

1. Abrir el proyecto raiz en Android Studio.
2. Verificar que el modulo seleccionado sea `:app`.
3. Configurar `GROQ_API_KEY` en `local.properties`.
4. Sincronizar Gradle.
5. Ejecutar en emulador o dispositivo Android.
6. Probar una consulta textual.
7. Revisar Logcat si hay errores.

Si sigue instalada una APK vieja:

1. Desinstalar la app del emulador o dispositivo.
2. Usar `Clean Project`.
3. Usar `Rebuild Project`.
4. Ejecutar nuevamente.

Para compilar desde terminal, en esta maquina se uso el JDK embebido de Android Studio y el SDK local. Si la terminal no encuentra Java, configurar `JAVA_HOME`.

## 16. Que mostrar en la presentacion

Guion breve:

1. PTAH busca facilitar consultas sobre reglamentacion institucional usando lenguaje natural.
2. Esta app movil es el cliente conversacional textual del sistema.
3. La entrega parcial se limita a texto, API, estados, arquitectura y documentacion.
4. La pantalla principal permite escribir una consulta y ver una respuesta tipo chat.
5. La arquitectura usa MVVM: UI, ViewModel, Repository y Network.
6. La consulta fluye desde `ChatScreen` hasta Groq API y vuelve como respuesta textual.
7. Groq API se usa temporalmente para validar el flujo hasta conectar el motor semantico real.
8. Si la respuesta es correcta, se agrega al historial y se muestra latencia.
9. Si ocurre un error, se muestra en pantalla y la app no se cierra.
10. La latencia es una metrica inicial para documentar el comportamiento del prototipo.
11. Para la etapa siguiente quedan backend semantico definitivo, STT, TTS y evaluacion completa.

## 17. Preguntas posibles del profesor y respuestas sugeridas

Pregunta: Que problema resuelve PTAH?

Respuesta: Facilita consultar reglamentacion institucional en lenguaje natural, evitando depender de busquedas manuales o palabras clave exactas.

Pregunta: Que entrega esta app en junio/julio?

Respuesta: Un prototipo funcional de cliente movil con chat textual, comunicacion con API, manejo de carga/error, arquitectura MVVM y latencia basica.

Pregunta: Por que usaron MVVM?

Respuesta: Porque separa interfaz, logica de estado y acceso a datos. Eso hace el codigo mas mantenible y permite reemplazar Groq API por el backend real sin reescribir la UI.

Pregunta: Por que usan Groq API?

Respuesta: Como integracion temporal para validar el flujo conversacional textual mientras se prepara la conexion definitiva con el motor semantico PTAH.

Pregunta: La voz ya esta implementada?

Respuesta: No. STT y TTS no forman parte del alcance de esta entrega parcial. Quedan para etapas posteriores.

Pregunta: Que pasa si falla la API?

Respuesta: La app captura el error, lo transforma en estado de UI, muestra un mensaje visible y evita que la aplicacion se cierre.

Pregunta: Donde esta la API key?

Respuesta: En `local.properties`, que no se sube al repositorio. Gradle la expone mediante `BuildConfig`.

Pregunta: La UI llama directamente a Groq API?

Respuesta: No. La UI llama al ViewModel, el ViewModel al Repository y el Repository al API Service.

Pregunta: Como se mide la latencia?

Respuesta: El ViewModel toma tiempo antes de iniciar la consulta y vuelve a medir cuando llega respuesta o error. La diferencia se muestra en milisegundos.

Pregunta: Que significa error 429?

Respuesta: Que se alcanzo un limite de uso o cuota de la API. La app lo muestra como error controlado.

Pregunta: Que ventaja tiene usar Repository?

Respuesta: Centraliza la logica de datos y permite cambiar la fuente de informacion sin afectar la pantalla.

Pregunta: Que pasara cuando este el backend real PTAH?

Respuesta: Se deberia reemplazar o adaptar la capa Repository/Network para llamar al backend semantico real, manteniendo el mismo flujo UI-ViewModel.

Pregunta: Por que no se hardcodea la API key?

Respuesta: Porque seria inseguro y podria filtrarse en el repositorio. Se configura localmente y se ignora por Git.

## 18. Pendientes y proximos pasos

Pendientes reales:

- Reemplazar Groq API por el backend semantico PTAH real o integrarlo como capa definitiva si corresponde.
- Agregar reconocimiento de voz STT.
- Agregar sintesis de voz TTS.
- Mejorar evaluacion de metricas.
- Agregar pruebas unitarias y de UI.
- Mejorar documentacion si cambia la arquitectura.
- Preparar informe tecnico intermedio.
- Revisar si `PtahApp/` debe archivarse o eliminarse en una limpieza futura.
- Revisar documentacion historica del mock para que no confunda con el flujo actual.

## 19. Resumen final para estudiar

## Resumen para defensa oral

PTAH Android es una app movil en Kotlin y Jetpack Compose que funciona como cliente conversacional textual para consultar reglamentacion institucional. Para la entrega parcial de junio/julio se implementa un prototipo con chat, comunicacion con API, estados de carga/respuesta/error, manejo de errores y latencia basica.

La arquitectura sigue MVVM: `ChatScreen` muestra la interfaz, `ChatViewModel` administra estado e historial, `PtahRepository` arma la consulta y `GroqApiService`/`RetrofitProvider` realizan la llamada HTTP a Groq API. Groq API se usa temporalmente para validar el flujo textual hasta conectar el motor semantico real de PTAH.

Cuando el usuario escribe y envia una consulta, la app agrega el mensaje al historial, muestra carga, mide latencia, llama al Repository, recibe una respuesta textual, la muestra en pantalla y registra el tiempo. Si falla la API o falta la clave, muestra un error visible sin cerrar la app.

Para junio queda listo el flujo textual funcional y explicable. Para despues quedan STT, TTS, backend semantico definitivo, pruebas avanzadas y evaluacion completa.
