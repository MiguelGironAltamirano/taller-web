import { expect, test } from '@playwright/test'
import { accounts } from './demoUsers.ts'
import { actors, advance, catalog, createRequest, login, openTab, uniqueTag } from './support.ts'

const CODE = /SOL-\d{4}-\d{6}/

test.describe('Requester', () => {
  test('registers a request applying the triage suggestion and finds it in "Mis solicitudes"', async ({ page }) => {
    const tag = uniqueTag()
    await login(page, accounts.estudiante)
    await page.getByRole('link', { name: 'Nueva solicitud' }).click()
    await expect(page.getByRole('heading', { name: 'Nueva solicitud', level: 1 })).toBeVisible()

    // "foco" and "luz" match the demo rule Electricidad / Alta.
    await page.getByLabel('Descripción *').fill(`Se quemó el foco del aula y no hay luz desde ayer (${tag}).`)
    const suggestion = page.getByRole('status').filter({ hasText: 'Sugerimos categoría' })
    await expect(suggestion).toContainText('Electricidad')
    await expect(suggestion).toContainText('Alta')
    await suggestion.getByRole('button', { name: 'Aceptar' }).click()
    await expect(suggestion).toBeHidden()
    await expect(page.getByLabel('Categoría *')).toHaveValue('Electricidad')
    await expect(page.getByRole('radio', { name: /^Alta/ })).toHaveAttribute('aria-checked', 'true')
    await expect(page.getByText('Área responsable: Mantenimiento')).toBeVisible()

    await page.getByLabel('Facultad *').selectOption('Facultad de Ingeniería de Sistemas e Informática')
    await page.getByLabel('Pabellón *').selectOption('Pabellón Principal')
    await page.getByLabel('Ambiente *').selectOption('Aula 101')
    await page.getByRole('button', { name: 'Registrar solicitud' }).click()

    await expect(page.getByRole('heading', { name: 'Solicitud registrada' })).toBeVisible()
    const code = (await page.getByText(CODE).first().textContent())!.trim()
    await page.getByRole('button', { name: 'Ver solicitud' }).click()
    await expect(page).toHaveURL(new RegExp(`/solicitudes/${code}$`))
    await expect(page.getByRole('heading', { name: code, level: 1 })).toBeVisible()
    const detail = page.getByRole('region', { name: 'Detalle' })
    await expect(detail).toContainText('Registrada')
    await expect(detail).toContainText('Alta · 24 h')
    await expect(detail).toContainText('Mantenimiento')

    await page.getByRole('link', { name: /^Mis solicitudes/ }).click()
    await page.getByRole('searchbox', { name: 'Buscar' }).fill(tag)
    const rows = page.getByRole('table', { name: 'Solicitudes' }).getByRole('row').filter({ hasText: CODE })
    await expect(rows).toHaveCount(1)
    await expect(rows.first()).toContainText(code)
    await expect(page.getByText(/^1 de \d+ solicitudes$/)).toBeVisible()
  })

  test('filters, searches and paginates "Mis solicitudes" on the server', async ({ page }) => {
    // Read-only journey on the docente demo account (15 requests, no spec creates requests for it).
    await login(page, accounts.docente)
    await page.goto('/solicitudes')
    const table = page.getByRole('table', { name: 'Solicitudes' })
    const dataRows = table.getByRole('row').filter({ hasText: CODE })
    await expect(page.getByText(/^1–10 de \d+$/)).toBeVisible()
    await expect(dataRows).toHaveCount(10)
    const firstOfPageOne = await dataRows.first().textContent()

    const nextPage = page.waitForRequest((r) => r.url().includes('/api/v1/requests?') && r.url().includes('page=1'))
    await page.getByRole('button', { name: 'Siguiente' }).click()
    await nextPage
    await expect(page.getByText(/^11–\d+ de \d+$/)).toBeVisible()
    await expect(dataRows.first()).not.toHaveText(firstOfPageOne!)
    await expect(page.getByRole('button', { name: 'Siguiente' })).toBeDisabled()

    // Status filter from the strip: only closed requests, sent to the server as a query parameter.
    const closed = page.waitForRequest((r) => r.url().includes('status=CERRADA'))
    await page.getByRole('button', { name: /^Cerrada \d+$/ }).click()
    await closed
    await expect(page.getByRole('button', { name: 'Quitar filtro Estado: Cerrada' })).toBeVisible()
    await expect(dataRows.first()).toBeVisible()
    for (const row of await dataRows.all()) await expect(row).toContainText('Cerrada')

    // Priority filter on top of it.
    const priority = page.waitForRequest((r) => r.url().includes(`priorityId=${catalog.priority.media}`))
    await page.getByRole('combobox', { name: 'Prioridad' }).selectOption('Media')
    await priority
    await expect(dataRows.first()).toBeVisible()
    for (const row of await dataRows.all()) await expect(row).toContainText('Media')

    // Free-text search by code, after clearing the filters.
    await page.getByRole('button', { name: 'Limpiar filtros' }).first().click()
    const someCode = (await dataRows.first().textContent())!.match(CODE)![0]
    const search = page.waitForRequest((r) => r.url().includes(`q=${someCode}`))
    await page.getByRole('searchbox', { name: 'Buscar' }).fill(someCode)
    await search
    await expect(dataRows).toHaveCount(1)
    await expect(dataRows.first()).toContainText(someCode)
  })

  test('opens the detail, reads the history and adds a comment', async ({ page, request }) => {
    const a = await actors(request)
    const code = await createRequest(a.requester, { description: `El tomacorriente de la pared chispea al conectar (${uniqueTag()}).` })
    await advance(a, code, 'EN_EVALUACION')

    await login(page, accounts.estudiante)
    await page.goto(`/solicitudes/${code}`)
    await expect(page.getByRole('heading', { name: code, level: 1 })).toBeVisible()

    await openTab(page, 'Historial')
    const panel = page.getByRole('tabpanel')
    await expect(panel).toContainText('Registrada → En evaluación')
    await expect(panel).toContainText('Solicitud creada')

    await openTab(page, 'Comentarios')
    await expect(panel).toContainText('Aún no hay comentarios.')
    const text = `¿Pueden venir antes de las 10:00? ${uniqueTag()}`
    await page.getByRole('textbox', { name: 'Escribe un comentario' }).fill(text)
    await page.getByRole('button', { name: 'Enviar' }).click()
    await expect(panel).toContainText(text)
    await expect(panel).toContainText(accounts.estudiante.name)
    await expect(page.getByRole('tab', { name: /^Comentarios/ })).toContainText('1')
    await expect(page.getByRole('textbox', { name: 'Escribe un comentario' })).toHaveValue('')
  })

  test('confirms a resolved request and rates the attention', async ({ page, request }) => {
    const a = await actors(request)
    const code = await createRequest(a.requester, { description: `No funciona el interruptor de la luz del aula (${uniqueTag()}).` })
    const solution = 'Se cambió el interruptor y se probó el circuito.'
    await advance(a, code, 'RESUELTA', solution)

    await login(page, accounts.estudiante)
    await page.goto(`/solicitudes/${code}`)
    const card = page.getByRole('region', { name: '¿Quedó resuelto?' })
    await expect(card).toContainText(solution)
    await card.getByRole('radio', { name: '4 estrellas' }).click()
    await expect(card.getByRole('radio', { name: '4 estrellas' })).toHaveAttribute('aria-checked', 'true')
    await card.getByRole('textbox', { name: 'Comentario' }).fill('Atención rápida, gracias.')
    await card.getByRole('button', { name: 'Confirmar y cerrar' }).click()

    await expect(page.getByRole('status').filter({ hasText: 'Gracias por tu calificación' })).toBeVisible()
    await expect(card).toBeHidden()
    await expect(page.getByRole('region', { name: 'Detalle' })).toContainText('Cerrada')

    const detail = await a.requester.get<{ status: string; rating: { score: number; comment: string } | null }>(`/requests/${code}`)
    expect(detail.status).toBe('CERRADA')
    expect(detail.rating).toMatchObject({ score: 4, comment: 'Atención rápida, gracias.' })
  })

  test('rejects the proposed solution and the request goes back to "En atención"', async ({ page, request }) => {
    const a = await actors(request)
    const code = await createRequest(a.requester, { description: `Sigue goteando el caño del baño del segundo piso (${uniqueTag()}).`, categoryId: catalog.category.gasfiteria })
    await advance(a, code, 'RESUELTA', 'Se ajustó la llave de paso.')

    await login(page, accounts.estudiante)
    await page.goto(`/solicitudes/${code}`)
    await page.getByRole('button', { name: 'Rechazar solución' }).click()
    const dialog = page.getByRole('dialog', { name: 'Rechazar solución' })
    await dialog.getByRole('textbox', { name: 'Motivo' }).fill('Gotea')
    await dialog.getByRole('button', { name: 'Rechazar solución' }).click()
    await expect(dialog.getByRole('alert')).toContainText('al menos 10 caracteres')

    await dialog.getByRole('textbox', { name: 'Motivo' }).fill('El caño sigue goteando por la noche.')
    await dialog.getByRole('button', { name: 'Rechazar solución' }).click()
    await expect(dialog).toBeHidden()
    await expect(page.getByRole('status').filter({ hasText: 'Solución rechazada' })).toBeVisible()
    await expect(page.getByRole('region', { name: '¿Quedó resuelto?' })).toBeHidden()
    await expect(page.getByRole('region', { name: 'Detalle' })).toContainText('En atención')

    await openTab(page, 'Historial')
    await expect(page.getByRole('tabpanel')).toContainText('Resuelta → En atención')
    await expect(page.getByRole('tabpanel')).toContainText('El caño sigue goteando por la noche.')
  })
})
