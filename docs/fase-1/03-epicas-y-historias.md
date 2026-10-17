# 03 — Épicas, historias de usuario y criterios de aceptación

Cada historia indica su prioridad (**M** MVP, **S** release candidate), los requerimientos de [02](02-requerimientos.md) que cubre, las tareas en que se divide y sus escenarios BDD. Cada escenario enlaza con la prueba automatizada que lo ejecuta; si todavía no existe, queda marcado **Por escribir** (Lita lo registra como no cubierto en el informe de pruebas hasta que exista).

**Dónde viven las pruebas** (rutas relativas a `backend/src/test/java/pe/edu/unmsm/servicios/` y `frontend/src/`):

| Abreviatura | Archivo |
|---|---|
| `AuthApi` | `auth/infrastructure/web/AuthApiTests` |
| `LoginLimit` | `auth/infrastructure/web/LoginAttemptLimitTests` |
| `ReqApi` | `requests/infrastructure/RequestsApiTests` |
| `CatApi` | `catalog/infrastructure/CatalogApiTests` |
| `WfApi` | `workflow/infrastructure/WorkflowApiTests` |
| `StateMachine` | `workflow/domain/StateMachineTest` |
| `SlaTest` | `requests/domain/SlaTest` |
| `FE` | pruebas del frontend (`*.test.ts(x)`), con la API simulada |

## Épicas

| Épica | Historias | Módulo |
|---|---|---|
| E1. Autenticación y cuentas | HU-01 a HU-05 | `auth` |
| E2. Registro y consulta de solicitudes | HU-06 a HU-11 | `requests`, `catalog` |
| E3. Flujo de atención | HU-12 a HU-21 | `workflow` |
| E4. Seguimiento y calidad del servicio | HU-22 a HU-24 | `notifications`, `triage`, `satisfaction` |
| E5. Indicadores | HU-25 | `dashboard` |
| E6. Administración | HU-26, HU-27 | `admin` |

Estado de las historias a 17 de octubre de 2026: **HU-01 a HU-09, HU-12 a HU-20 están construidas y probadas en el backend y en el frontend**; HU-05, HU-10, HU-11, HU-21 a HU-27 tienen la interfaz hecha contra la API simulada y su backend está pendiente (release candidate).

---

## E1. Autenticación y cuentas

### HU-01 — Registrarme como solicitante (M)

Como estudiante, docente o personal administrativo, quiero crear mi cuenta con mi correo institucional para poder registrar solicitudes. *(RF-01)*

**Tareas:** formulario con Zod · `POST /auth/register` · BCrypt · validar dominio y contraseña · mensajes de error por campo.

| Escenario | Prueba |
|---|---|
| **Registro correcto.** Dado un correo `@unmsm.edu.pe` sin cuenta, cuando envío un nombre, una contraseña válida y mi tipo, entonces se crea una cuenta de Solicitante con la contraseña cifrada | `AuthApi.registerCreatesRequesterWithHashedPassword` |
| **Correo ajeno.** Dado un correo de otro dominio, cuando intento registrarme, entonces el sistema rechaza el registro con el error en el campo correo | `AuthApi.registerRejectsNonUnmsmEmail` |
| **Contraseña débil.** Dada una contraseña sin dígito o demasiado corta, cuando intento registrarme, entonces se rechaza con el error en el campo contraseña | `AuthApi.registerRejectsWeakPassword` |
| **Correo repetido.** Dado un correo ya registrado, cuando intento registrarme, entonces el sistema responde que ya existe (`409`) | `AuthApi.registerRejectsDuplicateEmail` |
| **Formulario.** Dado el formulario de registro, cuando dejo campos inválidos, entonces se muestran los mensajes y no se envía | `FE register.test.tsx`, `schemas/auth.test.ts` |

### HU-02 — Iniciar sesión (M)

Como usuario registrado, quiero iniciar sesión con mi correo y contraseña para acceder a lo que corresponde a mi rol. *(RF-02, RNF-03)*

**Tareas:** pantalla de login · `POST /auth/login` · token de acceso (15 min) y cookie de renovación · límite de intentos.

