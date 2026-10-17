# 05 — Wireframes, mockups y prototipo

El diseño visual final se hizo con Claude Design a partir del spec y del enunciado (ver el [registro de uso de IA](../registro-uso-ia.md)) y se reconstruyó en React + Tailwind en `frontend/`. Los wireframes de abajo son esquemas de las pantallas que ya existen en el prototipo; sirven para revisar la estructura sin depender del color ni del estilo.

## Mapa de pantallas por rol

| Rol | Menú lateral | Ruta |
|---|---|---|
| Público | Iniciar sesión · Registrarme | `/login`, `/registro` |
| Solicitante | Inicio · Mis solicitudes · Nueva solicitud · Notificaciones | `/inicio`, `/solicitudes`, `/solicitudes/nueva`, `/notificaciones` |
| Técnico | Mi bandeja · Calendario de vencimientos · Notificaciones | `/solicitudes`, `/calendario`, `/notificaciones` |
| Supervisor | Dashboard · Bandeja del área · Equipo · Reportes · Notificaciones | `/dashboard`, `/solicitudes`, `/equipo`, `/reportes`, `/notificaciones` |
| Administrador | Dashboard global · Solicitudes · Usuarios · Catálogos · Reglas de triage · Auditoría · Configuración | `/dashboard`, `/solicitudes`, `/admin/usuarios`, `/admin/catalogos`, `/admin/triage`, `/admin/auditoria`, `/admin/configuracion` |
| Todos | Detalle de una solicitud | `/solicitudes/:codigo` |

En móvil el menú lateral se reemplaza por una barra inferior con cuatro accesos y un botón "Menú" que abre el resto.

## Wireframes

### Inicio de sesión (HU-02)

```text
┌──────────────────────────────────────────────┐
│  Servicios Universitarios UNMSM              │
│                                              │
│   Correo institucional                       │
│   [ usuario@unmsm.edu.pe                   ] │
│   Contraseña                                 │
│   [ ••••••••                          👁   ] │
│                                              │
│   [          Iniciar sesión              ]   │
│   ¿No tienes cuenta? Regístrate              │
│   ¿Olvidaste tu contraseña?                  │
│                                              │
│   (solo con la API simulada)                 │
│   Cuentas de ejemplo: [Solicitante] [Técnico]│
│                       [Supervisor] [Admin]   │
└──────────────────────────────────────────────┘
```

### Nueva solicitud (HU-06, HU-07, HU-09, HU-23)

```text
┌─ Menú ─┬─────────────────────────────────────────────────┐
│ Inicio │  Nueva solicitud                                │
│ Mis    │                                                 │
│ solic. │  Descripción  (20 a 2000 caracteres)            │
│ ▶Nueva │  [                                           ]  │
│ Avisos │  [                                    123/2000] │
│        │  ┌ Sugerencia: Categoría "Electricidad",     ┐  │
│        │  │ prioridad Alta   [Aceptar] [Descartar]    │  │
│        │  └───────────────────────────────────────────┘  │
│        │  Categoría [ Electricidad            ▾ ]        │
│        │  Prioridad [ Alta                    ▾ ]        │
│        │  Facultad  [ ▾ ]  Pabellón [ ▾ ]  Ambiente [ ▾ ]│
│        │  Evidencias (hasta 5 · JPG, PNG, PDF · 5 MB)    │
│        │  [ + Adjuntar ]                                 │
│        │                      [Cancelar] [Registrar]     │
└────────┴─────────────────────────────────────────────────┘
```

### Bandeja de solicitudes (HU-08, HU-12, HU-13)

La misma pantalla sirve a los cuatro roles; el título, los filtros rápidos y las acciones cambian según el rol.

```text
┌─ Menú ─┬─────────────────────────────────────────────────┐
│ Dash.  │  Bandeja del área                [Lista|Kanban] │
│ ▶Band. │  [ Buscar…            ] Estado▾ Categoría▾ …    │
│ Equipo │  Vistas: Todas · Abiertas · Críticas sin asignar│
│ Report.│          · Mis vencidas                         │
│ Avisos │ ┌──────────┬────────────┬────────┬───────┬─────┐│
│        │ │ Código   │ Categoría  │Prior.  │Estado │Vence││
│        │ ├──────────┼────────────┼────────┼───────┼─────┤│
│        │ │SOL-2026… │Electricidad│ Alta   │Regist.│ 5 h ││
│        │ │SOL-2026… │Redes       │ Media  │Asign. │ ⚠ V ││
│        │ └──────────┴────────────┴────────┴───────┴─────┘│
│        │  « 1 2 3 »                          [Exportar]  │
└────────┴─────────────────────────────────────────────────┘
```

En la vista Kanban las tarjetas se agrupan por estado y solo se pueden arrastrar a estados que la máquina de estados permite.

### Detalle de una solicitud con acciones del flujo (HU-12 a HU-18)

