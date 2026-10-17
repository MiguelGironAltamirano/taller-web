import type {
  AppConfig,
  ClosureReason,
  HistoryEntry,
  LocationKind,
  RequestStatus,
  RequesterType,
  Role,
} from '../shared/api/types'
import { STATUS_ORDER } from '../shared/api/mappers'

/** Estado en memoria de la API simulada. Se reinicia al recargar la página o con `resetMockData()`. */

export const HOUR = 3_600_000
export const DEMO_PASSWORD = 'Demo12345!'

export type MockUser = {
  id: number
  fullName: string
  email: string
  role: Role
  areaId: number | null
  requesterType: RequesterType | null
  active: boolean
  password: string
}

export type MockComment = { id: number; authorId: number; authorName: string; text: string; createdAt: number }
export type MockEvidence = { id: number; fileName: string; kind: 'IMG' | 'PDF'; sizeBytes: number }
export type MockHistory = Omit<HistoryEntry, 'createdAt'> & { createdAt: number }

export type MockRequest = {
  seq: number
  year: number
  requesterId: number
  categoryId: number
  /** Id de la ubicación de nivel AMBIENTE. */
  locationId: number
  priorityId: number
  status: RequestStatus
  createdAt: number
  dueAt: number
  technicianId: number | null
  description: string
  closureReason: ClosureReason | null
  solution: string | null
  rating: { score: number; comment: string | null } | null
  comments: MockComment[]
  evidences: MockEvidence[]
  history: MockHistory[]
}

export type MockNotification = {
  id: number
  recipientId: number
  text: string
  createdAt: number
  read: boolean
  requestCode: string | null
}

export type MockTriageRule = { id: number; keywords: string[]; categoryId: number; priorityId: number; active: boolean }

export type MockDb = {
  now: number
  nextId: number
  /** `active: false` = desactivada por el administrador (como en el backend); sin el campo, activa. */
  areas: { id: number; name: string; active?: boolean }[]
  categories: { id: number; name: string; areaId: number; active?: boolean }[]
  priorities: { id: number; name: string; slaHours: number }[]
  locations: { id: number; name: string; kind: LocationKind; parentId: number | null }[]
  users: MockUser[]
  requests: MockRequest[]
  notifications: MockNotification[]
  rules: MockTriageRule[]
  config: AppConfig
}

export const formatCode = (year: number, seq: number) => `SOL-${year}-${String(seq).padStart(6, '0')}`
export const codeOf = (r: MockRequest) => formatCode(r.year, r.seq)

const AREAS = ['Mantenimiento e Infraestructura', 'Soporte Informático', 'Administración Académica']

const CATEGORIES: [string, number][] = [
  ['Electricidad', 1],
  ['Plomería y sanitarios', 1],
  ['Mobiliario y carpintería', 1],
  ['Red e internet', 2],
  ['Equipos de cómputo', 2],
  ['Cuentas y accesos', 2],
  ['Constancias y certificados', 3],
  ['Matrícula y traslados', 3],
]

const PRIORITIES: [string, number][] = [
  ['Crítica', 8],
  ['Alta', 24],
  ['Media', 72],
  ['Baja', 120],
]

const LOCATION_TREE: Record<string, Record<string, string[]>> = {
  'Facultad de Ingeniería de Sistemas e Informática': {
    'Pabellón A': ['Aula 204', 'Aula 205', 'Laboratorio 3'],
    'Pabellón B': ['Aula 301', 'Sala de docentes'],
  },
  'Facultad de Medicina': { 'Pabellón Central': ['Aula 12', 'Laboratorio de Anatomía'] },
  'Facultad de Letras y Ciencias Humanas': { 'Pabellón 1': ['Auditorio', 'Aula 105'] },
  'Facultad de Derecho y Ciencia Política': { 'Pabellón Principal': ['Aula 3B', 'Decanato'] },
  'Biblioteca Central': { 'Edificio principal': ['Sala de lectura', 'Cubículos de estudio'] },
  'Ciudad Universitaria': { 'Edificio Administrativo': ['Oficina de Registros Académicos', 'Mesa de partes'] },
}

