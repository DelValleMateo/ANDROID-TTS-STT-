# Documentación Técnica – Módulo STT (Speech To Text)

**Proyecto:** PTAH Android  
**Sprint:** 7 – Primera integración STT  
**Responsable de documentación:** Jesús  
**Fecha:** Segundo cuatrimestre 2026

---

## 1. Objetivo del módulo STT

Permitir que el usuario pueda presionar un botón en la pantalla de chat, hablar en voz natural, y ver el texto reconocido automáticamente en el campo de consulta. El texto reconocido puede enviarse a la IA o editarse antes del envío.

El flujo completo implementado es:

```
[Usuario presiona 🎙️]
        ↓
Verificación de permiso RECORD_AUDIO
        ↓ (si fue otorgado)
SpeechRecognizerManager.startListening()
        ↓ (StateFlow)
SttState.Listening → UI: banner rojo + punto pulsante "Escuchando..."
        ↓
SttState.Processing → UI: spinner "Procesando voz..."
        ↓
SttState.Result(text) → ChatViewModel → inputText = text
        ↓
Usuario revisa y presiona "Enviar"
        ↓
[Flujo textual existente: Groq API → respuesta]
```

---

## 2. Tecnología elegida: SpeechRecognizer nativo de Android

### Descripción
`android.speech.SpeechRecognizer` es la API nativa de Android para reconocimiento de voz, disponible desde API 8 (Android 2.2). En la mayoría de los dispositivos está respaldada por el motor de Google Speech Recognition.

### Motivos de elección
- **Sin dependencias externas:** No requiere agregar ninguna librería de terceros al proyecto.
- **Compatible con la API mínima del proyecto (24):** Funciona desde Android 7.0 sin restricciones.
- **Integración directa con el ecosistema Android:** La API expone callbacks bien definidos (`RecognitionListener`) que se traducen limpiamente a un `StateFlow<SttState>`.
- **Idioma español:** Soporte nativo para `es-AR` (español argentino).
- **Mantenimiento:** Mantenida por Google, evoluciona con el sistema operativo.

---

## 3. Alternativas evaluadas

| Tecnología | Ventajas | Desventajas | Descartada por |
|------------|----------|-------------|---------------|
| **OpenAI Whisper** | Alta precisión, funciona offline | Requiere descargar modelos grandes (>100 MB), integración compleja | Peso del modelo y complejidad para el sprint inicial |
| **Google Cloud Speech-to-Text API** | Muy alta precisión, múltiples idiomas | Requiere cuenta de Google Cloud, costo por uso, latencia de red adicional | Costo y complejidad de configuración |
| **CMU Sphinx (offline)** | Funciona sin internet | Baja precisión en español, sin mantenimiento activo | Baja calidad de reconocimiento |
| **SpeechRecognizer nativo ✓** | Sin dependencias, API estable | Requiere internet en la mayoría de dispositivos | **Elegida** |

---

## 4. Permisos necesarios

### En AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### Permiso en tiempo de ejecución (Android 6.0+)
`RECORD_AUDIO` es un permiso peligroso que requiere aprobación explícita del usuario en tiempo de ejecución. El flujo implementado:

1. El usuario toca el botón 🎙️.
2. `ChatScreen` verifica con `ContextCompat.checkSelfPermission()`.
3. Si no está concedido: se lanza `rememberLauncherForActivityResult(RequestPermission)`.
4. El sistema muestra el diálogo del sistema.
5. Si el usuario acepta: se inicia el reconocimiento.
6. Si el usuario deniega: se muestra un Snackbar con mensaje informativo.

---

## 5. Arquitectura del módulo

### Nuevos archivos creados

| Archivo | Capa | Responsabilidad |
|---------|------|-----------------|
| `SttState.kt` | UI/Domain | Estado sellado del ciclo de vida STT |
| `SpeechRecognizerManager.kt` | Data | Wrapper de SpeechRecognizer con StateFlow |
| `UserEvent.kt` (modificado) | UI | Eventos ShowSttError y RequestMicPermission |
| `ChatViewModel.kt` (modificado) | UI/ViewModel | Observa sttState, expone onMicClicked() |
| `ChatScreen.kt` (modificado) | UI | Botón mic, banner, animaciones, permisos |
| `ServiceLocator.kt` (modificado) | DI | createSpeechManager(context) |

### Diagrama de estados STT

```
        ┌─────────────────────────────┐
        │           Idle              │◄──── cancel() / resetToIdle()
        └─────────┬───────────────────┘
                  │ startListening()
                  ▼
        ┌─────────────────────────────┐
        │         Listening           │ ──── cancel() ──► Idle
        └─────────┬───────────────────┘
                  │ onEndOfSpeech()
                  ▼
        ┌─────────────────────────────┐
        │        Processing           │
        └──────┬──────────┬───────────┘
               │          │
         onResults()   onError()
               │          │
               ▼          ▼
         Result(text)  Error(msg)
               │          │
          inputText=text  Snackbar
               │
          resetToIdle()
               │
              Idle
```

---

## 6. Flujo esperado en la interfaz

| Estado STT | Botón micrófono | Banner | Campo de texto |
|------------|----------------|--------|----------------|
| `Idle` | 🎙️ gris (clickeable) | Oculto | Editable normalmente |
| `Listening` | 🔴 rojo (clickeable para cancelar) | "Escuchando..." + punto rojo pulsante + ❌ Cancelar | Editable normalmente |
| `Processing` | ⏳ spinner (deshabilitado) | "Procesando voz..." + spinner | Editable normalmente |
| `Result(text)` | 🎙️ gris | Oculto | Campo lleno con texto reconocido |
| `Error(msg)` | 🎙️ gris | Oculto | Sin cambios, Snackbar con error |
| `PermissionDenied` | 🎙️ gris | Oculto | Sin cambios, Snackbar de permiso |

---

## 7. Primeras pruebas y resultados

Ver documento: [`mock_server/MatrizPruebasSTT.md`](./mock_server/MatrizPruebasSTT.md)

---

## 8. Limitaciones conocidas y documentadas

1. **Requiere internet** en la mayoría de los dispositivos Android (el motor de reconocimiento de Google procesa en la nube).
2. **Comportamiento en emulador:** El emulador puede no reconocer voz correctamente dependiendo de la configuración de audio del host. Se recomienda probar en dispositivo físico para resultados representativos.
3. **Precisión variable por ruido:** En ambientes con ruido de fondo, el motor puede devolver `ERROR_NO_MATCH` o reconocer palabras incorrectas.
4. **Un idioma a la vez:** El reconocedor está configurado para `es-AR`. No reconoce automáticamente mezcla de idiomas.
5. **Límite de duración:** El motor tiene un límite de escucha interno (aprox. 10-15 segundos) tras el cual dispara `ERROR_SPEECH_TIMEOUT`.
6. **No funciona en paralelo:** Solo puede haber una instancia activa de `SpeechRecognizer` a la vez. El `SpeechRecognizerManager` destruye la instancia anterior antes de crear una nueva.

---

## 9. Handover a Sprint 8

Para el próximo sprint (Text To Speech / TTS), la interfaz de `SttState` y el patrón de `StateFlow` pueden replicarse directamente para modelar el estado del sintetizador de voz (`TtsState`). El `ServiceLocator` deberá exponer también un `createTtsManager(context)`.
