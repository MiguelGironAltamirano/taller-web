import { expect, test } from '@playwright/test'
import { accounts } from './demoUsers.ts'
import { actors, Api, createRequest, expectEveryRow, login, openTab, refetchList, uniqueTag } from './support.ts'

const CODE = /SOL-\d{4}-\d{6}/

type Kpi = { value: number }
type Indicators = {
  rangeDays: number
  areaName: string | null
  kpis: Record<'registered' | 'pending' | 'attended' | 'resolvedPct' | 'avgAttentionHours' | 'overdue', Kpi>
  slaCompliancePct: number
}
type TeamMember = { fullName: string; assigned: number; inProgress: number; overdue: number; capacity: number }

const KPIS = [
  { key: 'registered', label: 'Solicitudes registradas', suffix: '' },
  { key: 'pending', label: 'Pendientes', suffix: '' },
  { key: 'attended', label: 'Atendidas', suffix: '' },
  { key: 'resolvedPct', label: '% resueltas', suffix: '%' },
  { key: 'avgAttentionHours', label: 'Tiempo promedio de atención', suffix: ' h' },
  { key: 'overdue', label: 'Solicitudes vencidas', suffix: '' },
] as const

test.describe('Supervisor', () => {
  test('filters the area inbox by status, technician and priority', async ({ page }) => {
    await login(page, accounts.supervisor)
    await page.getByRole('link', { name: /^Bandeja del área/ }).click()
    await expect(page.getByRole('heading', { name: 'Bandeja del área', level: 1 })).toBeVisible()
    const rows = page.getByRole('table', { name: 'Solicitudes' }).getByRole('row').filter({ hasText: CODE })
    await expect(rows.first()).toBeVisible()

    await refetchList(page, 'technicianId=', () => page.getByRole('combobox', { name: 'Técnico' }).selectOption('Jorge Ramírez Soto'))
    await expect(page.getByRole('button', { name: 'Quitar filtro Técnico: Jorge Ramírez Soto' })).toBeVisible()
    await expectEveryRow(rows, 'Jorge Ramírez Soto')

    await refetchList(page, 'status=EN_ATENCION', () => page.getByRole('button', { name: /^En atención \d+$/ }).click())
    await expectEveryRow(rows, 'En atención')
    await expectEveryRow(rows, 'Jorge Ramírez Soto')

    await page.getByRole('button', { name: 'Limpiar filtros' }).first().click()
    await expect(page.getByRole('button', { name: /^Quitar filtro/ })).toHaveCount(0)
    await refetchList(page, 'priorityId=1', () => page.getByRole('combobox', { name: 'Prioridad' }).selectOption('Crítica'))
    await expectEveryRow(rows, 'Crítica')
  })

  test('evaluates a request, assigns a technician showing the load and changes the priority', async ({ page, request }) => {
    const { requester, technician } = await actors(request)
    const code = await createRequest(requester, { description: `El enchufe del laboratorio no da corriente (${uniqueTag()}).` })

    await login(page, accounts.supervisor)
    await page.goto(`/solicitudes/${code}`)
    const detail = page.getByRole('region', { name: 'Detalle' })
    await expect(detail).toContainText('Registrada')

    await detail.getByRole('button', { name: 'Pasar a evaluación' }).click()
    const evaluate = page.getByRole('dialog', { name: 'Pasar a evaluación' })
    await evaluate.getByRole('button', { name: 'Pasar a evaluación' }).click()
    await expect(evaluate).toBeHidden()
    await expect(detail).toContainText('En evaluación')

    await detail.getByRole('button', { name: 'Asignar técnico' }).click()
    const assign = page.getByRole('dialog', { name: 'Asignar técnico' })
    const select = assign.getByRole('combobox', { name: 'Técnico del área' })
    // Every technician option shows its current load; only the placeholder has none. The exact numbers are not
    // compared because other journeys assign work to the same technicians while this one runs.
    await expect(select.getByRole('option', { name: /· \d+ solicitudes activas$/ }).first()).toBeAttached()
    await expect(select.getByRole('option').filter({ hasNotText: 'solicitudes activas' })).toHaveText(['Selecciona un técnico'])
    await expect(select.getByRole('option', { name: new RegExp(`^${accounts.tecnico.name} · \\d+ solicitudes activas$`) })).toHaveCount(1)
    await select.selectOption(String(technician.userId))
    await assign.getByRole('button', { name: 'Asignar' }).click()
    await expect(assign).toBeHidden()
    await expect(detail).toContainText('Asignada')
    await expect(detail).toContainText(accounts.tecnico.name)

    await detail.getByRole('button', { name: 'Cambiar prioridad' }).click()
    const priority = page.getByRole('dialog', { name: 'Cambiar prioridad' })
    await priority.getByRole('combobox', { name: 'Nueva prioridad' }).selectOption({ label: 'Crítica · 8 h' })
    await expect(priority.getByText(/^Nuevo vencimiento: /)).toBeVisible()
    await priority.getByRole('button', { name: 'Cambiar prioridad' }).click()
    await expect(priority).toBeHidden()
    await expect(detail).toContainText('PrioridadCrítica')
    await expect(detail).toContainText('Plazo de atención · 8 h')

    await openTab(page, 'Historial')
    const history = page.getByRole('tabpanel')
    await expect(history).toContainText('Prioridad Media → Prioridad Crítica')
    await expect(history).toContainText(/Nuevo vencimiento: /)
    await expect(history).toContainText(`Asignada a ${accounts.tecnico.name}`)
  })

  test('dashboard shows the real indicators for 7 days, 30 days and this month', async ({ page, request }) => {
    const api = await Api.as(request, accounts.supervisorAdministrativo.email)
    await login(page, accounts.supervisorAdministrativo)
    await expect(page.getByRole('heading', { name: 'Dashboard · Servicios Administrativos' })).toBeVisible()

    for (const range of [
      { id: '7', label: '7 días' },
      { id: '30', label: '30 días' },
      { id: 'mes', label: 'Este mes' },
    ]) {
      await page.getByRole('group', { name: 'Rango' }).getByRole('button', { name: range.label }).click()
      await expect(page.getByRole('button', { name: range.label })).toHaveAttribute('aria-pressed', 'true')
      // Compared with a fresh API read on every retry, so the check survives requests created meanwhile.
      await expect(async () => {
        const data = await api.get<Indicators>(`/dashboard/indicators?range=${range.id}`)
        await expect(page.getByText(`Últimos ${data.rangeDays} días · Servicios Administrativos`)).toBeVisible({ timeout: 1_000 })
        for (const k of KPIS) {
          const value = data.kpis[k.key].value
          const shown = k.key === 'avgAttentionHours' && value === 0 ? '—' : `${value}${k.suffix}`
          await expect(page.getByRole('group', { name: k.label })).toContainText(shown, { timeout: 1_000 })
        }
        await expect(page.getByRole('img', { name: `Cumplimiento de SLA ${data.slaCompliancePct} por ciento` })).toBeVisible({
          timeout: 1_000,
        })
      }).toPass({ timeout: 15_000 })
    }
  })

  test('team page shows the current load of every technician of the area', async ({ page, request }) => {
    const api = await Api.as(request, accounts.supervisorAdministrativo.email)
    const team = await api.get<TeamMember[]>('/dashboard/team')
    expect(team.length).toBeGreaterThan(0)

    await login(page, accounts.supervisorAdministrativo)
    await page.getByRole('link', { name: 'Equipo' }).click()
    await expect(page.getByText('Carga actual por técnico · Servicios Administrativos')).toBeVisible()
    const table = page.getByRole('table', { name: 'Carga por técnico' })
    for (const t of team) {
      const row = table.getByRole('row').filter({ hasText: t.fullName })
      const cells = row.getByRole('cell')
      await expect(cells.nth(1)).toHaveText(String(t.assigned))
      await expect(cells.nth(2)).toHaveText(String(t.inProgress))
      await expect(cells.nth(3)).toHaveText(String(t.overdue))
      await expect(cells.nth(4)).toContainText(`${t.assigned + t.inProgress} de ${t.capacity}`)
      await expect(row.getByRole('progressbar', { name: `Carga de ${t.fullName}` })).toBeVisible()
    }
  })
})
