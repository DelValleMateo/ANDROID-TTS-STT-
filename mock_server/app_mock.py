import http.server
import json
from datetime import datetime

class MateoMockHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        # Validamos la ruta que pidió Luca
        if self.path == '/busqueda/semantica':
            # 1. Leer lo que nos envían
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)

            # 2. Monitoreo en consola (Log de QA)
            print(f"\n[{datetime.now().strftime('%H:%M:%S')}] >>> PETICIÓN RECIBIDA")
            try:
                payload = json.loads(post_data.decode('utf-8'))
                print(f"Consulta recibida: {payload.get('consulta', 'Sin consulta')}")
            except:
                print("Error: El JSON recibido no es válido")

            # 3. Respuesta del contrato
            response_data = {
                "estado": "exito",
                "mensaje": "Búsqueda completada",
                "resultados": [
                    {
                        "id_articulo": "Art-45",
                        "titulo": "Licencias Estudiantiles",
                        "contenido": "Corresponderá al estudiante un máximo de 15 días anuales por examen...",
                        "score_relevancia": 0.98
                    }
                ]
            }

            # 4. Enviar Headers y JSON
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(response_data).encode('utf-8'))
            print(f"[{datetime.now().strftime('%H:%M:%S')}] <<< RESPUESTA ENVIADA")
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == "__main__":
    PORT = 3000
    server = http.server.HTTPServer(('', PORT), MateoMockHandler)
    print(f"--- MOCK SERVER INICIADO (PUERTO {PORT}) ---")
    print("Esperando a Luca en http://localhost:3000/busqueda/semantica")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServidor apagado.")