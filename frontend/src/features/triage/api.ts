import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useCatalogs } from '../../shared/api/catalogs'
import { apiDelete, apiGet, apiPost, apiPut } from '../../shared/api/client'
import { triageRulesKey } from '../../shared/api/keys'
import type { TriageRuleBody, TriageRuleDto, TriageSuggestionDto } from '../../shared/api/types'
import type { Priority } from '../../shared/domain'
import type { TriageSuggestion } from './lib/suggest'

const MIN_LENGTH = 8
const DEBOUNCE_MS = 350

/**
 * Sugerencia de categoría y prioridad mientras el solicitante escribe la descripción
 * (spec §5.1). Espera a que deje de teclear; sin coincidencias devuelve `null`.
 */
export function useTriageSuggestion(description: string): TriageSuggestion | null {
  const { data: catalogs } = useCatalogs()
  const [debounced, setDebounced] = useState('')

  useEffect(() => {
    const id = setTimeout(() => setDebounced(description.trim()), DEBOUNCE_MS)
    return () => clearTimeout(id)
  }, [description])

  const { data } = useQuery({
    queryKey: ['triage', 'suggestion', debounced],
    queryFn: () => apiPost<TriageSuggestionDto>('/triage/suggestions', { description: debounced }),
    enabled: debounced.length >= MIN_LENGTH,
    staleTime: Infinity,
  })

  if (!data || !catalogs || debounced.length < MIN_LENGTH) return null
  const category = catalogs.categories.find((c) => c.id === data.categoryId)
  const priority = catalogs.priorities.find((p) => p.id === data.priorityId)
  return category && priority ? { category: category.name, priority: priority.name as Priority } : null
}

export function useTriageRules() {
  return useQuery({
    queryKey: triageRulesKey,
    queryFn: () => apiGet<TriageRuleDto[]>('/admin/triage-rules'),
  })
}

export function useSaveRule() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number | null; body: TriageRuleBody }) =>
      id === null ? apiPost<TriageRuleDto>('/admin/triage-rules', body) : apiPut<TriageRuleDto>(`/admin/triage-rules/${id}`, body),
    onSuccess: () => client.invalidateQueries({ queryKey: triageRulesKey }),
  })
}

export function useDeleteRule() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => apiDelete(`/admin/triage-rules/${id}`),
    onSuccess: () => client.invalidateQueries({ queryKey: triageRulesKey }),
  })
}