| Escenario | Prueba |
|---|---|
| **Login correcto.** Dado un usuario activo, cuando envío sus credenciales, entonces recibo un token que abre mi perfil y una cookie de renovación `HttpOnly` | `AuthApi.loginReturnsWorkingAccessTokenAndRefreshCookie` |
| **Datos del supervisor.** Dado un supervisor, cuando inicio sesión, entonces mi perfil incluye mi área | `AuthApi.supervisorLoginCarriesTheAreaInTheUserAndInMe` |
| **Credenciales inválidas.** Dada una contraseña incorrecta o un correo desconocido, cuando inicio sesión, entonces la respuesta es la misma en ambos casos (`401`), sin revelar si el correo existe | `AuthApi.wrongPasswordAndUnknownEmailReturnTheSameProblem` |
| **Usuario inactivo.** Dado un usuario desactivado, cuando inicio sesión, entonces se rechaza | `AuthApi.inactiveUserCannotLogin` |
| **Límite de intentos.** Dados cinco intentos fallidos, cuando intento un sexto, entonces recibo `429` con `Retry-After` | `LoginLimit.sixthFailedAttemptIsRateLimitedWithRetryAfter` |
| **El límite no afecta a otros.** Dado un bloqueo por correo, entonces las demás cuentas pueden iniciar sesión | `LoginLimit.blockIsKeyedByLowercasedEmailAndDoesNotAffectOtherAccounts` |
| **Reinicio.** Dado un login correcto, entonces el contador de fallos vuelve a cero | `LoginLimit.successfulLoginResetsTheCounter` |

### HU-03 — Mantener y cerrar la sesión (M)

Como usuario, quiero que mi sesión se renueve sola y poder cerrarla para no tener que iniciar sesión cada 15 minutos y para proteger mi cuenta. *(RF-03)*

**Tareas:** `POST /auth/refresh` con rotación · `POST /auth/logout` · restaurar la sesión al recargar la página.

| Escenario | Prueba |
|---|---|
| **Rotación.** Dado un token de renovación válido, cuando renuevo, entonces recibo un token nuevo y el anterior ya no sirve | `AuthApi.refreshRotatesTheTokenAndRejectsReuse` |
| **Sin cookie o desconocida.** Dada una renovación sin cookie válida, entonces se rechaza (`401`) | `AuthApi.refreshWithoutOrWithUnknownCookieIsUnauthorized` |
| **Token vencido.** Dado un token de renovación vencido, entonces se rechaza | `AuthApi.refreshRejectsExpiredToken` |
| **Cerrar sesión.** Dada una sesión abierta, cuando cierro sesión, entonces el token queda revocado y la cookie se elimina | `AuthApi.logoutRevokesTheTokenAndClearsTheCookie` |

### HU-04 — Acceder solo a lo que corresponde a mi rol (M)

Como institución, quiero que cada rol solo vea y haga lo que le corresponde para proteger la información de las solicitudes. *(RF-04, RNF-02)*

**Tareas:** autorización por rol en cada endpoint · `AccessPolicy` (visibilidad de una solicitud) · rutas protegidas en el frontend.

| Escenario | Prueba |
|---|---|
| **Solo solicitantes registran.** Dado un técnico, supervisor o administrador, cuando intenta registrar una solicitud, entonces recibe `403` | `ReqApi.onlyRequestersCanCreateRequests` |
| **Visibilidad por rol.** Dado el detalle de una solicitud, entonces lo ve su dueño, el supervisor de su área, el técnico asignado y el administrador, y nadie más (`404`) | `ReqApi.detailRespectsVisibilityByRole` |
| **Técnico no asignado.** Dado un técnico del área que no tiene la solicitud asignada, cuando la consulta, entonces no la ve | `ReqApi.aTechnicianOfTheAreaThatIsNotAssignedCannotSeeTheRequest` |
| **Listados filtrados.** Dado cada rol, cuando lista solicitudes, entonces solo recibe las suyas | `ReqApi.listShowsOnlyWhatEachRoleCanSee` |
| **Transiciones por rol.** Dada una transición, cuando la intenta un rol que no corresponde, entonces recibe `403` | `WfApi.onlyTheRightRoleMayPerformEachTransition` |
| **Técnicos del área.** Dada la lista de técnicos, entonces solo la consulta un supervisor y de su propia área | `WfApi.technicianListingIsRestrictedToSupervisorsAndTheirOwnArea` |
| **Sin sesión.** Dada una petición sin token, entonces recibe `401` | `ReqApi.unknownCodeAndMissingTokenAreHandled` |

### HU-05 — Cambiar mi contraseña (S)

Como usuario, quiero cambiar mi contraseña para mantener mi cuenta segura. *(RF-05)*

