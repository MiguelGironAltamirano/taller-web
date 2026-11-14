import type { ReactNode } from 'react'
import { BrandLogo } from '../../shared/ui'

/** Marco de las pantallas públicas: panel institucional sobrio (escritorio) y formulario. */
export function AuthShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen bg-bg">
      <aside className="relative hidden w-[min(44%,600px)] shrink-0 flex-col justify-between overflow-hidden bg-brand-deep p-12 text-white lg:flex">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0"
          style={{
            background:
              'radial-gradient(70% 55% at 0% 0%, rgb(255 255 255 / 0.12), transparent 70%),' +
              'linear-gradient(165deg, var(--brand) 0%, var(--brand-deep) 80%)',
          }}
        />
        <svg
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 size-full opacity-[0.07] [mask-image:linear-gradient(to_bottom,black,transparent_65%)]"
        >
          <defs>
            <pattern id="auth-grid" width="40" height="40" patternUnits="userSpaceOnUse">
              <path d="M40 0H0v40" fill="none" stroke="white" strokeWidth="1" />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#auth-grid)" />
        </svg>

        <div className="relative animate-rise">
          <BrandLogo inverse />
        </div>

        <div className="relative animate-rise [animation-delay:120ms]">
          <p className="m-0 max-w-[440px] text-[36px] font-bold leading-[1.1] tracking-tight xl:text-[42px]">
            Un solo lugar para tus solicitudes.
          </p>
          <p className="m-0 mt-4 max-w-[400px] text-[15px] leading-relaxed text-white/70">
            Registra, sigue y confirma la atención de mantenimiento, soporte informático y trámites.
          </p>
          <div className="mt-12 h-px w-full bg-white/15" />
          <p className="m-0 mt-5 text-[13px] text-white/55">Universidad Nacional Mayor de San Marcos</p>
        </div>
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
