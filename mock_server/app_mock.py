"""
Mock Server para PTAH - Sprint 5: Manejo de Errores
====================================================
USO:
    python app_mock.py              → modo normal (respuesta 200 OK)
    python app_mock.py --error 500  → fuerza un Error 500 en cada petición
    python app_mock.py --error 404  → fuerza un Error 404
    python app_mock.py --timeout    → simula un timeout (espera 20 segundos sin responder)
    python app_mock.py --empty      → responde 200 pero con lista de resultados vacía

LOGCAT ESPERADO (filtrar por tags):
    adb logcat -s PtahLatency ChatViewModel PtahRepository
"""

import argparse
import http.server
import json
import time
from datetime import datetime

# ---------------------------------------------------------------------------
# Argumentos de línea de comandos
# ---------------------------------------------------------------------------
parser = argparse.ArgumentParser(description="Mock Server de PTAH para testing de errores")
parser.add_argument("--error", type=int, default=None,
                    help="Código HTTP de error a forzar (ej: 500, 404, 503)")
parser.add_argument("--timeout", action="store_true",
                    help="Simula un timeout durmiendo 20 segundos antes de responder")
parser.add_argument("--empty", action="store_true",
                    help="Responde 200 pero con resultados vacíos")
args = parser.parse_args()


class MateoMockHandler(http.server.BaseHTTPRequestHandler):

    def log_message(self, format, *a):
        """Suprimimos el log HTTP por defecto de Python para usar el nuestro."""
        pass

    def do_POST(self):
        if self.path != '/busqueda/semantica':
            self._send_json(404, {"error": "Ruta no encontrada"})
            return

        # 1. Leer y loguear el payload
        content_length = int(self.headers.get('Content-Length', 0))
        post_data = self.rfile.read(content_length)
        print(f"\n[{datetime.now().strftime('%H:%M:%S')}] >>> PETICIÓN RECIBIDA en {self.path}")
        try:
            payload = json.loads(post_data.decode('utf-8'))
            print(f"    Consulta: {payload.get('consulta', '(sin consulta)')}")
        except Exception:
            print("    ⚠ Payload no es JSON válido")

        # 2. ── MODO TIMEOUT ──────────────────────────────────────────────────
        if args.timeout:
            print("    [MODO TIMEOUT] Durmiendo 20s para provocar timeout en OkHttp…")
            time.sleep(20)
            self._send_json(200, {"estado": "exito", "mensaje": "Respuesta tardía", "resultados": []})
            return

        # 3. ── MODO ERROR HTTP FORZADO ───────────────────────────────────────
        if args.error:
            code = args.error
            mensajes = {
                400: "Petición malformada",
                401: "No autorizado",
                403: "Acceso prohibido",
                404: "Recurso no encontrado",
                500: "Error interno del servidor",
                502: "Bad Gateway",
                503: "Servicio no disponible",
                504: "Gateway Timeout",
            }
            msg = mensajes.get(code, f"Error HTTP {code}")
            print(f"    [MODO ERROR] Respondiendo {code}: {msg}")
            self._send_json(code, {"error": msg})
            return

        # 4. ── MODO NORMAL (200 OK) ──────────────────────────────────────────
        if args.empty:
            resultados = []
            print("    [MODO EMPTY] Respondiendo 200 con lista vacía")
        else:
            resultados = [
                {
                    "id_articulo": "Art-45",
                    "titulo": "Licencias Estudiantiles",
                    "contenido": "Corresponderá al estudiante un máximo de 15 días anuales por examen…",
                    "score_relevancia": 0.98
                }
            ]

        response_data = {
            "estado": "exito",
            "mensaje": "Búsqueda completada",
            "resultados": resultados
        }
        self._send_json(200, response_data)
        print(f"[{datetime.now().strftime('%H:%M:%S')}] <<< RESPUESTA 200 ENVIADA "
              f"({len(resultados)} resultado(s))")

    def _send_json(self, code: int, body: dict):
        data = json.dumps(body, ensure_ascii=False).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)


if __name__ == "__main__":
    PORT = 3000
    server = http.server.HTTPServer(('', PORT), MateoMockHandler)

    modo = "NORMAL"
    if args.timeout:
        modo = "TIMEOUT (20s)"
    elif args.error:
        modo = f"ERROR FORZADO → HTTP {args.error}"
    elif args.empty:
        modo = "EMPTY (200 sin resultados)"

    print(f"{'='*55}")
    print(f"  MOCK SERVER PTAH  |  Puerto {PORT}  |  Modo: {modo}")
    print(f"{'='*55}")
    print(f"  Endpoint: http://localhost:{PORT}/busqueda/semantica")
    print(f"  Para detener: Ctrl+C")
    print(f"{'='*55}\n")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n\nServidor apagado. ¡Hasta la próxima!")