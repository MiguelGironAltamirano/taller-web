import { expect, test } from '@playwright/test'
import { accounts } from './demoUsers.ts'
import { Api, login, uniqueTag } from './support.ts'

test.describe('Fingerprint or passkey', () => {
  test('registers a passkey from the profile and logs in with it without a password', async ({ page, context, request }) => {
    // Chromium's virtual authenticator plays the fingerprint sensor: it keeps the passkey and verifies the user.
    const cdp = await context.newCDPSession(page)
    await cdp.send('WebAuthn.enable')
    await cdp.send('WebAuthn.addVirtualAuthenticator', {
      options: {
        protocol: 'ctap2',
        transport: 'internal',
        hasResidentKey: true,
        hasUserVerification: true,
        isUserVerified: true,
        automaticPresenceSimulation: true,
      },
    })

    // A dedicated account, so the shared demo users never get a passkey.
    const admin = await Api.as(request, accounts.admin.email)
    const email = `${uniqueTag()}@unmsm.edu.pe`
    const created = await admin.post<{ temporaryPassword: string }>('/admin/users', {
      fullName: 'Solicitante Passkey E2E',
      email,
      role: 'SOLICITANTE',
      areaId: null,
    })
    await login(page, { email, home: '/inicio' }, created.temporaryPassword)

    await page.getByRole('button', { name: 'Menú de perfil' }).click()
    await page.getByRole('menu', { name: 'Menú de perfil' }).getByRole('menuitem', { name: 'Huella y passkeys' }).click()
    const dialog = page.getByRole('dialog', { name: 'Huella y passkeys' })
    await expect(dialog.getByText('Aún no registraste ninguna.')).toBeVisible()
    await dialog.getByLabel('Contraseña actual').fill(created.temporaryPassword)
    await dialog.getByLabel('Nombre (opcional)').fill('Laptop E2E')
    await dialog.getByRole('button', { name: 'Agregar huella o passkey' }).click()
    const registered = dialog.getByRole('list', { name: 'Passkeys registradas' })
    await expect(registered).toContainText('Laptop E2E')
    await expect(registered).toContainText('Sin usar todavía')
    await dialog.getByRole('button', { name: 'Cerrar' }).click()

    await page.getByRole('button', { name: 'Menú de perfil' }).click()
    await page.getByRole('menu', { name: 'Menú de perfil' }).getByRole('menuitem', { name: 'Cerrar sesión' }).click()
    await expect(page).toHaveURL(/\/login$/)

    await page.getByRole('button', { name: 'Ingresar con huella o passkey' }).click()
    await expect(page).toHaveURL(/\/inicio$/)
    await expect(page.getByRole('heading', { name: 'Hola, Solicitante' })).toBeVisible()
  })
})
