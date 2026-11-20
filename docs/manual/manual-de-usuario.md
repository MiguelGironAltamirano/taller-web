# Manual de usuario — Sistema de Servicios Universitarios

Guía para quien usa el sistema, sin conocimientos técnicos. Consolida las notas de [registro y consulta de solicitudes](usuario-registro-solicitudes.md) y agrega el acceso, la cancelación, el borrador, los mensajes de error y el uso desde el celular.

## 1. Qué es y quién lo usa

El sistema permite reportar incidencias o pedir servicios (por ejemplo, un enchufe dañado en un aula) y seguir su atención hasta que se resuelven.

| Rol | Qué hace |
|---|---|
| **Solicitante** | Estudiante, docente o personal administrativo. Registra solicitudes y sigue su avance. |
| **Técnico** | Atiende las solicitudes que le asignan. |
| **Supervisor** | Evalúa, prioriza y asigna las solicitudes de su área. |
| **Administrador** | Gestiona usuarios, catálogos, reglas de sugerencia y configuración. |

Cualquier persona con correo institucional `@unmsm.edu.pe` puede crear su cuenta de Solicitante. Las cuentas de Técnico, Supervisor y Administrador las crea un Administrador.

## 2. Ingresar al sistema

### Con correo y contraseña
1. Abre la página de acceso.
2. Escribe tu correo institucional y tu contraseña, y pulsa **Ingresar**.
3. Si el correo o la contraseña no coinciden, se muestra el aviso «Credenciales inválidas.». Revísalos y vuelve a intentar.

### Con huella o passkey
Puedes ingresar sin escribir tu contraseña usando la huella, el rostro o el PIN de tu dispositivo.

**Registrar una huella o passkey (una sola vez por dispositivo):**
1. Ingresa con tu contraseña.
2. Abre el **Menú de perfil** y elige **Huella y passkeys**.
3. Escribe tu **Contraseña actual** y, si quieres, un **Nombre** para reconocer el dispositivo (por ejemplo, «Laptop»).
4. Pulsa **Agregar huella o passkey** y confirma con tu huella, rostro o PIN cuando el dispositivo lo pida.
5. La passkey aparece en la lista **Passkeys registradas**. Desde ahí también puedes eliminarla.

**Ingresar con ella:** en la página de acceso pulsa **Ingresar con huella o passkey** y confirma en tu dispositivo.

### Una sola sesión a la vez
Solo puedes tener **una sesión abierta**. Si ingresas desde otro dispositivo o navegador, la sesión anterior se cierra automáticamente.

En el dispositivo anterior verás la página de acceso con el mensaje: *«Tu sesión se cerró porque ingresaste desde otro dispositivo. Solo puedes tener una sesión abierta a la vez.»* Para seguir trabajando allí, vuelve a ingresar (esto cerrará la sesión del otro dispositivo).

Si no fuiste tú quien ingresó en otro lugar, cambia tu contraseña de inmediato: abre el **Menú de perfil**, elige **Cambiar contraseña** y sigue los pasos. Cambiarla no cierra la sesión en la que estás.

### Cerrar sesión
En el **Menú de perfil**, elige **Cerrar sesión**.

## 3. Registrar una solicitud

Los pasos completos, los tiempos de atención (SLA) y las reglas de cada campo están en [Registro y consulta de solicitudes](usuario-registro-solicitudes.md). En resumen:

1. Elige **Nueva solicitud**.
2. Completa categoría, descripción (20 a 2000 caracteres), prioridad sugerida y ubicación (Facultad → Pabellón → Ambiente).
3. Adjunta evidencias si tienes (hasta 5 archivos JPG, PNG o PDF de 5 MB cada uno).
4. Pulsa **Registrar solicitud**. Un aviso muestra el código, por ejemplo `SOL-2026-000001`.

### Si no terminas: el borrador
Lo que escribes en el formulario se **guarda solo** («Borrador guardado»), en el servidor, así que puedes retomarlo desde cualquier dispositivo.

- Si sales del formulario (por el menú o con el botón **Cancelar**), **no pierdes nada**. Al pulsar **Cancelar** aparece el aviso «Guardamos tu borrador».
- Para retomarlo, en **Inicio** busca la tarjeta **Borrador sin enviar** («Tienes una solicitud sin enviar») y pulsa **Retomar borrador**. También se ofrece al abrir **Nueva solicitud**.
- Para eliminarlo, pulsa **Descartar**.
- **Las evidencias no se guardan en el borrador**: tendrás que adjuntarlas de nuevo.

## 4. Cancelar una solicitud y restaurarla

### Cuándo se puede cancelar
Solo el solicitante, y solo **antes de que se asigne un técnico**, es decir, mientras la solicitud está **Registrada** o **En evaluación**.

### Cancelar
1. Abre la solicitud desde **Mis solicitudes**.
2. Pulsa **Cancelar solicitud**.
3. Escribe el motivo en **¿Por qué la cancelas?** y confirma con **Cancelar solicitud**.

