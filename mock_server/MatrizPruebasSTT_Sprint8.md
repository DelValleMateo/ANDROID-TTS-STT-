# Matriz de Pruebas E2E – Sprint 8: STT Integrado al Flujo Conversacional

**Proyecto:** PTAH Android  
**Sprint:** 8 – STT Integrado al Flujo Conversacional  
**Responsable:** Mateo  
**Motor de reconocimiento:** SpeechRecognizer nativo de Android (Google)  
**Idioma configurado:** es-AR  
**Dispositivo recomendado:** Dispositivo físico Android (emulador con micrófono virtual habilitado como alternativa)

---

## Objetivo de la batería

Validar el ciclo completo **Hablar → Reconocer → (Editar) → Consultar → Responder** cubriendo el flujo ideal y todos los casos de borde identificados por el equipo.

---

## Configuración previa a las pruebas

1. Ejecutar la app desde Android Studio con la API key configurada en `local.properties`.
2. Abrir Logcat y filtrar por los tags: `SpeechRecognizerMgr ChatViewModel PtahLatency`.
3. Para pruebas de red: usar WiFi y tener disponible la opción de desactivarla rápidamente.
4. Tener a mano el mock server (`mock_server/app_mock.py`) para E2E-04b y E2E-06.

---

## Escenarios E2E Obligatorios

### E2E-01 — Flujo Ideal: Reconocimiento correcto y respuesta exitosa

**Objetivo:** Verificar el camino feliz completo desde la voz hasta la respuesta de la IA.

**Precondiciones:**
- WiFi activo.
- Permiso de micrófono concedido.

**Pasos:**
1. Abrir la app. Verificar que el historial está vacío y el placeholder "Escribí una consulta o usá el micrófono 🎙️" es visible.
2. Tocar el botón 🎙️.
3. Esperar el banner "Escuchando..." (punto rojo pulsante visible).
4. Decir en voz clara: **"¿Cuántos días de licencia tiene un docente?"**
5. Esperar la transición automática a "Procesando voz..." (spinner).
6. Observar que el campo de texto se llena con el texto reconocido.
7. Verificar que el label del campo dice **"Consulta (por voz)"** y el borde es color secundario.
8. Tocar **"Enviar"**.
9. Verificar: burbuja del usuario visible con el texto reconocido + badge 🎙️ "por voz".
10. Verificar: spinner "Cargando..." visible mientras la API responde.
11. Verificar: burbuja de respuesta del sistema visible.
12. Verificar: latencia en ms mostrada en el StatusRow.

**Resultado esperado:** Ciclo completo exitoso. Badge 🎙️ visible en la burbuja del usuario.

**Logcat a verificar:**
```
D/ChatViewModel: STT Result → inputText: "..." | origin: VOICE
D/ChatViewModel: Inicio de consulta a Groq: ...
D/ChatViewModel: Fin de consulta a Groq. Latencia: XXXms
```

| Campo | Esperado | Obtenido | ¿OK? |
|-------|----------|----------|------|
| Banner "Escuchando..." visible | ✓ | | |
| Transición a "Procesando voz..." | ✓ | | |
| Campo lleno con texto reconocido | ✓ | | |
| Label "Consulta (por voz)" | ✓ | | |
| Borde secundario en el campo | ✓ | | |
| Badge 🎙️ en burbuja de usuario | ✓ | | |
| Respuesta de IA en historial | ✓ | | |
| Latencia mostrada | ✓ | | |

---

### E2E-02 — Intervención: Reconocimiento incorrecto que el usuario edita antes de enviar

**Objetivo:** Confirmar que el campo es editable post-STT y que el badge de voz desaparece al editar.

**Precondiciones:**
- WiFi activo. Permiso concedido.

