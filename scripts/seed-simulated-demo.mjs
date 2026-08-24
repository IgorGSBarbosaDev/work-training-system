import { readFile } from 'node:fs/promises'
import {
  api,
  listAll,
  login,
  resetPasswordFromMailpit,
  waitFor,
  waitForHealth,
  writeArtifact,
  ARTIFACT_DIR,
} from './acceptance/http.mjs'

const status = 'ACTIVE'
const seed = process.env.SIMULATED_SEED ?? '20260824'
const namespace = process.env.SIMULATED_NAMESPACE ?? `SIM-${seed}`
const slug = namespace.toLowerCase().replace(/[^a-z0-9]+/g, '-')
const password = process.env.SIMULATED_PASSWORD ?? 'ChangeMe-Simulated-2026!'
const adminEmail = process.env.DEMO_ADMIN_EMAIL ?? 'admin@example.test'
const adminPassword = process.env.DEMO_ADMIN_PASSWORD ?? 'ChangeMe-Admin-2026!'

function isoDate(daysFromToday) {
  const value = new Date()
  value.setUTCDate(value.getUTCDate() + daysFromToday)
  return value.toISOString().slice(0, 10)
}

function isoInstant(daysFromToday) {
  const value = new Date()
  value.setUTCDate(value.getUTCDate() + daysFromToday)
  return value.toISOString()
}

function pageItems(data) {
  return Array.isArray(data) ? data : data?.content ?? data?.items ?? data?.data ?? data?.results ?? []
}

async function ensureNew(path, predicate, body, token, label) {
  const existing = (await listAll(path, token)).find(predicate)
  if (existing) return existing
  return (await api(path, { method: 'POST', token, body, expected: [201] })).data
}

async function ensureUser({ email, role, employeeId, adminToken }) {
  const existing = (await listAll('/users', adminToken)).find((item) => item.email?.toLowerCase() === email.toLowerCase())
  if (existing) throw new Error(`Usuário ${email} já existe. Use outro SIMULATED_SEED ou remova a fixture ${namespace}.`)
  const user = (await api('/users', {
    method: 'POST',
    token: adminToken,
    body: { email, role, employeeId, sendActivationEmail: true },
    expected: [201],
  })).data
  await resetPasswordFromMailpit(user.id, email, password, adminToken)
  return user
}

