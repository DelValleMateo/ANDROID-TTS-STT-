import urllib.request
import json

url = "http://localhost:3000/busqueda/semantica"
payload = {"consulta": "¿Cuántos días me corresponden por licencia de examen?"}

try:
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'}, method='POST')

    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read().decode('utf-8'))
        print("✅ TEST EXITOSO")
        print(f"Respuesta: {json.dumps(result, indent=2, ensure_ascii=False)}")
except Exception as e:
    print(f"❌ TEST FALLIDO: {e}")