**Pasos:**
1. Tocar 🎙️ y decir: **"Hola mundo"** (texto simple, fácil de reconocer).
2. Verificar que el campo se llena con "Hola mundo" y muestra el badge "por voz".
3. Editar manualmente el campo: reemplazar "Hola mundo" por **"¿Cuántos días de licencia tiene un docente?"**.
4. Verificar que el label vuelve a "Consulta" (sin "(por voz)") y el borde es el color estándar.
5. Tocar **"Enviar"**.
6. Verificar que la burbuja del usuario **NO** muestra el badge 🎙️ (porque el texto fue editado manualmente).
7. Verificar respuesta de la IA.

**Resultado esperado:** El badge de voz desaparece al editar. El envío funciona normalmente.

| Campo | Esperado | Obtenido | ¿OK? |
|-------|----------|----------|------|
| Campo editable post-STT | ✓ | | |
| Label vuelve a "Consulta" al editar | ✓ | | |
| Badge 🎙️ AUSENTE en burbuja (texto editado) | ✓ | | |
| Respuesta de IA en historial | ✓ | | |

---

### E2E-03 — Interrupción: El usuario presiona Cancelar mientras el STT está escuchando

**Objetivo:** Confirmar que cancelar el STT devuelve la app a estado Idle sin texto ni errores.

**Precondiciones:**
- Permiso concedido. No es necesario WiFi activo.

**Pasos:**
1. Tocar 🎙️.
2. Esperar que aparezca el banner "Escuchando...".
3. **Sin hablar**, tocar el botón ❌ "Cancelar" del banner.
4. Verificar que el banner desaparece (con animación slide + fade).
5. Verificar que el campo de texto sigue **vacío**.
6. Verificar que el botón mic vuelve al estado gris 🎙️ (no rojo).
7. Verificar que NO aparece ningún Snackbar de error.
8. Verificar en Logcat: `D/SpeechRecognizerMgr: Cancelando reconocimiento STT.`

**Resultado esperado:** Estado Idle limpio. Sin texto residual. Sin crash.

| Campo | Esperado | Obtenido | ¿OK? |
|-------|----------|----------|------|
| Banner desaparece con animación | ✓ | | |
| Campo vacío | ✓ | | |
| Botón mic gris (Idle) | ✓ | | |
| Sin Snackbar de error | ✓ | | |
| Log de cancelación en Logcat | ✓ | | |

---

### E2E-04 — Fallo de Red durante el dictado

**Objetivo:** Verificar el comportamiento cuando se pierde la conexión durante el uso del STT.

**Precondiciones:**
- Permiso concedido.

#### E2E-04a — Sin WiFi durante el dictado (fallo STT)

**Pasos:**
1. **Desactivar WiFi** en el dispositivo.
2. Tocar 🎙️ e intentar hablar.
3. Esperar el error del motor de reconocimiento.
4. Verificar que aparece un **Snackbar con el error de red del STT** (ej: "Error de red. Verificá tu conexión a Internet.").
5. Verificar que la app no se cierra.
6. Reactivar WiFi.

**Resultado esperado:** Snackbar de error STT. Sin crash.

| Campo | Esperado | Obtenido | ¿OK? |
|-------|----------|----------|------|
| Snackbar con error de red STT | ✓ | | |
| App no se cierra | ✓ | | |
| Log: `E/SpeechRecognizerMgr: onError código=...` | ✓ | | |

#### E2E-04b — Sin WiFi al enviar consulta a la API (fallo Groq)

**Pasos:**
1. Tocar 🎙️ y dictar un texto cualquiera. Esperar resultado en el campo.
2. **Desactivar WiFi**.
3. Tocar **"Enviar"**.
4. Verificar: Snackbar "Reintentar" con mensaje de conexión.
5. Verificar: StatusRow muestra el error (fondo rojo).
6. Tocar **"Reintentar"** en el Snackbar.
7. Reactivar WiFi y esperar la respuesta.

**Resultado esperado:** Snackbar con "Reintentar". Al reintentar con WiFi, la consulta tiene éxito.

| Campo | Esperado | Obtenido | ¿OK? |
|-------|----------|----------|------|
| Snackbar con "Reintentar" | ✓ | | |
| StatusRow en estado Error | ✓ | | |
| Reintento exitoso al recuperar WiFi | ✓ | | |
| App no se cierra | ✓ | | |

