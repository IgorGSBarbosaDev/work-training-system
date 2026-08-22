import { expect, test, type Page } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const users = {
  admin: {
    email: process.env.DEMO_ADMIN_EMAIL ?? 'admin@example.test',
    password: process.env.DEMO_ADMIN_PASSWORD ?? 'ChangeMe-Admin-2026!',
  },
  manager: {
    email: process.env.DEMO_MANAGER_EMAIL ?? 'manager@example.test',
    password: process.env.DEMO_MANAGER_PASSWORD ?? 'ChangeMe-Manager-2026!',
  },
  employee: {
    email: process.env.DEMO_EMPLOYEE_EMAIL ?? 'employee@example.test',
    password: process.env.DEMO_EMPLOYEE_PASSWORD ?? 'ChangeMe-Employee-2026!',
  },
}

async function login(page: Page, user: { email: string; password: string }) {
  await page.goto('/login')
  await page.getByLabel('E-mail corporativo').fill(user.email)
  await page.getByLabel('Senha').fill(user.password)
  await page.getByRole('button', { name: 'Entrar' }).click()
}

function acceptanceState() {
  return JSON.parse(readFileSync(resolve(process.cwd(), '..', 'acceptance-artifacts', 'demo-state.json'), 'utf8')) as {
    assignment: { id: string }
    training: { videoId: string; questionnaireId: string }
    qr: { token: string }
  }
}

test.describe('technical acceptance by role', () => {
  test('admin can access operational dashboard and audit', async ({ page }) => {
    await login(page, users.admin)
    await expect(page).toHaveURL(/\/admin\/dashboard$/)
    await expect(page.getByText('Visão operacional consolidada')).toBeVisible()

    await page.goto('/admin/treinamentos')
    await expect(page.getByRole('heading', { name: 'Treinamentos' })).toBeVisible()
    await page.goto('/admin/auditoria')
    await expect(page.getByRole('heading', { name: 'Auditoria' })).toBeVisible()
  })

  test('employee can see assignments, qualifications and certificate area', async ({ page }) => {
    await login(page, users.employee)
    await expect(page).toHaveURL(/\/meu\/dashboard$/)
    await expect(page.getByText('Próximas ações')).toBeVisible()

    await page.goto('/meu/atribuicoes')
    await expect(page.getByRole('heading', { name: 'Minhas atribuições' })).toBeVisible()
    await page.goto('/meu/qualificacoes')
    await expect(page.getByRole('heading', { name: 'Minhas qualificações' })).toBeVisible()
  })

  test('employee can open the protected learning, questionnaire and evidence surfaces', async ({ page }) => {
    const state = acceptanceState()
    await login(page, users.employee)

    await page.goto(`/meu/atribuicoes/${state.assignment.id}`)
    await expect(page.getByRole('heading', { name: 'Treinamento de aceite técnico' })).toBeVisible()
    await expect(page.getByText('Seu progresso')).toBeVisible()

    await page.goto(`/meu/atribuicoes/${state.assignment.id}/videos/${state.training.videoId}`)
    await expect(page.getByRole('heading', { name: 'Vídeo de aceite' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Carregar vídeo protegido' })).toBeVisible()

    await page.goto(`/meu/atribuicoes/${state.assignment.id}/questionarios/${state.training.questionnaireId}`)
    await expect(page.getByText(/Questionário de aceite|Nova tentativa indisponível/)).toBeVisible()

    await page.goto('/meu/certificados')
    await expect(page.getByRole('heading', { name: 'Meus certificados' })).toBeVisible()
    await expect(page.getByText(/Certificado /)).toBeVisible()

    await page.goto('/meu/qr-code')
    await expect(page.getByRole('heading', { name: 'Meu QR Code' })).toBeVisible()
  })

  test('manager stays within the team scope', async ({ page }) => {
    const state = acceptanceState()
    await login(page, users.manager)
    await expect(page).toHaveURL(/\/equipe\/dashboard$/)
    await expect(page.getByText('Panorama da equipe')).toBeVisible()

    await page.goto(`/equipe/verificar-qr/${encodeURIComponent(state.qr.token)}`)
    await expect(page.getByRole('heading', { name: 'Colaborador Demo' })).toBeVisible()

    await page.goto('/admin/dashboard')
    await expect(page).toHaveURL(/\/erro\/403$/)
  })
})
