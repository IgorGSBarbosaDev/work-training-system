export type Role = 'ADMIN' | 'MANAGER' | 'SUPERVISOR' | 'EMPLOYEE'
export type User = { id: string; email: string; role: Role; employeeId?: string | null }
export type Session = { accessToken: string; refreshToken: string; user: User; expiresIn?: number }

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
const SESSION_KEY = 'work-training-session'

export const authStore = {
  get: (): Session | null => { try { return JSON.parse(localStorage.getItem(SESSION_KEY) || 'null') } catch { return null } },
  set: (session: Session) => localStorage.setItem(SESSION_KEY, JSON.stringify(session)),
  clear: () => localStorage.removeItem(SESSION_KEY),
}

export class ApiError extends Error { constructor(public status: number, message: string) { super(message) } }

async function refresh(): Promise<boolean> {
  const session = authStore.get()
  if (!session?.refreshToken) return false
  const response = await fetch(`${API_BASE}/auth/refresh`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken: session.refreshToken }) })
  if (!response.ok) return false
  const next = await response.json()
  authStore.set({ ...session, ...next, user: next.user || session.user })
  return true
}

export async function api<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const session = authStore.get()
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (session?.accessToken) headers.set('Authorization', `Bearer ${session.accessToken}`)
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers })
  if (response.status === 401 && retry && await refresh()) return api<T>(path, init, false)
  if (!response.ok) { let message = `Request failed (${response.status})`; try { message = (await response.json()).message || message } catch {} throw new ApiError(response.status, message) }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export async function login(email: string, password: string): Promise<Session> {
  const session = await api<Session>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }, false)
  authStore.set(session); return session
}
export const apiBaseUrl = API_BASE
