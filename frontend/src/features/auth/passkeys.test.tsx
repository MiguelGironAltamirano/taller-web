import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { handlers, resetMockData } from '../../mocks/handlers'
import { getDb } from '../../mocks/db'
import { sessionStore } from '../../shared/session/session-store'
import { ToastProvider } from '../../shared/ui'
import { server } from '../../test/msw-server'
import { renderApp } from '../../test/render-app'
import { PasskeysModal } from './PasskeysModal'

const encoder = new TextEncoder()
const buffer = (text: string) => encoder.encode(text).buffer as ArrayBuffer

/** Credencial que devolvería el navegador tras la huella; el mock solo mira su `id`. */
const fakeCredential = (id: string, response: Record<string, unknown>) => ({
  id,
  rawId: buffer(id),
  type: 'public-key',
  authenticatorAttachment: 'platform',
  response,
  getClientExtensionResults: () => ({}),
})

const credentials = { create: vi.fn(), get: vi.fn() }

/** jsdom no tiene WebAuthn: se simula el navegador con lector de huella. */
function installWebAuthn() {
  Object.defineProperty(window, 'PublicKeyCredential', { value: function PublicKeyCredential() {}, configurable: true })
  Object.defineProperty(navigator, 'credentials', { value: credentials, configurable: true })
}

function removeWebAuthn() {
  Reflect.deleteProperty(window, 'PublicKeyCredential')
  Reflect.deleteProperty(navigator, 'credentials')
}

beforeEach(() => {
  resetMockData()
  server.use(...handlers)
  credentials.create.mockReset()
  credentials.get.mockReset()
  installWebAuthn()
})

afterEach(removeWebAuthn)

function renderModal() {
  // La solicitante del mock (id 1) con sesión iniciada.
  sessionStore.set({
    accessToken: 'mock-token-1',
    user: { id: 1, fullName: 'Lucía Quispe Huamán', email: 'lquispe@unmsm.edu.pe', role: 'SOLICITANTE', areaId: null },
  })
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <PasskeysModal open onClose={() => {}} />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('PasskeysModal', () => {
  it('registers a passkey after confirming the password and lists it', async () => {
    credentials.create.mockResolvedValue(
      fakeCredential('cred-laptop', {
        clientDataJSON: buffer('{}'),
        attestationObject: buffer('att'),
        getTransports: () => ['internal'],
      }),
    )
    const user = userEvent.setup()
    renderModal()
    expect(await screen.findByText('Aún no registraste ninguna.')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Contraseña actual'), 'Demo12345!')
    await user.type(screen.getByLabelText('Nombre (opcional)'), 'Mi laptop')
    await user.click(screen.getByRole('button', { name: 'Agregar huella o passkey' }))

    const list = await screen.findByRole('list', { name: 'Passkeys registradas' })
    expect(within(list).getByText('Mi laptop')).toBeInTheDocument()
    expect(within(list).getByText(/Sin usar todavía/)).toBeInTheDocument()
    // Las opciones del servidor llegan al navegador con los binarios ya convertidos.
    const { publicKey } = credentials.create.mock.calls[0][0] as { publicKey: PublicKeyCredentialCreationOptions }
    expect(publicKey.challenge).toBeInstanceOf(ArrayBuffer)
    expect(publicKey.user.id).toBeInstanceOf(ArrayBuffer)
    expect(publicKey.user.name).toBe('lquispe@unmsm.edu.pe')
    expect(publicKey.authenticatorSelection?.userVerification).toBe('required')
    expect(getDb().passkeys.map((p) => p.credentialId)).toEqual(['cred-laptop'])
  })

  it('asks again for the password when it is wrong, without opening the device dialog', async () => {
    const user = userEvent.setup()
    renderModal()

    await user.type(await screen.findByLabelText('Contraseña actual'), 'Incorrecta1')
    await user.click(screen.getByRole('button', { name: 'Agregar huella o passkey' }))

    expect(await screen.findByText('La contraseña actual no es correcta.')).toBeInTheDocument()
    expect(credentials.create).not.toHaveBeenCalled()
  })

  it('explains that the registration was cancelled on the device', async () => {
    credentials.create.mockRejectedValue(new DOMException('cancelado', 'NotAllowedError'))
    const user = userEvent.setup()
    renderModal()

    await user.type(await screen.findByLabelText('Contraseña actual'), 'Demo12345!')
    await user.click(screen.getByRole('button', { name: 'Agregar huella o passkey' }))

    expect(await screen.findByText(/Se canceló el registro en el dispositivo/)).toBeInTheDocument()
    expect(getDb().passkeys).toHaveLength(0)
  })

  it('deletes a passkey', async () => {
    getDb().passkeys.push({ id: 900, userId: 1, credentialId: 'cred-old', name: 'Celular viejo', createdAt: Date.now(), lastUsedAt: null })
    const user = userEvent.setup()
    renderModal()

    await user.click(await screen.findByRole('button', { name: 'Eliminar Celular viejo' }))

    expect(await screen.findByText('Aún no registraste ninguna.')).toBeInTheDocument()
    expect(getDb().passkeys).toHaveLength(0)
  })

  it('tells when the browser does not support passkeys', async () => {
    removeWebAuthn()
    renderModal()

    expect(await screen.findByText(/Este navegador no admite passkeys/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Agregar huella o passkey' })).not.toBeInTheDocument()
  })
})

describe('Login with fingerprint or passkey', () => {
  it('signs in with a registered passkey without typing the email', async () => {
    getDb().passkeys.push({ id: 901, userId: 1, credentialId: 'cred-1', name: 'Mi laptop', createdAt: Date.now(), lastUsedAt: null })
    credentials.get.mockResolvedValue(
      fakeCredential('cred-1', {
        clientDataJSON: buffer('{}'),
        authenticatorData: buffer('auth'),
        signature: buffer('sig'),
        userHandle: buffer('user-1'),
      }),
    )
    const user = userEvent.setup()
    renderApp('/login')

    await user.click(await screen.findByRole('button', { name: 'Ingresar con huella o passkey' }))

    expect(await screen.findByRole('heading', { name: 'Hola, Lucía' })).toBeInTheDocument()
    const { publicKey } = credentials.get.mock.calls[0][0] as { publicKey: PublicKeyCredentialRequestOptions }
    expect(publicKey.userVerification).toBe('required')
    expect(publicKey.allowCredentials).toEqual([])
  })

  it('shows the generic error when the passkey is not recognised', async () => {
    credentials.get.mockResolvedValue(
      fakeCredential('desconocida', {
        clientDataJSON: buffer('{}'),
        authenticatorData: buffer('auth'),
        signature: buffer('sig'),
        userHandle: null,
      }),
    )
    const user = userEvent.setup()
    renderApp('/login')

    await user.click(await screen.findByRole('button', { name: 'Ingresar con huella o passkey' }))

    expect(await screen.findByText('No pudimos verificar tu passkey')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
  })

  it('hides the option when the browser does not support passkeys', async () => {
    removeWebAuthn()
    renderApp('/login')

    expect(await screen.findByRole('button', { name: 'Ingresar' })).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Ingresar con huella o passkey' })).not.toBeInTheDocument(),
    )
  })
})
