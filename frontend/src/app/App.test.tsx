import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../test/msw-server'
import { App } from './App'

function renderApp() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <App />
    </QueryClientProvider>,
  )
}

describe('App', () => {
  it('shows the API version when the backend responds', async () => {
    server.use(http.get('*/api/v1/system/info', () => HttpResponse.json({ name: 'servicios', version: '0.0.1-SNAPSHOT' })))
    renderApp()
    expect(screen.getByRole('heading', { name: 'Servicios Universitarios' })).toBeInTheDocument()
    expect(await screen.findByText('Versión 0.0.1-SNAPSHOT')).toBeInTheDocument()
  })

  it('shows an unavailable message when the backend fails', async () => {
    server.use(http.get('*/api/v1/system/info', () => new HttpResponse(null, { status: 503 })))
    renderApp()
    expect(await screen.findByText('API no disponible')).toBeInTheDocument()
  })
})
