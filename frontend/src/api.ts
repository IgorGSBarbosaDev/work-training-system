export type Role = 'ADMIN' | 'MANAGER' | 'SUPERVISOR' | 'EMPLOYEE'

export type User = {
  id: string
  email: string
  role: Role
  status: string
  employeeId?: string | null
  permissions?: string[]
}

export type Session = {
  accessToken: string
  refreshToken: string
  tokenType?: string
  expiresIn?: number
  user: User
}

export type ApiProblem = {
  status?: number
  error?: string
  code?: string
  message?: string
  path?: string
  requestId?: string
  fieldErrors?: Array<{ field: string; code?: string; message: string }>
}

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
const SESSION_KEY = 'work-training-session'
export const SESSION_EXPIRED_EVENT = 'work-training-session-expired'

function storage(): Storage | null {
  return typeof window === 'undefined' ? null : window.localStorage
}

export const authStore = {
  get: (): Session | null => {
    try {
      return JSON.parse(storage()?.getItem(SESSION_KEY) || 'null') as Session | null
    } catch {
      return null
    }
  },
  set: (session: Session) => storage()?.setItem(SESSION_KEY, JSON.stringify(session)),
  clear: () => storage()?.removeItem(SESSION_KEY),
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public problem?: ApiProblem,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function readProblem(response: Response): Promise<ApiProblem | undefined> {
  try {
    return (await response.json()) as ApiProblem
  } catch {
    return undefined
  }
}

async function refreshSession(): Promise<boolean> {
  const session = authStore.get()
  if (!session?.refreshToken) return false

  const response = await fetch(`${API_BASE}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: session.refreshToken }),
  })

  if (!response.ok) return false
  const next = (await response.json()) as Session
  authStore.set({ ...session, ...next, user: next.user || session.user })
  return true
}

export async function api<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const session = authStore.get()
  const headers = new Headers(init.headers)
  const isFormData = typeof FormData !== 'undefined' && init.body instanceof FormData

  if (init.body && !isFormData && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (session?.accessToken) headers.set('Authorization', `Bearer ${session.accessToken}`)

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...init, headers })
  } catch {
    throw new ApiError(0, 'Não foi possível conectar ao servidor. Verifique sua conexão.')
  }

  if (response.status === 401 && retry && (await refreshSession())) {
    return api<T>(path, init, false)
  }

  if (!response.ok) {
    const problem = await readProblem(response)
    if (response.status === 401 && retry) {
      authStore.clear()
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
    }
    throw new ApiError(
      response.status,
      problem?.message || `Não foi possível concluir a solicitação (${response.status}).`,
      problem,
    )
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export async function login(email: string, password: string): Promise<Session> {
  const session = await api<Session>(
    '/auth/login',
    { method: 'POST', body: JSON.stringify({ email, password }) },
    false,
  )
  authStore.set(session)
  return session
}

export async function logout(): Promise<void> {
  const session = authStore.get()
  try {
    if (session?.refreshToken) {
      await api<void>(
        '/auth/logout',
        { method: 'POST', body: JSON.stringify({ refreshToken: session.refreshToken }) },
        false,
      )
    }
  } finally {
    authStore.clear()
  }
}

export const apiBaseUrl = API_BASE
