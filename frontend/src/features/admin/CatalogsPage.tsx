import { useMemo, useState } from 'react'
import { buildLocationTree, useCatalogs } from '../../shared/api/catalogs'
import { ApiError } from '../../shared/api/client'
import { REQUEST_STATES, type Priority } from '../../shared/domain'
import { Button, Card, EmptyState, GridRow, GridTable, PageHeader, PriorityBadge, SkeletonRows, StatusBadge, Tabs, useToast } from '../../shared/ui'
import {
  useCreateArea,
  useCreateCategory,
  useCreateLocation,
  useDeleteArea,
  useDeleteCategory,
  useDeleteLocation,
  useStates,
  useUpdatePriority,
} from './api'
import { EntityDrawer } from './components/EntityDrawer'
import {
  areaSchema,
  categorySchema,
  locationSchema,
  prioritySchema,
  type AreaFormValues,
  type CategoryFormValues,
  type LocationFormValues,
  type PriorityFormValues,
} from './schemas/forms'

type TabId = 'areas' | 'categories' | 'priorities' | 'locations' | 'states'
type Drawer = { kind: 'area' } | { kind: 'category' } | { kind: 'priority'; id: number } | { kind: 'location' } | null

const TABS = [
  { id: 'areas' as const, label: 'Áreas' },
  { id: 'categories' as const, label: 'Categorías' },
  { id: 'priorities' as const, label: 'Prioridades' },
  { id: 'locations' as const, label: 'Ubicaciones' },
  { id: 'states' as const, label: 'Estados' },
]

