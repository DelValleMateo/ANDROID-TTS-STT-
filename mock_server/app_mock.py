"""
Mock Server para PTAH - Sprint 8: STT Integrado al Flujo Conversacional
========================================================================
USO (línea de comandos — modo global):
    python app_mock.py              → responde 200 OK en TODOS los endpoints
    python app_mock.py --error 500  → fuerza HTTP 500 en cada petición
    python app_mock.py --error 404  → fuerza HTTP 404
    python app_mock.py --timeout    → simula timeout (duerme 30s sin responder)
    python app_mock.py --empty      → responde 200 con resultados vacíos

ENDPOINTS DEDICADOS (para probar sin reiniciar el servidor):
    POST /openai/v1/chat/completions   → endpoint compatible con la app real (Groq-like)
    POST /busqueda/semantica           → endpoint original del mock semántico
    POST /timeout                      → fuerza un timeout de 30s en esa petición
    POST /error500                     → fuerza HTTP 500 en esa petición
    POST /error400                     → fuerza HTTP 400 en esa petición
    GET  /health                       → verifica que el servidor está vivo

CÓMO REDIRIGIR LA APP AL MOCK SERVER (para E2E-04b, E2E-06):
    1. En RetrofitProvider.kt cambiar temporalmente:
           const val BASE_URL = "http://10.0.2.2:3000/"  // emulador
           const val BASE_URL = "http://<IP-PC>:3000/"   // dispositivo físico
    2. Ejecutar: python app_mock.py
    3. Usar /timeout o /error500 como endpoint en los pasos del test.
    4. IMPORTANTE: revertir BASE_URL antes de hacer push.

LOGCAT ESPERADO (filtrar por tags):
    adb logcat -s PtahLatency ChatViewModel PtahRepository SpeechRecognizerMgr
"""

import argparse
import http.server
import json
import time
from datetime import datetime

# ---------------------------------------------------------------------------
# Argumentos de línea de comandos (modo global)
# ---------------------------------------------------------------------------
parser = argparse.ArgumentParser(description="Mock Server de PTAH para testing de errores")
parser.add_argument("--error", type=int, default=None,
                    help="Código HTTP de error a forzar globalmente (ej: 500, 404, 503)")
parser.add_argument("--timeout", action="store_true",
                    help="Simula un timeout durmiendo 30 segundos antes de responder")
parser.add_argument("--empty", action="store_true",
                    help="Responde 200 pero con resultados vacíos")
args = parser.parse_args()

TIMEOUT_SECONDS = 30   # Mayor que el OkHttp timeout configurado (15s) para garantizar el corte

# ---------------------------------------------------------------------------
# Respuesta simulada de la IA (formato OpenAI-compatible)
# ---------------------------------------------------------------------------
def make_groq_response(content: str) -> dict:
    """Genera un JSON compatible con la respuesta de Groq API."""
    return {
        "id": "mock-chat-completion",
        "object": "chat.completion",
        "created": int(time.time()),
        "model": "llama-3.1-8b-instant",
        "choices": [
            {
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": content
                },
                "finish_reason": "stop"
            }
        ],
        "usage": {
            "prompt_tokens": 10,
            "completion_tokens": 20,
            "total_tokens": 30
        }
    }


