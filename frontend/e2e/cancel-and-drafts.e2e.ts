import { expect, test, type APIRequestContext } from '@playwright/test'
import { accounts } from './demoUsers.ts'
import { Api, createRequest, login, uniqueTag } from './support.ts'

/** A requester created for the test: its requests and its draft never reach the shared demo accounts. */
async function freshRequester(request: APIRequestContext) {
  const admin = await Api.as(request, accounts.admin.email)
  const email = `${uniqueTag()}@unmsm.edu.pe`
  const created = await admin.post<{ temporaryPassword: string }>('/admin/users', {
    fullName: 'Solicitante Cancela E2E',
    email,
    role: 'SOLICITANTE',
    areaId: null,
  })
  return { email, password: created.temporaryPassword, home: '/inicio' }
}

test.describe('Cancel and restore a request', () => {
  test('the requester cancels with a reason, undoes it, cancels again and restores it', async ({ page, request }) => {
    const requester = await freshRequester(request)
    const api = await Api.as(request, requester.email, requester.password)
    const code = await createRequest(api, { description: `El enchufe del aula hace chispas al conectar (${uniqueTag()}).` })
    await login(page, requester, requester.password)
    await page.goto(`/solicitudes/${code}`)
    const detail = page.getByRole('region', { name: 'Detalle' })
    await expect(detail).toContainText('Registrada')

    const cancel = async (reason: string) => {
      await page.getByRole('button', { name: 'Cancelar solicitud' }).click()
      const dialog = page.getByRole('dialog', { name: 'Cancelar solicitud' })
      await dialog.getByLabel('¿Por qué la cancelas?').fill(reason)
      await dialog.getByRole('button', { name: 'Cancelar solicitud' }).click()
      await expect(dialog).toBeHidden()
    }

    await cancel('Lo reporté por error')
    const notice = page.getByRole('region', { name: 'Solicitud cancelada' })
    await expect(notice).toContainText('Motivo: Lo reporté por error')
    await expect(notice).toContainText('Puedes restaurarla hasta el')
    await expect(detail).toContainText('Cancelada')

    // «Deshacer» in the notice brings it back at once.
    await page.getByRole('button', { name: 'Deshacer' }).click()
    await expect(notice).toBeHidden()
    await expect(detail).toContainText('Registrada')
    // The confirmation toast sits over the «Acciones» panel on this viewport: close it, as a person would.
    await page.getByRole('button', { name: 'Cerrar aviso' }).click()

    // Later, from the detail: «Restaurar solicitud».
    await cancel('Ya lo arreglaron')
    await expect(notice).toBeVisible()
    await page.getByRole('button', { name: 'Cerrar aviso' }).click()
    await page.getByRole('button', { name: 'Restaurar solicitud' }).click()
    await page.getByRole('dialog', { name: 'Restaurar solicitud' }).getByRole('button', { name: 'Restaurar' }).click()
    await expect(notice).toBeHidden()
    await expect(detail).toContainText('Registrada')
    await expect(page.getByRole('button', { name: 'Cancelar solicitud' })).toBeVisible()
  })
})

test.describe('Draft of a new request', () => {
  test('what was typed is kept when leaving and can be picked up from the home page', async ({ page, request }) => {
    const requester = await freshRequester(request)
    const description = `La ventana del aula no cierra y entra el frío (${uniqueTag()}).`
    await login(page, requester, requester.password)

    await page.getByRole('link', { name: 'Nueva solicitud' }).click()
    await page.getByLabel('Descripción *').fill(description)
    await expect(page.getByText(/Borrador guardado/)).toBeVisible()

    // Leaving through the menu loses nothing: the home page offers the draft.
    await page.getByRole('navigation').getByRole('link', { name: 'Inicio' }).first().click()
    const card = page.getByRole('region', { name: 'Borrador sin enviar' })
    await expect(card).toContainText('Tienes una solicitud sin enviar')
    await card.getByRole('link', { name: 'Retomar borrador' }).click()

    await expect(page.getByLabel('Descripción *')).toHaveValue(description)
    await expect(page.getByText(/Las evidencias no se guardan en el borrador/)).toBeVisible()

    // «Cancelar» keeps it too, and says where to find it.
    await page.getByRole('button', { name: 'Cancelar', exact: true }).click()
    await expect(page.getByRole('status').filter({ hasText: 'Guardamos tu borrador' })).toBeVisible()
    await page.getByRole('link', { name: 'Nueva solicitud' }).click()
    const offer = page.getByRole('region', { name: 'Borrador sin enviar' })
    await offer.getByRole('button', { name: 'Descartar' }).click()
    await expect(offer).toBeHidden()
    await page.reload()
    await expect(page.getByRole('region', { name: 'Borrador sin enviar' })).toBeHidden()
    await expect(page.getByLabel('Descripción *')).toHaveValue('')
  })
})
