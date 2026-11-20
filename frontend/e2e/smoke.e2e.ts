import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { DEMO_PASSWORD, demoUsers } from './demoUsers.ts'

for (const [role, user] of Object.entries(demoUsers)) {
  test(`${role} logs in and lands on ${user.home}`, async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible()

    await page.getByLabel('Correo').fill(user.email)
    await page.getByLabel('Contraseña').fill(DEMO_PASSWORD)
    await page.getByRole('button', { name: 'Ingresar', exact: true }).click()

    await expect(page).toHaveURL(new RegExp(`${user.home}$`))
    await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeHidden()
  })
}

test('login page has no WCAG 2.1 AA accessibility violations', async ({ page }, testInfo) => {
  await page.goto('/login')
  await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible()

  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze()
  await testInfo.attach('axe-results', { body: JSON.stringify(results.violations, null, 2), contentType: 'application/json' })

  const summary = results.violations.map((v) => `${v.id} (${v.impact}): ${v.nodes.length} node(s) - ${v.help}`)
  expect(summary, summary.join('\n')).toEqual([])
})
