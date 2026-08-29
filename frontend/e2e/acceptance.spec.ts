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
  await page.locator('input[autocomplete="current-password"]').fill(user.password)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page).toHaveURL(/\/(?:admin|equipe|meu)\/dashboard$/)
}

function acceptanceState() {
  return JSON.parse(readFileSync(resolve(process.cwd(), '..', 'acceptance-artifacts', 'demo-state.json'), 'utf8')) as {
    assignment: { id: string }
    training: { videoId: string; questionnaireId: string }
    browser: { assignmentId: string; videoId: string; questionnaireId: string }
    qr: { token: string }
  }
}

test.describe('technical acceptance by role', () => {
  test('employee completes the exclusive training with real video and opens the certificate', async ({ page }) => {
    test.setTimeout(90_000)
    const state = acceptanceState()
    await login(page, users.employee)

    await page.goto(`/meu/atribuicoes/${state.browser.assignmentId}`)
    await expect(page.getByRole('heading', { name: 'Treinamento navegador de aceite' })).toBeVisible()
    const start = page.getByRole('button', { name: 'Iniciar treinamento' })
    if (await start.isVisible()) await start.click()
    await page.getByRole('link', { name: /Continuar treinamento/ }).click()
    await page.getByRole('button', { name: 'Carregar vídeo protegido' }).click()

    const video = page.locator('video')
    await expect(video).toBeVisible()
    await video.evaluate(async (element: HTMLVideoElement) => {
      await element.play()
    })
    await expect.poll(() => video.evaluate((element: HTMLVideoElement) => element.currentTime), {
      timeout: 20_000,
      message: 'the protected MP4 should play beyond the 80% threshold',
    }).toBeGreaterThanOrEqual(8.2)
    await video.evaluate((element: HTMLVideoElement) => element.pause())
    await expect(page.getByText('Vídeo obrigatório concluído.')).toBeVisible({ timeout: 15_000 })

    await page.goto(`/meu/atribuicoes/${state.browser.assignmentId}/questionarios/${state.browser.questionnaireId}`)
    await page.getByText('Vídeo reproduzido e avaliação aprovada').click()
    await page.getByRole('button', { name: /Enviar respostas/ }).click()
    await expect(page.getByRole('heading', { name: 'Avaliação aprovada' })).toBeVisible()

    await page.goto('/meu/certificados')
    const certificate = page.locator('a[href^="/meu/certificados/"]').first()
    await expect(certificate).toBeVisible({ timeout: 30_000 })
    await certificate.click()
    await expect(page.getByRole('heading', { name: 'Certificado de conclusão' })).toBeVisible()
    await expect(page.getByRole('button', { name: /Baixar PDF/ })).toBeVisible()
  })

  test('admin can access operational dashboard and audit', async ({ page }) => {
    await login(page, users.admin)
    await expect(page).toHaveURL(/\/admin\/dashboard$/)
    await expect(page.getByRole('heading', { name: 'Riscos que pedem decisão' })).toBeVisible()

    await page.goto('/admin/treinamentos')
    await expect(page.getByRole('heading', { name: 'Treinamentos' })).toBeVisible()
    await page.goto('/admin/auditoria')
    await expect(page.getByRole('heading', { name: 'Auditoria' })).toBeVisible()
  })

  test('admin dashboard keeps tabs and filters usable on a mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await login(page, users.admin)
    await expect(page.getByRole('tablist', { name: 'Visões do dashboard' })).toBeVisible()
    await page.getByRole('tab', { name: 'Treinamentos' }).click()
    await expect(page).toHaveURL(/visao=treinamentos/)
    await expect(page.getByRole('region', { name: 'Filtros compartilhados' })).toBeVisible()
    await expect(page.getByLabel('Unidade')).toBeVisible()
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
    await expect(page.getByText(/Questionário de aceite|Nova tentativa indisponível|A avaliação não está disponível neste estado/)).toBeVisible()

    await page.goto('/meu/certificados')
    await expect(page.getByRole('heading', { name: 'Meus certificados' })).toBeVisible()
    await expect(page.getByText(/Certificado /).first()).toBeVisible()

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

    await page.goto(`/verificar/${encodeURIComponent(state.qr.token)}`)
    await expect(page.getByRole('heading', { name: 'Colaborador Demo' })).toBeVisible()

    await page.goto('/admin/dashboard')
    await expect(page).toHaveURL(/\/erro\/403$/)
  })

  test('QR deep link returns to verification after management login', async ({ page }) => {
    const state = acceptanceState()
    await page.goto(`/verificar/${encodeURIComponent(state.qr.token)}`)
    await expect(page).toHaveURL(/\/login$/)

    await page.getByLabel('E-mail corporativo').fill(users.manager.email)
    await page.locator('input[autocomplete="current-password"]').fill(users.manager.password)
    await page.getByRole('button', { name: 'Entrar' }).click()

    await expect(page).toHaveURL(new RegExp(`/verificar/${encodeURIComponent(state.qr.token)}$`))
    await expect(page.getByRole('heading', { name: 'Colaborador Demo' })).toBeVisible()
  })
})
