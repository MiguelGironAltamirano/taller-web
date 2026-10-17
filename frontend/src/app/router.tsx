import { createBrowserRouter, type RouteObject } from 'react-router'
import { ErrorPage, LoginPage, PublicOnly, RegisterPage, RequireAuth, RequireRole, RoleHome } from '../features/auth'
import { AuditPage, CatalogsPage, ConfigPage, UsersPage } from '../features/admin'
import { DashboardPage, ReportsPage, TeamPage } from '../features/dashboard'
import { NotificationsPage } from '../features/notifications'
import { CalendarPage, HomePage, NewRequestPage, RequestDetailPage, RequestListPage } from '../features/requests'
import { RulesPage } from '../features/triage'
import { AppLayout } from './layout/AppLayout'

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
            children: [{ path: '/dashboard', element: <DashboardPage /> }],
          },
          {
            element: <RequireRole roles={['supervisor']} />,
            children: [
              { path: '/equipo', element: <TeamPage /> },
              { path: '/reportes', element: <ReportsPage /> },
            ],
          },
          {
            element: <RequireRole roles={['administrador']} />,
            children: [
              { path: '/admin/usuarios', element: <UsersPage /> },
              { path: '/admin/catalogos', element: <CatalogsPage /> },
              { path: '/admin/triage', element: <RulesPage /> },
              { path: '/admin/auditoria', element: <AuditPage /> },
              { path: '/admin/configuracion', element: <ConfigPage /> },
            ],
          },
          { path: '*', element: <ErrorPage kind="404" /> },
        ],
      },
    ],
  },
]

export const createAppRouter = () => createBrowserRouter(appRoutes)
