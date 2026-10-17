# Registro de uso de IA

Requisito del enunciado (sección 7): cada uso significativo de IA se registra con herramienta, prompt, resultado, si se usó, validación, modificaciones y responsable.

| Fecha | Herramienta | Prompt (resumen) | Resultado generado | ¿Se utilizó? | Validación realizada | Modificaciones | Responsable |
|---|---|---|---|---|---|---|---|
| 2026-10-14 al 16 | Claude Design | Diseño final de la interfaz a partir del spec y del enunciado: 16 vistas, 4 roles, modo claro/oscuro, móvil | Prototipo interactivo (`Servicios Universitarios UNMSM.dc.html`) | Sí, como referencia visual y de comportamiento | Revisión con el equipo de las pantallas y los flujos por rol | El prototipo no se copió: se reconstruyó en React + Tailwind | Sevan |
| 2026-10-17 | Claude Code (Sonnet 5.5) | Implementar el diseño en `frontend/` en una rama nueva, con la estructura del spec, datos simulados y sin conectar con el backend | Dominio con pruebas, `shared/ui`, API simulada con MSW, layout y todas las pantallas, documentación (`plan/sevan.md` §9, `docs/api/ui-extensions.md`, `odd/tasks/ui-design-system.md`) | Parcialmente: se revisó y se corrigió antes de integrarlo | `pnpm lint`, `typecheck`, `test` (121 pruebas) y `build`; recorrido manual en Chrome con los cuatro roles y vista móvil | Se corrigieron conflictos de clases de Tailwind detectados en el navegador (filtros apilados, iconos de escritorio en móvil); la prueba de triage se ajustó al spec (más coincidencias) y no al prototipo | Sevan |

## Notas de validación

- La lógica pura (SLA, transiciones, triage, filtros, calendario, reportes, esquemas) se escribió con pruebas; en SLA, transiciones, triage, código, esquemas, filtros y reportes la prueba se vio fallar antes de implementar. En `calendar`, `ui.test`, `files.test` y `handlers.test` las pruebas se escribieron junto con el código.
- La IA no tiene acceso al backend ni a datos reales: todo lo que muestra la interfaz son datos de ejemplo.
