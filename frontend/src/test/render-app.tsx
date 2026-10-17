import { QueryClient } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { AppProviders } from '../app/AppProviders'
import { appRoutes } from '../app/router'

/** Renderiza la aplicación completa (proveedores + rutas) iniciando en `path`. */
export function renderApp(path = '/') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] })
  return render(
    <AppProviders queryClient={client}>
      <RouterProvider router={router} />
    </AppProviders>,
  )
}
