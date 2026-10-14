import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../shared/api/client'

type SystemInfo = { name: string; version: string }

export function App() {
  const info = useQuery({
    queryKey: ['system-info'],
    queryFn: () => apiGet<SystemInfo>('/system/info'),
  })

  let status = 'Conectando…'
  if (info.isSuccess) status = `Versión ${info.data.version}`
  if (info.isError) status = 'API no disponible'

  return (
    <main className="grid min-h-screen place-items-center bg-slate-50">
      <div className="text-center">
        <h1 className="text-3xl font-bold text-slate-900">Servicios Universitarios</h1>
        <p className="mt-2 text-slate-600">{status}</p>
      </div>
    </main>
  )
}
