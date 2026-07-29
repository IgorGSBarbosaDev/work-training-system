import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from './api'

export type RemoteState<T> = {
  data: T | null
  loading: boolean
  error: string
  reload: () => void
}

export function useApiData<T>(path: string | null): RemoteState<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(Boolean(path))
  const [error, setError] = useState('')
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!path) {
      setData(null)
      setLoading(false)
      return
    }

    let active = true
    setLoading(true)
    setError('')

    api<T>(path)
      .then((result) => {
        if (active) setData(result)
      })
      .catch((reason: unknown) => {
        if (!active) return
        setError(
          reason instanceof ApiError || reason instanceof Error
            ? reason.message
            : 'Não foi possível carregar os dados.',
        )
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [path, version])

  const reload = useCallback(() => setVersion((current) => current + 1), [])
  return { data, loading, error, reload }
}