type SeedUser = [string, string, Role, number | null, RequesterType | null, boolean]
const USERS: SeedUser[] = [
  ['Lucía Quispe Huamán', 'lquispe@unmsm.edu.pe', 'SOLICITANTE', null, 'ESTUDIANTE', true],
  ['Pedro Ramos Gil', 'pramos@unmsm.edu.pe', 'SOLICITANTE', null, 'DOCENTE', true],
  ['Marisol Cabrera Tello', 'mcabrera@unmsm.edu.pe', 'SOLICITANTE', null, 'ADMINISTRATIVO', true],
  ['Carlos Mendoza Rojas', 'cmendoza@unmsm.edu.pe', 'TECNICO', 1, null, true],
  ['Jorge Paucar Vilca', 'jpaucar@unmsm.edu.pe', 'TECNICO', 1, null, true],
  ['Rosa Alarcón Díaz', 'ralarcon@unmsm.edu.pe', 'TECNICO', 2, null, true],
  ['Milagros Torres Cárdenas', 'mtorres@unmsm.edu.pe', 'TECNICO', 2, null, false],
  ['Luis Huamán Soto', 'lhuaman@unmsm.edu.pe', 'TECNICO', 3, null, true],
  ['Ana Villanueva Ramos', 'avillanueva@unmsm.edu.pe', 'SUPERVISOR', 1, null, true],
  ['Roberto Salazar Chávez', 'rsalazar@unmsm.edu.pe', 'ADMINISTRADOR', null, null, true],
  ['Hugo Landa Prado', 'hlanda@unmsm.edu.pe', 'SOLICITANTE', null, 'ESTUDIANTE', true],
]

const FIS = 'Facultad de Ingeniería de Sistemas e Informática'

