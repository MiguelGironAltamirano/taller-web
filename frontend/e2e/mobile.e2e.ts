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
      { link: 'Avisos', heading: 'Notificaciones' },
    ]) {
      await bottomNav.getByRole('link', { name: link }).click()
      await expect(page.getByRole('heading', { level: 1, name: heading })).toBeVisible()
      await expectNoHorizontalScroll(page)
    }
  })

  test('the login page fits the phone and offers the passkey option', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('button', { name: 'Ingresar con huella o passkey' })).toBeVisible()
    await expectNoHorizontalScroll(page)
  })
})
