import { Route, Routes } from 'react-router'
import { LoginPage } from '../features/auth/LoginPage'
import { HomeRedirect, ProtectedRoute } from '../features/auth/ProtectedRoute'
import { RegisterPage } from '../features/auth/RegisterPage'
import { InboxPage } from '../features/inbox/InboxPage'
import { RequesterHomePage } from '../features/requests/RequesterHomePage'
import { AppLayout } from './AppLayout'

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
      <Route path="/" element={<HomeRedirect />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route element={<ProtectedRoute roles={['SOLICITANTE']} />}>
            <Route path="/solicitudes" element={<RequesterHomePage />} />
          </Route>
          <Route element={<ProtectedRoute roles={['SUPERVISOR', 'TECNICO', 'ADMINISTRADOR']} />}>
            <Route path="/bandeja" element={<InboxPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<HomeRedirect />} />
    </Routes>
  )
}
