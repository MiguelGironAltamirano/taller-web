import type { ReactNode } from 'react'

/** Marco de las pantallas públicas: panel institucional a la izquierda (oculto en móvil) y formulario a la derecha. */
export function AuthShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-wrap bg-surface">
      <aside className="hidden min-h-[260px] flex-[1_1_360px] flex-col justify-between bg-[#8b1a1a] p-10 text-white md:flex">
        <div className="flex items-center gap-2.5">
          <div className="grid size-9 place-items-center rounded-lg bg-white font-bold text-[#8b1a1a]">U</div>
          <div className="font-semibold">UNMSM</div>
        </div>
        <div>
          <div className="max-w-[420px] text-[32px] font-bold leading-[1.15]">Servicios Universitarios</div>
          <p className="m-0 mt-3 max-w-[420px] text-base leading-normal text-[#f6dcdc]">
            Registra, sigue y resuelve solicitudes de mantenimiento, soporte informático y trámites administrativos.
          </p>
        </div>
        <div className="text-[13px] text-[#f6dcdc]">Universidad Nacional Mayor de San Marcos</div>
      </aside>
      <main className="flex flex-[1_1_380px] items-center justify-center overflow-auto bg-surface p-6">{children}</main>
    </div>
  )
}