/** [categoría, facultad, pabellón, ambiente, prioridad, estado(0-5), horas desde el registro, técnico, descripción, solicitante?] */
type SeedRequest = [string, string, string, string, string, number, number, string | null, string, string?]
const REQUESTS: SeedRequest[] = [
  ['Electricidad', FIS, 'Pabellón A', 'Aula 204', 'Alta', 3, 30, 'Carlos Mendoza Rojas', 'Los tomacorrientes de la pared norte no tienen energía desde el lunes y no se pueden conectar los equipos del docente.'],
  ['Red e internet', FIS, 'Pabellón B', 'Sala de docentes', 'Alta', 3, 10, 'Rosa Alarcón Díaz', 'No hay conexión wifi en la sala de docentes; el punto de acceso parece apagado.'],
  ['Equipos de cómputo', FIS, 'Pabellón A', 'Laboratorio 3', 'Media', 2, 20, 'Milagros Torres Cárdenas', 'Cinco computadoras del laboratorio no encienden y el proyector no muestra imagen.'],
  ['Plomería y sanitarios', 'Facultad de Medicina', 'Pabellón Central', 'Aula 12', 'Crítica', 4, 6, 'Jorge Paucar Vilca', 'Hay una fuga de agua en el techo del aula que moja el piso durante las clases.'],
  ['Constancias y certificados', 'Ciudad Universitaria', 'Edificio Administrativo', 'Oficina de Registros Académicos', 'Baja', 1, 30, null, 'Solicito constancia de estudios para trámite de beca, con el ciclo actual incluido.'],
  ['Mobiliario y carpintería', 'Facultad de Letras y Ciencias Humanas', 'Pabellón 1', 'Aula 105', 'Baja', 0, 5, null, 'Tres carpetas del fondo tienen las patas rotas y las sillas están sueltas.'],
  ['Cuentas y accesos', FIS, 'Pabellón A', 'Aula 205', 'Media', 5, 120, 'Milagros Torres Cárdenas', 'No puedo ingresar al aula virtual con mi correo institucional desde hace dos días.'],
  ['Electricidad', 'Facultad de Derecho y Ciencia Política', 'Pabellón Principal', 'Aula 3B', 'Crítica', 3, 12, 'Carlos Mendoza Rojas', 'Salen chispas del tablero eléctrico del aula al encender las luces. Se necesita revisión inmediata.'],
  ['Matrícula y traslados', 'Ciudad Universitaria', 'Edificio Administrativo', 'Mesa de partes', 'Baja', 2, 50, 'Luis Huamán Soto', 'Necesito regularizar mi matrícula por un curso que no aparece en mi horario.'],
  ['Equipos de cómputo', 'Facultad de Letras y Ciencias Humanas', 'Pabellón 1', 'Auditorio', 'Media', 4, 60, 'Rosa Alarcón Díaz', 'El proyector del auditorio no detecta la señal HDMI de ninguna laptop.'],
  ['Mobiliario y carpintería', FIS, 'Pabellón B', 'Aula 301', 'Media', 3, 100, 'Carlos Mendoza Rojas', 'La puerta del aula no cierra bien y la cerradura está trabada.'],
  ['Red e internet', 'Biblioteca Central', 'Edificio principal', 'Sala de lectura', 'Alta', 1, 3, null, 'La red cableada de la sala de lectura no entrega internet en los puestos de consulta.'],
  ['Plomería y sanitarios', FIS, 'Pabellón A', 'Aula 204', 'Media', 5, 200, 'Carlos Mendoza Rojas', 'El caño del lavadero del pasillo gotea de forma continua.'],
  ['Electricidad', FIS, 'Pabellón B', 'Aula 301', 'Media', 1, 8, null, 'Dos focos del pasillo están quemados y la zona queda a oscuras en la noche.', 'Pedro Ramos Gil'],
  ['Red e internet', 'Facultad de Medicina', 'Pabellón Central', 'Laboratorio de Anatomía', 'Alta', 2, 15, 'Rosa Alarcón Díaz', 'Sin conexión de red en las computadoras del laboratorio.', 'Marisol Cabrera Tello'],
  ['Equipos de cómputo', FIS, 'Pabellón A', 'Laboratorio 3', 'Baja', 0, 2, null, 'Solicito instalación de software de modelado en 10 equipos del laboratorio.', 'Hugo Landa Prado'],
  ['Plomería y sanitarios', 'Facultad de Letras y Ciencias Humanas', 'Pabellón 1', 'Aula 105', 'Alta', 3, 40, 'Jorge Paucar Vilca', 'El inodoro del baño de varones está atorado y hay mal olor en el pasillo.', 'Pedro Ramos Gil'],
  ['Mobiliario y carpintería', 'Biblioteca Central', 'Edificio principal', 'Cubículos de estudio', 'Baja', 2, 12, 'Jorge Paucar Vilca', 'Dos mesas de los cubículos tienen el tablero suelto.', 'Marisol Cabrera Tello'],
]

const SOLUTIONS: Record<number, string> = {
  3: 'Se reparó la tubería del techo y se selló la filtración. El piso fue secado.',
  9: 'Se reemplazó el cable HDMI y se reconfiguró la salida del proyector.',
}
const EVIDENCES: Record<number, [string, number][]> = {
  0: [['tablero_aula204.jpg', 412_000], ['tomacorriente.png', 288_000]],
  2: [['lab3_equipos.jpg', 530_000]],
  3: [['fuga_techo.jpg', 610_000]],
  7: [['tablero_chispas.jpg', 450_000], ['video_tablero.pdf', 1_900_000]],
}
const REASONS = [
  'Solicitud creada',
  'Se inició la evaluación del caso',
  'Asignada al técnico del área',
  'Atención iniciada en sitio',
  'Trabajo concluido; pendiente de confirmación',
  'Solución confirmada por el solicitante',
]

