import { expect, test, type Page } from '@playwright/test'
import { accounts } from './demoUsers.ts'
import { actors, advance, Api, createRequest, login, uniqueTag } from './support.ts'

type Config = { autoCloseHours: number; emailNotifications: boolean; triageSuggestions: boolean }

/** Same format as the app (`dateLong`, Lima time zone): `16 oct, 10:00`. */
const limaDateLong = (ms: number) =>
  new Date(ms)
    .toLocaleString('es-PE', { timeZone: 'America/Lima', day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false })
    .replace('-', ' ')
    .replace('.', '')

async function adminLogin(page: Page, path: string) {
  await login(page, accounts.admin)
  await page.goto(path)
}

test.describe('Administrator', () => {
  test('creates a user, sees the temporary password once and deactivates the account', async ({ page }) => {
    const tag = uniqueTag()
    const email = `${tag}@unmsm.edu.pe`
    await adminLogin(page, '/admin/usuarios')
    await page.getByRole('button', { name: 'Nuevo usuario' }).click()
    const form = page.getByRole('dialog', { name: 'Nuevo usuario' })
    await form.getByLabel('Nombre completo').fill(`Técnico E2E ${tag}`)
    await form.getByLabel('Correo institucional').fill(email)
    await form.getByLabel('Rol').selectOption('Técnico')
    await form.getByLabel('Área (obligatoria para Técnico y Supervisor)').selectOption('Infraestructura')
    await form.getByRole('button', { name: 'Guardar' }).click()

    const created = page.getByRole('dialog', { name: 'Usuario creado' })
    await expect(created).toContainText(email)
    await expect(created.getByTestId('temporary-password')).toHaveText(/\S{8,}/)
    await created.getByRole('button', { name: 'Entendido' }).click()
    await expect(created).toBeHidden()

    await page.getByRole('searchbox', { name: 'Buscar' }).fill(tag)
    const row = page.getByRole('table', { name: 'Usuarios' }).getByRole('row').filter({ hasText: email })
    await expect(row).toHaveCount(1)
    await expect(row).toContainText('Técnico')
    await expect(row).toContainText('Infraestructura')
    await expect(row).toContainText('Activo')

    await row.getByRole('button', { name: 'Desactivar' }).click()
    await expect(page.getByRole('status').filter({ hasText: 'Usuario desactivado' })).toBeVisible()
    await expect(row).toContainText('Inactivo')
    await expect(row.getByRole('button', { name: 'Activar' })).toBeVisible()
  })

  test('creates and deletes an area and a category in the catalogs', async ({ page }) => {
    const area = `Área E2E ${uniqueTag()}`
    const category = `Categoría E2E ${uniqueTag()}`
    await adminLogin(page, '/admin/catalogos')

    await page.getByRole('button', { name: 'Nueva área' }).click()
    const areaForm = page.getByRole('dialog', { name: 'Nueva área' })
    await areaForm.getByLabel('Nombre del área').fill(area)
    await areaForm.getByRole('button', { name: 'Guardar' }).click()
    await expect(areaForm).toBeHidden()
    const areaRow = page.getByRole('table', { name: 'Áreas' }).getByRole('row').filter({ hasText: area })
    await expect(areaRow).toHaveCount(1)

    await page.getByRole('tab', { name: 'Categorías' }).click()
    await page.getByRole('button', { name: 'Nueva categoría' }).click()
    const categoryForm = page.getByRole('dialog', { name: 'Nueva categoría' })
    await categoryForm.getByLabel('Nombre de la categoría').fill(category)
    await categoryForm.getByLabel('Área por defecto').selectOption(area)
    await categoryForm.getByRole('button', { name: 'Guardar' }).click()
    await expect(categoryForm).toBeHidden()
    const categoryRow = page.getByRole('table', { name: 'Categorías' }).getByRole('row').filter({ hasText: category })
    await expect(categoryRow).toContainText(area)

    // An area that still has categories is kept (the API answers 409); once its category is gone it can be deleted.
    await page.getByRole('tab', { name: 'Áreas' }).click()
    await areaRow.getByRole('button', { name: 'Eliminar' }).click()
    // Error toasts are announced as alerts since the toast redesign (PR #16); a 'status' match was racy with the
    // still-visible "category created" toast.
    await expect(page.getByRole('alert').filter({ hasText: /tiene categor/i })).toBeVisible()
    await expect(areaRow).toHaveCount(1)

    await page.getByRole('tab', { name: 'Categorías' }).click()
    await categoryRow.getByRole('button', { name: 'Eliminar' }).click()
    await expect(categoryRow).toHaveCount(0)
    await page.getByRole('tab', { name: 'Áreas' }).click()
    await areaRow.getByRole('button', { name: 'Eliminar' }).click()
    await expect(page.getByRole('status').filter({ hasText: 'Área eliminada' })).toBeVisible()
    await expect(areaRow).toHaveCount(0)
  })

  test('creates, tries, edits, deactivates and deletes a triage rule', async ({ page }) => {
    const keyword = uniqueTag()
    await adminLogin(page, '/admin/triage')
    await page.getByRole('button', { name: 'Nueva regla' }).click()
    const form = page.getByRole('dialog', { name: 'Regla de triage' })
    await form.getByLabel('Palabras clave (separadas por comas)').fill(`${keyword}, ${keyword}x`)
    await form.getByLabel('Categoría sugerida').selectOption('Gasfitería')
    await form.getByLabel('Prioridad sugerida').selectOption('Media')
    await form.getByRole('button', { name: 'Guardar' }).click()
    await expect(form).toBeHidden()

    const rule = page.getByRole('list', { name: 'Reglas de triage' }).getByRole('listitem').filter({ hasText: `${keyword}x` })
    await expect(rule).toContainText('Gasfitería')
    await expect(rule).toContainText('Media')

    await page.getByRole('textbox', { name: 'Descripción de ejemplo' }).fill(`Hay un problema con ${keyword} en el pasillo`)
    await expect(page.getByText(/Sugeriría Gasfitería con prioridad Media/)).toBeVisible()

    await rule.getByRole('button', { name: 'Editar' }).click()
    const edit = page.getByRole('dialog', { name: 'Editar regla de triage' })
    await expect(edit.getByLabel('Palabras clave (separadas por comas)')).toHaveValue(`${keyword}, ${keyword}x`)
    await edit.getByLabel('Prioridad sugerida').selectOption('Crítica')
    await edit.getByRole('button', { name: 'Guardar cambios' }).click()
    await expect(edit).toBeHidden()
    await expect(rule).toContainText('Crítica')

    const active = rule.getByRole('switch', { name: `Regla activa: ${keyword}, ${keyword}x` })
    await expect(active).toBeChecked()
    await active.click()
    await expect(active).not.toBeChecked()

    await rule.getByRole('button', { name: 'Eliminar' }).click()
    await expect(page.getByRole('status').filter({ hasText: 'Regla eliminada' })).toBeVisible()
    await expect(rule).toHaveCount(0)
  })

  test('searches the audit log by request code', async ({ page, request }) => {
    const a = await actors(request)
    const code = await createRequest(a.requester, { description: `Se fundió el fluorescente de la biblioteca (${uniqueTag()}).` })
    await advance(a, code, 'EN_EVALUACION')

    await adminLogin(page, '/admin/auditoria')
    const audit = page.waitForResponse((r) => r.url().includes('/api/v1/admin/audit') && r.url().includes(code))
    await page.getByRole('searchbox', { name: 'Buscar' }).fill(code)
    await audit
    const rows = page.getByRole('table', { name: 'Auditoría' }).getByRole('row').filter({ hasText: code })
    await expect(rows).toHaveCount(2)
    await expect(rows.filter({ hasText: 'Registrada → En evaluación' })).toContainText(accounts.supervisor.name)
    await expect(rows.filter({ hasText: 'Solicitud creada' })).toContainText(accounts.estudiante.name)
  })

  test('changes the automatic closure period and the requester sees the new deadline', async ({ page, request, browser }) => {
    const admin = await Api.as(request, accounts.admin.email)
    const original = await admin.get<Config>('/admin/config')
    const a = await actors(request)
    const code = await createRequest(a.requester, { description: `El tablero eléctrico hace ruido extraño (${uniqueTag()}).` })
    await advance(a, code, 'RESUELTA')
    const detail = await a.requester.get<{ history: { toStatus: string; createdAt: string }[] }>(`/requests/${code}`)
    const resolvedAt = Date.parse(detail.history.findLast((h) => h.toStatus === 'RESUELTA')!.createdAt)
    // Longer than the default, so no demo request closes automatically while the test runs.
    const hours = original.autoCloseHours + 24

    const requesterPage = await (await browser.newContext()).newPage()
    try {
      await adminLogin(page, '/admin/configuracion')
      const field = page.getByRole('textbox', { name: 'Cierre automático tras resolución (horas)' })
      await expect(field).toHaveValue(String(original.autoCloseHours))
      await field.fill(String(hours))
      await page.getByRole('button', { name: 'Guardar cambios' }).click()
      await expect(page.getByRole('status').filter({ hasText: 'Configuración guardada' })).toBeVisible()
      await page.reload()
      await expect(field).toHaveValue(String(hours))

      await login(requesterPage, accounts.estudiante)
      await requesterPage.goto(`/solicitudes/${code}`)
      const card = requesterPage.getByRole('region', { name: '¿Quedó resuelto?' })
      await expect(card).toContainText(`Si no respondes antes del ${limaDateLong(resolvedAt + hours * 3_600_000)}`)
    } finally {
      await admin.put('/admin/config', original)
      await requesterPage.context().close()
    }
  })
})