async function createTraining(index, adminToken) {
  const training = (await api('/trainings', {
    method: 'POST',
    token: adminToken,
    body: {
      name: `${namespace} - Treinamento ${index + 1}`,
      code: `${namespace}-TRAINING-${index + 1}`,
      description: `Treinamento simulado ${index + 1} para validação do produto.`,
      category: index === 0 ? 'Segurança' : index === 1 ? 'Operações' : 'Conformidade',
      isRegulatoryStandard: index === 2,
      status,
      initialVersion: {
        workloadMinutes: 10 + index * 5,
        validityType: index === 0 ? 'MONTHS' : 'DAYS',
        validityValue: index === 0 ? 12 : index === 1 ? 30 : 90,
        passingScore: 70,
        maxAttempts: 3,
        retryIntervalMinutes: 0,
      },
    },
    expected: [201],
  })).data
  const versions = (await api(`/trainings/${training.id}/versions`, { token: adminToken, expected: [200] })).data
  const version = versions.find((item) => item.status === 'DRAFT') ?? versions[0]
  const module = (await api(`/training-versions/${version.id}/modules`, {
    method: 'POST',
    token: adminToken,
    body: { title: `Módulo ${index + 1}`, description: 'Conteúdo simulado.', order: 1, status },
    expected: [201],
  })).data

  const fixture = Buffer.from(`simulated-demo-video-${namespace}-${index}\n`, 'utf8')
  const upload = (await api('/uploads', {
    method: 'POST',
    token: adminToken,
    body: {
      purpose: 'TRAINING_VIDEO',
      fileName: `${slug}-${index + 1}.mp4`,
      contentType: 'video/mp4',
      sizeBytes: fixture.length,
    },
    expected: [201],
  })).data
  const uploadHeaders = { ...(upload.requiredHeaders ?? {}), 'Content-Type': 'video/mp4' }
  const binaryResponse = await fetch(upload.uploadUrl, {
    method: upload.method ?? 'PUT',
    headers: uploadHeaders,
    body: fixture,
  })
  if (!binaryResponse.ok) {
    const uploadError = await binaryResponse.text()
    throw new Error(`Upload do vídeo simulado falhou com HTTP ${binaryResponse.status}: ${uploadError.slice(0, 300)}`)
  }
  const completedUpload = await api(`/uploads/${upload.uploadId}/complete`, { method: 'POST', token: adminToken, expected: [200] })
  const video = (await api(`/modules/${module.id}/videos`, {
    method: 'POST',
    token: adminToken,
    body: {
      title: `Vídeo simulado ${index + 1}`,
      description: 'Arquivo de fixture para testar o fluxo de upload e progresso.',
      order: 1,
      durationSeconds: 10,
      storageObjectKey: completedUpload.data.objectKey ?? upload.objectKey,
      required: true,
      status,
      fileId: upload.fileId,
    },
    expected: [201],
  })).data
  const questionnaire = (await api(`/modules/${module.id}/questionnaire`, {
    method: 'POST',
    token: adminToken,
    body: {
      title: `Questionário simulado ${index + 1}`,
      passingScore: 70,
      maxAttempts: 3,
      retryIntervalMinutes: 0,
      shuffleQuestions: false,
      status,
    },
    expected: [201],
  })).data
  const question = (await api(`/questionnaires/${questionnaire.id}/questions`, {
    method: 'POST',
    token: adminToken,
    body: { statement: `Qual é a resposta correta do treinamento ${index + 1}?`, order: 1, status },
    expected: [201],
  })).data
  const correctOption = (await api(`/questions/${question.id}/options`, {
    method: 'POST',
    token: adminToken,
    body: { text: 'Resposta correta', correct: true, order: 1, status },
    expected: [201],
  })).data
  const incorrectOption = (await api(`/questions/${question.id}/options`, {
    method: 'POST',
    token: adminToken,
    body: { text: 'Resposta incorreta', correct: false, order: 2, status },
    expected: [201],
  })).data
  const publishedVersion = (await api(`/training-versions/${version.id}/publish`, {
    method: 'POST',
    token: adminToken,
    expected: [200],
  })).data
  return { training, version: publishedVersion, module, video, questionnaire, question, correctOption, incorrectOption }
}

async function createAssignment(employeeId, training, index, adminToken, origin = 'EMPLOYEE') {
  const existing = (await listAll(`/training-assignments?employeeId=${employeeId}&trainingId=${training.training.id}`, adminToken)).find((item) =>
    item.employee?.id === employeeId
    && item.training?.id === training.training.id
    && item.trainingVersionId === training.version.id
    && !['CANCELLED', 'WAIVED'].includes(item.status))
  if (existing) return existing

  const idempotencyKey = `${slug}-assignment-${index}`
  const response = await api('/training-assignments', {
    method: 'POST',
    token: adminToken,
    headers: { 'Idempotency-Key': idempotencyKey },
    body: {
      employeeId,
      trainingId: training.training.id,
      trainingVersionId: training.version.id,
      origin,
      dueDate: isoDate(index % 3 === 0 ? 3 : 30),
      priority: index % 4 === 0 ? 'HIGH' : 'NORMAL',
      idempotencyKey,
    },
    expected: [201],
  })
  return response.data
}

async function executeAssignment(assignment, training, employeeToken, optionId, label) {
  await api(`/training-assignments/${assignment.id}/start`, { method: 'POST', token: employeeToken, expected: [200] })
  await api(`/training-assignments/${assignment.id}/videos/${training.video.id}/progress`, {
    method: 'PUT',
    token: employeeToken,
    body: {
      positionSeconds: 2,
      watchedSeconds: 2,
      reportedPercentage: 20,
      eventAt: new Date().toISOString(),
      finalEvent: false,
    },
    expected: [200],
  })
  await new Promise((resolve) => setTimeout(resolve, 4_000))
  await api(`/training-assignments/${assignment.id}/videos/${training.video.id}/progress`, {
    method: 'PUT',
    token: employeeToken,
    body: {
      positionSeconds: 10,
      watchedSeconds: 10,
      reportedPercentage: 100,
      eventAt: new Date().toISOString(),
      finalEvent: true,
    },
    expected: [200],
  })
  const attempt = await api(`/training-assignments/${assignment.id}/questionnaires/${training.questionnaire.id}/attempts`, {
    method: 'POST',
    token: employeeToken,
    headers: { 'Idempotency-Key': `${slug}-${label}-attempt` },
    body: { answers: [{ questionId: training.question.id, answerOptionId: optionId }] },
    expected: [201, 200],
  })
  return { assignmentId: assignment.id, attempt: attempt.data }
}

