import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { RouteError } from './RouteError'

function Boom({ error }: { error: Error }): never {
  throw error
}

function renderFailing(error: Error) {
  const router = createMemoryRouter([{ path: '/', element: <Boom error={error} />, errorElement: <RouteError /> }])
  render(<RouterProvider router={router} />)
}

describe('RouteError', () => {
  // React and the router log the thrown error; the test only checks what the person sees.
  beforeEach(() => vi.spyOn(console, 'error').mockImplementation(() => {}))
  afterEach(() => vi.restoreAllMocks())

  it('explains an unexpected failure without technical details and offers a way out', () => {
    renderFailing(new TypeError("Cannot read properties of undefined (reading 'name')"))

    expect(screen.getByRole('heading', { name: 'Algo salió mal en esta pantalla' })).toBeInTheDocument()
    expect(screen.getByText(/Tus datos están a salvo/)).toBeInTheDocument()
    expect(screen.queryByText(/Cannot read/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Recargar la página' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir al inicio' })).toHaveAttribute('href', '/')
  })

  it('asks to reload when a new version was published and a screen can no longer load', () => {
    renderFailing(new TypeError('Failed to fetch dynamically imported module: /assets/DashboardPage-abc123.js'))

    expect(screen.getByRole('heading', { name: 'Hay una versión nueva de la aplicación' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Recargar la página' })).toBeInTheDocument()
  })
})