**Tareas:** modal en el perfil · `PUT /auth/password` · exigir la contraseña actual · revocar las demás sesiones.

| Escenario | Prueba |
|---|---|
| **Cambio correcto.** Dada la contraseña actual correcta y una nueva válida, cuando la cambio, entonces puedo iniciar sesión con la nueva | **Por escribir** (backend pendiente) |
| **Contraseña actual errónea.** Dada una contraseña actual incorrecta, cuando intento cambiarla, entonces se rechaza | **Por escribir** |
| **Validación en el formulario.** Dada una nueva contraseña inválida, entonces el formulario lo indica | `FE schemas/auth.test.ts` |

---

## E2. Registro y consulta de solicitudes

### HU-06 — Registrar una solicitud (M)

Como solicitante, quiero registrar una solicitud de servicio para informar un problema y recibir un código con el que seguirla. *(RF-07, RF-08, RF-09)*

**Tareas:** formulario (categoría, Facultad → Pabellón → Ambiente, descripción, prioridad) · `POST /requests` · código correlativo por año · área derivada de la categoría · vencimiento según SLA · primera entrada de historial.

| Escenario | Prueba |
|---|---|
| **Registro correcto.** Dado un solicitante autenticado, cuando registra una solicitud con todos los datos obligatorios, entonces el sistema la registra, genera un código único y muestra el estado "Registrada" con su primera entrada de historial | `ReqApi.createReturnsTheDetailWithCodeStatusAndFirstHistoryEntry` |
| **Persistencia.** Dada una solicitud registrada, entonces queda guardada con su historial | `ReqApi.persistsTheRequestAndItsHistory` |
| **Área por categoría.** Dada una categoría, entonces el área de la solicitud es la de la categoría | `ReqApi.theAreaIsAlwaysTheDefaultAreaOfTheCategory` |
| **Vencimiento.** Dada una prioridad, entonces el vencimiento es el registro más las horas de su SLA | `ReqApi.dueDateIsRegistrationPlusTheSlaHoursOfThePriority`, `SlaTest` |
| **Códigos concurrentes.** Dadas diez creaciones simultáneas, entonces los diez códigos son distintos y consecutivos | `ReqApi.tenParallelCreationsGetTenDistinctConsecutiveCodes` |
| **Cambio de año.** Dado un nuevo año, entonces el correlativo reinicia | `ReqApi.theSequenceRestartsEachYear` |
| **Formato del código.** Dado un código, entonces tiene la forma `SOL-AAAA-NNNNNN` | `RequestCodeTest` |
| **Formulario completo.** Dado el formulario, cuando completo los datos, entonces se envía y veo el código asignado | `FE new-request.test.tsx` |

### HU-07 — Que el sistema valide mi solicitud (M)

Como solicitante, quiero que el sistema me avise si falta o está mal un dato para no registrar una solicitud incompleta. *(RF-07)*

**Tareas:** validación en el servidor con error por campo · validación equivalente en el formulario · contador de caracteres.

| Escenario | Prueba |
|---|---|
| **Datos incompletos.** Dado un solicitante registrando, cuando omite un campo obligatorio o usa una referencia inexistente, entonces el sistema responde `400` indicando el campo y no registra la solicitud | `ReqApi.invalidReferencesAndMissingFieldsAreRejectedWithFieldErrors` |
| **Longitud.** Dada una descripción de menos de 20 o más de 2000 caracteres, entonces se rechaza | `ReqApi.descriptionLengthIsValidated` |
| **Espacios.** Dada una descripción que solo llega a 20 caracteres contando espacios de los bordes, entonces se rechaza | `ReqApi.surroundingWhitespaceDoesNotCountAsDescription` |
| **En el formulario.** Dado el formulario, cuando omito un campo, entonces veo el mensaje y no se envía | `FE schemas/newRequest.test.ts` |

### HU-08 — Ver mis solicitudes y su detalle con historial (M)

Como solicitante, quiero ver mis solicitudes y el historial de cada una para saber en qué estado están y quién las tiene. *(RF-11)*

**Tareas:** `GET /requests` filtrado por rol, con estado y paginación · `GET /requests/{codigo}` · línea de tiempo del historial · marca de vencida.

