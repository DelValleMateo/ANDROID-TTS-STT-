# Sprint 11 - Integración PTAH y Optimización (RAFA - Red / Backend)

## Objetivo

Preparar la comunicación cliente-servidor para reemplazar el proveedor temporal por el
backend PTAH definitivo sin modificar la UI, `ChatViewModel`, STT, TTS ni la máquina de
estados conversacional.

## Estado anterior

El módulo activo es `:app`. La aplicación ya tenía el flujo completo
UI -> ViewModel -> Repository -> Retrofit, con Groq como proveedor temporal. El Repository
conservaba la interfaz adecuada `ask(query: String): Result<QueryResponse>`, pero además
conocía directamente los DTOs, el modelo y la forma de respuesta de Groq.

La red tenía timeouts de 15/30/15 segundos, autenticación Bearer, logging BODY en debug,
medición de latencia y un interceptor que convertía errores. Había responsabilidades
duplicadas entre interceptor, Repository y ViewModel, logs con consultas/respuestas o
cuerpos de error, y mensajes de error acoplados a Groq.

## Auditoría del contrato

No se encontró en el repositorio una URL, endpoint, autenticación, versionado, request ni
response del backend PTAH definitivo.

- `CONTRATO_API.md` y `PtahApp/` describen el mock histórico
  `POST /busqueda/semantica`; el propio documento lo vincula a Sprint 1/4 y localhost.
- `mock_server/` contiene endpoints de prueba de Sprint 8, incluido uno compatible con
  Groq. No es un backend productivo.
- `PROYECTO ANDROID.pdf` menciona HTTP/Retrofit y también “websocket”, pero no define
  protocolo, URL, eventos ni payloads suficientes para implementar una integración.
- La documentación del proyecto declara expresamente que Groq es provisorio.

Por estas razones no se inventó un contrato PTAH ni se reutilizó el mock como si fuera el
servicio definitivo.

## Proveedor, endpoint, request y response actuales

El proveedor funcional continúa siendo Groq:

- Base URL por defecto: `https://api.groq.com/openai/v1/`.
- Endpoint: `POST chat/completions`.
- Modelo por defecto: `openai/gpt-oss-120b`.
- Autenticación temporal: `Authorization: Bearer <GROQ_API_KEY>`.

