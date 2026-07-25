import { describe, expect, it } from 'vitest'
import { apiBaseUrl } from './api'

describe('api configuration', () => {
  it('uses the documented versioned API by default', () => {
    expect(apiBaseUrl).toBe('/api/v1')
  })
})
