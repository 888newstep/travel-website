import { beforeEach, describe, expect, it } from 'vitest'
import { act, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { clearStoredAuth, persistAuth } from '../lib/auth'
import { ProtectedRoute } from './ProtectedRoute'

function renderProtectedPage() {
  render(
    <MemoryRouter initialEntries={['/profile']}>
      <Routes>
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <div>个人中心</div>
            </ProtectedRoute>
          }
        />
        <Route path="/login" element={<div>登录页面</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('未登录时跳转登录页', () => {
    renderProtectedPage()
    expect(screen.getByText('登录页面')).toBeTruthy()
  })

  it('登出后立即跳转登录页', () => {
    persistAuth({ token: 'token-1' })
    renderProtectedPage()
    expect(screen.getByText('个人中心')).toBeTruthy()

    act(() => {
      clearStoredAuth()
    })

    expect(screen.getByText('登录页面')).toBeTruthy()
  })
})
