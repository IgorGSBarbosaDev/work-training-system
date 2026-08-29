import { mkdir, writeFile } from 'node:fs/promises'
import { createHash, randomUUID } from 'node:crypto'

export const API_BASE_URL = process.env.API_BASE_URL ?? 'http://localhost:8080/api/v1'
export const MAILPIT_BASE_URL = process.env.MAILPIT_BASE_URL ?? 'http://localhost:8025'
export const ARTIFACT_DIR = process.env.ACCEPTANCE_ARTIFACT_DIR ?? 'acceptance-artifacts'
export const API_ORIGIN = new URL(API_BASE_URL).origin

export class AcceptanceError extends Error {
  constructor(message, details = {}) {
    super(message)
    this.name = 'AcceptanceError'
    this.details = details
  }
}

function urlFor(base, path) {
  if (/^https?:\/\//i.test(path)) return path
  return `${base.replace(/\/$/, '')}/${path.replace(/^\//, '')}`
}

export async function request(path, options = {}) {
  const { base = API_BASE_URL, token, body, expected, headers = {}, ...fetchOptions } = options
  const requestHeaders = { Accept: 'application/json', ...headers }
  if (token) requestHeaders.Authorization = `Bearer ${token}`
  if (body !== undefined) {
    requestHeaders['Content-Type'] = 'application/json'
    fetchOptions.body = JSON.stringify(body)
  }

  const requestUrl = urlFor(base, path)
  let response
  try {
    response = await fetch(requestUrl, { ...fetchOptions, headers: requestHeaders })
  } catch (error) {
    throw new AcceptanceError(`Fetch failed for ${requestUrl}: ${error instanceof Error ? error.message : String(error)}`)
  }
  const raw = await response.text()
  let data = raw
  try {
    data = raw ? JSON.parse(raw) : null
  } catch {
    // Binary and plain text responses remain available through `raw`.
  }

  if (expected && !expected.includes(response.status)) {
    throw new AcceptanceError(`Unexpected HTTP ${response.status} for ${fetchOptions.method ?? 'GET'} ${path}`, {
      status: response.status,
      expected,
      body: typeof data === 'string' ? data.slice(0, 500) : data,
    })
  }

  return { status: response.status, headers: response.headers, data, raw, url: response.url }
}

export async function api(path, options = {}) {
  return request(path, { ...options, expected: options.expected ?? [200, 201, 202, 204] })
}

export async function login(email, password) {
  const response = await api('/auth/login', { method: 'POST', body: { email, password }, expected: [200] })
  if (!response.data?.accessToken) throw new AcceptanceError('Login response did not contain an access token')
  return response.data
}

export function itemsFromPage(data) {
  if (Array.isArray(data)) return data
  return data?.content ?? data?.items ?? data?.data ?? data?.results ?? []
}

export async function listAll(path, token) {
  const values = []
  let page = 0
  while (page < 100) {
    const response = await api(`${path}${path.includes('?') ? '&' : '?'}page=${page}&size=100`, { token })
    const pageValues = itemsFromPage(response.data)
    values.push(...pageValues)
    if (Array.isArray(response.data) || response.data?.last === true || pageValues.length === 0
      || (Number.isInteger(response.data?.totalPages) && page + 1 >= response.data.totalPages)) break
    page += 1
  }
  return values
}

export async function tryGet(path, token) {
  const response = await request(path, { token, expected: [200, 404] })
  return response.status === 404 ? null : response.data
}

export async function waitFor(label, operation, { timeoutMs = 60_000, intervalMs = 1_000 } = {}) {
  const deadline = Date.now() + timeoutMs
  let lastError
  while (Date.now() < deadline) {
    try {
      const value = await operation()
      if (value) return value
    } catch (error) {
      lastError = error
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }
  throw new AcceptanceError(`Timed out waiting for ${label}`, { cause: lastError?.message })
}

export async function writeArtifact(name, value) {
  await mkdir(ARTIFACT_DIR, { recursive: true })
  const content = typeof value === 'string' ? value : JSON.stringify(value, null, 2)
  await writeFile(`${ARTIFACT_DIR}/${name}`, content, 'utf8')
}

export function sha256(buffer) {
  return createHash('sha256').update(buffer).digest('hex')
}

export function idempotencyKey(name) {
  return `acceptance-${name}-${randomUUID()}`
}

function messageId(summary) {
  return summary?.ID ?? summary?.Id ?? summary?.id
}

async function mailpitMessages() {
  const response = await request('/api/v1/messages?limit=100', { base: MAILPIT_BASE_URL, expected: [200] })
  return response.data?.messages ?? response.data?.Messages ?? response.data?.items ?? []
}

export async function waitForMailpitMessage(recipient, { timeoutMs = 30_000 } = {}) {
  return waitFor(`Mailpit message for ${recipient}`, async () => {
    const summaries = await mailpitMessages()
    const message = summaries.find((summary) => {
      const address = summary?.To?.[0]?.Address ?? summary?.to?.[0]?.address ?? summary?.recipient
      return address?.toLowerCase() === recipient.toLowerCase()
    })
    return message ? { id: messageId(message), subject: summarySubject(message), recipient } : null
  }, { timeoutMs, intervalMs: 500 })
}

function summarySubject(summary) {
  return summary?.Subject ?? summary?.subject ?? 'unknown'
}

async function mailpitMessageText(id) {
  const detail = await request(`/api/v1/message/${encodeURIComponent(id)}`, { base: MAILPIT_BASE_URL, expected: [200] })
  const data = detail.data
  const structured = [data?.Text, data?.text, data?.Body, data?.body, data?.HTML, data?.html]
    .filter((value) => typeof value === 'string')
    .join('\n')
  if (structured) return structured
  const plain = await request(`/view/${encodeURIComponent(id)}.txt`, { base: MAILPIT_BASE_URL, expected: [200] })
  return plain.raw
}

export async function waitForResetToken(recipient, { after = 0, timeoutMs = 30_000 } = {}) {
  return waitFor(`activation e-mail for ${recipient}`, async () => {
    const summaries = await mailpitMessages()
    for (const summary of summaries) {
      const id = messageId(summary)
      const address = summary?.To?.[0]?.Address ?? summary?.to?.[0]?.address ?? summary?.recipient
      const receivedAt = Date.parse(summary?.Created ?? summary?.created ?? summary?.Date ?? '') || 0
      if (!id || (address && address.toLowerCase() !== recipient.toLowerCase()) || (receivedAt && receivedAt < after)) continue
      const text = await mailpitMessageText(id)
      const match = text.match(/[?&]token=([^\s&<>"']+)/i)
      if (match) return decodeURIComponent(match[1])
    }
    return null
  }, { timeoutMs, intervalMs: 500 })
}

export async function resetPasswordFromMailpit(userId, email, password, adminToken) {
  const startedAt = Date.now()
  await api(`/users/${userId}/password-reset`, { method: 'POST', token: adminToken, expected: [202] })
  const token = await waitForResetToken(email, { after: startedAt })
  await api('/auth/password/reset', { method: 'POST', body: { token, newPassword: password }, expected: [204] })
}

export async function waitForHealth() {
  return waitFor('backend readiness', async () => {
    const response = await request(`${API_ORIGIN}/actuator/health/readiness`, { expected: [200, 503] })
    return response.status === 200
  })
}
