# Demo temporal en Railway (Fase III)

Railway aloja durante un día una **demo con datos de prueba**. La decisión de arquitectura para los entornos duraderos sigue siendo Dokploy en una VPS propia, con `dev` para staging y `main` para producción ([arquitectura, §8](fase-1/04-arquitectura.md#8-flujo-de-trabajo-y-despliegue)). Esta demo no representa un despliegue de producción ni sustituye esos dos ambientes.

## Selección de plataforma y costo

Railway permite conectar el repositorio, construir los dos Dockerfiles del monorepo, añadir MySQL con volumen, comunicar los servicios por red privada y publicar solo el frontend con HTTPS. Evita administrar una VPS y Dokploy para una demostración de un día. Una VPS/Dokploy sigue siendo la opción prevista para ambientes permanentes; Vercel puede servir el frontend, pero no ejecuta como tal este JAR persistente ni el contenedor MySQL. Separar Vercel y una base de datos externa añadiría configuración y rompería el despliegue de tres servicios planteado para la demo. Otros PaaS con contenedores requerirían verificar por separado sus límites de prueba y almacenamiento.

Railway indica una prueba de $5 sin tarjeta durante 30 días, con límites de recursos; el consumo real se consulta en **Workspace Settings → Usage**. Antes de crear servicios, comprobar que la cuenta está en Trial y que hay crédito. No contratar Hobby/Pro ni añadir tarjeta. Los servicios y volúmenes activos consumen recursos de la prueba; detener la demo al terminar. Véanse [Free Trial](https://docs.railway.com/pricing/free-trial) y [Pricing](https://docs.railway.com/pricing).

## Arquitectura

```text
Navegador --HTTPS--> frontend (nginx:80) --/api/ por HTTP privado--> backend (Spring:8080)
                                                                  |-- MySQL 8.4:3306, volumen
                                                                  `-- /app/evidences, volumen
```

Solo `frontend` tiene dominio público. La SPA llama `/api/` en el mismo origen; nginx reenvía al backend por `backend.railway.internal`. Así la cookie de renovación `HttpOnly; Secure; SameSite=Strict` funciona sin modificar la autenticación ni abrir CORS. El DNS del proxy se toma de `/etc/resolv.conf` en tiempo de arranque. En Docker Compose el upstream predeterminado continúa siendo `http://backend:8080`.

## Pasos reproducibles

1. Integrar a `dev` la PR `feature/deploy-railway` solo después de los tres jobs de CI en verde. En Railway crear un proyecto de demo en una cuenta **Trial**; usar un entorno único, sin réplicas adicionales.
2. Añadir **MySQL** con la imagen `mysql:8.4`, volumen en `/var/lib/mysql` y variables `MYSQL_DATABASE=servicios`, `MYSQL_USER=servicios`, `MYSQL_PASSWORD` y `MYSQL_ROOT_PASSWORD` aleatorias. También puede usarse la plantilla si se confirma su versión 8.4. Mantener la base de datos en la red privada, sin TCP Proxy público.
3. Añadir `backend` desde el repositorio `MiguelGironAltamirano/taller-web`, rama `dev`, Root Directory `/backend`; Railway detecta `backend/Dockerfile`. Puerto 8080, sin dominio público. Adjuntar volumen en `/app/evidences`. El script de arranque asigna el directorio al usuario `app` y luego inicia Java como UID 10001, porque Railway monta el volumen con propietario root.
4. Añadir `frontend` del mismo repositorio y rama, Root Directory `/frontend`; Railway detecta `frontend/Dockerfile`. Puerto 80. Generar un dominio público de Railway y comprobar HTTPS. Configurar `BACKEND_UPSTREAM=http://backend.railway.internal:8080` (o usar la referencia `${{backend.RAILWAY_PRIVATE_DOMAIN}}` para el host). Solo este servicio necesita dominio público.
5. Configurar en `backend` las siguientes variables **dentro de Railway**. Las referencias a MySQL se escriben en el panel de Railway y nunca se guardan en Git. Si el servicio de MySQL tiene otro nombre, ajustar el prefijo de referencia:

   | Variable | Valor en Railway |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql.railway.internal:3306/${{mysql.MYSQL_DATABASE}}` |
   | `SPRING_DATASOURCE_USERNAME` | `${{mysql.MYSQL_USER}}` |
   | `SPRING_DATASOURCE_PASSWORD` | `${{mysql.MYSQL_PASSWORD}}` |
   | `APP_JWT_SECRET` | valor aleatorio de 32 caracteres o más, generado fuera del repositorio |
   | `APP_FLYWAY_LOCATIONS` | `classpath:db/migration,classpath:db/seed-dev,classpath:db/seed-demo` |
   | `APP_EVIDENCES_STORAGE_PATH` | `/app/evidences` |
   | `APP_AUTH_REFRESH_COOKIE_SECURE` | `true` |
   | `APP_CORS_ALLOWED_ORIGINS` | vacío |

   La ubicación `seed-dev` crea los usuarios de [la guía, §6](guia-levantamiento.md#6-usuarios-de-demostración) y `seed-demo` crea solicitudes de ejemplo. **No usar esos seeds en producción.** La imagen del backend incluye la comprobación de salud `/actuator/health/readiness`.
6. Esperar a que MySQL, backend y frontend estén activos. Revisar los logs del backend para confirmar que Flyway aplica las migraciones y no hay errores. Si el backend no resuelve MySQL o falla la escritura de evidencias, corregir las referencias o el volumen antes de publicar la URL.
7. Verificar desde una terminal: `curl -i https://<dominio>/api/v1/system/info`, `curl -i https://<dominio>/` y `SMOKE_BASE_URL=https://<dominio> bash infra/smoke.sh`. La segunda modalidad de `smoke.sh` prueba la URL existente y no crea ni destruye contenedores locales. En el navegador, iniciar sesión como `estudiante@unmsm.edu.pe`, crear una solicitud y subir una evidencia; recargar y comprobar que se conserva la sesión y el archivo.
8. Registrar URL, resultado de las comprobaciones, captura del navegador, logs y consumo medido en [evidencias de Fase III](fase-3/evidencias.md). La demo de Railway cubre T1 parcialmente y aporta evidencia a T7. El pipeline CI existente cubre integración continua; T2 exige todavía un despliegue automático posterior al CI verde.

La primera carga temporal puede hacerse con `railway up ./backend --path-as-root --service backend` y su equivalente para frontend desde la rama de despliegue basada en `dev`. Una vez integrada la PR a `dev`, conectar cada servicio a `MiguelGironAltamirano/taller-web`, rama `dev`, con Root Directory `/backend` o `/frontend` según corresponda. Esto fija la fuente para futuros despliegues.

## Apagar al terminar

En Railway, abrir el proyecto y eliminar el proyecto de demo completo, incluidos servicios y volúmenes, **después de exportar las evidencias necesarias**. Esto destruye los datos de prueba. Si se desea conservar el proyecto, detener o eliminar los tres servicios y revisar que los volúmenes no sigan consumiendo crédito. Comprobar en **Workspace Settings → Usage** el consumo final. No dejar MySQL ni el backend activos después del día de demo.
