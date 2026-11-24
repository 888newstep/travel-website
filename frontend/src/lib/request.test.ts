import { describe, expect, it, vi } from 'vitest'
import { AuthError, NetworkError, RequestTimeoutError, isRequestTimeoutError, withRequestTimeout } from './request'
import { formatTime, formatDate } from './format'

describe('withRequestTimeout', () => {
  it('在时限内返回结果并清除定时器', async () => {
    const result = await withRequestTimeout(Promise.resolve('ok'), 1000)
    expect(result).toBe('ok')
  })

  it('超时后抛 RequestTimeoutError', async () => {
    vi.useFakeTimers()
    const pending = new Promise<string>(() => {})
    const wrapped = withRequestTimeout(pending, 50)
    const assertion = expect(wrapped).rejects.toBeInstanceOf(RequestTimeoutError)
    vi.advanceTimersByTime(60)
    await assertion
    vi.useRealTimers()
  })

  it('底层错误原样透传', async () => {
    await expect(withRequestTimeout(Promise.reject(new NetworkError()), 1000)).rejects.toBeInstanceOf(NetworkError)
  })

  it('isRequestTimeoutError 只识别超时类型', () => {
    expect(isRequestTimeoutError(new RequestTimeoutError())).toBe(true)
    expect(isRequestTimeoutError(new AuthError())).toBe(false)
    expect(isRequestTimeoutError(null)).toBe(false)
  })
})

describe('formatTime / formatDate', () => {
  it('ISO 时间转 YYYY-MM-DD HH:mm', () => {
    expect(formatTime('2026-11-19T14:30:25')).toBe('2026-11-19 14:30')
  })

  it('空值返回"未知"', () => {
    expect(formatTime(undefined)).toBe('未知')
    expect(formatTime('')).toBe('未知')
    expect(formatTime(null)).toBe('未知')
  })

  it('formatDate 只保留日期', () => {
    expect(formatDate('2026-11-19T14:30:25')).toBe('2026-11-19')
    expect(formatDate(undefined)).toBe('未知')
  })
})
