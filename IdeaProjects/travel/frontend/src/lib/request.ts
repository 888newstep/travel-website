export const DEFAULT_REQUEST_TIMEOUT_MS = 12000

export class RequestTimeoutError extends Error {
  constructor(message: string = 'Request timed out') {
    super(message)
    this.name = 'RequestTimeoutError'
  }
}

export class NetworkError extends Error {
  constructor(message: string = 'Network error') {
    super(message)
    this.name = 'NetworkError'
  }
}

export class AuthError extends Error {
  constructor(message: string = 'Authentication required') {
    super(message)
    this.name = 'AuthError'
  }
}

export function withRequestTimeout<T>(promise: Promise<T>, timeoutMs: number = DEFAULT_REQUEST_TIMEOUT_MS): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timeoutId = window.setTimeout(() => {
      reject(new RequestTimeoutError())
    }, timeoutMs)

    promise
      .then((value) => {
        window.clearTimeout(timeoutId)
        resolve(value)
      })
      .catch((error) => {
        window.clearTimeout(timeoutId)
        reject(error)
      })
  })
}

export function isRequestTimeoutError(error: unknown) {
  return error instanceof RequestTimeoutError
}

export function isNetworkError(error: unknown) {
  return error instanceof NetworkError || (error instanceof Error && error.message.includes('Network Error'))
}

export function isAuthError(error: unknown) {
  return error instanceof AuthError
}

export function getErrorMessage(error: unknown): string {
  if (error instanceof RequestTimeoutError) {
    return '请求处理超时，请稍后重试'
  }
  if (error instanceof NetworkError) {
    return '网络连接异常，请检查网络后重试'
  }
  if (error instanceof AuthError) {
    return '登录状态已失效，请重新登录'
  }
  if (error instanceof Error) {
    return error.message || '请求失败'
  }
  return '请求失败'
}

export async function retryRequest<T>(
  fn: () => Promise<T>,
  options: { maxRetries?: number; delayMs?: number; shouldRetry?: (error: unknown) => boolean } = {}
): Promise<T> {
  const { maxRetries = 2, delayMs = 1000, shouldRetry } = options

  let lastError: unknown

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await fn()
    } catch (error) {
      lastError = error

      if (attempt === maxRetries) {
        break
      }

      if (shouldRetry && !shouldRetry(error)) {
        break
      }

      if (isRequestTimeoutError(error) || isNetworkError(error)) {
        await new Promise((resolve) => setTimeout(resolve, delayMs * (attempt + 1)))
        continue
      }

      break
    }
  }

  throw lastError
}
