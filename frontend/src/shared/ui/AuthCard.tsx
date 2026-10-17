import type { ReactNode } from 'react'

export function AuthCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <main className="grid min-h-screen place-items-center bg-slate-50 px-4 py-8">
      <div className="w-full max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-sm font-medium text-slate-500">Servicios Universitarios</p>
        <h1 className="mb-4 text-2xl font-bold text-slate-900">{title}</h1>
        {children}
      </div>
    </main>
  )
}

export const inputClass =
  'mt-1 w-full rounded border border-slate-300 px-3 py-2 text-slate-900 focus:border-slate-600 focus:outline-none'
export const buttonClass =
  'w-full rounded bg-slate-900 px-4 py-2 font-medium text-white hover:bg-slate-700 disabled:opacity-60'

export function FieldError({ message }: { message?: string }) {
  return message ? (
    <p role="alert" className="mt-1 text-sm text-red-700">
      {message}
    </p>
  ) : null
}
