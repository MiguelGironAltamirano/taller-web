import { useNavigate } from 'react-router'
import { Button } from '../../shared/ui'
import { homePathFor, useAuth } from './context'

const COPY = {
  '403': { title: 'Sin permiso', message: 'No tienes permiso para ver esta página con tu rol actual.' },
  '404': { title: 'Página no encontrada', message: 'La página que buscas no existe o fue movida.' },
} as const

/** Páginas 403 y 404 (spec: códigos HTTP del contrato). */
export function ErrorPage({ kind }: { kind: keyof typeof COPY }) {
  const navigate = useNavigate()
  const { user } = useAuth()
  const { title, message } = COPY[kind]
  return (
    <div className="flex flex-col items-center gap-2.5 px-4 py-12 text-center">
      <div className="text-[56px] font-bold text-primary">{kind}</div>
      <h1 className="m-0 text-xl font-semibold">{title}</h1>
      <p className="m-0 max-w-sm text-muted">{message}</p>
      <Button variant="primary" onClick={() => navigate(user ? homePathFor(user.role) : '/login')}>
        Volver al inicio
      </Button>
    </div>
  )
}
