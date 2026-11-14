import type { ComponentType } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router'
import { ErrorPage, LoginPage, PublicOnly, RegisterPage, RequireAuth, RequireRole, RoleHome } from '../features/auth'
import { NotificationsPage } from '../features/notifications'
import { CalendarPage, HomePage, NewRequestPage, RequestDetailPage, RequestListPage } from '../features/requests'
import { RulesPage } from '../features/triage'
import { AppLayout } from './layout/AppLayout'

/**
 * Carga la pantalla al entrar a su ruta (React Router `lazy`), así el paquete inicial no arrastra
 * las pantallas que el rol no abre (p. ej. los gráficos de Recharts del dashboard). Las de `requests`, notificaciones y
 * triage siguen cargándose junto al layout, que ya importa esos módulos.
 */
const lazyPage = (load: () => Promise<ComponentType>): Pick<RouteObject, 'lazy'> => ({
  lazy: async () => ({ Component: await load() }),
})

/**
 * Rutas de la aplicación. El acceso por rol se aplica con `RequireRole`; el servidor volverá a
 * validar cada petición (la interfaz solo oculta lo que el rol no puede usar).
 */
export const appRoutes: RouteObject[] = [
  {
    element: <PublicOnly />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/registro', element: <RegisterPage /> },
    ],
  },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: '/', element: <RoleHome /> },
          { path: '/solicitudes', element: <RequestListPage /> },
          { path: '/solicitudes/:code', element: <RequestDetailPage /> },
          { path: '/notificaciones', element: <NotificationsPage /> },
          {
            element: <RequireRole roles={['solicitante']} />,
            children: [
              { path: '/inicio', element: <HomePage /> },
              { path: '/solicitudes/nueva', element: <NewRequestPage /> },
            ],
          },
          { element: <RequireRole roles={['tecnico']} />, children: [{ path: '/calendario', element: <CalendarPage /> }] },
          {
            element: <RequireRole roles={['supervisor', 'administrador']} />,
            children: [{ path: '/dashboard', ...lazyPage(() => import('../features/dashboard/DashboardPage').then((m) => m.DashboardPage)) }],
          },
          {
            element: <RequireRole roles={['supervisor']} />,
            children: [
              { path: '/equipo', ...lazyPage(() => import('../features/dashboard/TeamPage').then((m) => m.TeamPage)) },
              { path: '/reportes', ...lazyPage(() => import('../features/dashboard/ReportsPage').then((m) => m.ReportsPage)) },
            ],
          },
          {
            element: <RequireRole roles={['administrador']} />,
            children: [
              { path: '/admin/usuarios', ...lazyPage(() => import('../features/admin/UsersPage').then((m) => m.UsersPage)) },
              { path: '/admin/catalogos', ...lazyPage(() => import('../features/admin/CatalogsPage').then((m) => m.CatalogsPage)) },
              { path: '/admin/triage', element: <RulesPage /> },
              { path: '/admin/auditoria', ...lazyPage(() => import('../features/admin/AuditPage').then((m) => m.AuditPage)) },
              { path: '/admin/configuracion', ...lazyPage(() => import('../features/admin/ConfigPage').then((m) => m.ConfigPage)) },
            ],
          },
          { path: '*', element: <ErrorPage kind="404" /> },
        ],
      },
    ],
  },
]

export const createAppRouter = () => createBrowserRouter(appRoutes)