La solicitud pasa a **Cancelada** y aparece el aviso **Solicitud cancelada** con tu motivo y la fecha límite para restaurarla («Puedes restaurarla hasta el …»). Mientras está cancelada, **el tiempo de atención (SLA) no corre**.

### Deshacer o restaurar
- **Deshacer:** justo después de cancelar, pulsa **Deshacer** en el aviso (o en el mensaje emergente) y la solicitud vuelve al instante.
- **Restaurar más tarde:** dentro de los **7 días** siguientes, abre la solicitud, pulsa **Restaurar solicitud** y confirma con **Restaurar**. Vuelve **al estado en que estaba** antes de cancelarla.
- Pasados los 7 días, ya no se puede restaurar; registra una solicitud nueva.

## 5. Consultar y seguir solicitudes

Cada rol ve en **Solicitudes** solo lo que le corresponde (el solicitante, las suyas; el técnico, las asignadas; el supervisor, las de su área; el administrador, todas). Se puede buscar, filtrar, ordenar, exportar a CSV y compartir el enlace de una vista filtrada. Los detalles están en [Registro y consulta de solicitudes](usuario-registro-solicitudes.md#2-consultar-solicitudes).

En el **detalle** de una solicitud ves su estado, ubicación, tiempo de atención, historial, comentarios y evidencias.

**Notificaciones:** la campana (**Notificaciones** en el menú, **Avisos** en el celular) muestra los cambios de estado y asignaciones, con un contador de no leídas.

## 6. Mensajes de error

Todos los mensajes están en español y dicen **qué pasó** y **qué puedes hacer**. Aparecen siempre del mismo modo:

| Lo que ves | Qué significa | Qué hacer |
|---|---|---|
| Mensaje rojo **debajo de un campo** | Ese dato no es válido (por ejemplo, un correo que no es `@unmsm.edu.pe`). | Corrige el campo. |
| **Alerta arriba del formulario** | El formulario no se pudo enviar (por ejemplo, «Credenciales inválidas.»). | Lee el motivo, corrige y reintenta. |
| **Aviso emergente** rojo | Una acción puntual falló (por ejemplo, «No pudimos enviar el comentario.»). | Inténtalo de nuevo. Puedes cerrarlo con **Cerrar aviso**. |
| **«No pudimos cargar…»** con botón **Reintentar** | La pantalla o la sección no pudo traer sus datos. | Pulsa **Reintentar**. Esta pantalla nunca dice «Sin resultados» cuando falló la carga. |
| **«Algo salió mal en esta pantalla»** | La pantalla falló o hay una versión nueva del sistema. | Pulsa **Recargar la página** o **Ir al inicio**. |
| **Página no encontrada** o sin permiso | El enlace no existe o no corresponde a tu rol. | Vuelve al inicio. |
| «No pudimos conectar con el servidor. Revisa tu conexión…» | Sin internet o servidor no disponible. | Revisa tu conexión y vuelve a intentar en unos segundos. |
| «El archivo supera el tamaño máximo permitido de 5 MB.» | La evidencia es muy grande. | Reduce el archivo o elige otro. |

Los errores nunca muestran códigos técnicos ni nombres internos. Si un problema persiste, avisa al administrador indicando qué estabas haciendo.

## 7. Uso desde el celular y la tablet

El sistema se adapta al tamaño de la pantalla:

- **Celular:** una barra superior con el menú (cajón) y una **barra de navegación inferior** con las opciones principales (Inicio, Solicitudes, Nueva, Avisos). En **Mis solicitudes** hay un botón **Nueva solicitud**. Las bandejas se muestran como tarjetas en lugar de tablas y los botones son más grandes para tocarlos con el dedo.
- **Tablet y computadora:** menú lateral que se puede contraer, ruta de navegación (migas de pan), buscador y tablas.
- **Pantalla grande:** el inicio de sesión muestra un panel institucional y el detalle se organiza en dos columnas.

Se puede usar con **modo claro u oscuro**, y todo flujo clave se puede completar solo con el teclado. Más detalles en la [guía de accesibilidad y diseño responsive](../calidad/guia-accesibilidad.md).

## 8. Preguntas frecuentes

**Me apareció «Tu sesión se cerró porque ingresaste desde otro dispositivo».**
Es normal: ingresaste en otro lugar. Solo se permite una sesión a la vez. Vuelve a ingresar si quieres usar este dispositivo.

**Cancelé una solicitud por error.**
Pulsa **Deshacer** de inmediato o, dentro de 7 días, **Restaurar solicitud** desde el detalle.

**Salí del formulario sin querer.**
Tu borrador se guardó. Búscalo en **Inicio** → **Borrador sin enviar** → **Retomar borrador**.

**¿Puedo cancelar una solicitud que ya tiene técnico?**
No. Escribe un comentario en la pestaña de comentarios de la solicitud: el técnico asignado recibe un aviso y puede responderte.

**No tengo huella ni passkey en mi dispositivo.**
Ingresa con tu contraseña; la huella o passkey es opcional.
