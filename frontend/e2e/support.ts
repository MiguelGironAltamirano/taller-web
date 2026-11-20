import { expect, type APIRequestContext, type Locator, type Page } from '@playwright/test'
import { accounts, DEMO_PASSWORD } from './demoUsers.ts'

/**
 * Helpers shared by the journeys. They run against the real stack: the UI is driven through roles and labels, and
 * the REST API (same origin, through nginx) only prepares data a journey needs but does not test.
 */

/** Short unique marker so every test creates and finds its own data, even when the suite runs again on the same stack. */
export const uniqueTag = () => `e2e${Date.now().toString(36)}${Math.floor(Math.random() * 1e4).toString(36)}`

/** Demo catalog ids (V3__catalog_seed.sql). */
export const catalog = {
  category: { electricidad: 1, gasfiteria: 2 },
  priority: { critica: 1, alta: 2, media: 3, baja: 4 },
  /** Facultad de Ingeniería de Sistemas e Informática → Pabellón Principal → Aula 101. */
  room: { aula101: 3 },
  area: { mantenimiento: 1 },
} as const

/** Logs in through the login form and waits for the landing page of the role. */
export async function login(page: Page, account: { email: string; home?: string }, password = DEMO_PASSWORD) {
  await page.goto('/login')
  await page.getByLabel('Correo').fill(account.email)
  await page.getByLabel('Contraseña').fill(password)
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  if (account.home) await expect(page).toHaveURL(new RegExp(`${account.home}$`))
  else await expect(page).not.toHaveURL(/\/login$/)
}

/** Minimal authenticated REST client for test data set-up. */
export class Api {
  private constructor(
    private readonly request: APIRequestContext,
    private readonly token: string,
    readonly userId: number,
  ) {}

  static async as(request: APIRequestContext, email: string, password = DEMO_PASSWORD): Promise<Api> {
    const response = await request.post('/api/v1/auth/login', { data: { email, password } })
    expect(response.status(), `login of ${email}`).toBe(200)
    const body = (await response.json()) as { accessToken: string; user: { id: number } }
    return new Api(request, body.accessToken, body.user.id)
  }

  private async send<T>(method: 'GET' | 'POST' | 'PUT' | 'PATCH', path: string, data?: unknown): Promise<T> {
    const response = await this.request.fetch(`/api/v1${path}`, {
      method,
      data,
      headers: { Authorization: `Bearer ${this.token}` },
    })
    expect(response.ok(), `${method} ${path} → ${response.status()} ${await response.text()}`).toBeTruthy()
    return (response.status() === 204 ? undefined : await response.json()) as T
  }

  get<T>(path: string) {
    return this.send<T>('GET', path)
  }
  post<T>(path: string, data?: unknown) {
    return this.send<T>('POST', path, data)
  }
  put<T>(path: string, data?: unknown) {
    return this.send<T>('PUT', path, data)
  }
  patch<T>(path: string, data?: unknown) {
    return this.send<T>('PATCH', path, data)
  }
}

type NewRequest = { description: string; categoryId?: number; priorityId?: number; locationId?: number }

/** Registers a request as the given requester (Electricidad / Media / Aula 101 unless told otherwise). */
export async function createRequest(requester: Api, body: NewRequest): Promise<string> {
  const created = await requester.post<{ code: string }>('/requests', {
    categoryId: catalog.category.electricidad,
    priorityId: catalog.priority.media,
    locationId: catalog.room.aula101,
    ...body,
  })
  return created.code
}

export type Actors = { requester: Api; supervisor: Api; technician: Api }

export async function actors(request: APIRequestContext, requesterEmail: string = accounts.estudiante.email): Promise<Actors> {
  const [requester, supervisor, technician] = await Promise.all([
    Api.as(request, requesterEmail),
    Api.as(request, accounts.supervisor.email),
    Api.as(request, accounts.tecnico.email),
  ])
  return { requester, supervisor, technician }
}

const STEPS = ['EN_EVALUACION', 'ASIGNADA', 'EN_ATENCION', 'RESUELTA'] as const
export type Stage = (typeof STEPS)[number]

/**
 * Moves a Mantenimiento request through the workflow up to `stage`, each transition by the role that owns it
 * (supervisor evaluates and assigns to "Técnico Mantenimiento", who starts and resolves it).
 */
export async function advance(a: Actors, code: string, stage: Stage, solution = 'Se reemplazó el componente dañado.') {
  for (const step of STEPS.slice(0, STEPS.indexOf(stage) + 1)) {
    if (step === 'EN_EVALUACION') await a.supervisor.post(`/requests/${code}/transitions`, { to: step })
    if (step === 'ASIGNADA') await a.supervisor.post(`/requests/${code}/transitions`, { to: step, technicianId: a.technician.userId })
    if (step === 'EN_ATENCION') await a.technician.post(`/requests/${code}/transitions`, { to: step })
    if (step === 'RESUELTA') await a.technician.post(`/requests/${code}/transitions`, { to: step, reason: solution })
  }
}

/**
 * Runs an action that changes the request list (filter, search, page) and waits until the server answered a query
 * containing `urlFragment` and the list stopped fetching. The list keeps the previous rows on screen while it
 * loads, so asserting right after the action could read stale rows.
 */
export async function refetchList(page: Page, urlFragment: string, action: () => Promise<unknown>) {
  const answered = page.waitForResponse((r) => r.url().includes('/api/v1/requests?') && r.url().includes(urlFragment) && r.ok())
  await action()
  await answered
  await expect(page.locator('[aria-busy="true"]')).toHaveCount(0)
}

/** At least one row is shown and every one contains `text` (retried, so it waits for the new rows). */
export async function expectEveryRow(rows: Locator, text: string) {
  await expect(rows.first()).toBeVisible()
  await expect(rows.filter({ hasNotText: text })).toHaveCount(0)
}

/** Opens one tab of the request detail ("Resumen", "Comentarios", "Evidencias", "Historial"). */
export async function openTab(page: Page, name: string) {
  await page.getByRole('tab', { name: new RegExp(`^${name}`) }).click()
}
