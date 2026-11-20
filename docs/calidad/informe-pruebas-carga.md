# Informe de pruebas de carga y estrés (k6)

Responsable: Lita. Ejecutado el 2026-11-20 sobre `dev` (con la corrección de la sección 5.1). Requerimiento: RNF-10. Plan y umbrales: [plan de pruebas §6](plan-de-pruebas.md#6-pruebas-de-carga-y-estrés-k6). Scripts: [`infra/k6/`](../../infra/k6/).

## 1. Resumen

| Prueba | Perfil | Resultado | Veredicto |
|---|---|---|---|
| Humo | 1 usuario, los cuatro roles | 100 % de checks, 0 errores | Aprobada |
| Carga | 20 usuarios iniciando sesión (3 min) y 50 trabajando en su bandeja (5 min), a la vez | 12 738 peticiones, **0 errores**. p95: login 80 ms, listado 224 ms, contadores 25 ms | **Aprobada**: cumple todos los umbrales |
| Estrés | Rampa de 0 a 200 usuarios en 10 min y recuperación con 20 | 83 080 peticiones, **0 errores**. El p95 del listado supera 1 s recién al sostener 200 usuarios | **Aprobada**: se recupera al bajar la carga (p95 276 ms) |
| Pico | De 10 a 150 usuarios en 10 s, 1 min, y de vuelta a 10 | 17 869 peticiones, **0 errores**. p95 639 ms durante el pico | **Aprobada** |

**Hallazgos:**

1. **Error corregido.** La prueba de carga encontró un deadlock de MySQL cuando varios usuarios inician sesión a la vez: 7 de 1 741 logins respondieron `500`. Se corrigió antes de las mediciones finales (sección 5.1).
2. **Capacidad.** El sistema sostiene unas **160 peticiones por segundo** en esta máquina. A partir de unos 180 usuarios muy activos deja de crecer el rendimiento y solo sube la latencia.
3. **Cuello de botella.** No es el backend, es **MySQL**: llega a ~960 % de CPU (unos 10 de 12 hilos), mientras el backend no pasa de medio núcleo. La causa medida es la forma de dos consultas del listado (sección 5.2), con mejoras de 100× a 1 000× disponibles.

## 2. Entorno

| Elemento | Valor |
|---|---|
| Máquina | Intel Core i5-12400F (6 núcleos / 12 hilos), 15,8 GB de RAM, Windows 11 Pro |
| Docker | Docker Desktop 29.8.2, 12 CPU y 7,7 GB asignados, sin límites por contenedor |
| Sistema bajo prueba | Pila propia `taller-web-load` (`infra/docker-compose.yml`): nginx → backend (Spring Boot, Java 25) → MySQL 8.4, con las imágenes de producción |
| Datos | `db/seed-dev` + `db/seed-demo` + **20 000 solicitudes** de `infra/k6/seed-load.sql`: 20 066 en total, en 4 áreas, 7 categorías, 11 ambientes y 5 solicitantes, con estados reales (35 % cerradas, 15 % registradas…) |
| Generador | k6 v1.4.2 (imagen oficial `grafana/k6:1.4.2`), dentro de la red de Compose: el tráfico pasa por nginx como el de un navegador y la red del host no distorsiona los tiempos |
| Otras cargas | La pila de desarrollo (`infra`, puerto 8088) estaba encendida pero en reposo durante las pruebas |

**Sobre los datos.** Con solo 5 solicitantes, cada uno tiene unas 4 000 solicitudes. Es más que lo realista y deja la vista del solicitante en el peor caso; las cifras de esa vista son, por tanto, conservadoras.

## 3. Diseño de las pruebas

- **Cuentas.**
  - **Sesión única:** un usuario tiene una sola sesión abierta, así que un nuevo login de la misma cuenta cerraría la sesión de los usuarios virtuales que la usan.
  - **Bandeja:** las 13 cuentas demo (administrador, 4 supervisores, 4 técnicos y 4 solicitantes) inician sesión **una vez** en `setup`. Los usuarios virtuales comparten esos tokens, como varias pestañas de un mismo navegador.
  - **Login:** usa **cuentas creadas por el administrador en cada corrida**, una por usuario virtual. Así nunca cierra la sesión de la bandeja y cada cuenta queda muy por debajo del límite de intentos (5 simultáneos por cuenta). No hizo falta subir el límite.
- **Comportamiento.**
  - Abrir la bandeja carga el listado (10 por página) y los contadores. Luego la persona filtra o busca 0 a 2 veces más, como en la interfaz: estado, texto (`proyector`, `foco`, código…), orden y páginas 0 a 2.
  - Entre clic y clic hay un tiempo de lectura aleatorio: 1 a 3 s en la carga y 0,5 a 1,5 s en el estrés y el pico, que representan usuarios más acelerados.
- **Lectura del estrés y del pico.**
  - Cada petición lleva la etiqueta del tramo de carga en que ocurrió (`ramp-100-150`, `hold-200`, `recovery-20`…), y cada tramo tiene su umbral.
  - El primer tramo que falla marca el punto de quiebre. En estas dos pruebas solo la recuperación debe aprobar; que fallen los tramos de máxima carga es un dato, no un error del script. Por eso `stress.js` termina con código 99 cuando el tramo `hold-200` supera 1 s, como ocurrió.
- **Recursos.** `run.sh` registra la CPU y la memoria de cada contenedor cada 5 s mientras corre k6.

## 4. Resultados

### 4.1 Carga (`load.js`)

| Métrica | Umbral del plan | Resultado |
|---|---|---|
| Login: p95 | < 800 ms | **80 ms** (mediana 71 ms, p99 140 ms) |
| Login: errores | < 1 % | **0 %** (0 de 1 761) |
| Listado: p95 / p99 | < 500 ms / < 1 s | **224 ms / 385 ms** (mediana 33 ms) |
| Contadores: p95 | < 400 ms | **25 ms** |
| Bandeja: errores | < 1 % | **0 %** (0 de 10 943) |
| Checks | > 99 % | **100 %** (21 800) |

Rendimiento: 41 peticiones/s sostenidas. Recursos: MySQL 1,35 núcleos de media (máximo 2,3); backend 0,5 (máximo 1,4, en el arranque de los logins); nginx 0,02. Memoria máxima: backend 513 MiB, MySQL 515 MiB.

El login cuesta unos 70 ms por petición, casi todo el hash BCrypt, que es intencional. No es un problema: es lo que hace costoso adivinar contraseñas.

### 4.2 Estrés (`stress.js`)

| Tramo | Usuarios | Peticiones/s | Listado p95 | Listado p99 | Errores |
|---|---|---|---|---|---|
| `ramp-0-50` | 0 → 50 | 34 | 205 ms | 371 ms | 0 % |
| `ramp-50-100` | 50 → 100 | 104 | 231 ms | 461 ms | 0 % |
| `ramp-100-150` | 100 → 150 | 157 | 465 ms | 672 ms | 0 % |
| `ramp-150-200` | 150 → 200 | 164 | 990 ms | 1,44 s | 0 % |
| `hold-200` | 200 | 143 | **1,58 s** ✗ | 2,14 s | 0 % |
| `drop-200-20` | 200 → 20 | 123 | 820 ms | 1,15 s | 0 % |
| `recovery-20` | 20 | 28 | **276 ms** ✓ | 339 ms | 0 % |

**Punto de quiebre: alrededor de 180-200 usuarios muy activos.** Ahí el p95 del listado pasa de 1 s y el rendimiento se estanca en ~160 peticiones/s: el sistema está saturado y las peticiones esperan en cola.

Lo importante es lo que **no** pasó: ningún error, ningún `5xx` ni corte de conexión, y la memoria estable. Al bajar la carga, la latencia vuelve a la de reposo (p95 276 ms). El sistema se degrada de forma ordenada y se recupera solo.

**Recursos:** MySQL subió de ~2 núcleos a **~9,6 (960 %)** desde 150 usuarios y se mantuvo saturado hasta la bajada. El backend no pasó de 0,5 núcleos ni nginx de 0,1.

### 4.3 Pico (`spike.js`)

| Tramo | Usuarios | Listado p95 | Listado p99 | Errores |
|---|---|---|---|---|
| `baseline-10` | 10 | 173 ms | 294 ms | 0 % |
| `spike-150` | 10 → 150 en 10 s | 639 ms | 867 ms | 0 % |
| `after-10` | 150 → 10 | 368 ms | 616 ms | 0 % |

Un salto brusco de 15× no produjo errores. El p95 de `after-10` (368 ms) es mayor que el de la línea base porque ese tramo incluye los 10 s de bajada, en los que aún se atiende la cola del pico, y aun así está bajo los 500 ms.

### 4.4 Umbrales del plan

Los umbrales iniciales se confirman con esta línea base: la carga los cumple con holgura (p95 del listado 224 ms frente a 500 ms). No se ajusta ninguno.

## 5. Hallazgos y acciones

### 5.1 Deadlock en logins simultáneos (corregido)

- **Síntoma.** En la primera ejecución de la carga, 7 de 1 741 logins (0,4 %) respondieron `500`, todos en el mismo segundo del arranque, cuando 20 usuarios distintos iniciaron sesión a la vez. MySQL registró `Deadlock found when trying to get lock` al insertar en `refresh_tokens`. El 0,4 % quedaba dentro del umbral de errores, pero un login correcto no debe fallar nunca.
- **Causa.**
  - Al iniciar sesión se cierran las otras sesiones del usuario (regla de sesión única, #27) con `update refresh_tokens … where user_id = ?`.
  - Si el usuario no tiene tokens activos, como en su primer login, InnoDB bloquea el **hueco** del índice `user_id`, y usuarios con ids contiguos comparten ese hueco.
  - Cada uno lo bloquea y luego no puede insertar su token nuevo en él, porque el otro también lo tiene.
- **Corrección** (commit `fix(auth): simultaneous logins no longer deadlock in MySQL`):
  - Los tokens se revocan por clave primaria, después de leer sus ids sin bloquear: solo se bloquean filas, nunca huecos.
  - Login, renovación, cambio de contraseña y desactivación toman el bloqueo del usuario y usan `READ COMMITTED`, para que la lectura posterior al bloqueo vea lo que confirmó el login anterior y la sesión única siga siendo exacta.
  - Nueva prueba en `SingleSessionApiTests`: 20 primeros logins simultáneos de usuarios distintos responden `200`. Antes de la corrección fallaba siempre.
  - Después de la corrección: 0 errores en 1 761 logins.

### 5.2 Las consultas del listado no aprovechan los índices (recomendación al módulo `requests`)

`EXPLAIN ANALYZE` sobre las 20 066 solicitudes, con la base en reposo:

| Consulta | Hoy | Alternativa medida | Mejora |
|---|---|---|---|
| Página del administrador (sin filtros, más recientes) | 157 ms | 0,13 ms empezando por `service_requests` (`STRAIGHT_JOIN`) | ~1 200× |
| Total de esa página (`count(*)`) | 168 ms | 1 ms contando solo `service_requests` | ~160× |
| Total de un supervisor (su área) | 95 ms | 0,8 ms | ~110× |
| Total de un solicitante | 31 ms | 0,5 ms | ~60× |
| Página de un supervisor | 0,16 ms | — | ya usa el índice |
| Contadores (`/requests/counts`) | 3 ms | — | ya usa `ix_service_requests_status` |

**Qué pasa:**
- El `count(*)` de cada página repite los **9 joins** del listado (categoría, prioridad, prioridad solicitada, ambiente, pabellón, facultad, solicitante, área, técnico) sobre todas las filas. Esos joins no cambian el total, porque son claves foráneas obligatorias; solo hacen falta los de las columnas por las que filtra la búsqueda de texto.
- En la página sin filtros, el optimizador empieza por un producto cruzado categorías × prioridades y ordena las 20 066 filas, en lugar de recorrer el índice `created_at` y quedarse con 10.
- Como cada página ejecuta ambas consultas, MySQL es lo que se satura en el estrés.

**Recomendación** (`JdbcRequestQueries`, módulo de Sevan):
1. Contar solo sobre `service_requests`, con los joins que exijan los filtros activos.
2. Obtener primero los 10 ids de la página (ordenados y paginados sobre `service_requests` y sus índices) y después unir los datos solo para esos 10.

Con las mejoras medidas, el punto de quiebre debería subir muy por encima de 200 usuarios. Conviene repetir `bash infra/k6/run.sh stress` después de aplicarlas para comprobarlo.

### 5.3 Otras observaciones

- **Búsqueda de texto.** `LIKE '%término%'` en cinco columnas no puede usar índices. Con 20 000 solicitudes sigue en el orden de los 150 ms y no es el problema principal. Si el volumen creciera mucho, la alternativa es un índice `FULLTEXT` sobre la descripción.
- **Memoria.** Estable en todas las pruebas: backend entre 513 y 536 MiB y MySQL ~515 MiB, sin crecimiento durante el estrés.
- **Fuera de alcance de este informe.** Prueba de resistencia de varias horas (soak), escenarios de escritura (registrar solicitudes y transiciones) y subida de evidencias. Se proponen como extensión.

## 6. Cómo repetirlas

```bash
bash infra/k6/run.sh smoke     # 10 s: comprueba que todo responde antes de medir
bash infra/k6/run.sh load      # 5 min
bash infra/k6/run.sh stress    # 13 min
bash infra/k6/run.sh spike     # 4 min
```

- **Qué hace `run.sh`:** levanta la pila de carga en `http://localhost:8095` (o la reutiliza), carga los datos una sola vez, ejecuta k6 y guarda en `infra/k6/results/` (ignorado por git) el resumen JSON y el CSV de recursos.
- **Para apagar la pila al terminar:** `K6_DOWN=1`.
- **Requisito:** Docker. k6 no se instala, se usa su imagen.
- **Datos de la pila:** la pila de carga lleva datos de demostración. Nunca se debe apuntar estos scripts a producción.
