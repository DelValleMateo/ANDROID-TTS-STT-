# Sprint 9 — Text To Speech

## Resultado

PTAH puede leer en voz alta cualquier respuesta del asistente. Cada burbuja del sistema ofrece
`Escuchar`; durante la reproducción muestra `Detener` y el estado `Reproduciendo...`.

## Tecnología y configuración

Se utiliza `android.speech.tts.TextToSpeech`, incluido en Android (sin dependencia externa). El
adaptador `AndroidTextToSpeechManager`:

- se inicializa con el contexto de aplicación;
- solicita español de Argentina (`es-AR`) y usa español de España como alternativa;
- informa si falta una voz en español;
- reemplaza el audio anterior al iniciar otra respuesta;
- divide respuestas que exceden el límite del motor y las encola en orden;
- permite detener inmediatamente;
- ejecuta `stop()` y `shutdown()` al liberar el ViewModel.

No requiere permiso de micrófono: `RECORD_AUDIO` pertenece solamente al módulo STT.

## Arquitectura

```text
Respuesta API -> ChatViewModel -> ChatMessage/estado TtsState -> ChatScreen
                                  |
                                  +-> SpeechOutput -> AndroidTextToSpeechManager -> TextToSpeech
```

La UI no conoce `TextToSpeech`. Solo observa `TtsState` y envía acciones al `ChatViewModel`. El
ViewModel depende del contrato `SpeechOutput`, creado por `ServiceLocator`, por lo que el motor se
puede reemplazar por un fake en tests o por otra tecnología sin cambiar Compose.

Estados disponibles: `Initializing`, `Idle`, `Speaking(messageId)` y `Error(message)`. El ID evita
marcar la burbuja incorrecta cuando dos respuestas tienen el mismo texto.

## Handover: cómo controlar la reproducción

Desde Compose:

- iniciar: `viewModel.onSpeakClicked(message)` (solo acepta mensajes del sistema);
- detener: `viewModel.onStopSpeakingClicked()`;
- dibujar estado: observar `viewModel.ttsState`; si es `Speaking` y el `messageId` coincide, esa
  burbuja está reproduciéndose.

Desde la capa de arquitectura, `SpeechOutput.speak(id, text)`, `stop()` y `shutdown()` son las únicas
operaciones necesarias. No llamar al motor Android directamente desde la pantalla.

## Accesibilidad

TTS permite consumir respuestas a personas con baja visión, dificultades de lectura o situaciones
en las que mirar la pantalla no resulta práctico. Los botones tienen descripciones semánticas y el
estado de reproducción usa una región viva para lectores de pantalla. El audio no se inicia solo:
queda bajo control explícito del usuario.

## Problemas encontrados y decisiones

- El motor posee un máximo de caracteres por utterance: se fragmentan respuestas largas y se
  considera finalizada la reproducción al terminar el último fragmento.
- La inicialización es asíncrona: si se pulsa antes de terminar, se conserva la última solicitud.
- Las voces dependen del dispositivo: se agregó fallback regional y un error visible si no existe
  español instalado.
- Reproducciones consecutivas pueden emitir callbacks tardíos: se validan contra el ID activo.

## Reporte de estabilidad

| Caso | Cobertura / resultado |
|---|---|
| Respuesta corta | Test unitario: conserva un único fragmento. |
| Respuesta larga | Test unitario: divide bajo el límite y conserva todo el texto. |
| Reproducción consecutiva | Implementado con `QUEUE_FLUSH`; la solicitud nueva reemplaza la anterior. |
| Detener | `stop()` limpia solicitud pendiente, detiene el motor y vuelve a `Idle`. |
| Cambiar respuesta | El estado usa ID; callbacks anteriores no alteran la respuesta activa. |
| Cerrar aplicación | `ViewModel.onCleared()` llama `shutdown()` y libera el motor. |

Los casos con salida sonora real deben validarse en un dispositivo o emulador con motor TTS y voz
española instalados. Checklist manual: reproducir texto corto/largo, alternar rápidamente entre dos
respuestas, detener a mitad, volver a reproducir y cerrar la actividad durante el audio; confirmar
que no continúa el sonido y que Logcat no informa fugas o excepciones.
