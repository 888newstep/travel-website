import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ErrorBoundary } from './ErrorBoundary'

function Boom(): never {
  throw new Error('render exploded')
}

describe('ErrorBoundary', () => {
  it('子组件正常时原样渲染', () => {
    render(
      <ErrorBoundary>
        <div>正常内容</div>
      </ErrorBoundary>,
    )
    expect(screen.getByText('正常内容')).toBeTruthy()
  })

  it('渲染异常时显示降级 UI 与错误信息', () => {
    // 拦截 console.error，避免测试输出噪音
    vi.spyOn(console, 'error').mockImplementation(() => {})
    render(
      <ErrorBoundary>
        <Boom />
      </ErrorBoundary>,
    )
    expect(screen.getByText('页面出现了一点问题')).toBeTruthy()
    expect(screen.getByText('render exploded')).toBeTruthy()
    expect(screen.getByRole('button', { name: '重试' })).toBeTruthy()
    vi.restoreAllMocks()
  })
})
