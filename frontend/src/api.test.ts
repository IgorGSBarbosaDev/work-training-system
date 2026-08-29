import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, apiBaseUrl, authStore, SESSION_EXPIRED_EVENT, type Session } from './api'

const session: Session = {
  accessToken: 'expired-access', refreshToken: 'valid-refresh',
  user: { id: 'user-1', email: 'employee@example.test', role: 'EMPLOYEE', status: 'ACTIVE' },
}

function browserStorage() {
  const values = new Map<string, string>()
  return {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
    removeItem: (key: string) => values.delete(key),
    clear: () => values.clear(),
    key: (index: number) => [...values.keys()][index] ?? null,
    get length() { return values.size },
  } as Storage
}

describe('api configuration', () => {
  beforeEach(() => {
    vi.stubGlobal('window', { localStorage: browserStorage(), dispatchEvent: vi.fn() })
    authStore.set(session)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('uses the documented versioned API by default', () => {
    expect(apiBaseUrl).toBe('/api/v1')
  })

  it('shares one refresh request between concurrent unauthorized calls', async () => {
    const attempts = new Map<string, number>()
    const fetchMock = vi.fn(async (input: string | URL | Request) => {
      const url = String(input)
      if (url.endsWith('/auth/refresh')) {
        return new Response(JSON.stringify({ accessToken: 'renewed-access', refreshToken: 'renewed-refresh' }), {
          status: 200, headers: { 'Content-Type': 'application/json' },
        })
      }
      const count = (attempts.get(url) ?? 0) + 1
      attempts.set(url, count)
      return count === 1
        ? new Response(null, { status: 401 })
        : new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'Content-Type': 'application/json' } })
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(Promise.all([api('/first'), api('/second')])).resolves.toEqual([{ ok: true }, { ok: true }])
    expect(fetchMock.mock.calls.filter(([input]) => String(input).endsWith('/auth/refresh'))).toHaveLength(1)
    expect(authStore.get()?.accessToken).toBe('renewed-access')
  })

  it('clears and announces an expired session only once after a shared refresh failure', async () => {
    const dispatch = vi.mocked(window.dispatchEvent)
    vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 401 })))

    const results = await Promise.allSettled([api('/first'), api('/second')])
    expect(results.every((result) => result.status === 'rejected')).toBe(true)
    expect(dispatch).toHaveBeenCalledTimes(1)
    expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({ type: SESSION_EXPIRED_EVENT }))
    expect(authStore.get()).toBeNull()
  })
})
