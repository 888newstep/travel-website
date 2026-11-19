import { beforeEach, describe, expect, it } from 'vitest'
import { clearStoredAuth, getStoredToken, getStoredUsername, persistAuth } from './auth'

describe('auth 存储', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('persistAuth 支持多种 token 字段名', () => {
    expect(persistAuth({ token: 't1', username: 'u1' })).toBe(true)
    expect(getStoredToken()).toBe('t1')
    expect(getStoredUsername()).toBe('u1')

    expect(persistAuth({ accessToken: 't2' })).toBe(true)
    expect(getStoredToken()).toBe('t2')

    expect(persistAuth('raw-token')).toBe(true)
    expect(getStoredToken()).toBe('raw-token')
  })

  it('空 payload 与无 token 对象返回 false', () => {
    expect(persistAuth(null)).toBe(false)
    expect(persistAuth({ username: 'no-token' })).toBe(false)
    expect(getStoredToken()).toBeNull()
  })

  it('clearStoredAuth 清空并触发事件', () => {
    persistAuth({ token: 't', username: 'u' })
    let fired = 0
    window.addEventListener('auth-change', () => fired++, { once: true })
    clearStoredAuth()
    expect(fired).toBe(1)
    expect(getStoredToken()).toBeNull()
    expect(getStoredUsername()).toBe('')
  })
})
