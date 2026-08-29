import { describe, expect, it } from 'vitest'
import { buildDashboardPath } from './pages-dashboard'
import { buildManagementPath } from './pages-management'

describe('dashboard filters', () => {
  it('preserves shared filters and database pagination in a tab request', () => {
    const current = new URLSearchParams('unitId=unit-1&status=EXPIRED&periodFrom=2026-01-01&page=3&ignored=value')
    expect(buildDashboardPath('treinamentos', current)).toBe(
      '/admin/dashboard/trainings?unitId=unit-1&status=EXPIRED&periodFrom=2026-01-01&page=3&size=20',
    )
  })

  it('does not send pagination for the overview', () => {
    expect(buildDashboardPath('geral', new URLSearchParams('page=4&trainingId=training-1'))).toBe(
      '/admin/dashboard/overview?trainingId=training-1',
    )
  })
})

describe('management filters', () => {
  it('maps certificate dates to the backend contract and discards unsupported filters', () => {
    expect(buildManagementPath('certificates', '/certificates', 2, {
      status: 'ACTIVE', from: '2026-08-01', to: '2026-08-31', recipient: 'ignored@example.test',
    })).toBe('/certificates?page=2&size=15&status=ACTIVE&issuedFrom=2026-08-01&issuedTo=2026-08-31')
  })
})
