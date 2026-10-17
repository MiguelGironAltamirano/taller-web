import { useMemo, useState } from 'react'
import { useCatalogs } from '../../shared/api/catalogs'
import type { AdminUser, Role } from '../../shared/api/types'
import { roleFromApi } from '../../shared/api/mappers'
import { ROLE_LABELS, ROLES } from '../../shared/domain'
import { normalizeText } from '../../shared/lib/format'
import { Avatar, Button, Card, EmptyState, GridRow, GridTable, PageHeader, Select, SkeletonRows, ToneBadge, useToast } from '../../shared/ui'
import { useAdminUsers, useSaveUser, useSetUserActive } from './api'
import { EntityDrawer } from './components/EntityDrawer'
import { userSchema, type UserFormValues } from './schemas/forms'

const COLUMNS = '1.4fr 1.6fr 110px 1.4fr 90px 170px'

/** Gestión de usuarios (spec §5: admin, prioridad S). */
export function UsersPage() {
  const toast = useToast()
  const { data: catalogs } = useCatalogs()
  const { data: users = [], isLoading } = useAdminUsers()
  const save = useSaveUser()
  const setActive = useSetUserActive()
  const [q, setQ] = useState('')
  const [role, setRole] = useState('')
  const [editing, setEditing] = useState<AdminUser | 'new' | null>(null)

  const visible = useMemo(
    () =>
      users.filter(
        (u) =>
          (!q || normalizeText(`${u.fullName} ${u.email}`).includes(normalizeText(q))) && (!role || u.role === role),
      ),
    [users, q, role],
  )
  const editedUser = editing && editing !== 'new' ? editing : null

  return (
    <>
      <PageHeader
        title="Usuarios"
        subtitle={`${users.length} usuarios registrados`}
        actions={
          <Button variant="primary" onClick={() => setEditing('new')}>
            Nuevo usuario
          </Button>
        }
      />
      <div className="flex flex-wrap gap-2">
        <input
          type="search"
          aria-label="Buscar"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Buscar por nombre o correo"
          className="h-9 min-w-0 flex-[1_1_220px] rounded-lg border border-line bg-surface px-2.5 max-md:h-11"
        />
        <Select aria-label="Filtrar por rol" value={role} onChange={(e) => setRole(e.target.value)} className="h-9 w-auto max-md:h-11">
          <option value="">Todos los roles</option>
          {ROLES.map((r) => (
            <option key={r} value={r.toUpperCase()}>
              {ROLE_LABELS[r]}
            </option>
          ))}
        </Select>
      </div>
      <Card className="overflow-hidden">
        {isLoading && <SkeletonRows count={5} />}
        {!isLoading && visible.length === 0 && <EmptyState icon="users" title="Sin resultados" compact />}
        {visible.length > 0 && (
          <GridTable columns={COLUMNS} minWidth="920px" label="Usuarios" heads={['Nombre', 'Correo', 'Rol', 'Área', 'Estado', '']}>
            {visible.map((u) => (
              <GridRow key={u.id} columns={COLUMNS} className="min-h-12 py-1.5">
                <div className="flex min-w-0 items-center gap-2">
                  <Avatar name={u.fullName} size="sm" />
                  <span className="truncate">{u.fullName}</span>
                </div>
                <div className="truncate">{u.email}</div>
                <div>{ROLE_LABELS[roleFromApi(u.role)]}</div>
                <div className="truncate">{u.areaName ?? '—'}</div>
                <div>
                  <ToneBadge tone={u.active ? 'ok' : 'muted'}>{u.active ? 'Activo' : 'Inactivo'}</ToneBadge>
                </div>
                <div className="flex gap-2">
                  <Button size="sm" onClick={() => setEditing(u)}>
                    Editar
                  </Button>
                  <Button
                    size="sm"
                    onClick={() =>
                      setActive.mutate(
                        { id: u.id, active: !u.active },
                        { onSuccess: () => toast(u.active ? 'Usuario desactivado' : 'Usuario activado') },
                      )
                    }
                  >
                    {u.active ? 'Desactivar' : 'Activar'}
                  </Button>
                </div>
              </GridRow>
            ))}
          </GridTable>
        )}
      </Card>

      {editing && (
        <EntityDrawer<UserFormValues>
          title={editedUser ? 'Editar usuario' : 'Nuevo usuario'}
          cta={editedUser ? 'Guardar cambios' : 'Guardar'}
          schema={userSchema}
          defaults={{
            fullName: editedUser?.fullName ?? '',
            email: editedUser?.email ?? '',
            role: editedUser?.role ?? 'SOLICITANTE',
            areaId: editedUser?.areaId ? String(editedUser.areaId) : '',
          }}
          fields={[
            { name: 'fullName', label: 'Nombre completo', type: 'text' },
            { name: 'email', label: 'Correo institucional', type: 'text' },
            {
              name: 'role',
              label: 'Rol',
              type: 'select',
              options: ROLES.map((r) => ({ value: r.toUpperCase(), label: ROLE_LABELS[r] })),
            },
            {
              name: 'areaId',
              label: 'Área (obligatoria para Técnico y Supervisor)',
              type: 'select',
              options: catalogs?.areas.map((a) => ({ value: a.id, label: a.name })),
            },
          ]}
          onSubmit={async (v) => {
            await save.mutateAsync({
              id: editedUser?.id ?? null,
              body: {
                fullName: v.fullName,
                email: v.email,
                role: v.role as Role,
                areaId: v.areaId ? Number(v.areaId) : null,
              },
            })
            toast(editedUser ? 'Usuario actualizado' : 'Usuario creado')
          }}
          onClose={() => setEditing(null)}
        />
      )}
    </>
  )
}
