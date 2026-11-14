import type { ReactNode } from 'react'
import { BrandLogo } from '../../shared/ui'

type AuthShellProps = { title: string; subtitle?: string; children: ReactNode }

/**
 * Marco de las pantallas públicas: panel con el escudo de la universidad (escritorio) y una tarjeta cuyo
 * encabezado guinda ocupa todo el ancho superior, con el título de la pantalla.
 */
export function AuthShell({ title, subtitle, children }: AuthShellProps) {
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
          <div className="flex w-full max-w-[440px] animate-rise flex-col items-center gap-7">
            <BrandLogo />
            <div className="w-full overflow-hidden rounded-2xl border border-primary/60 bg-surface shadow-[0_1px_2px_rgb(0_0_0/0.04),0_16px_40px_-20px_rgb(0_0_0/0.22)]">
              <header
                className="px-7 py-7 text-center text-white sm:px-9"
                style={{
                  background:
                    'radial-gradient(90% 120% at 50% 0%, rgb(255 255 255 / 0.12), transparent 70%),' +
                    'linear-gradient(180deg, var(--brand), var(--brand-deep))',
                }}
              >
                <h1 className="m-0 font-display text-[30px] font-semibold leading-tight">{title}</h1>
                {subtitle && <p className="m-0 mt-2 text-[13.5px] leading-relaxed text-white/80">{subtitle}</p>}
              </header>
              <div className="p-7 sm:p-9">{children}</div>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