class PtahMockHandler(http.server.BaseHTTPRequestHandler):

    def log_message(self, format, *a):
        """Suprimimos el log HTTP por defecto de Python para usar el nuestro."""
        pass

    # ── GET /health ──────────────────────────────────────────────────────────

    def do_GET(self):
        if self.path == "/health":
            self._send_json(200, {"status": "ok", "server": "PTAH Mock Sprint 8"})
            self._log("GET", "/health", 200)
        else:
            self._send_json(404, {"error": "Ruta no encontrada"})

    # ── POST ─────────────────────────────────────────────────────────────────

    def do_POST(self):
        # ── Modo global (desde args de CLI) ──────────────────────────────────
        if args.timeout and self.path not in ("/timeout", "/error500", "/error400"):
            self._force_timeout()
            return
        if args.error and self.path not in ("/timeout", "/error500", "/error400"):
            self._force_error(args.error)
            return

        # ── Ruta de la app real: OpenAI-compatible ───────────────────────────
        if self.path == "/openai/v1/chat/completions":
            self._handle_groq_endpoint()

        # ── Endpoints de testing dedicados ───────────────────────────────────
        elif self.path == "/timeout":
            self._force_timeout()

        elif self.path == "/error500":
            self._force_error(500)

        elif self.path == "/error400":
            self._force_error(400)

        # ── Endpoint semántico original ──────────────────────────────────────
        elif self.path == "/busqueda/semantica":
            self._handle_semantica()

        else:
            self._send_json(404, {"error": "Ruta no encontrada"})
            self._log("POST", self.path, 404)

    # ── Handlers específicos ─────────────────────────────────────────────────

    def _handle_groq_endpoint(self):
        """Simula la respuesta de Groq API para que la app funcione con el mock."""
        payload = self._read_json_body()
        consulta = ""
        if payload and "messages" in payload:
            for msg in payload["messages"]:
                if msg.get("role") == "user":
                    consulta = msg.get("content", "")
                    break

        self._log("POST", "/openai/v1/chat/completions", 200,
                  extra=f"consulta: \"{consulta[:60]}...\"" if len(consulta) > 60 else f"consulta: \"{consulta}\"")

        if args.empty:
            response = make_groq_response("")
            self._send_json(200, response)
            return

        respuesta = (
            f"[MOCK] Respuesta simulada para: \"{consulta}\". "
            "Según el reglamento interno, el artículo 45 establece que corresponde "
            "a los estudiantes un máximo de 15 días anuales por examen."
        )
        self._send_json(200, make_groq_response(respuesta))

    def _handle_semantica(self):
        """Endpoint semántico original del Sprint 5."""
        payload = self._read_json_body()
        consulta = payload.get("consulta", "(sin consulta)") if payload else "(sin consulta)"
        self._log("POST", "/busqueda/semantica", 200, extra=f"consulta: \"{consulta}\"")

        if args.empty:
            self._send_json(200, {"estado": "exito", "mensaje": "Búsqueda completada", "resultados": []})
            return

        resultados = [
            {
                "id_articulo": "Art-45",
                "titulo": "Licencias Estudiantiles",
                "contenido": "Corresponderá al estudiante un máximo de 15 días anuales por examen…",
                "score_relevancia": 0.98
            }
        ]
        self._send_json(200, {"estado": "exito", "mensaje": "Búsqueda completada", "resultados": resultados})

    def _force_timeout(self):
        """
        Duerme TIMEOUT_SECONDS para que OkHttp/Retrofit dispare SocketTimeoutException.
        Útil para E2E-04b y para probar el Snackbar de timeout de red.
        """
        self._log("POST", self.path, "TIMEOUT", extra=f"durmiendo {TIMEOUT_SECONDS}s...")
        print(f"    ⏳ [TIMEOUT] El servidor esperará {TIMEOUT_SECONDS}s sin responder.")
        print(f"       La app debería mostrar un Snackbar de timeout tras ~15s (OkHttp timeout).")
        time.sleep(TIMEOUT_SECONDS)
        # Respuesta tardía (la app ya habrá cortado la conexión)
        try:
            self._send_json(200, {"note": "respuesta tardía — la app ya debería haber cortado"})
        except Exception:
            pass

    def _force_error(self, code: int):
        """Responde con el código HTTP especificado. Útil para probar ErrorInterceptor."""
        mensajes = {
            400: "Petición malformada",
            401: "No autorizado — API key inválida",
            403: "Acceso prohibido",
            404: "Recurso no encontrado",
            429: "Límite de uso alcanzado",
            500: "Error interno del servidor mock",
            502: "Bad Gateway",
            503: "Servicio no disponible",
            504: "Gateway Timeout",
        }
        msg = mensajes.get(code, f"Error HTTP {code}")
        self._log("POST", self.path, code, extra=msg)
        print(f"    💥 [ERROR {code}] La app debería mostrar un Snackbar con el mensaje de error.")
        self._send_json(code, {"error": msg})

    # ── Utilidades ────────────────────────────────────────────────────────────

    def _read_json_body(self) -> dict | None:
        try:
            content_length = int(self.headers.get("Content-Length", 0))
            raw = self.rfile.read(content_length)
            return json.loads(raw.decode("utf-8"))
        except Exception:
            return None

    def _send_json(self, code: int, body: dict):
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _log(self, method: str, path: str, code, extra: str = ""):
        ts = datetime.now().strftime("%H:%M:%S")
        status_icon = "✅" if code == 200 else ("⏳" if code == "TIMEOUT" else "❌")
        extra_str = f" | {extra}" if extra else ""
        print(f"[{ts}] {status_icon} {method} {path} → {code}{extra_str}")


# ---------------------------------------------------------------------------
# Punto de entrada
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    PORT = 3000
    server = http.server.HTTPServer(("", PORT), PtahMockHandler)

    modo = "NORMAL"
    if args.timeout:
        modo = f"TIMEOUT GLOBAL ({TIMEOUT_SECONDS}s)"
    elif args.error:
        modo = f"ERROR FORZADO GLOBAL → HTTP {args.error}"
    elif args.empty:
        modo = "EMPTY (200 sin resultados)"

    print(f"{'='*60}")
    print(f"  MOCK SERVER PTAH — Sprint 8  |  Puerto {PORT}")
    print(f"  Modo global: {modo}")
    print(f"{'='*60}")
    print(f"  Endpoints disponibles:")
    print(f"    POST http://localhost:{PORT}/openai/v1/chat/completions  (app real)")
    print(f"    POST http://localhost:{PORT}/busqueda/semantica          (semántico)")
    print(f"    POST http://localhost:{PORT}/timeout                     (fuerza timeout)")
    print(f"    POST http://localhost:{PORT}/error500                    (fuerza HTTP 500)")
    print(f"    POST http://localhost:{PORT}/error400                    (fuerza HTTP 400)")
    print(f"    GET  http://localhost:{PORT}/health                      (liveness check)")
    print(f"{'='*60}")
    print(f"  Para redirigir la app: BASE_URL = \"http://10.0.2.2:{PORT}/\"  (emulador)")
    print(f"  Para detener: Ctrl+C")
    print(f"{'='*60}\n")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n\nServidor apagado. ¡Hasta la próxima!")