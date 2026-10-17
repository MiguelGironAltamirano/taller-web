import { Area, AreaChart, Bar, BarChart, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

type Datum = { label: string; value: number }

const TOOLTIP = {
  contentStyle: {
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    borderRadius: 8,
    color: 'var(--text)',
    fontSize: 12,
  },
  cursor: { fill: 'var(--surface2)' },
}

/** Resumen textual de una serie para lectores de pantalla (los gráficos no son legibles por sí solos). */
function SrSummary({ data }: { data: Datum[] }) {
  return (
    <ul className="sr-only">
      {data.map((d) => (
        <li key={d.label}>
          {d.label}: {d.value}
        </li>
      ))}
    </ul>
  )
}

type BarsProps = { data: Datum[]; color: string; label: string }

/** Barras horizontales con el valor al final (por categoría y por responsable). */
export function HorizontalBars({ data, color, label }: BarsProps) {
  if (data.length === 0) return <div className="py-6 text-center text-muted">Sin datos en el periodo.</div>
  return (
    <div role="img" aria-label={label}>
      <ResponsiveContainer width="100%" height={Math.max(120, data.length * 34)}>
        <BarChart data={data} layout="vertical" margin={{ left: 0, right: 24, top: 0, bottom: 0 }}>
          <XAxis type="number" hide allowDecimals={false} />
          <YAxis
            type="category"
            dataKey="label"
            width={140}
            tickLine={false}
            axisLine={false}
            tick={{ fontSize: 12, fill: 'var(--text)' }}
          />
          <Tooltip {...TOOLTIP} />
          <Bar dataKey="value" fill={color} radius={[0, 3, 3, 0]} barSize={12} label={{ position: 'right', fontSize: 12, fill: 'var(--text)' }} />
        </BarChart>
      </ResponsiveContainer>
      <SrSummary data={data} />
    </div>
  )
}

const PRIORITY_COLORS: Record<string, string> = {
  Crítica: '#DC2626',
  Alta: '#EA580C',
  Media: '#CA8A04',
  Baja: '#16A34A',
}

/** Dona de solicitudes por prioridad, con el total al centro y leyenda con valores. */
export function PriorityDonut({ data, total }: { data: Datum[]; total: number }) {
  return (
    <div className="flex flex-wrap items-center gap-4">
      <div role="img" aria-label="Solicitudes por prioridad" className="relative size-[130px]">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie data={total ? data : [{ label: 'Sin datos', value: 1 }]} dataKey="value" nameKey="label" innerRadius={42} outerRadius={62} stroke="none">
              {(total ? data : [{ label: 'Sin datos', value: 1 }]).map((d) => (
                <Cell key={d.label} fill={total ? PRIORITY_COLORS[d.label] : 'var(--surface2)'} />
              ))}
            </Pie>
            {total > 0 && <Tooltip {...TOOLTIP} />}
          </PieChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 grid place-items-center text-base font-bold">{total}</div>
      </div>
      <ul className="m-0 flex list-none flex-col gap-1.5 p-0 text-[13px]">
        {data.map((d) => (
          <li key={d.label} className="flex items-center gap-2">
            <span className="size-3 rounded-[3px]" style={{ background: PRIORITY_COLORS[d.label] }} />
            {d.label}
            <b>{d.value}</b>
          </li>
        ))}
      </ul>
    </div>
  )
}

/** Evolución de solicitudes registradas en el periodo. */
export function TimelineChart({ series, startLabel }: { series: number[]; startLabel: string }) {
  const data = series.map((value, i) => ({ label: `${i + 1}`, value }))
  return (
    <div>
      <div role="img" aria-label="Solicitudes registradas por día">
        <ResponsiveContainer width="100%" height={110}>
          <AreaChart data={data} margin={{ top: 6, right: 4, bottom: 0, left: 4 }}>
            <XAxis dataKey="label" hide />
            <YAxis hide allowDecimals={false} />
            <Tooltip {...TOOLTIP} labelFormatter={() => 'Registradas'} />
            <Area type="monotone" dataKey="value" stroke="var(--primary)" strokeWidth={2} fill="var(--primary)" fillOpacity={0.12} />
          </AreaChart>
        </ResponsiveContainer>
      </div>
      <div className="flex justify-between text-[11px] text-muted">
        <span>{startLabel}</span>
        <span>hoy</span>
      </div>
      <SrSummary data={series.map((value, i) => ({ label: `Tramo ${i + 1}`, value }))} />
    </div>
  )
}

/** Medidor semicircular del cumplimiento de SLA. */
export function SlaGauge({ pct }: { pct: number }) {
  const color = pct >= 85 ? '#16A34A' : pct >= 65 ? '#D97706' : '#DC2626'
  return (
    <div className="flex flex-col items-center">
      <svg viewBox="0 0 100 58" width="220" role="img" aria-label={`Cumplimiento de SLA ${pct} por ciento`}>
        <path d="M10 50 A40 40 0 0 1 90 50" fill="none" stroke="var(--surface2)" strokeWidth="12" strokeLinecap="round" />
        <path
          d="M10 50 A40 40 0 0 1 90 50"
          fill="none"
          stroke={color}
          strokeWidth="12"
          strokeLinecap="round"
          strokeDasharray={`${(pct * 1.2566).toFixed(1)} 125.66`}
        />
        <text x="50" y="48" textAnchor="middle" fontSize="16" fontWeight="700" fill="currentColor">
          {pct}%
        </text>
      </svg>
      <div className="text-xs text-muted">Solicitudes dentro de plazo</div>
    </div>
  )
}

/** Mini-gráfico de tendencia dentro de cada indicador. */
export function Sparkline({ series, color }: { series: number[]; color: string }) {
  const max = Math.max(1, ...series)
  const points = series
    .map((v, i) => `${(series.length > 1 ? i * (100 / (series.length - 1)) : 0).toFixed(1)},${(28 - 3 - (v / max) * 22).toFixed(1)}`)
    .join(' ')
  return (
    <svg viewBox="0 0 100 28" preserveAspectRatio="none" className="mt-1.5 h-7 w-full" aria-hidden="true">
      <polyline points={points} fill="none" stroke={color} strokeWidth="1.5" strokeLinejoin="round" vectorEffect="non-scaling-stroke" />
    </svg>
  )
}
