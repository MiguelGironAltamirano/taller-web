# Manual de usuario — Registro y consulta de solicitudes

Notas del módulo de Sevan (catálogos y solicitudes) para el manual de usuario que consolida Lita.

## 1. Registrar una solicitud (rol Solicitante)

1. En el menú, elige **Nueva solicitud** (en el celular, el botón del mismo nombre en "Mis solicitudes").
2. Completa **Datos de la solicitud**:
   - **Categoría \*:** al elegirla, debajo aparece el *área responsable* que la atenderá.
   - **Descripción \*:** cuenta qué ocurre y desde cuándo. Debe tener **entre 20 y 2000 caracteres**; los espacios al inicio y al final no cuentan. El contador `n / 2000` te indica cuánto llevas.
   - **Prioridad sugerida \*:** Crítica, Alta, Media o Baja. Cada una muestra su tiempo de atención.
3. Si lo que escribes coincide con alguna regla de sugerencia, aparece un aviso con una categoría y prioridad recomendadas: puedes **aceptarlas** o **ignorarlas**.
4. En **Ubicación**, elige **Facultad → Pabellón → Ambiente**; cada lista se habilita al elegir la anterior.
5. Adjunta **evidencias** si las tienes: hasta 5 archivos JPG, PNG o PDF de 5 MB cada uno.
6. Revisa el panel **Resumen** (categoría, área, ubicación, prioridad, evidencias y fecha en que vencería) y pulsa **Registrar solicitud**.

Si falta un dato o la descripción está fuera de rango, el formulario no se envía: los campos con error se marcan y se avisa "Revisa los campos marcados". Al registrarse, un aviso muestra el **código de la solicitud** (por ejemplo `SOL-2026-000001`) y la solicitud aparece en tu lista.

### Tiempos de atención (SLA)

| Prioridad | Horas |
|---|---|
| Crítica | 8 |
| Alta | 24 |
| Media | 72 |
| Baja | 120 |

La fecha de vencimiento es la fecha de registro más esas horas. Una solicitud se marca **Vencida** si pasó esa fecha sin llegar a "Resuelta".

## 2. Consultar solicitudes

La lista muestra solo lo que tu rol puede ver:

| Rol | Ve |
|---|---|
| Solicitante | Las que registró |
| Supervisor | Las de su área |
| Técnico | Las que tiene asignadas |
| Administrador | Todas |

Si abres un enlace a una solicitud que no te corresponde, el sistema responde como si no existiera.

Sobre la lista puedes:
- **Buscar** por código o texto, y filtrar por **categoría**, **prioridad** y (supervisor) **técnico**.
- Tocar los contadores por **estado** o la marca **Vencidas**.
- Usar las **vistas guardadas**: Todas, Mis vencidas, Abiertas, Por confirmar y Críticas sin asignar.
- Filtrar por **rango de fechas de registro** con **Desde** y **Hasta** (ambos días incluidos). El administrador también puede filtrar por **Área**.
- **Ordenar** por vencimiento, más recientes o prioridad, cambiar entre tabla y kanban (roles que atienden) y la densidad.
- **Exportar** a CSV las filas que se ven.

Los filtros activos aparecen como etiquetas que se quitan con un clic, y **Limpiar filtros** los quita todos. Como los filtros viajan en la dirección de la página, puedes copiar el enlace para compartir la misma vista.

El **detalle** de cada solicitud muestra su estado, ubicación, SLA, historial de cambios, comentarios y evidencias.
