import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ApiError, apiDelete, apiGet, apiPost } from '../../shared/api/client'
import type { PasskeyDto, PasskeyOptions } from '../../shared/api/types'
import { dateLong } from '../../shared/lib/format'
import {
  createPasskey,
  isPasskeyCancelled,
  isPasskeySupported,
  type CreationOptionsJSON,
} from '../../shared/lib/webauthn'
import { Alert, Button, Field, Icon, Input, Modal, ModalActions, PasswordInput, SkeletonRows, useToast } from '../../shared/ui'

type Props = { open: boolean; onClose: () => void }

const PASSKEYS_KEY = ['me', 'passkeys'] as const

/**
 * Huella y passkeys del usuario (docs/api/passkeys-v1.md §5): registrar una nueva pidiendo antes la contraseña,
 * ver las registradas y eliminarlas. La huella no sale del dispositivo; el sistema solo guarda una clave pública.
 */
export function PasskeysModal({ open, onClose }: Props) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const supported = isPasskeySupported()
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [formError, setFormError] = useState('')
  const [adding, setAdding] = useState(false)

  const list = useQuery({
    queryKey: PASSKEYS_KEY,
    queryFn: () => apiGet<PasskeyDto[]>('/me/passkeys'),
    enabled: open,
  })

  const remove = useMutation({
    mutationFn: (id: number) => apiDelete(`/me/passkeys/${id}`),
    onSuccess: () => {
      toast('Passkey eliminada')
      void queryClient.invalidateQueries({ queryKey: PASSKEYS_KEY })
    },
    onError: () => toast('No pudimos eliminar la passkey. Inténtalo de nuevo.', { tone: 'error' }),
  })

  const close = () => {
    setPassword('')
    setName('')
    setPasswordError('')
    setFormError('')
    onClose()
  }

  const add = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setPasswordError('')
    setFormError('')
    if (!password) {
      setPasswordError('Ingresa tu contraseña actual.')
      return
    }
    setAdding(true)
    try {
      const options = await apiPost<PasskeyOptions<CreationOptionsJSON>>('/me/passkeys/registration/options', {
        currentPassword: password,
      })
      const credential = await createPasskey(options.publicKey)
      await apiPost<PasskeyDto>('/me/passkeys', {
        challengeId: options.challengeId,
        name: name.trim() || undefined,
        credential,
      })
      toast('Listo: ya puedes ingresar con tu huella o passkey')
      setPassword('')
      setName('')
      void queryClient.invalidateQueries({ queryKey: PASSKEYS_KEY })
    } catch (error) {
      if (isPasskeyCancelled(error)) {
        setFormError('Se canceló el registro en el dispositivo. Puedes intentarlo de nuevo.')
      } else if (error instanceof ApiError && error.fieldErrors?.currentPassword) {
        setPasswordError(error.fieldErrors.currentPassword)
      } else if (error instanceof ApiError) {
        setFormError(error.detail || error.title)
      } else {
        setFormError('No pudimos registrar la passkey. Inténtalo de nuevo.')
      }
    } finally {
      setAdding(false)
    }
  }

  const passkeys = list.data ?? []

  return (
    <Modal
      open={open}
      onClose={close}
      title="Huella y passkeys"
      subtitle="Ingresa con la huella, el rostro o el PIN de tu dispositivo."
      onSubmit={add}
      width="min(480px,92%)"
    >
      <p className="m-0 text-[13px] text-muted">
        Tu huella nunca sale de tu dispositivo: el sistema solo guarda una clave pública. Tu contraseña sigue
        funcionando como respaldo.
      </p>

      <section aria-labelledby="passkeys-title" className="flex flex-col gap-2">
        <h3 id="passkeys-title" className="m-0 text-[11px] font-semibold uppercase tracking-wider text-muted">
          Registradas
        </h3>
        {list.isLoading ? (
          <SkeletonRows count={2} rowClass="h-11" />
        ) : list.isError ? (
          <Alert>No pudimos cargar tus passkeys.</Alert>
        ) : passkeys.length === 0 ? (
          <p className="m-0 rounded-lg border border-dashed border-line-strong px-3 py-3 text-[13px] text-muted">
            Aún no registraste ninguna.
          </p>
        ) : (
          <ul className="m-0 flex list-none flex-col gap-1.5 p-0" aria-label="Passkeys registradas">
            {passkeys.map((p) => (
              <li key={p.id} className="flex items-center gap-3 rounded-lg border border-line px-3 py-2">
                <Icon name="fingerprint" size={18} className="shrink-0 text-primary-fg" />
                <div className="min-w-0 flex-1">
                  <div className="truncate font-medium">{p.name}</div>
                  <div className="text-xs text-muted">
                    Creada {dateLong(Date.parse(p.createdAt))}
                    {' · '}
                    {p.lastUsedAt ? `Último uso ${dateLong(Date.parse(p.lastUsedAt))}` : 'Sin usar todavía'}
                    {p.synced && ' · Sincronizada en tus dispositivos'}
                  </div>
                </div>
                <Button
                  size="sm"
                  variant="ghost"
                  aria-label={`Eliminar ${p.name}`}
                  loading={remove.isPending && remove.variables === p.id}
                  onClick={() => remove.mutate(p.id)}
                >
                  Eliminar
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>

      {supported ? (
        <section aria-labelledby="passkey-add-title" className="flex flex-col gap-3 border-t border-line pt-3">
          <h3 id="passkey-add-title" className="m-0 text-[11px] font-semibold uppercase tracking-wider text-muted">
            Agregar en este dispositivo
          </h3>
          <Field label="Contraseña actual" error={passwordError}>
            {(a) => (
              <PasswordInput
                {...a}
                autoComplete="current-password"
                invalid={!!passwordError}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            )}
          </Field>
          <Field label="Nombre (opcional)" hint="Por ejemplo: Mi laptop o Mi celular.">
            {(a) => <Input {...a} maxLength={60} value={name} onChange={(e) => setName(e.target.value)} />}
          </Field>
          {formError && <Alert>{formError}</Alert>}
        </section>
      ) : (
        <Alert tone="info">Este navegador no admite passkeys. Prueba con uno actualizado (Chrome, Edge, Safari o Firefox).</Alert>
      )}

      <ModalActions>
        <Button onClick={close}>Cerrar</Button>
        {supported && (
          <Button type="submit" variant="primary" loading={adding}>
            {!adding && <Icon name="fingerprint" size={16} />}
            Agregar huella o passkey
          </Button>
        )}
      </ModalActions>
    </Modal>
  )
}