```text
┌─ Menú ─┬─────────────────────────────────────────────────┐
│        │  ← SOL-2026-000123            [Estado: Asignada]│
│        │  Descripción …                                  │
│        │  Categoría · Ubicación · Prioridad · Vence      │
│        │  Solicitante · Técnico · Área                   │
│        │                                                 │
│        │  Acciones (solo las permitidas para ti):        │
│        │  Supervisor: [Evaluar] [Asignar] [Reasignar]    │
│        │              [No procede] [Duplicada]           │
│        │  Técnico:    [Iniciar atención] [Resolver]      │
│        │                                                 │
│        │  ┌ ¿Quedó resuelto? (solo el solicitante) ───┐  │
│        │  │ Solución propuesta: …                     │  │
│        │  │ Califica la atención ★★★★★  (opcional)    │  │
│        │  │ [Confirmar y cerrar] [Rechazar solución]  │  │
│        │  └───────────────────────────────────────────┘  │
│        │  [ Historial | Comentarios | Evidencias ]       │
│        │  ● Resuelta      Carlos M.   16 oct 10:42       │
│        │  ● En atención   Carlos M.   16 oct 09:10       │
│        │  ● Asignada      Ana V.      15 oct 17:30       │
└────────┴─────────────────────────────────────────────────┘
```

### Modal de acción del flujo (HU-13, HU-14, HU-16)

```text
┌──────── Asignar solicitud ────────┐
│ Técnico                           │
│ [ Carlos Mendoza · 3 activas   ▾ ]│
│ Motivo (obligatorio al reasignar  │
│ y al cerrar)                      │
│ [                               ] │
│            [Cancelar] [Confirmar] │
└───────────────────────────────────┘
```

### Bandeja del técnico y calendario

```text
┌─ Menú ─┬─────────────────────────────────────────────────┐
│ ▶Mi    │  Mi bandeja                                     │
│ bandeja│  Tarjetas ordenadas por vencimiento             │
│ Calend.│  ┌ SOL-… Alta  En atención  vence en 3 h ─────┐ │
│ Avisos │  └────────────────────────────────────────────┘ │
│        │  Calendario de vencimientos: cuadrícula mensual │
│        │  con las solicitudes en su día de vencimiento   │
└────────┴─────────────────────────────────────────────────┘
```

### Dashboard (HU-25)

```text
┌─ Menú ─┬─────────────────────────────────────────────────┐
│ ▶Dash. │  Dashboard      Rango: 7 días ▾   Área: Todas ▾ │
│ Band.  │ ┌Regist.┐┌Pend.┐┌Atend.┐┌% resu.┐┌T.prom┐┌Venc.┐│
│ Equipo │ │  120  ││ 34  ││  86  ││  72 % ││ 18 h ││  5  ││
│        │ └───────┘└─────┘└──────┘└───────┘└──────┘└─────┘│
│        │  Por categoría (barras)   Por prioridad (anillo)│
│        │  Por técnico (barras)     Cumplimiento SLA      │
│        │  Próximas a vencer (lista)                      │
└────────┴─────────────────────────────────────────────────┘
```

### Administración (HU-26, HU-27)

```text
┌─ Menú ─┬─────────────────────────────────────────────────┐
│ Usuar. │  Usuarios                       [+ Nuevo usuario]│
│ Catál. │  [ Buscar por nombre o correo ]  Rol ▾          │
│ Triage │  Nombre · Correo · Rol · Área · Activo  [Editar]│
│ Audit. │                                                 │
│ Config.│  Reglas de triage: palabras clave → categoría y │
│        │  prioridad, con interruptor de activa           │
└────────┴─────────────────────────────────────────────────┘
```

## Prototipo

El prototipo es el propio frontend ejecutándose contra una API simulada (MSW), que respeta el [contrato de la API](../api/contrato-v1.md) y sus [extensiones](../api/ui-extensions.md). Cubre las pantallas de los cuatro roles, modo claro y oscuro, y vista móvil.

```bash
cd frontend
pnpm install
pnpm dev:mocks        # abre http://localhost:5173 con la API simulada
```

Cuentas de ejemplo (contraseña `Demo12345!`, todas `@unmsm.edu.pe`):

| Cuenta | Rol | Qué se puede probar |
|---|---|---|
| `lquispe@` | Solicitante | Registrar una solicitud, ver su historial, confirmar o rechazar una solución |
| `avillanueva@` | Supervisora de Mantenimiento | Evaluar, asignar, reasignar y cerrar; dashboard y reportes |
| `cmendoza@` | Técnico de Mantenimiento | Iniciar atención y resolver; calendario de vencimientos |
| `rsalazar@` | Administrador | Usuarios, catálogos, reglas de triage, auditoría y configuración |

**Recorrido sugerido para la demostración del flujo (HU-06, HU-12 a HU-17):**

1. Como `lquispe@`: registrar una solicitud y anotar el código.
2. Como `avillanueva@`: abrir la solicitud, evaluarla y asignarla a un técnico.
3. Como `cmendoza@`: iniciar la atención y resolver con una solución.
4. Como `lquispe@`: confirmar la solución (con o sin calificación) o rechazarla con un motivo.
5. Revisar el historial: debe mostrar una entrada por cada paso.

Los datos del prototipo viven en memoria y se reinician al recargar la página. Las cuentas de ejemplo no son las del backend real; esas están en la [guía de levantamiento](../guia-levantamiento.md).
