Objetivo del Sprint:
Estoy iniciando el "Sprint 5: Manejo de Errores". El objetivo es hacer la aplicación "indestructible" frente a cortes de red, timeouts, errores de servidor (4xx/5xx) y evitar que la app se cierre (crashee) por excepciones no controladas.

Necesito que me generes el código actualizado para las distintas capas de la aplicación cumpliendo con los siguientes requerimientos:

1. Capa de Red (OkHttp / Retrofit):

Proporcióname el código para configurar el cliente OkHttpClient con timeouts explícitos (ej. 15 segundos para connect, read y write).

Crea un Interceptor de red que capture y maneje códigos de error HTTP (como 400, 404, 500) y lance excepciones personalizadas o devuelva mensajes claros antes de que lleguen al ViewModel.

2. Capa de Arquitectura (ChatViewModel):

Actualiza la lógica de las peticiones de red dentro de las corrutinas envolviéndolas en bloques try/catch robustos que capturen excepciones específicas como IOException (sin internet) o HttpException.

Implementa una función reintentarUltimaConsulta() que guarde la última petición fallida y permita volver a ejecutarla sin que el usuario tenga que escribir todo de nuevo.

3. Capa de Interfaz de Usuario (Jetpack Compose):

Modifica la pantalla MainScreen para incluir un SnackbarHost.

Cuando el StateFlow del ViewModel emita un estado de "Error", la UI debe mostrar un Snackbar amigable (ej: "Se perdió la conexión al servidor") que incluya un botón de acción que diga "Reintentar" y ejecute la función del ViewModel.

4. Testing y Métricas (Guía técnica):

Escribe una breve guía sobre cómo puedo forzar de manera local un "Timeout" o un "Error 500" usando el Mock Server en Python o el Interceptor, para poder testear los Snackbars.

Indícame brevemente cómo puedo imprimir en Logcat el tiempo de latencia de la petición para luego documentarlo.