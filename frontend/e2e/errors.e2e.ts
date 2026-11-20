import { expect, test } from '@playwright/test'
import { accounts } from './demoUsers.ts'
import { login } from './support.ts'

/**
 * Clear error messages (guía de mensajes de error): when the server or the connection fails, the screen says what
 * happened and what to do, in the style of the app, and lets the person retry.
 */
test.describe('Error messages', () => {
  test('a screen whose data fails to load explains it and recovers with "Reintentar"', async ({ page }) => {
    await login(page, accounts.estudiante)

    // The backend is down for this call: nginx answers like the real proxy does.
    let down = true
    await page.route('**/api/v1/notifications', (route) =>
      down
        ? route.fulfill({
            status: 503,
            contentType: 'application/problem+json',
            body: JSON.stringify({
              type: 'about:blank',
              title: 'Service Unavailable',
              status: 503,
              detail: 'El servicio no está disponible en este momento. Inténtalo de nuevo en unos minutos',
            }),
          })
        : route.continue(),
    )
    await page.goto('/notificaciones')

    const alert = page.getByRole('main').getByRole('alert')
    await expect(alert).toContainText('No pudimos cargar tus notificaciones')
    await expect(alert).toContainText('El servicio no está disponible en este momento.')
    await expect(page.getByText('No tienes notificaciones.')).toBeHidden()
    await expect(page.getByText(/Service Unavailable|503/)).toBeHidden()

    down = false
    await alert.getByRole('button', { name: 'Reintentar' }).click()
    await expect(alert).toBeHidden()
    await expect(page.getByRole('heading', { name: 'Notificaciones', level: 1 })).toBeVisible()
  })

  test('without connection a failed action says so instead of a technical error', async ({ page, context }) => {
    await login(page, accounts.estudiante)
    await page.goto('/solicitudes')
    await page.getByRole('table', { name: 'Solicitudes' }).getByRole('row').nth(1).click()
    await page.getByRole('tab', { name: /^Comentarios/ }).click()

    await context.setOffline(true)
    try {
      await page.getByRole('textbox', { name: 'Escribe un comentario' }).fill('¿Hay novedades?')
      await page.getByRole('button', { name: 'Enviar' }).click()
      await expect(page.getByRole('alert').filter({ hasText: 'No pudimos conectar con el servidor' })).toBeVisible()
      await expect(page.getByText(/Failed to fetch|NetworkError/)).toBeHidden()
    } finally {
      await context.setOffline(false)
    }
  })

  test('an unknown API route answers in Spanish, never with the framework text', async ({ page, request }) => {
    await login(page, accounts.estudiante)
    const response = await request.get('/api/v1/no-existe')
    expect(response.status()).toBe(401)
    expect((await response.json()).detail).toBe('Tu sesión no es válida o expiró. Vuelve a iniciar sesión')
  })
})
