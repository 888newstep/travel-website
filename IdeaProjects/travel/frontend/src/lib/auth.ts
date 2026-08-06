export function getStoredToken() {
  return localStorage.getItem('token')
}

export function getStoredUsername() {
  return localStorage.getItem('username') || ''
}

export function notifyAuthChanged() {
  window.dispatchEvent(new Event('auth-change'))
}

export function persistAuth(payload: unknown) {
  if (!payload) {
    return false
  }

  if (typeof payload === 'string') {
    localStorage.setItem('token', payload)
    notifyAuthChanged()
    return true
  }

  if (typeof payload === 'object') {
    const record = payload as Record<string, unknown>
    const token = record.token || record.accessToken || record.jwt || record.data

    if (typeof token === 'string' && token) {
      localStorage.setItem('token', token)
    }

    const username = record.username || record.userName || record.nickname
    if (typeof username === 'string' && username) {
      localStorage.setItem('username', username)
    }

    notifyAuthChanged()
    return typeof token === 'string' && token.length > 0
  }

  return false
}

export function clearStoredAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  notifyAuthChanged()
}
