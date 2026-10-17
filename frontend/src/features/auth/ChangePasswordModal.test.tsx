import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { ToastProvider } from '../../shared/ui'
import { server } from '../../test/msw-server'
import { ChangePasswordModal } from './ChangePasswordModal'

describe('ChangePasswordModal', () => {
  it('shows error on current password field when the server rejects it', async () => {
    server.use(
      http.put('*/api/v1/auth/password', () => {
        return HttpResponse.json(
          {
            type: 'about:blank',
            title: 'Bad Request',
            status: 400,
            detail: 'La contraseña actual no es correcta',
            errors: { currentPassword: 'La contraseña actual no es correcta.' },
          },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        )
      }),
    )

    const onClose = vi.fn()
    const user = userEvent.setup()

    render(
      <ToastProvider>
        <ChangePasswordModal open={true} onClose={onClose} />
      </ToastProvider>,
    )

    await user.type(screen.getByLabelText('Contraseña actual'), 'WrongPass123')
    await user.type(screen.getByLabelText('Nueva contraseña'), 'NewPass123')
    await user.type(screen.getByLabelText('Confirmar nueva contraseña'), 'NewPass123')
    await user.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('La contraseña actual no es correcta.')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('calls onClose on successful password change', async () => {
    server.use(
      http.put('*/api/v1/auth/password', () => {
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const onClose = vi.fn()
    const user = userEvent.setup()

    render(
      <ToastProvider>
        <ChangePasswordModal open={true} onClose={onClose} />
      </ToastProvider>,
    )

    await user.type(screen.getByLabelText('Contraseña actual'), 'OldPass123')
    await user.type(screen.getByLabelText('Nueva contraseña'), 'NewPass123')
    await user.type(screen.getByLabelText('Confirmar nueva contraseña'), 'NewPass123')
    await user.click(screen.getByRole('button', { name: 'Guardar' }))

    await vi.waitFor(() => {
      expect(onClose).toHaveBeenCalled()
    })
  })
})