const RULES: [string[], string, string, boolean][] = [
  [['chispa', 'humo', 'incendio', 'cortocircuito'], 'Electricidad', 'Crítica', true],
  [['luz', 'foco', 'tomacorriente', 'enchufe', 'apagón', 'tablero', 'eléctric'], 'Electricidad', 'Alta', true],
  [['wifi', 'internet', 'conexión', 'red '], 'Red e internet', 'Alta', true],
  [['proyector', 'computadora', 'laptop', 'teclado', 'monitor'], 'Equipos de cómputo', 'Media', true],
  [['fuga', 'agua', 'caño', 'inodoro', 'baño', 'gotea'], 'Plomería y sanitarios', 'Alta', true],
  [['silla', 'carpeta', 'puerta', 'mesa', 'ventana', 'cerradura'], 'Mobiliario y carpintería', 'Baja', true],
  [['constancia', 'certificado', 'boleta'], 'Constancias y certificados', 'Baja', true],
  [['contraseña', 'correo', 'cuenta', 'acceso', 'aula virtual'], 'Cuentas y accesos', 'Media', true],
  [['matrícula', 'traslado', 'horario'], 'Matrícula y traslados', 'Baja', true],
]

/** Construye los datos iniciales tomando `now` como "ahora" para que los SLA sean coherentes. */
export function createSeed(now: number): MockDb {
  let id = 1000
  const nextId = () => ++id

  const areas = AREAS.map((name, i) => ({ id: i + 1, name }))
  const categories = CATEGORIES.map(([name, areaId], i) => ({ id: i + 1, name, areaId }))
  const priorities = PRIORITIES.map(([name, slaHours], i) => ({ id: i + 1, name, slaHours }))

  const locations: MockDb['locations'] = []
  const roomIdByKey = new Map<string, number>()
  let locId = 0
  for (const [faculty, pavilions] of Object.entries(LOCATION_TREE)) {
    const facultyId = ++locId
    locations.push({ id: facultyId, name: faculty, kind: 'FACULTAD', parentId: null })
    for (const [pavilion, rooms] of Object.entries(pavilions)) {
      const pavilionId = ++locId
      locations.push({ id: pavilionId, name: pavilion, kind: 'PABELLON', parentId: facultyId })
      for (const room of rooms) {
        const roomId = ++locId
        locations.push({ id: roomId, name: room, kind: 'AMBIENTE', parentId: pavilionId })
        roomIdByKey.set(`${faculty}|${pavilion}|${room}`, roomId)
      }
    }
  }

  const users: MockUser[] = USERS.map(([fullName, email, role, areaId, requesterType, active], i) => ({
    id: i + 1,
    fullName,
    email,
    role,
    areaId,
    requesterType,
    active,
    password: DEMO_PASSWORD,
  }))
  const userByName = (name: string) => users.find((u) => u.fullName === name)!
  const categoryId = (name: string) => categories.find((c) => c.name === name)!.id
  const priorityId = (name: string) => priorities.find((p) => p.name === name)!.id

  const year = new Date(now).getFullYear()
  const requests: MockRequest[] = REQUESTS.map((s, i) => {
    const [cat, fac, pav, room, prio, st, hoursAgo, techName, description, owner] = s
    const requester = userByName(owner ?? 'Lucía Quispe Huamán')
    const supervisor = users.find((u) => u.role === 'SUPERVISOR')!
    const tech = techName ? userByName(techName) : null
    const createdAt = now - hoursAgo * HOUR
    const prioLevel = priorities.find((p) => p.name === prio)!
    const history: MockHistory[] = []
    for (let k = 0; k <= st; k++) {
      const actor =
        k === 0 || k === 5 ? requester.fullName : k <= 2 ? `${supervisor.fullName} (Supervisora)` : (tech?.fullName ?? 'Técnico')
      history.push({
        fromStatus: k ? STATUS_ORDER[k - 1] : null,
        toStatus: STATUS_ORDER[k],
        actorName: actor,
        reason: REASONS[k] + (k === 2 && tech ? `: ${tech.fullName}` : ''),
        createdAt: createdAt + hoursAgo * HOUR * (k / (st + 1)),
      })
    }
    return {
      seq: 112 + i,
      year,
      requesterId: requester.id,
      categoryId: categoryId(cat),
      locationId: roomIdByKey.get(`${fac}|${pav}|${room}`)!,
      priorityId: priorityId(prio),
      status: STATUS_ORDER[st],
      createdAt,
      dueAt: createdAt + prioLevel.slaHours * HOUR,
      technicianId: tech?.id ?? null,
      description,
      closureReason: st === 5 ? 'CONFIRMADA' : null,
      solution: st >= 4 ? (SOLUTIONS[i] ?? 'Se resolvió el problema reportado.') : null,
      rating: st === 5 ? { score: 4, comment: null } : null,
      comments:
        st >= 3 && tech
          ? [
              {
                id: nextId(),
                authorId: tech.id,
                authorName: tech.fullName,
                text: 'Revisé el caso en sitio y estoy coordinando los materiales necesarios.',
                createdAt: createdAt + hoursAgo * HOUR * 0.6,
              },
            ]
          : [],
      evidences: (EVIDENCES[i] ?? []).map(([fileName, sizeBytes]) => ({
        id: nextId(),
        fileName,
        kind: fileName.endsWith('.pdf') ? ('PDF' as const) : ('IMG' as const),
        sizeBytes,
      })),
      history,
    }
  })

  const lucia = userByName('Lucía Quispe Huamán')
  const carlos = userByName('Carlos Mendoza Rojas')
  const ana = userByName('Ana Villanueva Ramos')
  const roberto = userByName('Roberto Salazar Chávez')
  const c = (n: number) => formatCode(year, n)
  const note = (recipientId: number, text: string, hoursAgo: number, read: boolean, n: number | null): MockNotification => ({
    id: nextId(),
    recipientId,
    text,
    createdAt: now - hoursAgo * HOUR,
    read,
    requestCode: n === null ? null : c(n),
  })
  const notifications: MockNotification[] = [
    note(lucia.id, `Tu solicitud ${c(116)} pasó a Resuelta. Confirma si quedó solucionada.`, 1.5, false, 116),
    note(lucia.id, `Tu solicitud ${c(112)} pasó a En atención.`, 5, false, 112),
    note(lucia.id, `Se agregó un comentario en ${c(119)}.`, 8, false, 119),
    note(lucia.id, `Tu solicitud ${c(124)} pasó a En evaluación.`, 27, true, 124),
    note(lucia.id, `Tu solicitud ${c(121)} fue asignada a Luis Huamán Soto.`, 30, true, 121),
    note(lucia.id, `Tu solicitud ${c(118)} está por vencer en 6 horas.`, 51, true, 118),
    note(lucia.id, `Tu solicitud ${c(119)} se cerró tras tu confirmación.`, 120, true, 119),
    note(carlos.id, `Se te asignó la solicitud ${c(112)}.`, 4, false, 112),
    note(carlos.id, `La solicitud ${c(119)} venció hace 4 horas.`, 9, false, 119),
    note(carlos.id, `Se te asignó la solicitud ${c(122)}.`, 30, true, 122),
    note(ana.id, `Nueva solicitud registrada: ${c(130)} (Electricidad).`, 8, false, 125),
    note(ana.id, `La solicitud ${c(119)} superó su SLA.`, 9, false, 119),
    note(ana.id, `${c(123)} fue resuelta por Jorge Paucar Vilca.`, 28, true, 123),
    note(roberto.id, 'Se desactivó la cuenta de Milagros Torres Cárdenas.', 20, false, null),
    note(roberto.id, 'Hay 3 solicitudes vencidas en el sistema.', 3, false, null),
  ]

  const rules: MockTriageRule[] = RULES.map(([keywords, cat, prio, active]) => ({
    id: nextId(),
    keywords,
    categoryId: categoryId(cat),
    priorityId: priorityId(prio),
    active,
  }))

  return {
    now,
    nextId: id,
    areas,
    categories,
    priorities,
    locations,
    users,
    requests,
    notifications,
    rules,
    config: { autoCloseHours: 72, emailNotifications: true, triageSuggestions: true },
  }
}

let db: MockDb = createSeed(Date.now())

export const getDb = (): MockDb => db
export const resetMockData = (now: number = Date.now()): MockDb => {
  db = createSeed(now)
  return db
}
export const newId = (): number => ++db.nextId
