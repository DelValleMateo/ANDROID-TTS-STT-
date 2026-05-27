# Proyecto PTAH - Cliente Movil Android

## Descripcion

Aplicacion Android en Kotlin para interactuar con el Proyecto PTAH, un sistema de busqueda semantica de reglamentacion institucional, mediante una interfaz conversacional textual tipo chatbot.

## Alcance de la entrega parcial junio/julio

- Prototipo funcional con interaccion textual.
- Interfaz tipo chatbot.
- Comunicacion con API.
- Manejo de estados: carga, respuesta y error.
- Arquitectura base.
- Informe tecnico intermedio.

No se implementan en esta etapa reconocimiento de voz, sintesis de voz, tests avanzados ni evaluacion experimental completa.

## Tecnologias utilizadas

- Kotlin.
- Android SDK.
- Jetpack Compose.
- Material 3.
- MVVM.
- Retrofit.
- Google IA como integracion temporal.

## Arquitectura

El flujo principal de la consulta textual es:

```text
ChatScreen -> ChatViewModel -> PtahRepository -> GoogleAiApiService/RetrofitProvider -> Google IA
```

- `ChatScreen`: pantalla Compose que muestra historial, input, carga y errores.
- `ChatViewModel`: administra estado de UI, historial conversacional, envio de consultas y latencia.
- `PtahRepository`: arma el prompt base del Proyecto PTAH, llama a la API y devuelve una respuesta textual limpia.
- `GoogleAiApiService` y `RetrofitProvider`: capa de red con Retrofit, API key, timeouts y manejo basico de errores.

Google IA se utiliza provisoriamente para validar el flujo textual de consulta-respuesta hasta conectar el motor semantico real de PTAH.

## Configuracion de API Key

Agregar la clave de Google IA en el archivo `local.properties` de la raiz del proyecto:

```properties
GOOGLE_AI_API_KEY=TU_API_KEY_ACA
```

No subir claves reales al repositorio. El archivo `local.properties` esta ignorado por Git. Tambien se ignoran archivos `.env`, `secrets.properties`, keystores y archivos similares.

Para un entorno productivo, la recomendacion es que la app consulte un backend propio y que el backend proteja la API key. En este prototipo parcial, la clave se expone por `BuildConfig` solo para validar el flujo textual.

## Como ejecutar el proyecto

1. Abrir el proyecto en Android Studio.
2. Configurar `GOOGLE_AI_API_KEY` en `local.properties`.
3. Sincronizar Gradle.
4. Ejecutar el modulo `:app` en un emulador o dispositivo Android.

Si se compila desde terminal, verificar que `JAVA_HOME` apunte a una instalacion valida de JDK.

## Estado actual

- Consulta textual desde la pantalla principal.
- Respuesta textual mediante Google IA.
- Historial conversacional con mensajes de usuario y sistema.
- Estado de carga durante la consulta.
- Manejo de errores visible en pantalla.
- Latencia basica registrada en Logcat y mostrada en la UI.

## Pendiente para etapas posteriores

- Conexion definitiva con el motor semantico PTAH real.
- Reconocimiento de voz.
- Sintesis de voz.
- Evaluacion completa de usabilidad, eficacia y eficiencia.
