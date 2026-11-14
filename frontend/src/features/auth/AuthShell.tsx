import type { ReactNode } from 'react'
import { BrandLogo } from '../../shared/ui'

/** Marco de las pantallas públicas: panel con el escudo de la universidad (escritorio) y formulario. */
export function AuthShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen bg-bg">
      <aside className="relative hidden w-[min(46%,640px)] shrink-0 overflow-hidden bg-brand lg:block">
        {/* El escudo se funde con el fondo: el rojo de la imagen coincide con el guinda y los bordes se desvanecen. */}
        <img
          src="/brand/escudo-unmsm.webp"
          alt="Escudo de la Universidad Nacional Mayor de San Marcos"
          width={1024}
          height={575}
          className="absolute left-1/2 top-[47%] w-[122%] max-w-none -translate-x-1/2 -translate-y-1/2 animate-rise select-none [mask-image:radial-gradient(closest-side,black_62%,transparent)]"
          draggable={false}
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0"
          style={{
            background:
              'radial-gradient(120% 70% at 50% 50%, transparent 55%, rgb(0 0 0 / 0.22)),' +
              'linear-gradient(to bottom, transparent 70%, var(--brand-deep))',
          }}
        />
        <p className="absolute inset-x-0 bottom-10 m-0 text-center text-[11px] font-semibold uppercase tracking-[0.28em] text-white/70">
          Universidad Nacional Mayor de San Marcos
        </p>
      </aside>

      <main className="relative flex flex-1 flex-col overflow-auto">
        <div className="flex flex-1 items-center justify-center p-6 md:p-10">
          <div className="flex w-full max-w-[400px] animate-rise flex-col gap-10">
            <div className="lg:hidden">
              <BrandLogo />
            </div>
            {children}
          </div>
        </div>
      </main>
    </div>
  )
}
