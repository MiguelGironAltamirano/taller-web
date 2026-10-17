# 01 — Problema, objetivos y alcance

## 1. Problema

En la universidad, las solicitudes de servicios (mantenimiento, infraestructura, soporte informático y trámites administrativos) se gestionan hoy por correos, formularios sueltos, llamadas telefónicas o sistemas independientes. Eso produce:

- Pérdida o duplicación de solicitudes.
- Falta de seguimiento: el solicitante no sabe en qué estado está su pedido.
- Ausencia de trazabilidad: no queda registro de quién hizo qué y cuándo.
- Dificultad para saber quién es el responsable de atender cada solicitud.
- Poca información sobre los tiempos de atención y sobre qué solicitudes están vencidas.
- Imposibilidad de medir la calidad del servicio.
- Falta de indicadores para decidir (por ejemplo, qué categoría se repite o qué técnico está sobrecargado).

## 2. Justificación

- **Para el solicitante** (estudiante, docente o personal administrativo): un solo lugar para registrar el problema, recibir un código y ver el avance hasta el cierre.
- **Para los técnicos y supervisores**: bandejas con lo que les corresponde, asignación explícita y plazos (SLA) por prioridad, en lugar de cadenas de correos.
- **Para la gestión**: un historial completo de cada solicitud y nueve indicadores de atención que hoy no existen.
- **Para el curso**: el proyecto permite demostrar el ciclo completo Especificar → Diseñar → Construir → Integrar → Probar → Asegurar → Desplegar → Monitorear → Mejorar, y no solo una aplicación que funcione.

## 3. Objetivos

**Objetivo general.** Construir una aplicación web full-stack que permita registrar, priorizar, asignar, atender y monitorear solicitudes de servicios universitarios, con trazabilidad completa y medición de la calidad del servicio.

**Objetivos específicos**

| Id | Objetivo | Cómo se verifica |
|---|---|---|
| OB-1 | Permitir que un solicitante registre una solicitud y consulte su estado en cualquier momento | Escenarios de las historias HU-06 a HU-08 |
| OB-2 | Garantizar que cada solicitud siga un ciclo de vida controlado, con un responsable y un historial | Pruebas de la máquina de estados (HU-12 a HU-19) |
| OB-3 | Dar a cada rol solo lo que le corresponde | Pruebas de autorización por rol (HU-04) |
| OB-4 | Medir los tiempos de atención con un SLA por prioridad | HU-20 y los indicadores del dashboard (HU-25) |
| OB-5 | Medir la satisfacción del solicitante | HU-17 y HU-24 |
| OB-6 | Entregar el sistema con calidad de ingeniería: pruebas automatizadas, CI/CD, seguridad OWASP y observabilidad | CI en cada pull request; informes de las fases III y IV |

## 4. Usuarios y actores

| Actor | Quién es | Qué necesita |
|---|---|---|
| **Solicitante** | Estudiante, docente o personal administrativo con correo `@unmsm.edu.pe`. El tipo se guarda como atributo. | Registrar solicitudes, seguirlas, comentar, adjuntar evidencias, confirmar o rechazar la solución y calificar la atención |
| **Técnico** | Personal de un área responsable (mantenimiento, soporte informático, etc.) | Ver las solicitudes que le asignaron, iniciar la atención y resolver con una solución explicada |
| **Supervisor** | Responsable de un área | Evaluar, priorizar, asignar y reasignar las solicitudes de su área; cerrar las que no proceden; ver la carga de su equipo |
| **Administrador** | Responsable del sistema | Gestionar usuarios, áreas, categorías, prioridades, ubicaciones, reglas de triage y configuración |
| **Sistema** | Procesos automáticos | Cerrar solicitudes resueltas sin respuesta a las 72 horas y notificar los cambios de estado |

Las cuentas de Técnico, Supervisor y Administrador las crea un Administrador. El autorregistro solo crea cuentas de Solicitante y exige un correo del dominio `unmsm.edu.pe`.

## 5. Alcance

### Incluido

- Los cinco módulos mínimos del enunciado: autenticación, registro de solicitudes, gestión de solicitudes, dashboard y administración.
- Tres mejoras: notificaciones dentro de la aplicación, triage automático por reglas de palabras clave y encuesta de satisfacción.
- Despliegue en staging y producción, CI/CD, seguridad según OWASP Top 10, pruebas automatizadas y observabilidad.

### Excluido

- Reservas de ambientes (dominio independiente, con calendarios y disponibilidad).
- Aplicación móvil nativa (la interfaz sí es responsive).
- Integración con sistemas institucionales existentes (SUM, correo institucional vía SSO).

### Opcional (prioridad C, se recorta primero si el plazo aprieta)

1. Notificaciones por correo.
2. Recuperación de contraseña por correo.
3. Reglas de triage gestionables desde la pantalla de administración (alternativa: reglas fijas en el seed).

## 6. Supuestos y restricciones

- El proyecto dura 16 semanas. Semana 8 = 12–18 oct de 2026, semana 10 = 26 oct–1 nov y semana 16 = 7–13 dic. Las fechas exactas de la presentación parcial y de la sustentación aún no están confirmadas (decisión D4 del spec).
- El desarrollo empezó de forma individual y el grupo se incorporó después con el mismo flujo de ramas (`feature/*` → pull request → `dev`).
- Los datos de demostración (usuarios, catálogos) se cargan con una migración solo para desarrollo y staging, nunca en producción.

## 7. Riesgos principales

| Riesgo | Mitigación |
|---|---|
| Poco tiempo entre la Fase I y el MVP | MVP recortado al flujo central; documentación en paralelo |
| Problemas de despliegue descubiertos tarde | Desplegar un esqueleto en staging desde el Sprint 0 |
| Recursos limitados de la VPS | Medir el consumo y ajustar la decisión de observabilidad (D3) |
| Incorporación del grupo a mitad del proyecto | Arquitectura por feature, ramas por feature, documentación en `docs/` |
