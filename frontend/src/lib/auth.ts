import { useSyncExternalStore } from 'react'

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

export function subscribeAuth(onStoreChange: () => void) {
  window.addEventListener('storage', onStoreChange)
  window.addEventListener('auth-change', onStoreChange)
  return () => {
    window.removeEventListener('storage', onStoreChange)
    window.removeEventListener('auth-change', onStoreChange)
  }
}

/** 订阅登录态：登录、退出（含其他标签页）都会触发依赖它的组件重渲染。 */
export function useStoredToken() {
  return useSyncExternalStore(subscribeAuth, getStoredToken, () => null)
}
