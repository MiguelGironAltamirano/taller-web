import { onlineManager } from '@tanstack/react-query'
import { afterEach, describe, expect, it } from 'vitest'
import { createQueryClient } from './queryClient'

describe('createQueryClient', () => {
  afterEach(() => onlineManager.setOnline(true))

  it('without connection an action fails at once (so the screen can say so) instead of waiting silently', async () => {
    onlineManager.setOnline(false)
    const client = createQueryClient()
    const mutation = client.getMutationCache().build(client, {
      mutationFn: () => Promise.reject(new TypeError('Failed to fetch')),
    })

    await expect(mutation.execute(undefined)).rejects.toThrow('Failed to fetch')
  })

  it('without connection a query ends in error instead of loading forever', async () => {
    onlineManager.setOnline(false)
    const client = createQueryClient()

    await expect(
      client.fetchQuery({ queryKey: ['x'], queryFn: () => Promise.reject(new TypeError('Failed to fetch')), retry: false }),
    ).rejects.toThrow('Failed to fetch')
  })
})