/** Catálogos del sistema: áreas, categorías, prioridades (SLA), ubicaciones y estados (solo lectura). */
export function CatalogsPage() {
  const toast = useToast()
  const { data: catalogs, isLoading } = useCatalogs()
  const states = useStates()
  const [tab, setTab] = useState<TabId>('areas')
  const [drawer, setDrawer] = useState<Drawer>(null)

  const createArea = useCreateArea()
  const deleteArea = useDeleteArea()
  const createCategory = useCreateCategory()
  const deleteCategory = useDeleteCategory()
  const updatePriority = useUpdatePriority()
  const createLocation = useCreateLocation()
  const deleteLocation = useDeleteLocation()

  const areaName = (id: number) => catalogs?.areas.find((a) => a.id === id)?.name ?? '—'
  const rooms = useMemo(
    () =>
      buildLocationTree(catalogs?.locations ?? []).flatMap((f) =>
        f.pavilions.flatMap((p) => p.rooms.map((r) => ({ id: r.id, faculty: f.name, pavilion: p.name, room: r.name }))),
      ),
    [catalogs],
  )

  /** Eliminar con aviso: el backend responde 409 si hay datos asociados. */
  const remove = (run: () => Promise<unknown>, ok: string) =>
    run().then(
      () => toast(ok),
      (e: unknown) => toast(e instanceof ApiError ? e.detail || e.title : 'No pudimos eliminar.', { bad: true }),
    )

  const action = (label: string, onClick: () => void) => (
    <Button size="sm" onClick={onClick}>
      {label}
    </Button>
  )

  const actions: Record<TabId, React.ReactNode> = {
    areas: (
      <Button variant="primary" onClick={() => setDrawer({ kind: 'area' })}>
        Nueva área
      </Button>
    ),
    categories: (
      <Button variant="primary" onClick={() => setDrawer({ kind: 'category' })}>
        Nueva categoría
      </Button>
    ),
    priorities: null,
    locations: (
      <Button variant="primary" onClick={() => setDrawer({ kind: 'location' })}>
        Nueva ubicación
      </Button>
    ),
    states: null,
  }

  return (
    <>
      <PageHeader title="Catálogos" subtitle={tab === 'states' ? 'Solo lectura' : 'Configuración de datos maestros'} actions={actions[tab]} />
      <Tabs label="Catálogos" items={TABS} value={tab} onChange={setTab} />
      <Card className="overflow-hidden">
        {isLoading && <SkeletonRows count={4} />}

        {catalogs && tab === 'areas' && (
          <GridTable columns="2fr 1fr 1fr 120px" label="Áreas" heads={['Área', 'Categorías', 'Técnicos', '']}>
            {catalogs.areas.map((a) => (
              <GridRow key={a.id} columns="2fr 1fr 1fr 120px" className="min-h-12 py-1.5">
                <div>{a.name}</div>
                <div>{catalogs.categories.filter((c) => c.areaId === a.id).length}</div>
                <div>—</div>
                <div>{action('Eliminar', () => remove(() => deleteArea.mutateAsync(a.id), 'Área eliminada'))}</div>
              </GridRow>
            ))}
          </GridTable>
        )}

        {catalogs && tab === 'categories' && (
          <GridTable columns="1.5fr 2fr 120px" label="Categorías" heads={['Categoría', 'Área por defecto', '']}>
            {catalogs.categories.map((c) => (
              <GridRow key={c.id} columns="1.5fr 2fr 120px" className="min-h-12 py-1.5">
                <div>{c.name}</div>
                <div>{areaName(c.areaId)}</div>
                <div>{action('Eliminar', () => remove(() => deleteCategory.mutateAsync(c.id), 'Categoría eliminada'))}</div>
              </GridRow>
            ))}
          </GridTable>
        )}

        {catalogs && tab === 'priorities' && (
          <GridTable columns="80px 1.5fr 1fr 120px" label="Prioridades" heads={['Orden', 'Prioridad', 'Horas de SLA', '']}>
            {catalogs.priorities.map((p, i) => (
              <GridRow key={p.id} columns="80px 1.5fr 1fr 120px" className="min-h-12 py-1.5">
                <div>{i + 1}</div>
                <div>
                  <PriorityBadge priority={p.name as Priority} />
                </div>
                <div>{p.slaHours} h</div>
                <div>{action('Editar', () => setDrawer({ kind: 'priority', id: p.id }))}</div>
              </GridRow>
            ))}
          </GridTable>
        )}

        {catalogs && tab === 'locations' && (
          <>
            {rooms.length === 0 && <EmptyState icon="folder" title="Sin ubicaciones" compact />}
            {rooms.length > 0 && (
              <GridTable columns="2fr 1.2fr 1.2fr 120px" minWidth="720px" label="Ubicaciones" heads={['Facultad', 'Pabellón', 'Ambiente', '']}>
                {rooms.map((r) => (
                  <GridRow key={r.id} columns="2fr 1.2fr 1.2fr 120px" className="min-h-12 py-1.5">
                    <div className="truncate">{r.faculty}</div>
                    <div>{r.pavilion}</div>
                    <div>{r.room}</div>
                    <div>{action('Eliminar', () => remove(() => deleteLocation.mutateAsync(r.id), 'Ubicación eliminada'))}</div>
                  </GridRow>
                ))}
              </GridTable>
            )}
          </>
        )}

        {tab === 'states' && (
          <GridTable columns="80px 1.2fr 3fr" label="Estados" heads={['Orden', 'Estado', 'Descripción']}>
            {(states.data ?? []).map((s, i) => (
              <GridRow key={s.status} columns="80px 1.2fr 3fr" className="min-h-12 py-1.5">
                <div>{s.order}</div>
                <div>
                  <StatusBadge state={REQUEST_STATES[i]} />
                </div>
                <div>{s.description}</div>
              </GridRow>
            ))}
          </GridTable>
        )}
      </Card>

      {drawer?.kind === 'area' && (
        <EntityDrawer<AreaFormValues>
          title="Nueva área"
          cta="Guardar"
          schema={areaSchema}
          defaults={{ name: '' }}
          fields={[{ name: 'name', label: 'Nombre del área', type: 'text' }]}
          onSubmit={async (v) => {
            await createArea.mutateAsync(v.name)
            toast('Área creada')
          }}
          onClose={() => setDrawer(null)}
        />
      )}
      {drawer?.kind === 'category' && (
        <EntityDrawer<CategoryFormValues>
          title="Nueva categoría"
          cta="Guardar"
          schema={categorySchema}
          defaults={{ name: '', areaId: '' }}
          fields={[
            { name: 'name', label: 'Nombre de la categoría', type: 'text' },
            { name: 'areaId', label: 'Área por defecto', type: 'select', options: catalogs?.areas.map((a) => ({ value: a.id, label: a.name })) },
          ]}
          onSubmit={async (v) => {
            await createCategory.mutateAsync({ name: v.name, areaId: Number(v.areaId) })
            toast('Categoría creada')
          }}
          onClose={() => setDrawer(null)}
        />
      )}
      {drawer?.kind === 'priority' && (
        <EntityDrawer<PriorityFormValues>
          title="Editar SLA"
          cta="Guardar cambios"
          schema={prioritySchema}
          defaults={{ slaHours: String(catalogs?.priorities.find((p) => p.id === drawer.id)?.slaHours ?? '') }}
          fields={[{ name: 'slaHours', label: 'Horas de SLA', type: 'text' }]}
          onSubmit={async (v) => {
            await updatePriority.mutateAsync({ id: drawer.id, slaHours: Number(v.slaHours) })
            toast('SLA actualizado')
          }}
          onClose={() => setDrawer(null)}
        />
      )}
      {drawer?.kind === 'location' && (
        <EntityDrawer<LocationFormValues>
          title="Nueva ubicación"
          cta="Guardar"
          schema={locationSchema}
          defaults={{ faculty: '', pavilion: '', room: '' }}
          fields={[
            { name: 'faculty', label: 'Facultad', type: 'text' },
            { name: 'pavilion', label: 'Pabellón', type: 'text' },
            { name: 'room', label: 'Ambiente', type: 'text' },
          ]}
          onSubmit={async (v) => {
            await createLocation.mutateAsync(v)
            toast('Ubicación agregada')
          }}
          onClose={() => setDrawer(null)}
        />
      )}
    </>
  )
}