---

### E2E-05 — Silencio: STT finaliza con reconocimiento vacío

**Objetivo:** Confirmar que el silencio total muestra un Snackbar de error sin crashear.

**Precondiciones:**
- WiFi activo. Permiso concedido.

**Pasos:**
1. Tocar 🎙️.
2. **No hablar.** Esperar en silencio completo (~15 segundos hasta el timeout).
3. Observar la transición automática: Listening → Processing → Error.
4. Verificar: Snackbar con mensaje de error (ej: "No se detectó voz. Intentá hablar más fuerte." o "No se detectó ninguna palabra. Intentá de nuevo.").
5. Verificar que el campo de texto sigue vacío.
6. Verificar que el botón mic vuelve al estado gris (Idle).

**Resultado esperado:** Snackbar de error de timeout/silencio. Campo vacío. Sin crash.

| Campo | Esperado | Obtenido | ¿OK? |
|-------|----------|----------|------|
| Transición automática a Error tras silencio | ✓ | | |
| Snackbar con mensaje de error | ✓ | | |
| Campo vacío | ✓ | | |
| Botón mic en Idle | ✓ | | |
| Log: `E/SpeechRecognizerMgr: onError código=6 o 7` | ✓ | | |

---

### E2E-06 — Estrés: Múltiples consultas secuenciales rápidas

**Objetivo:** Verificar que la app no crashea ni queda en estado inconsistente ante uso intensivo.

**Precondiciones:**
- WiFi activo. Permiso concedido.

**Pasos:**
1. **Ciclo 1:** Escribir "¿Qué es un arancel?" por teclado → Enviar. Sin esperar respuesta, pasar al paso 2.
2. **Ciclo 2:** Tocar 🎙️ → Dictar "Licencia por maternidad" → Esperar campo lleno → Enviar.
3. **Ciclo 3:** Escribir "Reglamento de cursado" → Enviar.
4. **Ciclo 4:** Tocar 🎙️ → Cancelar antes de hablar → Escribir "Beca estudiantil" → Enviar.
5. **Ciclo 5:** Tocar 🎙️ → Dictar texto → Editar campo → Enviar.
6. Esperar que todas las respuestas lleguen y se muestren en el historial.
7. Verificar que los badges 🎙️ aparecen únicamente en los mensajes dictados sin editar (Ciclo 2).
8. Verificar que el historial tiene exactamente **5 pares** de mensajes usuario/sistema.

**Resultado esperado:** 5 pares correctos en historial. Badges 🎙️ solo donde corresponde. Sin crash.

| Campo | Esperado | Obtenido | ¿OK? |
|-------|----------|----------|------|
| Sin crash durante 5 ciclos | ✓ | | |
| 5 mensajes de usuario en historial | ✓ | | |
| 5 respuestas de sistema en historial | ✓ | | |
| Badge 🎙️ en Ciclo 2 (dictado sin editar) | ✓ | | |
| Badge 🎙️ AUSENTE en Ciclo 5 (dictado editado) | ✓ | | |
| Sin estado STT residual (mic en Idle al final) | ✓ | | |

---

## Métricas a registrar

| Métrica | Ciclo 1 | Ciclo 2 | Ciclo 3 | Ciclo 4 | Ciclo 5 | Promedio |
|---------|---------|---------|---------|---------|---------|----------|
| Latencia STT (tap mic → texto en campo) ms | — | | — | — | | |
| Latencia API (Enviar → respuesta) ms | | | | | | |
| ¿Hubo crash? | | | | | | |

---

## Registro de problemas detectados

| # | Descripción del problema | Escenario relacionado | Pasado a |
|---|--------------------------|-----------------------|----------|
| | | | |

---

## Instrucciones rápidas de Logcat

```bash
# Filtrar solo tags relevantes del Sprint 8
adb logcat -s SpeechRecognizerMgr ChatViewModel PtahLatency

# Capturar log completo de una sesión de prueba
adb logcat -s SpeechRecognizerMgr ChatViewModel PtahLatency > sprint8_log.txt
```
