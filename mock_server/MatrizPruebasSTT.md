# Matriz de Pruebas – Sprint 7: Speech To Text

**Proyecto:** PTAH Android  
**Sprint:** 7 – Primera integración STT  
**Responsable:** Mateo  
**Motor de reconocimiento:** SpeechRecognizer nativo de Android (Google)  
**Idioma configurado:** es-AR  
**Dispositivo de prueba:** Emulador / dispositivo físico Android

---

## Casos de Prueba

| # | Caso | Entrada (voz) | Estado esperado UI | Resultado en campo de texto | Resultado real | Observaciones |
|---|------|--------------|-------------------|----------------------------|---------------|---------------|
| 1 | Frase corta simple | "Hola" | Listening → Processing → campo lleno | "Hola" | | |
| 2 | Frase corta técnica | "Vacaciones anuales" | Listening → Processing → campo lleno | "Vacaciones anuales" | | |
| 3 | Frase larga (10+ palabras) | "¿Cuántos días de licencia corresponden a un docente universitario de dedicación exclusiva?" | Listening → Processing → campo lleno | Texto completo | | Puede truncar |
| 4 | Silencio total | (sin hablar, esperar timeout) | Listening → Error | — | | ERROR_SPEECH_TIMEOUT esperado |
| 5 | Cancelación durante escucha | Presionar ❌ mientras dice "Escuchando..." | Listening → Idle | Campo vacío | | Banner debe desaparecer |
| 6 | Ruido ambiente sin voz clara | (ruido de fondo) | Listening → Error | — | | ERROR_NO_MATCH esperado |
| 7 | Error de permisos | Denegar permiso de micrófono al prompt | — | — | | Snackbar de error, no crash |
| 8 | Segunda consulta consecutiva | Hablar, esperar resultado, hablar de nuevo | Idle → Listening... | Segundo texto | | Verificar que se destruye el reconocedor anterior |
| 9 | Texto reconocido + edición manual | Hablar, luego editar el campo de texto | Campo lleno + cursor activo | Texto editado | | El campo debe ser editable |
| 10 | Texto reconocido + enviar | Hablar, presionar Enviar | Campo lleno → Loading → respuesta | Respuesta de Groq | | Flujo STT→Groq completo |
| 11 | Botón micrófono durante carga de Groq | Hablar mientras la IA está respondiendo | Mic disponible, carga en paralelo | — | | Los dos estados deben coexistir |
| 12 | Sin conexión a Internet | Desactivar WiFi y hablar | Listening → Error | — | | ERROR_NETWORK esperado |

---

## Métricas a registrar

| Métrica | Valor |
|---------|-------|
| Tiempo desde tap mic hasta "Escuchando..." | ms |
| Tiempo desde fin de habla hasta texto en campo | ms |
| Tasa de reconocimiento correcto (casos 1-3) | % |
| Casos que provocaron crash | # |

---

## Instrucciones para ejecutar las pruebas

### En emulador
1. Asegurarse de que el emulador tiene configurado el micrófono del host (AVD Settings → Microphone → Virtual microphone uses host audio input).
2. Ejecutar la app desde Android Studio.
3. Ir a Logcat, filtrar por los tags: `SpeechRecognizerMgr ChatViewModel`.

### En dispositivo físico
1. Conectar el dispositivo con `adb devices`.
2. Instalar el APK debug.
3. Probar en entorno silencioso y en entorno ruidoso.

### Forzar error de permisos
1. Android Studio → Device Explorer → Settings → Apps → PTAH → Permisos → Micrófono → Denegar.
2. Intentar presionar el botón de micrófono.
3. Verificar que aparece el Snackbar con el mensaje de error.

---

## Problemas detectados

*(Completar durante las pruebas)*

| # | Descripción del problema | Caso relacionado | Pasado a Luca |
|---|--------------------------|-----------------|--------------|
| | | | |
