import type { ReactNode } from 'react'
import { BrandLogo, Icon, type IconName } from '../../shared/ui'

const FEATURES: { icon: IconName; title: string; text: string }[] = [
  { icon: 'file', title: 'Registra en minutos', text: 'Mantenimiento, soporte informático y trámites en un solo lugar.' },
  { icon: 'clock', title: 'Sigue cada etapa', text: 'Estado, responsable y plazo de atención siempre a la vista.' },
  { icon: 'star', title: 'Califica la atención', text: 'Confirma la solución y ayúdanos a mejorar el servicio.' },
]

/** Marco de las pantallas públicas: panel institucional a la izquierda (oculto en móvil) y formulario a la derecha. */
export function AuthShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen bg-bg">
      <aside className="relative hidden w-[min(46%,620px)] shrink-0 flex-col justify-between overflow-hidden bg-brand p-10 text-white lg:flex xl:p-14">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 bg-[radial-gradient(120%_80%_at_0%_0%,rgb(255_255_255/0.10),transparent_55%),linear-gradient(160deg,transparent_30%,var(--brand-deep))]"
        />
        <svg aria-hidden="true" className="pointer-events-none absolute inset-0 size-full opacity-[0.07]">
          <defs>
            <pattern id="auth-grid" width="32" height="32" patternUnits="userSpaceOnUse">
              <path d="M32 0H0v32" fill="none" stroke="white" strokeWidth="1" />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#auth-grid)" />
        </svg>
        <svg
          aria-hidden="true"
          viewBox="0 0 400 400"
          className="pointer-events-none absolute -bottom-40 -right-40 size-[520px] opacity-[0.12]"
        >
          {[60, 110, 160, 200].map((r) => (
            <circle key={r} cx="200" cy="200" r={r} fill="none" stroke="white" strokeWidth="1.2" />
          ))}
        </svg>

        <div className="relative animate-rise">
          <BrandLogo inverse />
        </div>

        <div className="relative flex max-w-[460px] animate-rise flex-col gap-8 [animation-delay:120ms]">
          <div>
            <p className="m-0 mb-3 text-xs font-semibold uppercase tracking-[0.16em] text-white/65">
              Plataforma de atención
            </p>
            <p className="m-0 text-[34px] font-bold leading-[1.12] xl:text-[40px]">
              Tus solicitudes, atendidas con claridad.
            </p>
          </div>
          <ul className="m-0 flex list-none flex-col gap-4 p-0">
            {FEATURES.map((f, i) => (
              <li key={f.title} className="flex animate-rise gap-3.5" style={{ animationDelay: `${240 + i * 90}ms` }}>
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-white/10 ring-1 ring-inset ring-white/15">
                  <Icon name={f.icon} size={18} strokeWidth={1.75} />
                </span>
                <span className="pt-0.5">
                  <span className="block text-[15px] font-semibold">{f.title}</span>
                  <span className="block text-[13.5px] leading-snug text-white/70">{f.text}</span>
                </span>
              </li>
            ))}
          </ul>
        </div>

        <div className="relative flex items-center justify-between gap-4 text-[12.5px] text-white/60">
          <span>Universidad Nacional Mayor de San Marcos</span>
          <span className="inline-flex items-center gap-1.5">
            <Icon name="lock" size={13} />
            Acceso institucional
          </span>
        </div>
      </aside>

      <main className="flex flex-1 items-center justify-center overflow-auto p-6 md:p-10">
        <div className="flex w-full max-w-[400px] animate-rise flex-col gap-8">
          <div className="lg:hidden">
            <BrandLogo />
          </div>
          {children}
        </div>
      </main>
    </div>
  )
}
