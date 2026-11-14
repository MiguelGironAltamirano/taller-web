import type { ReactNode } from 'react'
import { cn } from '../../shared/lib/cn'
import { BrandLogo, Icon, PriorityBadge, StatusBadge } from '../../shared/ui'

const AREAS = ['Mantenimiento', 'Infraestructura', 'Soporte informático', 'Trámites']
const YEAR = new Date().getFullYear()

const CARD = 'rounded-2xl border border-white/60 bg-surface/95 text-fg shadow-[0_24px_48px_-16px_rgb(0_0_0/0.45)] backdrop-blur'

/** Vista previa ilustrativa de la plataforma: una solicitud en curso, un aviso y una calificación. */
function Showcase() {
  const steps = 5
  const current = 3
  return (
    <div aria-hidden="true" className="relative mx-auto h-[300px] w-full max-w-[460px] select-none">
      <div className={cn(CARD, 'absolute left-0 top-12 w-[min(100%,340px)] animate-float p-4')}>
        <div className="flex items-start justify-between gap-3">
          <div>
            <div className="text-[13px] font-semibold">SOL-2026-000128</div>
            <div className="mt-0.5 text-xs text-muted">Gasfitería · Aula 105</div>
          </div>
          <PriorityBadge priority="Alta" />
        </div>
        <div className="mt-4 flex items-center">
          {Array.from({ length: steps }, (_, i) => (
            <div key={i} className="flex flex-1 items-center last:flex-none">
              <span
                className={cn(
                  'grid size-6 place-items-center rounded-full',
                  i < current && 'bg-primary text-on-primary',
                  i === current && 'border-2 border-primary bg-surface shadow-[0_0_0_3px_var(--primary-soft)]',
                  i > current && 'border border-line-strong bg-surface',
                )}
              >
                {i < current && <Icon name="check" size={12} strokeWidth={3} />}
              </span>
              {i < steps - 1 && (
                <span className={cn('mx-1 h-[3px] flex-1 rounded-full', i < current ? 'bg-primary' : 'bg-line')} />
              )}
            </div>
          ))}
        </div>
        <div className="mt-4 flex items-center justify-between gap-3">
          <StatusBadge state="En atención" />
          <span className="text-[11px] font-medium text-muted">Técnico asignado</span>
        </div>
        <div className="mt-3">
          <div className="h-1.5 overflow-hidden rounded-full bg-line/70">
            <div className="h-full w-[62%] rounded-full bg-good" />
          </div>
          <div className="mt-1.5 flex justify-between text-[11px] text-muted">
            <span>Plazo de atención</span>
            <span className="font-medium text-fg">Vence en 9 h</span>
          </div>
        </div>
      </div>

      <div
        className={cn(CARD, 'absolute right-0 top-0 flex w-[264px] animate-float items-start gap-2.5 p-3')}
        style={{ animationDelay: '-2.3s' }}
      >
        <span className="grid size-8 shrink-0 place-items-center rounded-full bg-ok-bg text-ok-fg">
          <Icon name="checkCircle" size={16} strokeWidth={2} />
        </span>
        <div className="min-w-0">
          <div className="text-[12.5px] font-semibold leading-snug">Tu solicitud fue resuelta</div>
          <div className="mt-0.5 text-[11px] text-muted">Hace 2 min · Confirma la solución</div>
        </div>
      </div>

      <div
        className={cn(CARD, 'absolute bottom-0 right-6 w-[220px] animate-float p-3')}
        style={{ animationDelay: '-4.6s' }}
      >
        <div className="text-[12px] font-semibold">¿Cómo te atendimos?</div>
        <div className="mt-1.5 flex gap-0.5 text-[#e8a317]">
          {Array.from({ length: 5 }, (_, i) => (
            <svg key={i} width="16" height="16" viewBox="0 0 24 24" fill={i < 4 ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8">
              <path d="M12 2l3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z" />
            </svg>
          ))}
        </div>
      </div>
    </div>
  )
}

/** Marco de las pantallas públicas: panel institucional con vista previa (escritorio) y formulario. */
export function AuthShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen bg-bg">
      <aside className="relative hidden w-[min(50%,680px)] shrink-0 flex-col overflow-hidden bg-brand-deep px-12 py-10 text-white lg:flex xl:px-16">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0"
          style={{
            background:
              'radial-gradient(55% 45% at 15% 0%, rgb(255 255 255 / 0.16), transparent 70%),' +
              'radial-gradient(45% 40% at 100% 85%, rgb(255 110 110 / 0.22), transparent 70%),' +
              'linear-gradient(160deg, var(--brand) 0%, var(--brand-deep) 70%)',
          }}
        />
        <svg
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 size-full opacity-[0.09] [mask-image:radial-gradient(70%_60%_at_50%_45%,black,transparent)]"
        >
          <defs>
            <pattern id="auth-grid" width="36" height="36" patternUnits="userSpaceOnUse">
              <path d="M36 0H0v36" fill="none" stroke="white" strokeWidth="1" />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#auth-grid)" />
        </svg>

        <div className="relative flex animate-rise items-center justify-between">
          <BrandLogo inverse />
          <span className="inline-flex items-center gap-1.5 rounded-full bg-white/10 px-3 py-1 text-xs font-medium text-white/85 ring-1 ring-inset ring-white/15">
            <span className="size-1.5 rounded-full bg-[#7ee2a8]" />
            Plataforma institucional
          </span>
        </div>

        <div className="relative my-auto flex flex-col gap-10 py-10">
          <div className="animate-rise [animation-delay:100ms]">
            <p className="m-0 max-w-[520px] text-[38px] font-bold leading-[1.08] tracking-tight xl:text-[46px]">
              Cada solicitud, <span className="text-white/60">del registro a la solución.</span>
            </p>
            <p className="m-0 mt-4 max-w-[460px] text-[15px] leading-relaxed text-white/75">
              Registra un problema, sigue su avance etapa por etapa y confirma cuando quede resuelto, todo desde un solo
              lugar.
            </p>
          </div>
          <div className="animate-rise [animation-delay:220ms]">
            <Showcase />
          </div>
        </div>

        <div className="relative flex animate-rise flex-col gap-4 [animation-delay:320ms]">
          <ul className="m-0 flex list-none flex-wrap gap-2 p-0" aria-label="Áreas atendidas">
            {AREAS.map((a) => (
              <li key={a} className="rounded-full bg-white/[0.07] px-3 py-1 text-xs text-white/80 ring-1 ring-inset ring-white/10">
                {a}
              </li>
            ))}
          </ul>
          <div className="text-[12.5px] text-white/55">Universidad Nacional Mayor de San Marcos</div>
        </div>
      </aside>

      <main className="relative flex flex-1 flex-col overflow-auto">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 top-0 h-56 bg-[radial-gradient(60%_100%_at_50%_0%,var(--primary-soft),transparent)] opacity-70 lg:hidden"
        />
        <div className="relative flex flex-1 items-center justify-center p-6 md:p-10">
          <div className="flex w-full max-w-[420px] animate-rise flex-col gap-8">
            <div className="lg:hidden">
              <BrandLogo />
            </div>
            {children}
          </div>
        </div>
        <footer className="relative px-6 pb-6 text-center text-xs text-muted md:px-10">
          © {YEAR} Servicios Universitarios · UNMSM
        </footer>
      </main>
    </div>
  )
}
