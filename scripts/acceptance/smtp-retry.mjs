import { execFile } from 'node:child_process'
import { promisify } from 'node:util'
import { readFile } from 'node:fs/promises'
import { api, itemsFromPage, login, waitFor, waitForHealth, waitForMailpitMessage, writeArtifact } from './http.mjs'

const execFileAsync = promisify(execFile)
const adminEmail = process.env.DEMO_ADMIN_EMAIL ?? 'admin@example.test'
const adminPassword = process.env.DEMO_ADMIN_PASSWORD ?? 'ChangeMe-Admin-2026!'

async function compose(...args) {
  await execFileAsync('docker', ['compose', ...args], { cwd: process.cwd() })
}

async function main() {
  await waitForHealth()
  const state = JSON.parse(await readFile('acceptance-artifacts/demo-state.json', 'utf8'))
  const admin = await login(adminEmail, adminPassword)
  let mailpitStopped = false
  try {
    await compose('stop', 'mailpit')
    mailpitStopped = true
    const idempotencyKey = 'acceptance-smtp-failure-v1'
    await api('/training-assignments', {
      method: 'POST', token: admin.accessToken, headers: { 'Idempotency-Key': idempotencyKey },
      body: {
        employeeId: state.smtpEmployee.id, trainingId: state.browser.trainingId,
        trainingVersionId: state.browser.versionId, origin: 'EMPLOYEE',
        dueDate: new Date(Date.now() + 9 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
        priority: 'NORMAL', idempotencyKey,
      },
      expected: [201],
    })
    const failed = await waitFor('FAILED email delivery', async () => {
      const response = await api(`/admin/email-deliveries?status=FAILED&recipient=${encodeURIComponent(state.smtpEmployee.email)}&size=100`, { token: admin.accessToken, expected: [200] })
      return itemsFromPage(response.data).find((item) => item.subject === 'Novo treinamento atribuído') ?? null
    }, { timeoutMs: 30_000, intervalMs: 500 })

    await compose('start', 'mailpit')
    mailpitStopped = false
    await waitFor('Mailpit readiness', async () => fetch('http://localhost:8025/readyz').then((response) => response.ok).catch(() => false))
    const pending = await api(`/admin/email-deliveries/${failed.id}/retry`, { method: 'POST', token: admin.accessToken, expected: [202] })
    if (pending.data.status !== 'PENDING') throw new Error(`Retry status was ${pending.data.status}; expected PENDING`)
    const sent = await waitFor('SENT email delivery', async () => {
      const response = await api(`/admin/email-deliveries?status=SENT&recipient=${encodeURIComponent(state.smtpEmployee.email)}&size=100`, { token: admin.accessToken, expected: [200] })
      return itemsFromPage(response.data).find((item) => item.id === failed.id) ?? null
    }, { timeoutMs: 30_000, intervalMs: 500 })
    const message = await waitForMailpitMessage(state.smtpEmployee.email, { subject: 'Novo treinamento atribuído' })
    await writeArtifact('smtp-retry-evidence.json', {
      status: 'passed', transition: ['FAILED', 'PENDING', 'SENT'], deliveryId: sent.id,
      attemptCount: sent.attemptCount, recipient: state.smtpEmployee.email, subject: message.subject, messageId: message.id,
    })
    console.log(JSON.stringify({ status: 'passed', transition: ['FAILED', 'PENDING', 'SENT'], deliveryId: sent.id }, null, 2))
  } finally {
    if (mailpitStopped) await compose('start', 'mailpit').catch(() => {})
  }
}

main().catch(async (error) => {
  await writeArtifact('smtp-retry-error.json', { status: 'failed', message: error.message }).catch(() => {})
  console.error(error.message)
  process.exitCode = 1
})
