import { describe, expect, it } from 'vitest'
import html from '../index.html?raw'
import nginx from '../nginx.conf?raw'

// La CSP de nginx (`default-src 'self'`) bloquea lo que no sea del mismo origen y todo script o estilo en línea.
describe('index.html respects the Content-Security-Policy', () => {
  it('has no inline script', () => {
    const scripts = [...html.matchAll(/<script\b([^>]*)>/g)].map((m) => m[1])
    expect(scripts.length).toBeGreaterThan(0)
    for (const attrs of scripts) expect(attrs).toMatch(/\bsrc="/)
  })

  it('loads nothing from another origin (fonts are bundled, not taken from Google)', () => {
    expect(html).not.toMatch(/https?:\/\//)
  })

  it('keeps the policy closed: no unsafe-inline, no external hosts', () => {
    const csp = /Content-Security-Policy "([^"]+)"/.exec(nginx)?.[1] ?? ''
    expect(csp).toContain("default-src 'self'")
    expect(csp).not.toMatch(/unsafe-inline|unsafe-eval|https?:/)
  })
})
