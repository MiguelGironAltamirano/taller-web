import { expect, test } from '@playwright/test'
import { accounts } from './demoUsers.ts'
import { actors, advance, Api, createRequest, login, openTab, uniqueTag } from './support.ts'

/** Mantenimiento technician no other spec assigns work to, so its unread counter only moves in this file. */
const carmen = { email: 'carmen.vilchez@unmsm.edu.pe', name: 'Carmen Vílchez Paredes', home: '/solicitudes' }

test.describe('Technician', () => {
  test('starts the attention and resolves the request with a solution', async ({ page, request }) => {
    const a = await actors(request)
    const tag = uniqueTag()
    const code = await createRequest(a.requester, { description: `El foco del pasadizo parpadea todo el día (${tag}).` })
    await advance(a, code, 'ASIGNADA')

    await login(page, accounts.tecnico)
    await expect(page.getByRole('heading', { name: 'Mi bandeja', level: 1 })).toBeVisible()
    await page.getByRole('searchbox', { name: 'Buscar' }).fill(tag)
    const row = page.getByRole('table', { name: 'Solicitudes' }).getByRole('row').filter({ hasText: code })
    await expect(row).toContainText('Asignada')
    await row.click()
    await expect(page).toHaveURL(new RegExp(`/solicitudes/${code}$`))

    const detail = page.getByRole('region', { name: 'Detalle' })
    await detail.getByRole('button', { name: 'Iniciar atención' }).click()
    const start = page.getByRole('dialog', { name: 'Iniciar atención' })
    await start.getByRole('button', { name: 'Iniciar atención' }).click()
    await expect(start).toBeHidden()
    await expect(detail).toContainText('En atención')

    const solution = 'Se reemplazó el fluorescente y el arrancador.'
    await detail.getByRole('button', { name: 'Marcar como resuelta' }).click()
    const resolve = page.getByRole('dialog', { name: 'Marcar como resuelta' })
    await resolve.getByRole('button', { name: 'Marcar como resuelta' }).click()
    await expect(resolve.getByText(/al menos 5 caracteres/)).toBeVisible()
    await resolve.getByRole('textbox', { name: 'Comentario de solución' }).fill(solution)
    await resolve.getByRole('button', { name: 'Marcar como resuelta' }).click()
    await expect(resolve).toBeHidden()
    await expect(detail).toContainText('Resuelta')
    await expect(detail.getByRole('button', { name: 'Marcar como resuelta' })).toBeHidden()

    await openTab(page, 'Historial')
    await expect(page.getByRole('tabpanel')).toContainText('En atención → Resuelta')
    await expect(page.getByRole('tabpanel')).toContainText(solution)
    const seenByRequester = await a.requester.get<{ status: string; solution: string }>(`/requests/${code}`)
    expect(seenByRequester).toMatchObject({ status: 'RESUELTA', solution })
  })

  test('sees the assignment in the notifications badge and list, and marks it as read', async ({ page, request }) => {
    const [requester, supervisor] = await Promise.all([Api.as(request, accounts.estudiante.email), Api.as(request, accounts.supervisor.email)])
    const technician = await Api.as(request, carmen.email)
    const code = await createRequest(requester, { description: `Se desprendió la tapa del tablero eléctrico (${uniqueTag()}).` })
    await advance({ requester, supervisor, technician }, code, 'ASIGNADA')

    // The assigned technician is told twice (assignment and status change); the test follows the assignment one.
    const assigned = new RegExp(`^Se te asignó la solicitud ${code}\\.`)

    await login(page, carmen)
    const unreadBefore = (await technician.get<{ read: boolean }[]>('/notifications')).filter((n) => !n.read).length
    expect(unreadBefore).toBeGreaterThan(0)
    // Badge in the sidebar link and in the bell of the top bar.
    await expect(page.getByRole('link', { name: `Notificaciones ${unreadBefore}` })).toBeVisible()
    await expect(page.getByRole('button', { name: `Notificaciones, ${unreadBefore} sin leer` })).toBeVisible()

    // The bell opens the panel with the newest notifications.
    await page.getByRole('button', { name: /^Notificaciones, \d+ sin leer$/ }).click()
    const panel = page.getByRole('dialog', { name: 'Notificaciones' })
    await expect(panel.getByRole('button', { name: assigned })).toBeVisible()
    await panel.getByRole('button', { name: 'Ver todas' }).click()

    await expect(page.getByRole('heading', { name: 'Notificaciones', level: 1 })).toBeVisible()
    await expect(page.getByText(`${unreadBefore} sin leer`, { exact: true })).toBeVisible()
    const item = page.getByRole('main').getByRole('button', { name: assigned })
    await expect(item).toBeVisible()
    const markOne = item.locator('xpath=..').getByRole('button', { name: 'Marcar como leída' })
    await markOne.click()
    await expect(markOne).toBeHidden()
    await expect(page.getByText(`${unreadBefore - 1} sin leer`, { exact: true })).toBeVisible()
    await expect(page.getByRole('link', { name: unreadBefore - 1 > 0 ? `Notificaciones ${unreadBefore - 1}` : 'Notificaciones' })).toBeVisible()

    await page.getByRole('tab', { name: 'No leídas' }).click()
    await expect(page.getByRole('main').getByRole('button', { name: assigned })).toHaveCount(0)

    await page.getByRole('button', { name: 'Marcar todas como leídas' }).click()
    await expect(page.getByText('0 sin leer', { exact: true })).toBeVisible()
    await expect(page.getByText('No hay notificaciones sin leer.')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Notificaciones', exact: true })).toBeVisible()
  })
})
