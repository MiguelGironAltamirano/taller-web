import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import type { RequestDetailView } from '../../shared/domain'
import { ToastProvider } from '../../shared/ui'
import { server } from '../../test/msw-server'
import { EvidenceTab } from './EvidenceTab'

const mockRequest: RequestDetailView = {
  code: 'SOL-2026-000001',
  createdAt: Date.parse('2026-10-17T10:00:00Z'),
  dueAt: Date.parse('2026-10-17T18:00:00Z'),
  state: 'Registrada',
  category: 'Electricidad',
  priority: 'Alta',
  faculty: 'Sistemas',
  pavilion: 'Pabellón Antiguo',
  room: 'Aula 101',
  description: 'Problema eléctrico',
  requester: 'Ana Solicitante',
  area: 'Mantenimiento',
  technician: null,
  areaId: 1,
  requesterId: 10,
  technicianId: null,
  history: [],
  comments: [],
  evidences: [],
  solution: null,
  rating: null,
  allowedActions: [],
}

function renderComponent(req = mockRequest) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <EvidenceTab request={req} />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('EvidenceTab', () => {
  beforeEach(() => {
    server.use(
      http.get('*/api/v1/requests/:code/evidences', () => {
        return HttpResponse.json([
          {
            id: 1,
            fileName: 'foto.png',
            kind: 'IMG',
            sizeBytes: 10240,
          },
        ])
      }),
    )
  })

  it('renders existing evidences from query', async () => {
    renderComponent()
    expect(await screen.findByText('foto.png')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Descargar foto.png' })).toBeInTheDocument()
  })

  it('rejects an invalid file extension on client side without calling server', async () => {
    let serverCalled = false
    server.use(
      http.post('*/api/v1/requests/:code/evidences', () => {
        serverCalled = true
        return HttpResponse.json({}, { status: 201 })
      }),
    )

    renderComponent()

    const input = (await screen.findByLabelText('Adjuntar archivo de evidencia')) as HTMLInputElement
    const file = new File(['fake-exe-content'], 'malware.exe', { type: 'application/x-msdownload' })

    fireEvent.change(input, { target: { files: [file] } })

    expect(await screen.findByRole('alert')).toHaveTextContent('solo se permiten JPG, PNG o PDF')
    expect(serverCalled).toBe(false)
  })

  it('rejects a file exceeding 5 MB on client side without calling server', async () => {
    let serverCalled = false
    server.use(
      http.post('*/api/v1/requests/:code/evidences', () => {
        serverCalled = true
        return HttpResponse.json({}, { status: 201 })
      }),
    )

    const user = userEvent.setup()
    renderComponent()

    const input = await screen.findByLabelText('Adjuntar archivo de evidencia')
    const largeBlob = new Uint8Array(6 * 1024 * 1024)
    const file = new File([largeBlob], 'huge.png', { type: 'image/png' })

    await user.upload(input, file)

    expect(await screen.findByRole('alert')).toHaveTextContent('supera los 5 MB')
    expect(serverCalled).toBe(false)
  })

  it('uploads a valid file successfully', async () => {
    let uploadedFileReceived = false
    server.use(
      http.post('*/api/v1/requests/:code/evidences', () => {
        uploadedFileReceived = true
        return HttpResponse.json(
          {
            id: 2,
            fileName: 'valida.png',
            kind: 'IMG',
            sizeBytes: 1234,
          },
          { status: 201 },
        )
      }),
    )

    const user = userEvent.setup()
    renderComponent()

    const input = await screen.findByLabelText('Adjuntar archivo de evidencia')
    const file = new File(['fake-image-content'], 'valida.png', { type: 'image/png' })

    await user.upload(input, file)

    expect(uploadedFileReceived).toBe(true)
  })
})