| Escenario | Prueba |
|---|---|
| **Mis solicitudes.** Dado un solicitante con solicitudes, cuando abre su lista, entonces solo ve las suyas, de la más reciente a la más antigua | `ReqApi.listShowsOnlyWhatEachRoleCanSee`, `ReqApi.listIsPagedAndNewestFirst` |
| **Filtro por estado.** Dado un estado, cuando filtro, entonces solo veo las solicitudes en ese estado | `ReqApi.listCanBeFilteredByStatus` |
| **Paginación acotada.** Dado un tamaño de página excesivo o una página negativa, entonces el servidor los acota | `ReqApi.sizeIsCappedAndNegativePageIsTreatedAsFirst` |
| **Historial ordenado.** Dado el detalle, entonces el historial aparece ordenado por fecha | `ReqApi.historyOfTheDetailIsOrderedByTime` |
| **Vencidas.** Dada una solicitud abierta pasada su fecha de vencimiento, entonces se marca como vencida; una resuelta no | `ReqApi.anOpenRequestPastItsDueDateIsOverdueButAResolvedOneIsNot` |
| **Búsqueda y filtros en la interfaz.** Dada la lista, cuando busco por código, descripción, categoría o ambiente, o filtro por estado, categoría, prioridad, técnico o vencidas, entonces solo veo lo que coincide | `FE features/requests/lib/filters.test.ts` |

### HU-09 — Elegir la ubicación y la categoría desde catálogos (M)

Como solicitante, quiero elegir la categoría y la ubicación de una lista para no escribirlas a mano y evitar errores. *(RF-10)*

**Tareas:** `GET /catalogs` · árbol Facultad → Pabellón → Ambiente · solo elementos activos.

| Escenario | Prueba |
|---|---|
| **Catálogos disponibles.** Dado un usuario autenticado, cuando consulta los catálogos, entonces recibe áreas, categorías, prioridades y ubicaciones | `CatApi.returnsTheSeededAreasCategoriesPrioritiesAndLocations` |
| **Orden por SLA.** Dadas las prioridades, entonces llegan ordenadas con sus horas de SLA | `CatApi.prioritiesComeOrderedWithTheirSlaHours` |
| **Ubicaciones jerárquicas.** Dadas las ubicaciones, entonces incluyen nivel y padre para armar el árbol | `CatApi.locationsExposeKindAndParentSoTheTreeCanBeBuilt` |
| **Solo activos.** Dada una categoría o área inactiva, entonces no se lista | `CatApi.inactiveCategoriesAndAreasAreNotListed` |
| **Sin sesión.** Dada una consulta sin token, entonces se rechaza | `CatApi.catalogsRequireAuthentication` |

### HU-10 — Comentar una solicitud (S)

Como participante de una solicitud, quiero dejar comentarios para coordinar con las demás personas involucradas. *(RF-12)*

**Tareas:** `GET`/`POST /requests/{codigo}/comments` · pestaña de comentarios · respetar la visibilidad de la solicitud.

| Escenario | Prueba |
|---|---|
| **Comentario correcto.** Dado un participante de la solicitud, cuando comenta, entonces el comentario aparece con su autor y fecha | **Por escribir** (backend pendiente) |
| **Sin acceso.** Dado un usuario que no ve la solicitud, cuando intenta comentar, entonces recibe `404` | **Por escribir** |
| **Comentario vacío.** Dado un texto vacío, entonces se rechaza | **Por escribir** |

### HU-11 — Adjuntar evidencias (S)

Como solicitante, quiero adjuntar fotos o un PDF para mostrar el problema. *(RF-13)*

**Tareas:** `POST`/`GET /requests/{codigo}/evidences` · límites de cantidad, tamaño y tipo · almacenamiento en volumen persistente · descarga solo para usuarios autorizados.

| Escenario | Prueba |
|---|---|
| **Adjunto válido.** Dado un archivo JPG, PNG o PDF de hasta 5 MB, cuando lo adjunto, entonces queda asociado a la solicitud | **Por escribir** |
| **Archivo no permitido o grande.** Dado un archivo de otro tipo o de más de 5 MB, entonces se rechaza | **Por escribir**; en la interfaz, `FE shared/lib/files.test.ts` |
| **Límite de cinco.** Dada una solicitud con cinco evidencias, cuando adjunto otra, entonces se rechaza | **Por escribir** |

---

## E3. Flujo de atención