La API oficial de Groq confirma el endpoint, los campos requeridos `model` y `messages`, y
la lectura de `choices[0].message.content`:
[API Reference](https://console.groq.com/docs/api-reference) y
[modelo GPT-OSS 120B](https://console.groq.com/docs/model/openai/gpt-oss-120b).

Request temporal simplificado:

```json
{
  "model": "openai/gpt-oss-120b",
  "messages": [
    { "role": "system", "content": "instrucciones de respuesta en texto plano" },
    { "role": "user", "content": "consulta" }
  ]
}
```

Response temporal consumida:

```json
{
  "choices": [
    { "message": { "content": "respuesta" } }
  ]
}
```

Estos DTOs están marcados como específicos de Groq; `QueryResponse(answer)` es el modelo
estable que recibe la aplicación.

## Cambios realizados

- Se agregó `QueryRemoteDataSource`, frontera neutral entre Repository y proveedor.
- `GroqRemoteDataSource` arma y valida el contrato temporal de Groq.
- `PtahRepositoryImpl` dejó de conocer Retrofit y DTOs de Groq; normaliza la respuesta y
  convierte fallos en `QueryException`/`QueryError` neutrales.
- `RetrofitProvider` centraliza cliente, proveedor activo y configuración.
- `PTAH_API_BASE_URL` y `GROQ_MODEL` pueden configurarse desde `local.properties` o una
  propiedad Gradle. La URL se valida durante la configuración del build.
- Se mantuvo `GROQ_API_KEY` fuera de Git y se eliminaron logs sobre su presencia/longitud.
- El logging HTTP debug pasó de BODY a BASIC: no registra consultas, responses ni headers.
- La latencia registra método, path, status o fallo, incluso cuando hay excepción, sin query.
- Se eliminó el interceptor que consumía y registraba cuerpos de error del servidor.
- El ViewModel ya no importa `ApiException`, `HttpException`, `IOException` ni menciona Groq.
- Se deshabilitó el permiso global de tráfico HTTP en claro usando el valor seguro por
  defecto de Android.
- Se agregaron pruebas unitarias del Repository y del adaptador/contrato HTTP temporal.

## Flujo de red resultante

```text
ChatScreen / STT
       -> ChatViewModel
       -> PtahRepository.ask(query)
       -> QueryRemoteDataSource
       -> GroqRemoteDataSource (temporal)
       -> GroqApiService / Retrofit / OkHttp
       -> QueryResponse
       -> ChatViewModel / UI / TTS
```

La futura integración PTAH cambia desde `QueryRemoteDataSource` hacia abajo. El contrato de
la aplicación y el flujo conversacional permanecen iguales.

## Errores contemplados

| Caso | Resultado de datos |
| --- | --- |
| Consulta vacía | `INVALID_REQUEST` sin ejecutar red |
| Credencial temporal ausente | `CONFIGURATION` |
| Timeout de socket o HTTP 408 | `TIMEOUT` |
| DNS, conexión o I/O | `CONNECTION` |
| HTTP 401/403 | `AUTHENTICATION` |
| HTTP 429 | `RATE_LIMIT` |
| Otros HTTP 4xx | `CLIENT_ERROR` |
| HTTP 5xx | `SERVER_ERROR` |
| Body nulo, vacío o JSON inválido | `INVALID_RESPONSE` |
| Error no clasificado | `UNKNOWN` |
| Cancelación de corrutina | Se relanza; no se convierte en fallo recuperable |

Los cuerpos de error no se muestran ni se escriben en Logcat.

## Timeouts

- `connectTimeout = 15 s`: margen razonable para DNS/TCP/TLS en una red móvil sin dejar la
  interfaz esperando indefinidamente.
- `readTimeout = 30 s`: la generación temporal puede tardar más que una API CRUD; 30 s
  limita la espera sin cortar respuestas normales.
- `writeTimeout = 15 s`: el request es un JSON pequeño, por lo que no necesita el mismo
  margen que la lectura.

Se conservaron los valores anteriores porque no existe un SLA de PTAH que justifique otros.
Cuando el backend definitivo publique métricas/SLA deben ajustarse con pruebas reales.

## Configuración local

```properties
GROQ_API_KEY=valor_local_no_versionado
PTAH_API_BASE_URL=https://api.groq.com/openai/v1/
GROQ_MODEL=openai/gpt-oss-120b
```

`PTAH_API_BASE_URL` permite usar otro host que sea compatible con el contrato temporal.
Cambiar solo la URL no adapta un request/response diferente. Aunque la clave temporal se
inyecta por `BuildConfig`, una app móvil no puede proteger un secreto de servidor: PTAH
definitivo debe evitar distribuir credenciales privadas dentro del APK.

## Pendiente cuando esté disponible PTAH definitivo

1. Confirmar transporte (HTTP o WebSocket), URL/versionado, endpoint y autenticación.
2. Incorporar DTOs exactos del request/response real, sin reutilizar los de Groq.
3. Implementar `PtahRemoteDataSource : QueryRemoteDataSource` y seleccionarlo en la
   composición de red.
4. Mapear la respuesta real a texto para `QueryResponse` y acordar códigos/errores.
5. Ajustar timeouts según SLA y validar HTTPS/certificados.
6. Agregar tests de contrato contra un entorno de integración PTAH.

Ninguno de estos puntos puede completarse correctamente sin la especificación o un entorno
del backend definitivo.

## Archivos modificados

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/uader/ptah/data/PtahApiService.kt`
- `app/src/main/java/com/uader/ptah/data/PtahModels.kt`
- `app/src/main/java/com/uader/ptah/data/PtahRepository.kt`
- `app/src/main/java/com/uader/ptah/data/QueryRemoteDataSource.kt`
- `app/src/main/java/com/uader/ptah/data/network/NetworkConfig.kt`
- `app/src/main/java/com/uader/ptah/data/network/NetworkExceptions.kt`
- `app/src/main/java/com/uader/ptah/data/network/RetrofitProvider.kt`
- `app/src/main/java/com/uader/ptah/di/ServiceLocator.kt`
- `app/src/main/java/com/uader/ptah/ui/chat/ChatViewModel.kt`
- `app/src/test/java/com/uader/ptah/data/PtahRepositoryTest.kt`
- `app/src/test/java/com/uader/ptah/data/GroqRemoteDataSourceTest.kt`
- `CONTRATO_API.md`
- `DOCUMENTACION_TECNICA.md`
- `README.md`
- `DOCUMENTACION_SPRINT_11.md`

## Pruebas realizadas

- Baseline previo a los cambios: 5 tests, 3 pasaron y 2 fallaron en
  `ChatViewModelTest` (líneas 62 y 90). Son fallos preexistentes de sincronización de los
  estados STT/TTS; no pertenecen a red.
- `:app:compileDebugKotlin`: **correcto**.
- Tests específicos `PtahRepositoryTest` + `GroqRemoteDataSourceTest`:
  **13/13 correctos**.
- Suite completa final: 18 tests, **16 correctos y los mismos 2 fallos preexistentes** de
  `ChatViewModelTest`; no se agregaron regresiones de red.
- `:app:assembleDebug`: **correcto**.
- `:app:assembleRelease`: **correcto**, incluidos R8, recursos optimizados y lint vital.
- `git diff --check`: **correcto**.

Los comandos se ejecutaron con JDK 17 mediante override porque `gradle.properties` contiene
una ruta Windows versionada (`C:/Program Files/Android/Android Studio/jbr`) que no existe en
el entorno macOS. No se modificó esa configuración por quedar fuera del alcance de red.

No se hizo una llamada real a Groq porque el repositorio auditado no contiene ni debe
contener una API key. El endpoint/request/response se validó con documentación oficial y
MockWebServer, sin depender de secretos ni de un servicio externo durante los tests.
