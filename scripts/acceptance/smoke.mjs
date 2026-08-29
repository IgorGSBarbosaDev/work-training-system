import { readFile } from 'node:fs/promises'
import {
  AcceptanceError,
  API_ORIGIN,
  api,
  itemsFromPage,
  login,
  waitForMailpitMessage,
  waitFor,
  waitForHealth,
  writeArtifact,
} from './http.mjs'

const adminEmail = process.env.DEMO_ADMIN_EMAIL ?? 'admin@example.test'
const adminPassword = process.env.DEMO_ADMIN_PASSWORD ?? 'ChangeMe-Admin-2026!'
const managerEmail = process.env.DEMO_MANAGER_EMAIL ?? 'manager@example.test'
const managerPassword = process.env.DEMO_MANAGER_PASSWORD ?? 'ChangeMe-Manager-2026!'
const employeeEmail = process.env.DEMO_EMPLOYEE_EMAIL ?? 'employee@example.test'
const employeePassword = process.env.DEMO_EMPLOYEE_PASSWORD ?? 'ChangeMe-Employee-2026!'

function requireValue(value, label) {
  if (!value) throw new AcceptanceError(`Missing ${label} in demo state`)
  return value
}

const ASSESSMENT_RESULT = Object.freeze({ APPROVED: 'APPROVED', FAILED: 'FAILED' })

function assertAssessmentResult(response, expected) {
  const actual = response.data?.result
  if (actual !== expected) throw new Error(`Questionnaire result was ${actual}; expected ${expected}`)
}

