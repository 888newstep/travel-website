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
