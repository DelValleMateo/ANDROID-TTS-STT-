# Proyecto PTAH - Cliente Movil Android

## Descripcion

Aplicacion Android en Kotlin para interactuar con el Proyecto PTAH mediante una interfaz conversacional con texto, STT y TTS.

## Estado actual

- Interaccion por teclado y reconocimiento de voz (STT).
- Respuestas visuales y sintesis de voz (TTS).
- Maquina de estados conversacional y reintentos.
- Comunicacion remota mediante Repository Pattern.
- Medicion de latencia y manejo de errores de red.

## Tecnologias utilizadas

- Kotlin.
- Android SDK.
- Jetpack Compose.
- Material 3.
- MVVM.
- Retrofit.
- Groq API como integracion temporal compatible con OpenAI Chat Completions.

## Arquitectura

El flujo principal de la consulta textual es:

```text
ChatScreen -> ChatViewModel -> PtahRepository -> QueryRemoteDataSource
                                                -> adaptador Groq temporal
```

- `ChatScreen`: pantalla Compose que muestra historial, input, carga y errores.
- `ChatViewModel`: administra estado de UI, historial conversacional, envio de consultas y latencia.
- `PtahRepository`: mantiene el contrato estable `ask(query): Result<QueryResponse>`, normaliza respuestas y traduce errores de red.
- `QueryRemoteDataSource`: frontera reemplazable entre la aplicacion y el proveedor remoto.
- `GroqRemoteDataSource`, `GroqApiService` y `RetrofitProvider`: adaptador temporal, contrato HTTP y configuracion centralizada.

Groq se utiliza provisoriamente para validar el flujo textual de consulta-respuesta hasta conectar el motor semantico real de PTAH.

## Configuracion de API Key

Agregar la clave de Groq en el archivo `local.properties` de la raiz del proyecto:

```properties
GROQ_API_KEY=TU_API_KEY_ACA
```

Opcionalmente, para un servidor compatible con el contrato temporal de Groq:

```properties
PTAH_API_BASE_URL=https://host/openai/v1/
GROQ_MODEL=openai/gpt-oss-120b
```

Cambiar solo la URL no adapta un contrato diferente. Cuando se publique el contrato PTAH
definitivo se debe implementar su `QueryRemoteDataSource` y seleccionarlo en la composicion
de red. No se debe guardar una credencial de servidor PTAH dentro de la app.

No subir claves reales al repositorio. El archivo `local.properties` esta ignorado por Git. Tambien se ignoran archivos `.env`, `secrets.properties`, keystores y archivos similares.

Para un entorno productivo, la recomendacion es que la app consulte un backend propio y que el backend proteja la API key. En este prototipo parcial, la clave se expone por `BuildConfig` solo para validar el flujo textual.

## Como ejecutar el proyecto

1. Abrir el proyecto en Android Studio.
2. Configurar `GROQ_API_KEY` en `local.properties`.
3. Sincronizar Gradle.
4. Ejecutar el modulo `:app` en un emulador o dispositivo Android.

Si se compila desde terminal, verificar que `JAVA_HOME` apunte a una instalacion valida de JDK.

## Pendiente para etapas posteriores

- Conexion definitiva con el motor semantico PTAH real.
- Evaluacion completa de usabilidad, eficacia y eficiencia.

Ver `DOCUMENTACION_SPRINT_11.md` para la auditoria del contrato y la preparacion de red.
