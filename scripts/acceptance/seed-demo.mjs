import { readFile } from 'node:fs/promises'
import {
  api,
  listAll,
  login,
  resetPasswordFromMailpit,
  tryGet,
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

const status = 'ACTIVE'

async function findOrCreate(path, itemsPath, predicate, body, token) {
  const existing = (await listAll(itemsPath ?? path, token)).find(predicate)
  if (existing) return existing
  const response = await api(path, { method: 'POST', token, body, expected: [201] })
  return response.data
}

async function findUser(email, token) {
  return (await listAll('/users', token)).find((user) => user.email?.toLowerCase() === email.toLowerCase())
}

async function ensureUser({ email, password, role, employeeId, adminToken }) {
  let user = await findUser(email, adminToken)
  if (!user) {
    const response = await api('/users', {
      method: 'POST',
      token: adminToken,
      body: { email, role, employeeId, sendActivationEmail: true },
      expected: [201],
    })
    user = response.data
  }
  await resetPasswordFromMailpit(user.id, email, password, adminToken)
  return user
}

async function ensureManagerScope(userId, unitId, adminToken) {
  const current = await api(`/users/${userId}/permissions`, { token: adminToken, expected: [200] })
  const scopes = current.data?.scopes ?? []
  if (scopes.some((scope) => scope.type === 'UNIT' && scope.targetId === unitId)) return
  await api(`/users/${userId}/permissions`, {
    method: 'PATCH', token: adminToken,
    body: { permissions: current.data?.permissions ?? [], scopes: [...scopes, { type: 'UNIT', targetId: unitId }] },
    expected: [200],
  })
}

async function ensureTraining(adminToken) {
  const code = 'ACCEPTANCE-TRAINING'
  const existing = (await listAll('/trainings', adminToken)).find((training) => training.code === code)
  if (existing) {
    let previous
    try {
      previous = JSON.parse(await readFile('acceptance-artifacts/demo-state.json', 'utf8'))
    } catch {
      throw new Error('The acceptance training already exists but demo-state.json is unavailable; the public API has no content discovery endpoint for rebuilding the fixture.')
    }
    const versions = await api(`/trainings/${existing.id}/versions`, { token: adminToken, expected: [200] })
    const version = versions.data.find((item) => item.status === 'DRAFT') ?? versions.data[0]
    const modules = await api(`/training-versions/${version.id}/modules`, { token: adminToken, expected: [200] })
    const module = modules.data[0]
    const priorTraining = previous.training ?? {}
    if (previous.training?.id !== existing.id || previous.training?.versionId !== version.id || previous.training?.moduleId !== module.id) {
      throw new Error('demo-state.json does not describe the existing acceptance training')
    }
    const video = priorTraining.videoId ? (await api(`/videos/${priorTraining.videoId}`, { token: adminToken, expected: [200] })).data : null
    const questionnaire = priorTraining.questionnaireId ? { id: priorTraining.questionnaireId } : null
    const question = priorTraining.questionId ? { id: priorTraining.questionId } : null
    const correctOption = priorTraining.correctOptionId ? { id: priorTraining.correctOptionId } : null
    const incorrectOption = priorTraining.incorrectOptionId ? { id: priorTraining.incorrectOptionId } : null
    return { training: existing, version, module, video, questionnaire, question, correctOption, incorrectOption }
  }

  const trainingResponse = await api('/trainings', {
    method: 'POST',
    token: adminToken,
    body: {
      name: 'Treinamento de aceite técnico',
      code,
      description: 'Fixture fictícia para validar o fluxo integrado do MVP.',
      category: 'Aceite técnico',
      isRegulatoryStandard: false,
      status,
      initialVersion: {
        workloadMinutes: 10,
        validityType: 'MONTHS',
        validityValue: 12,
        passingScore: 70,
        maxAttempts: 3,
        retryIntervalMinutes: 0,
      },
    },
    expected: [201],
  })
  const training = trainingResponse.data
  const versionsResponse = await api(`/trainings/${training.id}/versions`, { token: adminToken, expected: [200] })
  const version = versionsResponse.data.find((item) => item.status === 'DRAFT') ?? versionsResponse.data[0]
  const moduleResponse = await api(`/training-versions/${version.id}/modules`, {
    method: 'POST', token: adminToken,
    body: { title: 'Módulo de aceite', description: 'Conteúdo de fixture.', order: 1, status },
    expected: [201],
  })
  const module = moduleResponse.data

  const fixture = Buffer.from('work-training-system acceptance video fixture\n', 'utf8')
  const uploadResponse = await api('/uploads', {
    method: 'POST', token: adminToken,
    body: {
      purpose: 'TRAINING_VIDEO', fileName: 'acceptance-fixture.mp4', contentType: 'video/mp4',
      sizeBytes: fixture.length,
    },
    expected: [201],
  })
  const upload = uploadResponse.data
  const uploadHeaders = { ...(upload.requiredHeaders ?? {}), 'Content-Type': 'video/mp4' }
  const binaryResponse = await fetch(upload.uploadUrl, { method: upload.method ?? 'PUT', headers: uploadHeaders, body: fixture })
  if (!binaryResponse.ok) throw new Error(`MinIO upload failed with HTTP ${binaryResponse.status}`)
  const completedUpload = await api(`/uploads/${upload.uploadId}/complete`, { method: 'POST', token: adminToken, expected: [200] })

  const videoResponse = await api(`/modules/${module.id}/videos`, {
    method: 'POST', token: adminToken,
    body: {
      title: 'Vídeo de aceite', description: 'Fixture de vídeo enviada pelo fluxo real.', order: 1,
      durationSeconds: 10, storageObjectKey: completedUpload.data.objectKey ?? upload.objectKey,
      required: true, status, fileId: upload.fileId,
    },
    expected: [201],
  })
  const questionnaireResponse = await api(`/modules/${module.id}/questionnaire`, {
    method: 'POST', token: adminToken,
    body: { title: 'Questionário de aceite', passingScore: 70, maxAttempts: 3, retryIntervalMinutes: 0, shuffleQuestions: false, status },
    expected: [201],
  })
  const questionnaire = questionnaireResponse.data
  const questionResponse = await api(`/questionnaires/${questionnaire.id}/questions`, {
    method: 'POST', token: adminToken,
    body: { statement: 'Qual ambiente é usado no aceite local?', order: 1, status },
    expected: [201],
  })
  const question = questionResponse.data
  const correctOptionResponse = await api(`/questions/${question.id}/options`, {
    method: 'POST', token: adminToken,
    body: { text: 'Docker Compose com PostgreSQL, MinIO e Mailpit', correct: true, order: 1, status },
    expected: [201],
  })
  const incorrectOptionResponse = await api(`/questions/${question.id}/options`, {
    method: 'POST', token: adminToken,
    body: { text: 'Uma base local sem serviços externos', correct: false, order: 2, status },
    expected: [201],
  })
  const publishedResponse = await api(`/training-versions/${version.id}/publish`, { method: 'POST', token: adminToken, expected: [200] })

  return {
    training, version: publishedResponse.data, module, video: videoResponse.data, questionnaire,
    question, correctOption: correctOptionResponse.data, incorrectOption: incorrectOptionResponse.data,
  }
}

async function ensureAutomaticScenario(adminToken, job, employeeId, sourceVideo) {
  let previous
  try {
    previous = JSON.parse(await readFile('acceptance-artifacts/demo-state.json', 'utf8'))
  } catch {
    previous = null
  }

  const previousAutomaticTraining = previous?.automatic?.trainingId
    ? await tryGet(`/trainings/${previous.automatic.trainingId}`, adminToken)
    : null
  if (previousAutomaticTraining && previous?.automatic?.activityId) {
    const assignment = (await listAll(`/training-assignments?employeeId=${employeeId}&trainingId=${previous.automatic.trainingId}`, adminToken))[0]
    return { trainingId: previous.automatic.trainingId, versionId: previous.automatic.versionId, activityId: previous.automatic.activityId, assignmentId: assignment?.id }
  }

  const existingTraining = (await listAll('/trainings', adminToken)).find((item) => item.code === 'ACCEPTANCE-AUTO-TRAINING')
  const training = existingTraining ?? (await api('/trainings', {
    method: 'POST', token: adminToken,
    body: {
      name: 'Treinamento automático de aceite', code: 'ACCEPTANCE-AUTO-TRAINING',
      description: 'Treinamento fictício usado para validar a regra automática por cargo.',
      category: 'Aceite técnico', isRegulatoryStandard: false, status,
      initialVersion: { workloadMinutes: 5, validityType: 'MONTHS', validityValue: 12, passingScore: 70, maxAttempts: 3, retryIntervalMinutes: 0 },
    },
    expected: [201],
  })).data
  const versions = await api(`/trainings/${training.id}/versions`, { token: adminToken, expected: [200] })
  const version = versions.data.find((item) => item.status === 'DRAFT') ?? versions.data[0]
  if (!existingTraining) {
    const module = (await api(`/training-versions/${version.id}/modules`, {
      method: 'POST', token: adminToken,
      body: { title: 'Módulo automático', description: 'Conteúdo mínimo para a atribuição automática.', order: 1, status },
      expected: [201],
    })).data
    await api(`/modules/${module.id}/videos`, {
      method: 'POST', token: adminToken,
      body: { title: 'Vídeo automático', description: 'Vídeo compartilhado da fixture de aceite.', order: 1, durationSeconds: 5, storageObjectKey: sourceVideo.storageObjectKey, required: true, status, fileId: sourceVideo.fileId },
      expected: [201],
    })
    await api(`/training-versions/${version.id}/publish`, { method: 'POST', token: adminToken, expected: [200] })
  }

  const activity = await findOrCreate('/activities', '/activities', (item) => item.name === 'Atividade automática de aceite',
    { name: 'Atividade automática de aceite', description: 'Fixture que atribui treinamento por cargo.', status }, adminToken)
  const requirements = await api(`/activities/${activity.id}/requirements`, { token: adminToken, expected: [200] })
  if (!requirements.data.some((item) => item.training?.id === training.id)) {
    await api(`/activities/${activity.id}/requirements`, {
      method: 'POST', token: adminToken,
      body: { trainingId: training.id, versionPolicy: 'LATEST_PUBLISHED', required: true, applyToCurrentEmployees: true },
      expected: [201],
    })
  }
  const jobActivities = await api(`/jobs/${job.id}/activities`, { token: adminToken, expected: [200] })
  if (!jobActivities.data.some((item) => item.activity?.id === activity.id)) {
    await api(`/jobs/${job.id}/activities`, {
      method: 'POST', token: adminToken,
      body: { activityId: activity.id, applyToCurrentEmployees: true },
      expected: [201],
    })
  }
  const assignment = await waitFor('automatic assignment', async () => {
    const assignments = await listAll(`/training-assignments?employeeId=${employeeId}&trainingId=${training.id}`, adminToken)
    return assignments.find((item) => item.origin === 'JOB' || item.origin === 'ACTIVITY') ?? null
  }, { timeoutMs: 30_000, intervalMs: 500 })
  return { trainingId: training.id, versionId: version.id, activityId: activity.id, assignmentId: assignment.id }
}

async function main() {
  await waitForHealth()
  const admin = await login(adminEmail, adminPassword)
  const unit = await findOrCreate('/units', '/units', (item) => item.code === 'ACCEPTANCE',
    { name: 'Unidade de aceite', code: 'ACCEPTANCE', status }, admin.accessToken)
  const sector = await findOrCreate('/sectors', '/sectors', (item) => item.code === 'ACCEPTANCE-SECTOR',
    { unitId: unit.id, name: 'Setor de aceite', code: 'ACCEPTANCE-SECTOR', status }, admin.accessToken)
  const job = await findOrCreate('/jobs', '/jobs', (item) => item.name === 'Colaborador de aceite',
    { name: 'Colaborador de aceite', description: 'Cargo fictício da fixture.', status }, admin.accessToken)
  const employees = await listAll('/employees', admin.accessToken)
  let employee = employees.find((item) => item.registration === 'ACCEPTANCE-001')
  if (!employee) {
    employee = (await api('/employees', {
      method: 'POST', token: admin.accessToken,
      body: { name: 'Colaborador Demo', registration: 'ACCEPTANCE-001', email: employeeEmail, jobId: job.id, sectorId: sector.id, unitId: unit.id, status },
      expected: [201],
    })).data
  }

  const managerUser = await ensureUser({ email: managerEmail, password: managerPassword, role: 'MANAGER', employeeId: null, adminToken: admin.accessToken })
  const employeeUser = await ensureUser({ email: employeeEmail, password: employeePassword, role: 'EMPLOYEE', employeeId: employee.id, adminToken: admin.accessToken })
  await ensureManagerScope(managerUser.id, unit.id, admin.accessToken)
  const trainingData = await ensureTraining(admin.accessToken)
  const automatic = await ensureAutomaticScenario(admin.accessToken, job, employee.id, trainingData.video)
  const assignmentBody = {
    employeeId: employee.id, trainingId: trainingData.training.id, trainingVersionId: trainingData.version.id,
    origin: 'EMPLOYEE', dueDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
    priority: 'NORMAL', idempotencyKey: 'acceptance-assignment-v1',
  }
  const assignmentResponse = await api('/training-assignments', {
    method: 'POST', token: admin.accessToken, headers: { 'Idempotency-Key': assignmentBody.idempotencyKey }, body: assignmentBody, expected: [201],
  })
  const assignmentRepeat = await api('/training-assignments', {
    method: 'POST', token: admin.accessToken, headers: { 'Idempotency-Key': assignmentBody.idempotencyKey }, body: assignmentBody, expected: [201],
  })
  if (assignmentResponse.data.id !== assignmentRepeat.data.id) throw new Error('Assignment idempotency did not reuse the original assignment')

  const qrResponse = await api(`/employees/${employee.id}/qr-code`, { method: 'POST', token: admin.accessToken, expected: [201] })
  const state = {
    generatedAt: new Date().toISOString(),
    admin: { email: adminEmail }, manager: { id: managerUser.id, email: managerEmail }, employee: { id: employee.id, userId: employeeUser.id, email: employeeEmail },
    unit: { id: unit.id }, sector: { id: sector.id }, job: { id: job.id }, training: { id: trainingData.training.id, versionId: trainingData.version.id, moduleId: trainingData.module.id, videoId: trainingData.video?.id, questionnaireId: trainingData.questionnaire?.id, questionId: trainingData.question?.id, correctOptionId: trainingData.correctOption?.id, incorrectOptionId: trainingData.incorrectOption?.id }, automatic,
    assignment: { id: assignmentResponse.data.id, idempotencyKey: assignmentBody.idempotencyKey }, qr: { id: qrResponse.data.id, token: qrResponse.data.token, verificationUrl: qrResponse.data.verificationUrl },
  }
  await writeArtifact('demo-state.json', state)
  await writeArtifact('seed-result.json', { status: 'passed', generatedAt: state.generatedAt, ids: { employeeId: employee.id, trainingId: trainingData.training.id, assignmentId: assignmentResponse.data.id, qrCodeId: qrResponse.data.id } })
  console.log(JSON.stringify({ status: 'passed', assignmentId: assignmentResponse.data.id, trainingId: trainingData.training.id }, null, 2))
}

main().catch(async (error) => {
  await writeArtifact('seed-error.json', { status: 'failed', message: error.message, details: error.details }).catch(() => {})
  console.error(error.message)
  process.exitCode = 1
})
