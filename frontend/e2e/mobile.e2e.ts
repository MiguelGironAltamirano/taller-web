import { expect, test, type Page } from '@playwright/test'
import { accounts } from './demoUsers.ts'
import { login } from './support.ts'

// A phone-sized screen (docs/calidad/guia-accesibilidad.md §7: «pruébalo a 375 px»).
test.use({ viewport: { width: 375, height: 667 }, hasTouch: true, isMobile: true })

/** The page itself must never scroll sideways: only wide tables scroll inside their card. */
async function expectNoHorizontalScroll(page: Page) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(overflow).toBeLessThanOrEqual(0)
}

test.describe('Mobile layout', () => {
  test('the requester gets the bottom navigation and no sideways scroll on the key pages', async ({ page }) => {
    await login(page, accounts.estudiante)

    const bottomNav = page.getByRole('navigation', { name: 'Navegación inferior' })
    await expect(bottomNav).toBeVisible()
    await expectNoHorizontalScroll(page)

    for (const { link, heading } of [
      { link: 'Solicitudes', heading: 'Mis solicitudes' },
      { link: 'Nueva', heading: 'Nueva solicitud' },
    ]) {
      await bottomNav.getByRole('link', { name: link }).click()
      await expect(page.getByRole('heading', { level: 1, name: heading })).toBeVisible()
      await expectNoHorizontalScroll(page)
    }
  })

  test('the bell opens a quick panel that fits the phone and leads to the full page', async ({ page }) => {
    await login(page, accounts.estudiante)
    const bottomNav = page.getByRole('navigation', { name: 'Navegación inferior' })

    // One way in to each destination: no «Avisos» tab and no hamburger, the menu opens from the bottom bar.
    await expect(bottomNav.getByRole('link', { name: 'Avisos' })).toHaveCount(0)
    await expect(page.getByRole('button', { name: 'Abrir menú' })).toHaveCount(0)

    const bell = page.getByRole('button', { name: /^Notificaciones/ })
    await expect(bell).toHaveAttribute('aria-expanded', 'false')
    await bell.click()
    await expect(bell).toHaveAttribute('aria-expanded', 'true')
    const panel = page.getByRole('dialog', { name: 'Notificaciones' })
    await expect(panel).toBeVisible()
    const box = await panel.boundingBox()
    expect(box!.x).toBeGreaterThanOrEqual(0)
    expect(box!.x + box!.width).toBeLessThanOrEqual(375)

    // Escape closes it and the focus goes back to the bell.
    await page.keyboard.press('Escape')
    await expect(panel).toBeHidden()
    await expect(bell).toBeFocused()

    await bell.click()
    await panel.getByRole('button', { name: /^Ver todas/ }).click()
    await expect(page.getByRole('heading', { level: 1, name: 'Notificaciones' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Volver' })).toBeVisible()
    await expect(bottomNav).toBeVisible()
    await expectNoHorizontalScroll(page)
  })

  test('the Menú tab opens only the account actions', async ({ page }) => {
    await login(page, accounts.estudiante)
    await page.getByRole('navigation', { name: 'Navegación inferior' }).getByRole('button', { name: 'Menú' }).click()
    const drawer = page.getByRole('complementary', { name: 'Menú' })
    for (const action of ['Cambiar contraseña', 'Huella y passkeys', 'Modo oscuro', 'Cerrar sesión']) {
      await expect(drawer.getByRole('button', { name: action })).toBeVisible()
    }
    await expect(drawer.getByRole('link')).toHaveCount(0)
  })

  test('the login page fits the phone and offers the passkey option', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('button', { name: 'Ingresar con huella o passkey' })).toBeVisible()
    await expectNoHorizontalScroll(page)
  })
})
