# Guía de mensajes de error

Responsable: Lita. Responde a la observación del profesor: los errores deben ser claros y verse como el resto de la interfaz. Aplica al frontend y a los mensajes (`detail`) que devuelve la API.

## 1. Cómo se escribe un mensaje

Cada mensaje dice **qué pasó** y **qué puede hacer la persona**, en español y sin términos técnicos.

| Sí | No |
|---|---|
| «No pudimos guardar la regla. Inténtalo de nuevo en unos segundos.» | «Error 500» |
| «Esta solicitud no puede pasar de «Registrada» a «Resuelta». Actualiza la página para ver su estado actual.» | «Transición no permitida: de REGISTRADA a RESUELTA» |
| «No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.» | «Failed to fetch» |
| «El archivo supera el tamaño máximo permitido de 5 MB.» | «Request Entity Too Large» |

- **Sin códigos ni nombres internos:** nada de códigos HTTP, nombres de clases, estados en mayúsculas (`EN_ATENCION`) ni texto del framework en inglés.
- **Sin culpar a la persona:** «No pudimos…» cuando falla el sistema; «Revisa…» solo cuando hay algo concreto que corregir.
- **Con una salida:** siempre hay algo que hacer: reintentar, corregir un campo, actualizar la página, volver al inicio.
- **Como oración:** empieza con mayúscula y termina en punto. Si el backend lo envía sin punto, `errorMessage` lo agrega.
- **Sin datos internos:** un error inesperado nunca muestra su causa; el detalle queda en el registro del servidor.

## 2. Dónde aparece cada error

Se usan solo los componentes existentes de `shared/ui`; ningún error tiene un estilo propio.

| Situación | Componente | Ejemplo |
|---|---|---|
| Un campo no es válido | Mensaje bajo el campo (`Field`) | «Usa tu correo institucional @unmsm.edu.pe.» |
| Un formulario no se pudo enviar | `Alert` arriba del formulario | «Credenciales inválidas.» |
| Una acción puntual falló | Aviso emergente (`toast` con `tone: 'error'`) | «No pudimos enviar el comentario.» |
| Una pantalla o sección no pudo cargar sus datos | `QueryErrorState`, con «Reintentar» | «No pudimos cargar los usuarios» |
| Una pantalla falló al dibujarse o no pudo cargarse | `RouteError`, con «Recargar la página» e «Ir al inicio» | «Algo salió mal en esta pantalla» |
| La página o el permiso no existen | `ErrorPage` (404 y 403) | «Página no encontrada» |

**Una pantalla que no pudo cargar nunca muestra «Sin resultados»**: eso haría creer que no hay datos.

## 3. En el código

- **Frontend:** todo error de una llamada pasa por `errorMessage(error, mensajeDeRespaldo)` (`frontend/src/shared/api/errors.ts`). Usa el `detail` del servidor; si no lo hay (proxy caído, sin conexión, respuesta vacía), explica el caso según el código; nunca muestra el `title` técnico. El respaldo describe lo que la pantalla intentaba hacer: «No pudimos guardar la configuración.».
- **Sin conexión:** el cliente de consultas usa `networkMode: 'always'`, así una consulta o una acción sin red falla y se explica, en lugar de quedarse pausada sin aviso.
- **Consultas:** `QueryErrorState` (`frontend/src/shared/api/QueryErrorState.tsx`) recibe la consulta y arma título, motivo y «Reintentar».
- **Backend:** cada módulo responde `application/problem+json` con su `detail` en español. Lo que ningún módulo maneja (JSON mal formado, un parámetro que falta, una ruta que no existe, un método no admitido) lo traduce `GeneralExceptionHandler`. Un error inesperado lo registra completo `UnexpectedErrorResolver` y responde un `500` genérico.
- **nginx:** si el backend no responde o el archivo es demasiado grande, responde en el mismo formato y en español, en lugar de su página HTML (`frontend/nginx.conf`).

## 4. Lista de verificación para cada PR

- [ ] Todo `catch` muestra `errorMessage(e, '…')`, no `e.message`, `e.title` ni un texto fijo en inglés.
- [ ] Toda pantalla que consulta datos tiene estado de carga, de error (con «Reintentar») y vacío, y el vacío no aparece cuando falló.
- [ ] Los mensajes nuevos del backend están en español, sin códigos internos, y tienen su prueba.
- [ ] Los errores se ven bien en modo claro, oscuro y móvil.
