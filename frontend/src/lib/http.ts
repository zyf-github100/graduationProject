import type { LoginResponse } from '../types'

const CONFIGURED_API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
const API_BASE_URL = import.meta.env.DEV ? '' : CONFIGURED_API_BASE_URL
const ACCESS_TOKEN_KEY = 'school-erp.access-token'
const REFRESH_TOKEN_KEY = 'school-erp.refresh-token'

interface ApiEnvelope<T> {
  code: string
  message: string
  data: T
  requestId: string
  timestamp: string
}

interface RequestOptions {
  body?: BodyInit | null
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  query?: Record<string, unknown>
  skipAuthRefresh?: boolean
  withAuth?: boolean
}

export class HttpError extends Error {
  code?: string
  requestId?: string
  status: number

  constructor(message: string, status: number, code?: string, requestId?: string) {
    super(message)
    this.name = 'HttpError'
    this.status = status
    this.code = code
    this.requestId = requestId
  }
}

let refreshPromise: Promise<void> | null = null

const readStorageValue = (key: string) => {
  if (typeof window === 'undefined') {
    return null
  }

  return window.sessionStorage.getItem(key) ?? window.localStorage.getItem(key)
}

const resolveStorageTarget = () => {
  if (typeof window === 'undefined') {
    return null
  }

  if (window.sessionStorage.getItem(ACCESS_TOKEN_KEY)) {
    return window.sessionStorage
  }

  return window.localStorage
}

const updateStoredSession = (session: Pick<LoginResponse, 'accessToken' | 'refreshToken'>) => {
  const target = resolveStorageTarget()
  if (!target) {
    return
  }

  target.setItem(ACCESS_TOKEN_KEY, session.accessToken)
  target.setItem(REFRESH_TOKEN_KEY, session.refreshToken)
}

export const getStoredAccessToken = () => readStorageValue(ACCESS_TOKEN_KEY)

export const getStoredRefreshToken = () => readStorageValue(REFRESH_TOKEN_KEY)

export const storeAuthSession = (
  session: Pick<LoginResponse, 'accessToken' | 'refreshToken'>,
  remember = true,
) => {
  if (typeof window === 'undefined') {
    return
  }

  const target = remember ? window.localStorage : window.sessionStorage
  const other = remember ? window.sessionStorage : window.localStorage

  other.removeItem(ACCESS_TOKEN_KEY)
  other.removeItem(REFRESH_TOKEN_KEY)
  target.setItem(ACCESS_TOKEN_KEY, session.accessToken)
  target.setItem(REFRESH_TOKEN_KEY, session.refreshToken)
}

export const clearStoredSession = () => {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.removeItem(ACCESS_TOKEN_KEY)
  window.localStorage.removeItem(REFRESH_TOKEN_KEY)
  window.sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  window.sessionStorage.removeItem(REFRESH_TOKEN_KEY)
}

const buildUrl = (path: string, query?: Record<string, unknown>) => {
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin)

  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') {
        return
      }

      if (Array.isArray(value)) {
        value.forEach((item) => {
          url.searchParams.append(key, String(item))
        })
        return
      }

      url.searchParams.set(key, String(value))
    })
  }

  return url.toString()
}

const parseEnvelope = async <T>(response: Response) => {
  const text = await response.text()
  if (!text) {
    return null
  }

  return JSON.parse(text) as ApiEnvelope<T>
}

const refreshAccessToken = async () => {
  const refreshToken = getStoredRefreshToken()
  if (!refreshToken) {
    clearStoredSession()
    throw new HttpError('登录状态已失效，请重新登录。', 401, 'UNAUTHORIZED')
  }

  const response = await fetch(buildUrl('/api/v1/auth/token/refresh'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken }),
  })

  const payload = await parseEnvelope<LoginResponse>(response)
  if (!response.ok || !payload || payload.code !== 'SUCCESS') {
    clearStoredSession()
    throw new HttpError(
      payload?.message ?? '登录状态已失效，请重新登录。',
      response.status || 401,
      payload?.code,
      payload?.requestId,
    )
  }

  updateStoredSession(payload.data)
}

const ensureFreshAccessToken = async () => {
  refreshPromise ??= refreshAccessToken().finally(() => {
    refreshPromise = null
  })

  return refreshPromise
}

export const request = async <T>(path: string, options: RequestOptions = {}): Promise<T> => {
  const headers = new Headers()
  const method = options.method ?? 'GET'

  if (options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.withAuth !== false) {
    const accessToken = getStoredAccessToken()
    if (accessToken) {
      headers.set('Authorization', `Bearer ${accessToken}`)
    }
  }

  const response = await fetch(buildUrl(path, options.query), {
    method,
    headers,
    body: options.body ?? null,
  })

  if (response.status === 401 && options.withAuth !== false && !options.skipAuthRefresh) {
    await ensureFreshAccessToken()
    return request<T>(path, {
      ...options,
      skipAuthRefresh: true,
    })
  }

  const payload = await parseEnvelope<T>(response)
  if (!response.ok || !payload || payload.code !== 'SUCCESS') {
    throw new HttpError(
      payload?.message ?? '请求失败，请稍后重试。',
      response.status,
      payload?.code,
      payload?.requestId,
    )
  }

  return payload.data
}