Estas historias implementan la [máquina de estados](../specs/2026-10-14-servicios-universitarios-design.md#6-máquina-de-estados) del spec. El orden de validación es siempre: `409` (la transición no está en la tabla), luego `403` (el usuario no puede hacerla) y luego `400` (faltan datos). La tabla completa de transiciones está cubierta por las pruebas de dominio `StateMachine`, que ejercitan cada fila válida (por ejemplo `supervisorMovesRegistradaToEnEvaluacion`, `requesterRejectsTheSolutionWithAReason`) y las transiciones inválidas (`transitionsOutsideTheTableAreConflicts`).

### HU-12 — Evaluar una solicitud (M)

Como supervisor, quiero pasar a evaluación las solicitudes de mi área para revisarlas antes de asignarlas. *(RF-14, RF-15)*

**Tareas:** acción "Evaluar" en el detalle · transición Registrada → En evaluación · solo el supervisor del área.

| Escenario | Prueba |
|---|---|
| **Evaluar.** Dada una solicitud Registrada de mi área, cuando la evalúo, entonces pasa a "En evaluación" y queda en el historial | `WfApi.theCompleteFlowLeavesSixHistoryEntriesWithUserDateAndStatuses`, `StateMachine.supervisorMovesRegistradaToEnEvaluacion` |
| **Otro supervisor.** Dado un supervisor de otra área, cuando intenta evaluarla, entonces recibe `403` | `WfApi.onlyTheRightRoleMayPerformEachTransition` |
| **Botones permitidos.** Dado el detalle, entonces solo muestra las acciones que el usuario puede ejecutar | `WfApi.theDetailListsOnlyTheTransitionsEachUserMayPerform`, `FE features/workflow/lib/transitions.test.ts` |

### HU-13 — Asignar una solicitud a un técnico (M)

Como supervisor, quiero asignar la solicitud a un técnico de mi área para que alguien sea responsable de atenderla. *(RF-15)*

**Tareas:** `GET /users/technicians` con la carga de cada técnico · selector de técnico en la acción "Asignar" · transición En evaluación → Asignada.

| Escenario | Prueba |
|---|---|
| **Asignación correcta.** Dada una solicitud en evaluación, cuando elijo un técnico activo de mi área, entonces queda Asignada a él | `WfApi.theCompleteFlowLeavesSixHistoryEntriesWithUserDateAndStatuses` |
| **Sin técnico.** Dada una asignación sin técnico, con uno de otra área o con uno inactivo, entonces recibo `400` y la solicitud no cambia | `WfApi.assigningWithoutATechnicianOrWithOneOfAnotherAreaOrInactiveIsABadRequest` |
| **Lista de técnicos.** Dado un supervisor, cuando consulta los técnicos, entonces ve los activos de su área con sus solicitudes en curso | `WfApi.aSupervisorListsTheActiveTechniciansOfTheirAreaWithTheirLoad`, `WfApi.theAreaDefaultsToTheOneOfTheSupervisor` |
| **Formulario.** Dada la acción de asignar, cuando no elijo técnico, entonces el formulario no se envía | `FE features/workflow/schemas/action.test.ts` |

### HU-14 — Reasignar una solicitud (M)

Como supervisor, quiero reasignar una solicitud a otro técnico, explicando el motivo, para equilibrar la carga o cubrir una ausencia. *(RF-15)*

**Tareas:** acción "Reasignar" con motivo obligatorio · transición Asignada → Asignada · el técnico nuevo debe ser distinto.

| Escenario | Prueba |
|---|---|
| **Reasignación correcta.** Dada una solicitud Asignada, cuando elijo otro técnico y escribo el motivo, entonces cambia de responsable y el historial registra el motivo | `WfApi.reassigningRequiresAReasonAndChangesTheTechnician` |
| **Sin motivo.** Dada una reasignación sin motivo, entonces recibo `400` | `WfApi.reassigningRequiresAReasonAndChangesTheTechnician`, `StateMachine.reassigningRequiresAReasonAndADifferentTechnician` |

### HU-15 — Atender y resolver una solicitud (M)

Como técnico, quiero iniciar la atención de lo que me asignaron y resolverlo explicando la solución para que el solicitante sepa qué se hizo. *(RF-16)*

**Tareas:** bandeja del técnico · acciones "Iniciar atención" y "Resolver" con solución obligatoria · solo el técnico asignado.

| Escenario | Prueba |
|---|---|
| **Atención y resolución.** Dada una solicitud Asignada a mí, cuando la inicio y luego la resuelvo con una solución, entonces pasa a "En atención" y luego a "Resuelta" | `WfApi.theCompleteFlowLeavesSixHistoryEntriesWithUserDateAndStatuses` |
| **Técnico no asignado.** Dado un técnico al que no se le asignó, cuando intenta iniciar la atención, entonces recibe `403` | `WfApi.anUnassignedTechnicianCannotStartAttending` |
| **Sin solución.** Dada una resolución sin solución escrita, entonces recibo `400` | `WfApi.resolvingWithoutASolutionCommentIsABadRequest` |

### HU-16 — Cerrar una solicitud que no procede (M)

Como supervisor, quiero cerrar las solicitudes duplicadas o improcedentes indicando el motivo para no desperdiciar la atención de los técnicos. *(RF-17)*

**Tareas:** acciones "No procede" y "Duplicada" con motivo · solo desde Registrada o En evaluación.

| Escenario | Prueba |
|---|---|
| **Cierre válido.** Dada una solicitud Registrada o En evaluación, cuando el supervisor del área la cierra como "No procede" o "Duplicada", entonces queda Cerrada con ese motivo | `WfApi.theSupervisorClosesAsNoProcedeOrDuplicadaOnlyFromRegistradaOrEnEvaluacion` |
| **Sin motivo o por otro rol.** Dado un cierre sin motivo, o por alguien que no es el supervisor del área, entonces recibe `400` o `403` | `WfApi.closingWithoutResolvingNeedsAClosureReasonAndOnlyTheSupervisorOfTheAreaMayDoIt` |
| **Solicitud ya asignada.** Dada una solicitud ya Asignada, entonces no se puede cerrar de esta forma (`409`) | `StateMachine.transitionsOutsideTheTableAreConflicts` |

### HU-17 — Confirmar o rechazar la solución (M)

Como solicitante, quiero confirmar que quedó resuelto, o rechazar la solución explicando qué falta, para que el cierre refleje la realidad. *(RF-18)*

**Tareas:** tarjeta "¿Quedó resuelto?" en el detalle · confirmar (transición a Cerrada) · rechazar con motivo (vuelve a En atención).

| Escenario | Prueba |
|---|---|
| **Confirmar.** Dada una solicitud Resuelta, cuando el solicitante la confirma, entonces queda Cerrada | `WfApi.theCompleteFlowLeavesSixHistoryEntriesWithUserDateAndStatuses` |
| **Rechazar.** Dada una solicitud Resuelta, cuando el solicitante rechaza la solución con un motivo, entonces vuelve a "En atención" | `WfApi.rejectingTheSolutionReturnsToEnAtencionAndRequiresAReason` |
| **Rechazo sin motivo.** Dado un rechazo sin motivo, entonces recibe `400` | `WfApi.rejectingTheSolutionReturnsToEnAtencionAndRequiresAReason` |
| **Interfaz.** Dada la tarjeta, cuando confirmo sin calificar, entonces la solicitud se cierra; si califico, la calificación se guarda después | `FE mocks/handlers.test.ts` (flujo completo y calificación posterior) |

### HU-18 — Consultar el historial de una solicitud (M)

Como participante, quiero ver quién hizo cada cambio, cuándo y por qué para tener trazabilidad completa. *(RF-19, RNF-13)*

**Tareas:** una entrada de historial por transición (usuario, fecha, estado anterior y nuevo, motivo) · línea de tiempo en el detalle.

| Escenario | Prueba |
|---|---|
| **Flujo completo.** Dado el ciclo registrar, evaluar, asignar, atender, resolver y cerrar, entonces el historial tiene seis entradas con usuario, fecha y estados | `WfApi.theCompleteFlowLeavesSixHistoryEntriesWithUserDateAndStatuses` |

### HU-19 — Que el sistema rechace transiciones inválidas (M)

Como institución, quiero que el sistema impida los saltos de estado que no están permitidos para que los datos sean confiables. *(RF-20, RNF-08)*

**Tareas:** tabla de transiciones como dominio puro · `409` ante lo no listado · bloqueo de la fila y control de versión.

| Escenario | Prueba |
|---|---|
| **Fuera de la tabla.** Dada una solicitud Registrada, cuando se intenta pasarla a Resuelta, entonces recibo `409` y la solicitud no cambia | `WfApi.aTransitionOutsideTheTableIsAConflictAndTheRequestDoesNotChange`, `StateMachine.transitionsOutsideTheTableAreConflicts` |
| **Solicitud cerrada.** Dada una solicitud Cerrada, entonces no admite ninguna transición | `WfApi.aClosedRequestAdmitsNoTransition` |
| **Estado destino inválido.** Dado un estado destino ausente o desconocido, entonces recibo `400` | `WfApi.aMissingOrUnknownTargetStatusIsABadRequest` |
| **Solicitud inexistente o sin sesión.** Entonces recibo `404` o `401` | `WfApi.anUnknownRequestIsNotFoundAndAMissingTokenIsUnauthorized` |
| **Simultaneidad.** Dadas dos transiciones idénticas al mismo tiempo, entonces solo una se aplica | `WfApi.twoSimultaneousIdenticalTransitionsOnlyOneWins` |
| **En la interfaz.** Dado un `409`, entonces se muestra "Transición no permitida" y se refresca el detalle | **Por escribir** (prueba de interfaz pendiente) |

### HU-20 — Conocer el plazo y las solicitudes vencidas (S)

Como supervisor, quiero ver el vencimiento de cada solicitud y cuáles están vencidas para priorizar la atención. *(RF-21)*

**Tareas:** SLA por prioridad · marca de vencida calculada en el servidor · recálculo al cambiar la prioridad · insignia de vencida en las bandejas.

| Escenario | Prueba |
|---|---|
| **Vencimiento.** Dada una prioridad, entonces el vencimiento es el registro más sus horas | `ReqApi.dueDateIsRegistrationPlusTheSlaHoursOfThePriority`, `SlaTest` |
| **Vencida.** Dada una solicitud que no llegó a Resuelta antes del vencimiento, entonces está vencida; una resuelta a tiempo no | `ReqApi.anOpenRequestPastItsDueDateIsOverdueButAResolvedOneIsNot`, `SlaTest` |
| **Interfaz.** Dado el cálculo de plazos, entonces la interfaz muestra tiempo restante y vencidas | `FE features/workflow/lib/sla.test.ts` |
| **Cambio de prioridad.** Dado un cambio de prioridad durante la evaluación, entonces el vencimiento se recalcula | **Por escribir** (cambio de prioridad pendiente en el backend) |

### HU-21 — Cierre automático a las 72 horas (S)

Como institución, quiero que una solicitud Resuelta sin respuesta se cierre sola a las 72 horas para no dejar solicitudes abiertas indefinidamente. *(RF-22)*

**Tareas:** tarea programada · cierre con motivo "automática" y sin calificación · historial y notificación.

| Escenario | Prueba |
|---|---|
| **Cierre automático.** Dada una solicitud Resuelta hace más de 72 horas sin respuesta, cuando corre la tarea, entonces queda Cerrada sin calificación | **Por escribir** (tarea programada pendiente) |
| **No antes de tiempo.** Dada una solicitud Resuelta hace menos de 72 horas, entonces no se cierra | **Por escribir** |

---

## E4. Seguimiento y calidad del servicio

### HU-22 — Recibir notificaciones de mis solicitudes (S)

Como solicitante, técnico o supervisor, quiero recibir un aviso dentro de la aplicación cuando cambia el estado de una solicitud mía o me asignan una para enterarme sin tener que revisar. *(RF-23)*

**Tareas:** `GET /notifications` y `PATCH /notifications/{id}/read` · generar la notificación en cada transición · panel y página de notificaciones.

| Escenario | Prueba |
|---|---|
| **Aviso por cambio de estado.** Dada una transición, entonces los interesados reciben una notificación con el código de la solicitud | **Por escribir** (backend pendiente) |
| **Marcar como leída.** Dada una notificación, cuando la marco como leída, entonces deja de contarse como pendiente | **Por escribir** |

### HU-23 — Recibir una sugerencia de categoría y prioridad (S)

Como solicitante, quiero que el sistema me sugiera la categoría y la prioridad según lo que escribo para registrar más rápido y con menos errores. *(RF-24)*

**Tareas:** `POST /triage/suggestions` · regla con más coincidencias · banner de sugerencia aceptable o descartable.

| Escenario | Prueba |
|---|---|
| **Sugerencia.** Dada una descripción con palabras clave de una regla, entonces el sistema sugiere su categoría y prioridad | `FE features/triage/lib/suggest.test.ts` |
| **Más coincidencias.** Dadas varias reglas, entonces gana la que tiene más coincidencias | `FE features/triage/lib/suggest.test.ts` |
| **Sin coincidencias.** Dada una descripción sin palabras clave, entonces no hay sugerencia | `FE features/triage/lib/suggest.test.ts` |
| **Endpoint del servidor.** Dada la misma descripción, entonces el servidor devuelve la misma sugerencia | **Por escribir** (backend pendiente) |

### HU-24 — Calificar la atención (S)

Como solicitante, quiero calificar la atención de 1 a 5 con un comentario para que la institución mida la calidad del servicio. *(RF-18, RF-25)*

**Tareas:** `POST /requests/{codigo}/rating` (después de cerrar por confirmación; la calificación es opcional) · estrellas y comentario en la tarjeta de solución.

| Escenario | Prueba |
|---|---|
| **Calificar.** Dada una solicitud cerrada por confirmación, cuando califico con una puntuación de 1 a 5, entonces queda guardada en la solicitud | `FE mocks/handlers.test.ts` (API simulada); backend **por escribir** |
| **Solo una vez y solo al cerrar.** Dada una solicitud sin cerrar o ya calificada, cuando califico, entonces recibo `409` | `FE mocks/handlers.test.ts`; backend **por escribir** |
| **Puntuación fuera de rango.** Dada una puntuación fuera de 1 a 5, entonces recibo `400` | `FE mocks/handlers.test.ts`; backend **por escribir** |

---

## E5. Indicadores

### HU-25 — Ver los indicadores de atención (S)

Como supervisor o administrador, quiero un dashboard con los indicadores de atención, filtrable por fechas y área, para tomar decisiones con datos. *(RF-26)*

**Tareas:** `GET /dashboard/indicators` con rango y área · nueve indicadores · gráficas · vista según el rol · reportes exportables.

| Escenario | Prueba |
|---|---|
| **Indicadores.** Dado un rango de fechas, entonces el dashboard muestra registradas, pendientes, atendidas, por categoría, por prioridad, tiempo promedio de atención, porcentaje resueltas, vencidas y por responsable | **Por escribir** (backend pendiente); cálculos de reportes en `FE features/dashboard/lib/reports.test.ts` |
| **Filtro por área.** Dado un área, entonces los indicadores solo cuentan sus solicitudes | **Por escribir** |
| **Vista por rol.** Dado un supervisor, entonces solo ve su área | **Por escribir** |

---

## E6. Administración

### HU-26 — Gestionar usuarios y catálogos (S)

Como administrador, quiero crear y desactivar usuarios, áreas, categorías, prioridades y ubicaciones para mantener el sistema al día sin tocar la base de datos. *(RF-27)*

**Tareas:** `/admin/*` (CRUD) · pantallas de usuarios y catálogos · consulta de estados en solo lectura · auditoría.

| Escenario | Prueba |
|---|---|
| **Crear un usuario con rol.** Dado un administrador, cuando crea un técnico con su área, entonces el usuario existe y puede iniciar sesión | **Por escribir** (backend pendiente) |
| **Solo el administrador.** Dado cualquier otro rol, cuando llama a `/admin/*`, entonces recibe `403` | **Por escribir** |
| **Validación del formulario.** Dado el formulario de usuario o catálogo, cuando falta un dato, entonces se muestra el error | `FE features/admin/schemas/forms.test.ts` |

### HU-27 — Gestionar las reglas de triage (S)

Como administrador, quiero crear y activar reglas de palabras clave para ajustar las sugerencias sin cambiar el código. *(RF-28)*

**Tareas:** CRUD de reglas · pantalla de reglas · activar y desactivar.

| Escenario | Prueba |
|---|---|
| **Regla válida.** Dado un administrador, cuando crea una regla con palabras clave, categoría y prioridad, entonces queda disponible para las sugerencias | **Por escribir** (backend pendiente) |
| **Validación del formulario.** Dada una regla sin palabras clave, entonces el formulario no se envía | `FE features/triage/schemas/rule.test.ts` |

---

## Resumen de cobertura

| | Historias | Con escenario y prueba automatizada | Con pruebas por escribir |
|---|---|---|---|
| Prioridad M | HU-01 a HU-04, HU-06 a HU-09, HU-12 a HU-19 | 16 de 16 | La prueba de interfaz del `409` de HU-19 |
| Prioridad S | HU-05, HU-10, HU-11, HU-20 a HU-27 | HU-20 (parcial), HU-23 y HU-24 (en la API simulada), validaciones de formularios | El backend de las demás |

Las pruebas por escribir son parte de los sprints 2 y 3; Lita las incorpora al informe de pruebas a medida que se construyen.
