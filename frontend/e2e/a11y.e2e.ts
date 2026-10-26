import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page, type TestInfo } from '@playwright/test'
import { accounts, type Account } from './demoUsers.ts'
import { login } from './support.ts'

const WCAG_21_AA = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']

/** Key pages per role, each with the heading that proves its data finished loading. */
const PAGES: { role: string; account: Account; pages: { path: string; heading: string | RegExp }[] }[] = [
  {
    role: 'requester',
    account: accounts.estudiante,
    pages: [
      { path: '/inicio', heading: /^Hola/ },
      { path: '/solicitudes', heading: 'Mis solicitudes' },
      { path: '/solicitudes/nueva', heading: 'Nueva solicitud' },
      { path: '/notificaciones', heading: 'Notificaciones' },
    ],
  },
  {
    role: 'supervisor',
    account: accounts.supervisor,
    pages: [
      { path: '/dashboard', heading: /^Dashboard/ },
      { path: '/solicitudes', heading: 'Bandeja del área' },
      { path: '/equipo', heading: 'Equipo' },
      { path: '/reportes', heading: 'Reportes' },
    ],
  },
  {
    role: 'technician',
    account: accounts.tecnico,
    pages: [
      { path: '/solicitudes', heading: 'Mi bandeja' },
      { path: '/calendario', heading: /^Calendario/ },
    ],
  },
  {
    role: 'administrator',
    account: accounts.admin,
    pages: [
      { path: '/dashboard', heading: 'Dashboard global' },
      { path: '/admin/usuarios', heading: 'Usuarios' },
      { path: '/admin/catalogos', heading: 'Catálogos' },
      { path: '/admin/triage', heading: 'Reglas de triage' },
      { path: '/admin/auditoria', heading: 'Auditoría' },
      { path: '/admin/configuracion', heading: 'Configuración' },
    ],
  },
]

async function scan(page: Page, testInfo: TestInfo, name: string): Promise<string[]> {
  const results = await new AxeBuilder({ page }).withTags(WCAG_21_AA).analyze()
  await testInfo.attach(`axe ${name}`, { body: JSON.stringify(results.violations, null, 2), contentType: 'application/json' })
  return results.violations.map((v) => `${name}: ${v.id} (${v.impact}) ${v.nodes.length} node(s) - ${v.help}`)
}

for (const { role, account, pages } of PAGES) {
  test(`${role} key pages have no WCAG 2.1 A/AA violations`, async ({ page }, testInfo) => {
    await login(page, account)
    const violations: string[] = []
    for (const { path, heading } of pages) {
      await page.goto(path)
      await expect(page.getByRole('heading', { name: heading, level: 1 })).toBeVisible()
      // Skeletons announce themselves with aria-busy / role="status"; wait until the page settled.
      await expect(page.locator('[aria-busy="true"]')).toHaveCount(0)
      violations.push(...(await scan(page, testInfo, path)))
    }
    expect(violations, violations.join('\n')).toEqual([])
  })
}

test('request detail has no WCAG 2.1 A/AA violations', async ({ page }, testInfo) => {
  await login(page, accounts.estudiante)
  await page.goto('/solicitudes')
  const first = page.getByRole('table', { name: 'Solicitudes' }).getByRole('row').filter({ hasText: /SOL-\d{4}-\d{6}/ }).first()
  await first.click()
  await expect(page.getByRole('heading', { level: 1, name: /SOL-\d{4}-\d{6}/ })).toBeVisible()
  const violations = await scan(page, testInfo, 'detail')
  await page.getByRole('tab', { name: /^Historial/ }).click()
  violations.push(...(await scan(page, testInfo, 'detail/historial')))
  expect(violations, violations.join('\n')).toEqual([])
})