async function main() {
  await waitForHealth()
  const state = JSON.parse(await readFile('acceptance-artifacts/demo-state.json', 'utf8'))
  const admin = await login(adminEmail, adminPassword)
  const manager = await login(managerEmail, managerPassword)
  const employee = await login(employeeEmail, employeePassword)
  const assignmentId = state.assignment.id
  const videoId = requireValue(state.training.videoId, 'training.videoId')
  const questionnaireId = requireValue(state.training.questionnaireId, 'training.questionnaireId')
  const questionId = requireValue(state.training.questionId, 'training.questionId')
  const correctOptionId = requireValue(state.training.correctOptionId, 'training.correctOptionId')
  const incorrectOptionId = requireValue(state.training.incorrectOptionId, 'training.incorrectOptionId')

  const checks = []
  const check = async (name, operation) => {
    await operation()
    checks.push({ name, status: 'passed' })
  }

  await check('admin health and identity', async () => {
    const health = await api(`${API_ORIGIN}/actuator/health/readiness`, { expected: [200] })
    if (health.status !== 200) throw new Error('Backend is not ready')
    await api('/auth/me', { token: admin.accessToken, expected: [200] })
  })
  await check('organization structure and employee', async () => {
    const units = await api('/units?size=100', { token: admin.accessToken, expected: [200] })
    const sectors = await api('/sectors?size=100', { token: admin.accessToken, expected: [200] })
    const jobs = await api('/jobs?size=100', { token: admin.accessToken, expected: [200] })
    const employeeResponse = await api(`/employees/${state.employee.id}`, { token: admin.accessToken, expected: [200] })
    if (!itemsFromPage(units.data).some((item) => item.id === state.unit.id) || !itemsFromPage(sectors.data).some((item) => item.id === state.sector.id) || !itemsFromPage(jobs.data).some((item) => item.id === state.job.id) || employeeResponse.data.id !== state.employee.id) throw new Error('Structure fixture was not persisted')
  })
  await check('dashboard aggregations, filters and pagination', async () => {
    const overview = await api(`/admin/dashboard/overview?unitId=${state.unit.id}`, { token: admin.accessToken, expected: [200] })
    for (const field of ['activeEmployees', 'employeesWithPendingItems', 'employeesWithBlockedActivities']) {
      if (typeof overview.data?.[field] !== 'number') throw new Error(`Dashboard overview is missing numeric field ${field}`)
    }
    for (const view of ['trainings', 'activities', 'employees']) {
      const response = await api(`/admin/dashboard/${view}?unitId=${state.unit.id}&page=0&size=20`, { token: admin.accessToken, expected: [200] })
      if (!Array.isArray(response.data?.content) || response.data.content.length === 0) {
        throw new Error(`Dashboard ${view} did not return the seeded data`)
      }
      if (response.data.size !== 20 || response.data.page !== 0) throw new Error(`Dashboard ${view} pagination contract is invalid`)
    }
    await api('/admin/dashboard/overview?periodFrom=2026-08-02&periodTo=2026-08-01', { token: admin.accessToken, expected: [400] })
    const team = await api(`/team/dashboard?unitId=${state.unit.id}`, { token: manager.accessToken, expected: [200] })
    if (team.data.activeEmployees < 1) throw new Error('Manager dashboard did not preserve the granted unit scope')
  })
  await check('published training and protected video', async () => {
    const training = await api(`/trainings/${state.training.id}`, { token: admin.accessToken, expected: [200] })
    if (training.data.status !== 'ACTIVE') throw new Error('Training is not active')
    const playback = await api(`/videos/${videoId}/playback-url`, { method: 'POST', token: employee.accessToken, expected: [200] })
    if (!playback.data?.url) throw new Error('Playback response did not contain a protected URL')
  })
  await check('assignment idempotency and employee start', async () => {
    const assignment = await api(`/training-assignments/${assignmentId}`, { token: admin.accessToken, expected: [200] })
    const repeated = await api('/training-assignments', { method: 'POST', token: admin.accessToken, headers: { 'Idempotency-Key': state.assignment.idempotencyKey }, body: { employeeId: state.employee.id, trainingId: state.training.id, trainingVersionId: state.training.versionId, origin: 'EMPLOYEE', dueDate: assignment.data.dueDate, priority: 'NORMAL', idempotencyKey: state.assignment.idempotencyKey }, expected: [201] })
    if (repeated.data.id !== assignmentId) throw new Error('Assignment idempotency mismatch')
    await api(`/training-assignments/${assignmentId}/start`, { method: 'POST', token: employee.accessToken, expected: [200] })
    const automaticAssignments = await api(`/training-assignments?employeeId=${state.employee.id}&trainingId=${state.automatic.trainingId}`, { token: admin.accessToken, expected: [200] })
    if (!itemsFromPage(automaticAssignments.data).some((item) => item.id === state.automatic.assignmentId)) throw new Error('Automatic assignment was not persisted')
  })
  await check('progress to eighty percent', async () => {
    const openedAt = new Date().toISOString()
    await api(`/training-assignments/${assignmentId}/videos/${videoId}/progress`, {
      method: 'PUT', token: employee.accessToken,
      body: { positionSeconds: 0, watchedSeconds: 0, reportedPercentage: 0, eventAt: openedAt, eventId: 'acceptance-progress-open', finalEvent: false }, expected: [200],
    })
    await new Promise((resolve) => setTimeout(resolve, 4_500))
    const eventAt = new Date().toISOString()
    const progress = await api(`/training-assignments/${assignmentId}/videos/${videoId}/progress`, {
      method: 'PUT', token: employee.accessToken,
      body: { positionSeconds: 8, watchedSeconds: 8, reportedPercentage: 80, eventAt, eventId: 'acceptance-progress-80', finalEvent: false }, expected: [200],
    })
    if (Number(progress.data.percentageWatched ?? progress.data.reportedPercentage ?? 0) < 80) throw new Error('Progress did not reach 80%')
  })
  let completedAttempt
  await check('approved questionnaire and automatic completion', async () => {
    const questionnaire = await api(`/training-assignments/${assignmentId}/questionnaires/${questionnaireId}`, { token: employee.accessToken, expected: [200] })
    if (!questionnaire.data) throw new Error('Questionnaire response is empty')
    const failedAnswerBody = { answers: [{ questionId, answerOptionId: incorrectOptionId }] }
    const failedAttempt = await api(`/training-assignments/${assignmentId}/questionnaires/${questionnaireId}/attempts`, { method: 'POST', token: employee.accessToken, headers: { 'Idempotency-Key': 'acceptance-attempt-failed-v1' }, body: failedAnswerBody, expected: [201, 200] })
    const repeatedFailedAttempt = await api(`/training-assignments/${assignmentId}/questionnaires/${questionnaireId}/attempts`, { method: 'POST', token: employee.accessToken, headers: { 'Idempotency-Key': 'acceptance-attempt-failed-v1' }, body: failedAnswerBody, expected: [201, 200] })
    if (repeatedFailedAttempt.data.attemptId !== failedAttempt.data.attemptId) throw new Error('Failed assessment idempotency mismatch')
    assertAssessmentResult(failedAttempt, ASSESSMENT_RESULT.FAILED)
    const answerBody = { answers: [{ questionId, answerOptionId: correctOptionId }] }
    completedAttempt = await api(`/training-assignments/${assignmentId}/questionnaires/${questionnaireId}/attempts`, { method: 'POST', token: employee.accessToken, headers: { 'Idempotency-Key': 'acceptance-attempt-v1' }, body: answerBody, expected: [201, 200] })
    const repeatedAttempt = await api(`/training-assignments/${assignmentId}/questionnaires/${questionnaireId}/attempts`, { method: 'POST', token: employee.accessToken, headers: { 'Idempotency-Key': 'acceptance-attempt-v1' }, body: answerBody, expected: [201, 200] })
    if (repeatedAttempt.data.attemptId !== completedAttempt.data.attemptId) throw new Error('Assessment idempotency mismatch')
    assertAssessmentResult(completedAttempt, ASSESSMENT_RESULT.APPROVED)
    await waitFor('assignment completion', async () => {
      const response = await api(`/training-assignments/${assignmentId}`, { token: employee.accessToken, expected: [200] })
      return response.data.status === 'COMPLETED' ? response.data : null
    }, { timeoutMs: 30_000, intervalMs: 500 })
  })
  let certificate
  await check('validity, certificate and validation', async () => {
    const completions = await api(`/employees/${state.employee.id}/completions`, { token: admin.accessToken, expected: [200] })
    const completion = itemsFromPage(completions.data)[0]
    if (!completion || completion.appliedValidityType !== 'MONTHS' || !completion.expirationDate) throw new Error('Completion validity was not calculated')
    const recalculated = await api(`/training-completions/${completion.id}/recalculate-expiration`, { method: 'POST', token: admin.accessToken, expected: [200] })
    if (!recalculated.data?.expirationDate) throw new Error('Expiration recalculation did not return a date')
    certificate = await waitFor('certificate generation', async () => {
      const response = await api('/me/certificates', { token: employee.accessToken, expected: [200] })
      return itemsFromPage(response.data)[0] ?? null
    }, { timeoutMs: 30_000, intervalMs: 1_000 })
    const download = await api(`/certificates/${certificate.id}/download`, { token: employee.accessToken, expected: [200] })
    const validation = await api(`/certificate-validations/${certificate.validationCode}`, { expected: [200] })
    if (!download.data?.url || !validation.data) throw new Error('Certificate download/validation evidence is incomplete')
  })
  let qr
  await check('QR generation, protected verification and access log', async () => {
    const revoked = await api(`/employees/${state.employee.id}/qr-code/revoke`, { method: 'POST', token: admin.accessToken, body: { reason: 'Rotação da fixture de aceite' }, expected: [200] })
    if (revoked.data?.status !== 'REVOKED') throw new Error('QR revocation was not persisted')
    const regenerated = await api(`/employees/${state.employee.id}/qr-code`, { method: 'POST', token: admin.accessToken, expected: [201] })
    state.qr = { id: regenerated.data.id, token: regenerated.data.token, verificationUrl: regenerated.data.verificationUrl }
    const verificationUrl = new URL(regenerated.data.verificationUrl)
    if (verificationUrl.pathname !== `/verificar/${regenerated.data.token}`) throw new Error('QR verification URL is not aligned with the canonical frontend route')
    await writeArtifact('demo-state.json', state)
    qr = await api('/me/qr-code', { token: employee.accessToken, expected: [200] })
    if (qr.data?.id !== state.qr.id) throw new Error('QR regeneration was not returned as the active code')
    const verification = await api(`/qr-verifications/${encodeURIComponent(state.qr.token)}`, { token: manager.accessToken, expected: [200] })
    if (verification.data?.employee?.registration !== 'ACCEPTANCE-001') throw new Error('QR verification did not resolve the demo employee')
    await api(`/qr-verifications/${state.qr.id}/access-log`, { token: admin.accessToken, expected: [200] })
  })
  await check('notification read and Mailpit delivery', async () => {
    const notifications = await waitFor('employee notification', async () => {
      const response = await api('/me/notifications?size=100', { token: employee.accessToken, expected: [200] })
      return itemsFromPage(response.data)[0] ?? null
    }, { timeoutMs: 30_000, intervalMs: 500 })
    await api(`/me/notifications/${notifications.id}/read`, { method: 'PATCH', token: employee.accessToken, expected: [200] })
    const deliveries = await api('/admin/email-deliveries?size=100', { token: admin.accessToken, expected: [200] })
    const deliveryItems = itemsFromPage(deliveries.data)
    if (!deliveryItems.length) throw new Error('No email delivery was recorded')
    const mailpitMessage = await waitForMailpitMessage(employeeEmail)
    await writeArtifact('mailpit-evidence.json', { status: 'passed', recipient: mailpitMessage.recipient, subject: mailpitMessage.subject, messageId: mailpitMessage.id })
    const failed = deliveryItems.find((item) => item.status === 'FAILED')
    if (failed) await api(`/admin/email-deliveries/${failed.id}/retry`, { method: 'POST', token: admin.accessToken, expected: [202] })
    else checks.push({ name: 'email retry', status: 'skipped', reason: 'No failed delivery exists in the healthy Mailpit environment' })
  })
  await check('audit log and authorization boundaries', async () => {
    const audit = await api('/audit-logs?size=100', { token: admin.accessToken, expected: [200] })
    if (!itemsFromPage(audit.data).length) throw new Error('Audit log is empty')
    await api('/admin/dashboard/overview', { token: manager.accessToken, expected: [403] })
    await api('/admin/dashboard/overview', { token: employee.accessToken, expected: [403] })
  })
  const result = { status: 'passed', generatedAt: new Date().toISOString(), checks, certificateId: certificate?.id, qrCodeId: qr?.id }
  await writeArtifact('smoke-result.json', result)
  console.log(JSON.stringify(result, null, 2))
}

main().catch(async (error) => {
  await writeArtifact('smoke-error.json', { status: 'failed', message: error.message, details: error.details }).catch(() => {})
  console.error(error.message)
  process.exitCode = 1
})
