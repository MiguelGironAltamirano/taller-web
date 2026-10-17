import { beforeAll, describe, expect, it, vi } from 'vitest'
import { addFiles, MAX_FILE_BYTES, type PickedFile } from './files'

const file = (name: string, size = 1024) => new File([new Uint8Array(size)], name)

beforeAll(() => {
  vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: () => 'blob:preview' }))
})

describe('addFiles', () => {
  it('acepta JPG, PNG y PDF', () => {
    const { files, error } = addFiles([], [file('a.jpg'), file('b.PNG'), file('c.pdf')])
    expect(files.map((f) => f.name)).toEqual(['a.jpg', 'b.PNG', 'c.pdf'])
    expect(files[2].isPdf).toBe(true)
    expect(files[2].previewUrl).toBeNull()
    expect(error).toBe('')
  })

  it('rechaza otros formatos', () => {
    const { files, error } = addFiles([], [file('virus.exe')])
    expect(files).toHaveLength(0)
    expect(error).toBe('"virus.exe": solo se permiten JPG, PNG o PDF.')
  })

  it('rechaza archivos de más de 5 MB y acepta exactamente 5 MB', () => {
    expect(addFiles([], [file('grande.jpg', MAX_FILE_BYTES + 1)]).error).toBe('"grande.jpg" supera los 5 MB.')
    expect(addFiles([], [file('justo.jpg', MAX_FILE_BYTES)]).files).toHaveLength(1)
  })

  it('limita a 5 archivos por solicitud', () => {
    const current: PickedFile[] = Array.from({ length: 5 }, (_, i) => ({
      name: `f${i}.jpg`,
      size: 1,
      isPdf: false,
      previewUrl: null,
    }))
    const { files, error } = addFiles(current, [file('sexto.jpg')])
    expect(files).toHaveLength(5)
    expect(error).toBe('Máximo 5 archivos.')
  })
})
