# Guía de accesibilidad y diseño responsive

Responsable: Lita. Entregable del plan (`plan/lita.md` §4, semanas 8–10). Aplica a todo el frontend; los componentes de `frontend/src/shared/ui/` ya cumplen estas reglas y son la forma recomendada de construir pantallas.

**Objetivo:** WCAG 2.1 nivel AA en las pantallas principales, y que todo flujo clave se pueda completar solo con teclado y con lector de pantalla.

## 1. Cómo usar esta guía

- **Al construir una pantalla:** usa los componentes de `shared/ui` (`Field`, `Input`, `Select`, `Button`, `Modal`, `Tabs`, `StatusBadge`, `PageHeader`…). Ya resuelven etiquetas, foco, contraste y estados.
- **Al revisar un PR:** recorre la [lista de verificación](#9-lista-de-verificación-para-cada-pr).
- **Si necesitas algo que `shared/ui` no tiene:** sigue las reglas de esta guía y, si se va a reutilizar, agrégalo a `shared/ui` con su prueba.

## 2. Principios de la interfaz

| Regla | Cómo se cumple en el proyecto |
|---|---|
| **Idioma declarado** | `<html lang="es">` en `frontend/index.html`. |
| **Un solo `h1` por página** | `PageHeader` renderiza el `h1`; las secciones usan `h2` (`CardTitle`, títulos de panel). El detalle usa el código de la solicitud como `h1`. |
| **Regiones** | El layout marca `nav` (menú «Principal» y «Ruta»), `main` (`id="main"`) y `header`; enlace «Saltar al contenido» visible al recibir foco. |
| **Nunca solo color** | Estados con ícono y texto (`StatusBadge`), prioridades con barras de nivel (`PriorityBadge`), errores con ícono y mensaje, avisos con título. |
| **Movimiento reducido** | `@media (prefers-reduced-motion: reduce)` en `index.css` desactiva animaciones y transiciones en toda la app. |
| **Modo claro y oscuro** | Los colores salen de tokens (`styles/tokens.css`) redefinidos bajo `.dark`; ningún componente usa colores sueltos salvo el guinda institucional fijo (`--brand`). |

## 3. Contraste (WCAG 1.4.3 y 1.4.11)

Mínimo **4.5:1** para texto normal y **3:1** para texto grande, íconos y bordes que transmiten estado. Valores medidos con los tokens actuales:

| Par de colores | Claro | Oscuro |
|---|---|---|
| Texto / superficie | 15.8:1 | 14.5:1 |
| Texto atenuado (`text-muted`) / superficie | 6.1:1 (5.7:1 sobre el fondo) | 6.9:1 |
| Guinda / superficie | 9.3:1 | 8.7:1 (`--primary-text`) |
| Blanco / guinda (botón primario) | 9.3:1 | 5.7:1 |
| Error / superficie | 6.6:1 | — |
| Insignias: En atención · Resuelta · Cerrada · Crítica | 7.1 · 5.5 · 6.1 · 6.8 :1 | — |

**Reglas:**
- No bajes la opacidad del texto para "suavizarlo" (`text-muted/70` y similares pueden caer por debajo de 4.5:1). Para jerarquía usa peso o tamaño: así se resolvió el encabezado de fin de semana del calendario.
- Un color nuevo se agrega como token con su versión oscura y se mide antes de usarlo.

## 4. Teclado y foco (WCAG 2.1.1, 2.4.3, 2.4.7)

- **Foco siempre visible.** Botones: anillo `ring-2 ring-primary/45` con separación; campos: borde guinda y anillo suave (rojo si el campo es inválido); el resto usa el `outline` global de `index.css`. No uses `outline: none` sin reemplazo.
- **Orden lógico.** El orden del DOM es el orden visual; no uses `tabindex` positivos.
- **Elementos interactivos reales.** Usa `<button>` o `<a>`/`<Link>`. Si una fila o tarjeta entera es clicable (`GridRow interactive`, tarjetas del kanban), debe tener `tabIndex={0}`, `role` adecuado y responder a `Enter`.
- **Sin interacción anidada.** No pongas un botón dentro de un elemento con `role="button"` (axe lo marca como `nested-interactive`); en el kanban la acción es una etiqueta visual y la tarjeta entera abre la solicitud.
- **Diálogos (`Modal`).** `role="dialog"`, `aria-modal="true"`, nombre accesible por su título, foco inicial en el primer control, `Escape` cierra y el foco vuelve al elemento que lo abrió.
- **Atajos.** `Ctrl/⌘ + K` abre la paleta de comandos; `Escape` cierra paneles y menús.

## 5. Formularios (WCAG 1.3.1, 3.3.1, 3.3.2, 3.3.3)

- **Siempre con `Field`.** Asocia la etiqueta (`label for`) y el mensaje (`aria-describedby`) y marca `aria-invalid`. Nunca uses el `placeholder` como etiqueta.
- **Mensajes junto al campo**, con ícono y en lenguaje claro que diga cómo corregir: «La contraseña debe tener al menos 8 caracteres», no «Mínimo 8».
- **Ayuda antes del error.** Si un campo tiene requisitos, muéstralos como `hint` antes de que la persona se equivoque (por ejemplo, la contraseña del registro).
- **Errores del servidor** con `Alert` (`role="alert"`) arriba del formulario; los de campo, en su campo.
- **Tipos y autocompletado** correctos (`type="email"`, `autoComplete="username"`, `current-password`, `new-password`).
- **Campos obligatorios** marcados en la etiqueta («Descripción *») y explicados en el subtítulo de la página.
- **Contraseñas** con `PasswordInput` (botón «Mostrar caracteres», `aria-pressed`).

## 6. Contenido dinámico y componentes

| Componente | Comportamiento accesible |
|---|---|
| Avisos (`useToast`) | `role="status"` (éxito, información) o `role="alert"` (error); duración de 4, 5 y 7 s, pausa al pasar el mouse o al recibir foco, botón «Cerrar aviso». |
| `Tabs` | `role="tablist"`/`tab` con `aria-selected`; el contenido que controlan va en un contenedor con `role="tabpanel"` (como en el detalle de la solicitud). |
| `StarRating` | Editable: `radiogroup` con un `radio` por estrella; solo lectura: `role="img"` con «4 de 5 estrellas». |
| `Stepper` | Lista ordenada con `aria-current="step"` y estado de cada etapa en texto oculto («completado», «actual», «pendiente»). |
| `ProgressBar` | `role="progressbar"` con `aria-valuenow`, `min`, `max` y nombre. |
| `GridTable` | `role="table"`, filas `row` y cada celda `cell` (el componente las envuelve automáticamente). |
| Íconos | `aria-hidden="true"`; si un botón solo tiene ícono, usa `IconButton` (exige `label`). |
| Carga | `Skeleton`/`SkeletonRows` con `role="status"` y nombre «Cargando». |
| Error de carga | `ErrorState`: `role="alert"`, explica qué falló y siempre ofrece «Reintentar» (ocupado mientras reintenta). |
| Notificaciones | La campana anuncia «Notificaciones, N sin leer»; el panel es un `dialog` con nombre. |

## 7. Diseño responsive

| Punto de corte | Ancho | Qué cambia |
|---|---|---|
| móvil (por defecto) | < 768 px | Menú lateral oculto: barra superior con menú (cajón) y navegación inferior fija; tarjetas en lugar de tablas en la bandeja; calendario con puntos en lugar de chips. |
| `md` | ≥ 768 px | Menú lateral colapsable, ruta de navegación y buscador en la barra superior, tablas. |
| `lg` | ≥ 1024 px | Panel institucional en el inicio de sesión; detalle en dos columnas. |
| `2xl` | ≥ 1536 px | Indicadores del dashboard en una sola fila de seis. |

**Reglas:**
- **Objetivo táctil de 44 px en móvil.** `Button`, `IconButton` y los campos crecen a `h-11`/`size-11` con `max-md:`.
- **Sin desplazamiento horizontal de página.** Solo las tablas anchas se desplazan dentro de su tarjeta (`GridTable` con `minWidth`).
- **Texto que se adapta.** Usa `truncate` con `title` para textos largos en celdas; nunca cortes información sin forma de verla completa.
- **Pruébalo a 375 px y a 1440 px** en modo claro y oscuro antes de abrir el PR.

## 8. Cómo se verifica

| Verificación | Herramienta | Dónde |
|---|---|---|
| Reglas WCAG 2.1 A/AA automáticas | axe (`@axe-core/playwright`) sobre 16 páginas de los cuatro roles, el detalle de una solicitud (con su historial) y el login | `frontend/e2e/a11y.e2e.ts`, `smoke.e2e.ts`; en CI (`smoke and e2e`) |
| Etiquetas, roles y descripciones | Testing Library (consultas por rol y etiqueta) | `shared/ui/ui.test.tsx` y pruebas de cada feature |
| Teclado | Recorrido manual (lista de la sección 9) | Informe de pruebas |
| Lector de pantalla | NVDA + Chrome (Windows) en los flujos clave | Informe de pruebas |
| Contraste | Medición de tokens (sección 3) al agregar colores | Esta guía |

## 9. Lista de verificación para cada PR

- [ ] Uso componentes de `shared/ui` o justifico por qué no.
- [ ] Hay un solo `h1` y los títulos siguen orden (`h1` → `h2` → `h3`).
- [ ] Todo control tiene nombre accesible (etiqueta visible o `aria-label` en `IconButton`).
- [ ] Los errores aparecen junto al campo, con texto claro, y el campo queda `aria-invalid`.
- [ ] La información no depende solo del color.
- [ ] Puedo completar el flujo solo con `Tab`, `Shift+Tab`, `Enter`, `Espacio`, flechas y `Escape`, y siempre veo dónde está el foco.
- [ ] No anidé elementos interactivos.
- [ ] Revisé la pantalla a 375 px y a 1440 px, en claro y oscuro.
- [ ] Si agregué una página principal, la sumé a `e2e/a11y.e2e.ts`.

### Flujos clave para la prueba con teclado y lector de pantalla

1. Iniciar sesión y cerrar sesión.
2. Registrar una solicitud (solicitante).
3. Evaluar y asignar una solicitud (supervisor).
4. Iniciar y resolver una solicitud (técnico).
5. Confirmar la solución y calificar, o rechazarla con motivo (solicitante).
6. Revisar y marcar notificaciones.

## 10. Pendientes conocidos

- La prueba manual con teclado y con NVDA de los seis flujos clave está pendiente (semanas 13–14); su resultado irá al informe de pruebas.
- Los desplegables usan `appearance: base-select` solo en Chrome y Edge; en Firefox y Safari la lista abierta es la nativa del navegador (accesible, sin el estilo propio).