async function manualCompletion(employeeId, training, completedAt, validityType, validityValue, adminToken, label) {
  const response = await api('/training-completions/manual', {
    method: 'POST',
    token: adminToken,
    body: {
      employeeId,
      trainingId: training.training.id,
      trainingVersionId: training.version.id,
      completedAt,
      score: 92,
      validityType,
      validityValue,
      notes: `Conclusão manual simulada: ${label}.`,
    },
    expected: [201],
  })
  const recalculated = await api(`/training-completions/${response.data.id}/recalculate-expiration`, {
    method: 'POST',
    token: adminToken,
    expected: [200],
  })
  return recalculated.data
}

async function main() {
  await waitForHealth()
  let previous
  try {
    previous = JSON.parse(await readFile(`${ARTIFACT_DIR}/simulated-demo-state.json`, 'utf8'))
  } catch {
    previous = null
  }
  if (previous?.namespace === namespace) {
    console.log(JSON.stringify({ status: 'already-seeded', namespace, state: `${ARTIFACT_DIR}/simulated-demo-state.json` }, null, 2))
    return
  }

  const admin = await login(adminEmail, adminPassword)
  const existingTraining = (await listAll('/trainings', admin.accessToken)).find((item) => item.code?.startsWith(`${namespace}-`))
  if (existingTraining) throw new Error(`A fixture ${namespace} já existe, mas o artefato de estado não foi encontrado.`)

  const fixtureAdmin = await ensureUser({
    email: `${slug}-admin@example.test`, role: 'ADMIN', employeeId: null, adminToken: admin.accessToken,
  })

  const units = []
  for (let index = 0; index < 3; index += 1) {
    units.push(await ensureNew('/units', (item) => item.code === `${namespace}-UNIT-${index + 1}`, {
      name: `${namespace} - Unidade ${index + 1}`, code: `${namespace}-UNIT-${index + 1}`, status,
    }, admin.accessToken, 'Unidade'))
  }
  const sectors = []
  for (let index = 0; index < 6; index += 1) {
    const unit = units[index % units.length]
    sectors.push(await ensureNew('/sectors', (item) => item.code === `${namespace}-S-${index + 1}`, {
      unitId: unit.id, name: `${namespace} - Setor ${index + 1}`, code: `${namespace}-S-${index + 1}`, status,
    }, admin.accessToken, 'Setor'))
  }
  const jobs = []
  for (let index = 0; index < 6; index += 1) {
    jobs.push(await ensureNew('/jobs', (item) => item.name === `${namespace} - Cargo ${index + 1}`, {
      name: `${namespace} - Cargo ${index + 1}`, description: 'Cargo simulado para filtros e escopos.', status,
    }, admin.accessToken, 'Cargo'))
  }

  const employees = []
  for (let index = 0; index < 12; index += 1) {
    employees.push((await api('/employees', {
      method: 'POST',
      token: admin.accessToken,
      body: {
        name: `${namespace} - Colaborador ${String(index + 1).padStart(2, '0')}`,
        registration: `${namespace}-EMP-${String(index + 1).padStart(3, '0')}`,
        email: `${slug}-employee-${index + 1}@example.test`,
        jobId: jobs[index % jobs.length].id,
        sectorId: sectors[index % sectors.length].id,
        unitId: units[index % units.length].id,
        status,
      },
      expected: [201],
    })).data)
  }

  const manager = await ensureUser({ email: `${slug}-manager@example.test`, role: 'MANAGER', employeeId: null, adminToken: admin.accessToken })
  const supervisor = await ensureUser({ email: `${slug}-supervisor@example.test`, role: 'SUPERVISOR', employeeId: null, adminToken: admin.accessToken })
  const employeeUsers = []
  for (let index = 0; index < employees.length; index += 1) {
    employeeUsers.push(await ensureUser({
      email: employees[index].email,
      role: 'EMPLOYEE',
      employeeId: employees[index].id,
      adminToken: admin.accessToken,
    }))
  }
  await api(`/users/${manager.id}/permissions`, {
    method: 'PATCH', token: admin.accessToken,
    body: { permissions: ['ASSIGN_TRAINING'], scopes: [{ type: 'UNIT', targetId: units[0].id }] }, expected: [200],
  })
  await api(`/users/${supervisor.id}/permissions`, {
    method: 'PATCH', token: admin.accessToken,
    body: { permissions: ['ASSIGN_TRAINING'], scopes: [{ type: 'SECTOR', targetId: sectors[0].id }, { type: 'SECTOR', targetId: sectors[1].id }] }, expected: [200],
  })

  const employeeTokens = []
  for (let index = 0; index < employees.length; index += 1) {
    employeeTokens.push((await login(employees[index].email, password)).accessToken)
  }
  const trainings = []
  for (let index = 0; index < 3; index += 1) trainings.push(await createTraining(index, admin.accessToken))

  const activities = []
  for (let index = 0; index < trainings.length; index += 1) {
    const activity = (await api('/activities', {
      method: 'POST', token: admin.accessToken,
      body: { name: `${namespace} - Atividade ${index + 1}`, description: 'Atividade simulada vinculada a um treinamento.', status },
      expected: [201],
    })).data
    await api(`/activities/${activity.id}/requirements`, {
      method: 'POST', token: admin.accessToken,
      body: { trainingId: trainings[index].training.id, versionPolicy: 'LATEST_PUBLISHED', required: true, applyToCurrentEmployees: false },
      expected: [201],
    })
    await api(`/jobs/${jobs[index].id}/activities`, {
      method: 'POST', token: admin.accessToken,
      body: { activityId: activity.id, applyToCurrentEmployees: true },
      expected: [201],
    })
    activities.push(activity)
  }

  const assignments = {}
  assignments.notStarted = await createAssignment(employees[0].id, trainings[0], 1, admin.accessToken)
  assignments.inProgress = await createAssignment(employees[1].id, trainings[0], 2, admin.accessToken)
  await api(`/training-assignments/${assignments.inProgress.id}/start`, { method: 'POST', token: employeeTokens[1], expected: [200] })
  assignments.completed = await createAssignment(employees[2].id, trainings[0], 3, admin.accessToken)
  const completedExecution = await executeAssignment(assignments.completed, trainings[0], employeeTokens[2], trainings[0].correctOption.id, 'completed')
  assignments.failed = await createAssignment(employees[3].id, trainings[0], 4, admin.accessToken)
  const failedExecution = await executeAssignment(assignments.failed, trainings[0], employeeTokens[3], trainings[0].incorrectOption.id, 'failed')
  assignments.canceled = await createAssignment(employees[4].id, trainings[1], 5, admin.accessToken)
  assignments.canceled = (await api(`/training-assignments/${assignments.canceled.id}/cancel`, {
    method: 'POST', token: admin.accessToken, body: { reason: 'Cenário simulado de cancelamento.' }, expected: [200],
  })).data
  assignments.waived = await createAssignment(employees[5].id, trainings[1], 6, admin.accessToken)
  assignments.waived = (await api(`/training-assignments/${assignments.waived.id}/waive`, {
    method: 'POST', token: admin.accessToken, body: { reason: 'Cenário simulado de dispensa.' }, expected: [200],
  })).data

  const completions = {}
  completions.expired = await manualCompletion(employees[6].id, trainings[1], isoInstant(-45), 'DAYS', 30, admin.accessToken, 'expirada')
  completions.expiring = await manualCompletion(employees[7].id, trainings[1], isoInstant(-20), 'DAYS', 30, admin.accessToken, 'expirando em breve')
  completions.valid = await manualCompletion(employees[8].id, trainings[2], isoInstant(-5), 'MONTHS', 12, admin.accessToken, 'válida')

  for (const activity of activities) {
    await api(`/activities/${activity.id}/qualifications/recalculate`, {
      method: 'POST', token: admin.accessToken, expected: [202],
    })
  }
  const qualifications = []
  for (const employee of employees) {
    const response = await api(`/employees/${employee.id}/qualifications?size=100`, {
      token: admin.accessToken, expected: [200],
    })
    qualifications.push(...pageItems(response.data))
  }
  if (qualifications.length === 0) throw new Error('A fixture simulada não gerou qualificações.')

  const certificate = await waitFor('certificado simulado', async () => {
    const response = await api('/me/certificates', { token: employeeTokens[6], expected: [200] })
    return pageItems(response.data)[0] ?? null
  }, { timeoutMs: 30_000, intervalMs: 500 })
  const notifications = await waitFor('notificação simulado', async () => {
    const response = await api('/me/notifications?size=100', { token: employeeTokens[5], expected: [200] })
    return pageItems(response.data)[0] ?? null
  }, { timeoutMs: 30_000, intervalMs: 500 })
  const audit = await api('/audit-logs?size=100', { token: admin.accessToken, expected: [200] })
  const state = {
    generatedAt: new Date().toISOString(),
    namespace,
    credentials: {
      admin: { email: fixtureAdmin.email, passwordVariable: 'SIMULATED_PASSWORD' },
      bootstrapAdmin: { email: adminEmail, passwordVariable: 'DEMO_ADMIN_PASSWORD' },
      manager: { email: `${slug}-manager@example.test`, passwordVariable: 'SIMULATED_PASSWORD' },
      supervisor: { email: `${slug}-supervisor@example.test`, passwordVariable: 'SIMULATED_PASSWORD' },
      employees: employees.map((employee) => ({ id: employee.id, email: employee.email, passwordVariable: 'SIMULATED_PASSWORD' })),
    },
    units, sectors, jobs, employees,
    users: { admin: fixtureAdmin, manager, supervisor, employees: employeeUsers },
    trainings: trainings.map((item) => ({
      training: item.training,
      version: item.version,
      module: item.module,
      video: item.video,
      questionnaire: item.questionnaire,
      question: item.question,
      correctOption: item.correctOption,
      incorrectOption: item.incorrectOption,
    })),
    activities,
    qualifications: qualifications.map((item) => ({
      id: item.id,
      employeeId: item.employee?.id ?? item.employeeId,
      activityId: item.activity?.id ?? item.activityId,
      status: item.status,
      nextExpirationDate: item.nextExpirationDate,
      blockingReasons: item.blockingReasons,
    })),
    assignments: {
      notStarted: assignments.notStarted.id,
      inProgress: assignments.inProgress.id,
      completed: assignments.completed.id,
      failed: assignments.failed.id,
      canceled: assignments.canceled.id,
      waived: assignments.waived.id,
    },
    executions: { completed: completedExecution, failed: failedExecution },
    completions,
    evidence: {
      certificateId: certificate.id,
      notificationId: notifications.id,
      qualificationCount: qualifications.length,
      qualificationStatuses: Object.fromEntries([...new Set(qualifications.map((item) => item.status))]
        .map((status) => [status, qualifications.filter((item) => item.status === status).length])),
      auditCount: pageItems(audit.data).length,
    },
  }
  await writeArtifact('simulated-demo-state.json', state)
  await writeArtifact('simulated-demo-result.json', {
    status: 'passed', generatedAt: state.generatedAt, namespace,
    counts: {
      units: units.length,
      sectors: sectors.length,
      jobs: jobs.length,
      employees: employees.length,
      trainings: trainings.length,
      assignments: Object.keys(state.assignments).length,
      qualifications: qualifications.length,
    },
    evidence: state.evidence,
  })
  console.log(JSON.stringify({ status: 'passed', namespace, state: `${ARTIFACT_DIR}/simulated-demo-state.json`, password }, null, 2))
}

main().catch(async (error) => {
  await writeArtifact('simulated-demo-error.json', { status: 'failed', message: error.message, details: error.details }).catch(() => {})
  console.error(error.message)
  process.exitCode = 1
})
