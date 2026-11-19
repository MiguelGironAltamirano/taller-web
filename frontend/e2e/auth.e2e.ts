import { expect, test } from '@playwright/test'
import { accounts } from './demoUsers.ts'
import { Api, login, uniqueTag } from './support.ts'

test.describe('Authentication', () => {
  test('logs out from the profile menu and the session cannot be restored', async ({ page }) => {
    await login(page, accounts.docente)
    await page.getByRole('button', { name: 'Menú de perfil' }).click()
    await page.getByRole('menu', { name: 'Menú de perfil' }).getByRole('menuitem', { name: 'Cerrar sesión' }).click()
    await expect(page).toHaveURL(/\/login$/)

    // The refresh cookie was revoked: reloading a private page goes back to the login form.
    await page.goto('/solicitudes')
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible()
  })

  test('changes the password of a fresh account and logs in with the new one', async ({ page, request }) => {
    // A dedicated account created by the administrator, so no shared demo password ever changes.
    const admin = await Api.as(request, accounts.admin.email)
    const email = `${uniqueTag()}@unmsm.edu.pe`
    const created = await admin.post<{ temporaryPassword: string }>('/admin/users', {
      fullName: 'Solicitante Contraseña E2E',
      email,
      role: 'SOLICITANTE',
      areaId: null,
    })
    const next = `Nueva${uniqueTag()}9`

    await login(page, { email, home: '/inicio' }, created.temporaryPassword)
    await page.getByRole('button', { name: 'Menú de perfil' }).click()
    await page.getByRole('menuitem', { name: 'Cambiar contraseña' }).click()
    const dialog = page.getByRole('dialog', { name: 'Cambiar contraseña' })
    await dialog.getByLabel('Contraseña actual').fill(created.temporaryPassword)
    await dialog.getByLabel('Nueva contraseña', { exact: true }).fill(next)
    await dialog.getByLabel('Confirmar nueva contraseña').fill(`${next}x`)
    await dialog.getByRole('button', { name: 'Guardar' }).click()
    await expect(dialog.getByText('Las contraseñas no coinciden.')).toBeVisible()

    await dialog.getByLabel('Confirmar nueva contraseña').fill(next)
    await dialog.getByRole('button', { name: 'Guardar' }).click()
    await expect(page.getByRole('status').filter({ hasText: 'Contraseña actualizada' })).toBeVisible()
    await expect(dialog).toBeHidden()

    await page.getByRole('button', { name: 'Menú de perfil' }).click()
    await page.getByRole('menuitem', { name: 'Cerrar sesión' }).click()
    await expect(page).toHaveURL(/\/login$/)

    // The temporary password no longer works; the new one does.
    await page.getByLabel('Correo').fill(email)
    await page.getByLabel('Contraseña').fill(created.temporaryPassword)
    await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
    await expect(page.getByRole('alert')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)

    await login(page, { email, home: '/inicio' }, next)
  })
})
