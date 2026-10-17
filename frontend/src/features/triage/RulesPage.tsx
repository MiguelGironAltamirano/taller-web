import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useCatalogs } from '../../shared/api/catalogs'
import { ApiError } from '../../shared/api/client'
import type { TriageRuleDto } from '../../shared/api/types'
import { PRIORITIES, type Priority } from '../../shared/domain'
import { cn } from '../../shared/lib/cn'
import {
  Alert,
  Button,
  Card,
  CardTitle,
  EmptyState,
  Field,
  Input,
  Modal,
  ModalActions,
  PageHeader,
  PriorityBadge,
  Select,
  SkeletonRows,
  Switch,
  Textarea,
  useToast,
} from '../../shared/ui'
import { useDeleteRule, useSaveRule, useTriageRules } from './api'
import { suggestTriage, type TriageRule } from './lib/suggest'
import { parseKeywords, ruleSchema, type RuleValues } from './schemas/rule'

function RuleDrawer({ rule, onClose }: { rule: TriageRuleDto | null; onClose: () => void }) {
  const toast = useToast()
  const { data: catalogs } = useCatalogs()
  const save = useSaveRule()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RuleValues>({
    resolver: zodResolver(ruleSchema),
    defaultValues: {
      keywords: rule?.keywords.join(', ') ?? '',
      categoryId: rule ? String(rule.categoryId) : '',
      priorityId: rule ? String(rule.priorityId) : '',
    },
  })

  const onSubmit = handleSubmit(async (v) => {
    try {
      await save.mutateAsync({
        id: rule?.id ?? null,
        body: { keywords: parseKeywords(v.keywords), categoryId: Number(v.categoryId), priorityId: Number(v.priorityId), active: rule?.active },
      })
      toast('Regla guardada')
      onClose()
    } catch (e) {
      setError('root', { message: e instanceof ApiError ? e.detail || e.title : 'No pudimos guardar la regla.' })
    }
  })

  return (
    <Modal open onClose={onClose} variant="drawer" title={rule ? 'Editar regla de triage' : 'Regla de triage'} onSubmit={onSubmit}>
      <Field label="Palabras clave (separadas por comas)" error={errors.keywords?.message}>
        {(a) => <Input {...a} invalid={!!errors.keywords} {...register('keywords')} />}
      </Field>
      <Field label="Categoría sugerida" error={errors.categoryId?.message}>
        {(a) => (
          <Select {...a} invalid={!!errors.categoryId} {...register('categoryId')}>
            <option value="">Selecciona</option>
            {catalogs?.categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
        )}
      </Field>
      <Field label="Prioridad sugerida" error={errors.priorityId?.message}>
        {(a) => (
          <Select {...a} invalid={!!errors.priorityId} {...register('priorityId')}>
            <option value="">Selecciona</option>
            {catalogs?.priorities.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </Select>
        )}
      </Field>
      {errors.root && <Alert>{errors.root.message}</Alert>}
      <ModalActions className="mt-auto">
        <Button className="flex-1" onClick={onClose}>
          Cancelar
        </Button>
        <Button type="submit" variant="primary" className="flex-1" loading={save.isPending}>
          {rule ? 'Guardar cambios' : 'Guardar'}
        </Button>
      </ModalActions>
    </Modal>
  )
}

/** Gestión de reglas de triage (spec §5: triage, prioridad S) con un probador de descripciones. */
export function RulesPage() {
  const toast = useToast()
  const { data: rules = [], isLoading } = useTriageRules()
  const save = useSaveRule()
  const remove = useDeleteRule()
  const [editing, setEditing] = useState<TriageRuleDto | 'new' | null>(null)
  const [tester, setTester] = useState('')

  const asRules = useMemo<TriageRule[]>(
    () =>
      rules.map((r) => ({
        id: r.id,
        keywords: r.keywords,
        category: r.categoryName,
        priority: (PRIORITIES as readonly string[]).includes(r.priorityName) ? (r.priorityName as Priority) : 'Media',
        active: r.active,
      })),
    [rules],
  )
  const result = suggestTriage(tester, asRules)

  const toggle = (r: TriageRuleDto) =>
    save.mutate({
      id: r.id,
      body: { keywords: r.keywords, categoryId: r.categoryId, priorityId: r.priorityId, active: !r.active },
    })

  return (
    <>
      <PageHeader
        title="Reglas de triage"
        subtitle="Palabras clave que sugieren categoría y prioridad"
        actions={
          <Button variant="primary" onClick={() => setEditing('new')}>
            Nueva regla
          </Button>
        }
      />
      <div className="flex flex-wrap items-start gap-3.5">
        <Card className="min-w-0 flex-[2_1_560px] overflow-hidden">
          {isLoading && <SkeletonRows count={4} rowClass="h-12" />}
          {!isLoading && rules.length === 0 && (
            <EmptyState icon="zap" title="Aún no hay reglas" description="Crea una regla para sugerir categoría y prioridad." compact />
          )}
          {rules.map((r) => (
            <div
              key={r.id}
              className={cn('flex flex-wrap items-center gap-3 border-b border-line px-4 py-3', !r.active && 'opacity-55')}
            >
              <div className="flex min-w-[220px] flex-1 flex-wrap gap-1">
                {r.keywords.map((k) => (
                  <span key={k} className="inline-flex h-6 items-center rounded-full bg-surface2 px-2 text-xs">
                    {k}
                  </span>
                ))}
              </div>
              <div className="text-[13px]">
                → <b>{r.categoryName}</b>
              </div>
              <PriorityBadge priority={(r.priorityName as Priority) ?? 'Media'} />
              <Switch checked={r.active} onChange={() => toggle(r)} label={`Regla activa: ${r.keywords.join(', ')}`} />
              <Button size="sm" onClick={() => setEditing(r)}>
                Editar
              </Button>
              <Button
                size="sm"
                className="text-err-fg"
                onClick={() => remove.mutate(r.id, { onSuccess: () => toast('Regla eliminada') })}
              >
                Eliminar
              </Button>
            </div>
          ))}
        </Card>
        <Card className="flex min-w-0 flex-[1_1_280px] flex-col gap-2.5 p-4">
          <CardTitle className="mb-0">Probador</CardTitle>
          <Textarea
            rows={4}
            aria-label="Descripción de ejemplo"
            placeholder="Escribe una descripción de ejemplo"
            value={tester}
            onChange={(e) => setTester(e.target.value)}
          />
          {result && (
            <div className="rounded-lg bg-info-bg px-3 py-2.5 text-info-fg">
              Sugeriría <b>{result.category}</b> con prioridad <b>{result.priority}</b>.
            </div>
          )}
          {!result && tester.trim().length >= 3 && (
            <div className="rounded-lg bg-surface2 px-3 py-2.5 text-muted">Sin coincidencias: no se mostraría sugerencia.</div>
          )}
        </Card>
      </div>
      {editing && <RuleDrawer rule={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
    </>
  )
}
