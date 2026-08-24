import { describe, expect, it } from 'vitest'
import { homeForRole } from './auth'
import { formatDate, labelForStatus } from './components'
import { buildPagedPath, buildStatusPayload } from './pages-organizational'

describe('frontend integration rules', () => {
  it('routes each authenticated profile to its permitted dashboard', () => {
    expect(homeForRole('EMPLOYEE')).toBe('/meu/dashboard')
    expect(homeForRole('MANAGER')).toBe('/equipe/dashboard')
    expect(homeForRole('SUPERVISOR')).toBe('/equipe/dashboard')
    expect(homeForRole('ADMIN')).toBe('/admin/dashboard')
  })

  it('presents domain statuses with text instead of relying only on color', () => {
    expect(labelForStatus('AVAILABLE')).toBe('Liberada')
    expect(labelForStatus('BLOCKED')).toBe('Bloqueada')
    expect(labelForStatus('EXPIRING')).toBe('Vencendo')
    expect(labelForStatus('NOT_ASSIGNED')).toBe('Não atribuída')
  })

  it('formats ISO dates in Brazilian Portuguese', () => {
    expect(formatDate('2026-07-28')).toContain('2026')
  })

  it('keeps organizational list filters and pagination on the backend contract', () => {
    expect(buildPagedPath('/activities', 2, { search: 'ponte', status: 'ACTIVE' })).toBe(
      '/activities?page=2&size=15&sort=name%2Casc&search=ponte&status=ACTIVE',
    )
    expect(buildStatusPayload('INACTIVE')).toEqual({ status: 'INACTIVE' })
  })
})
