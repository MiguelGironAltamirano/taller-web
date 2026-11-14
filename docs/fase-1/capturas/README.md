# Capturas del flujo de atención con el backend real

Recorrido del 14 de noviembre de 2026 en Chromium, contra el stack de Docker Compose (MySQL, backend y frontend con Nginx) y los usuarios de demostración de [`docs/guia-levantamiento.md`](../../guia-levantamiento.md#6-usuarios-de-demostración). El resultado de cada comprobación está en [`resultado.json`](resultado.json).

Las solicitudes se registraron por la API y todos los pasos siguientes se hicieron en la interfaz.

| Captura | Rol | Qué muestra |
|---|---|---|
| `01-solicitante-mis-solicitudes` | Solicitante | Lista de sus solicitudes |
| `02-solicitante-nueva-solicitud` | Solicitante | Formulario de registro |
| `03-solicitante-detalle-registrada` | Solicitante | Solicitud Registrada, sin botones de flujo |
| `04-supervisor-bandeja` | Supervisor | Bandeja del área |
| `05-supervisor-detalle-acciones` | Supervisor | Acciones permitidas sobre una solicitud Registrada |
| `06-supervisor-modal-evaluar` | Supervisor | Diálogo «Pasar a evaluación» |
| `07-supervisor-asignar-sin-tecnico-valida` | Supervisor | Asignar sin técnico: el formulario lo rechaza |
| `08-supervisor-asignar-con-tecnico` | Supervisor | Selector con el técnico del área y su carga |
| `09-supervisor-detalle-asignada` | Supervisor | Solicitud Asignada |
| `10-supervisor-reasignar-sin-motivo-valida` | Supervisor | Reasignar sin técnico ni motivo: errores en ambos campos |
| `11-tecnico-bandeja` | Técnico | Su bandeja |
| `12-tecnico-detalle-asignada` | Técnico | Detalle con «Iniciar atención» |
| `13-tecnico-resolver-sin-solucion-valida` | Técnico | Resolver sin comentario de solución: el formulario lo rechaza |
| `14-tecnico-detalle-resuelta` | Técnico | Solicitud Resuelta |
| `15-solicitante-confirmar-solucion` | Solicitante | Tarjeta «¿Quedó resuelto?» |
| `16-solicitante-detalle-cerrada` | Solicitante | Solicitud Cerrada tras confirmar |
| `17-supervisor-detalle-movil` | Supervisor | Detalle en una pantalla de 390 px |

El historial de la solicitud recorrida quedó con seis entradas (registrada, evaluación, asignación, atención, resolución y cierre) y cierre `CONFIRMADA`. Además se comprobaron por API los códigos `409`, `403`, `400` y `401` (ver `resultado.json`).
