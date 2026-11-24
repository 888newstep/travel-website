import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { attractionApi } from '../api/attraction.api'
import { AttractionsPage } from './AttractionsPage'

vi.mock('../api/attraction.api', () => ({
  attractionApi: {
    getAttractions: vi.fn(),
    searchAttractions: vi.fn(),
    getAttractionDetail: vi.fn(),
    getAttractionImages: vi.fn(),
    getAttractionReviews: vi.fn(),
    getAttractionNearby: vi.fn(),
    submitReview: vi.fn(),
    searchExternalPlaces: vi.fn(),
    getNearbyFacilities: vi.fn(),
    getAttractionWeather: vi.fn(),
    getRecommendations: vi.fn(),
  },
}))

const mockedGetAttractions = vi.mocked(attractionApi.getAttractions)

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/attractions']}>
        <AttractionsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AttractionsPage 列表三态', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('加载中展示 loading 指示器', () => {
    mockedGetAttractions.mockReturnValue(new Promise<never>(() => {}))
    renderPage()
    expect(screen.getByRole('status')).toBeTruthy()
    expect(screen.getByText('景点探索')).toBeTruthy()
  })

  it('列表为空时展示空状态与重新加载入口', async () => {
    mockedGetAttractions.mockResolvedValue([])
    renderPage()
    expect(await screen.findByText('暂未找到符合条件的景点。')).toBeTruthy()
    expect(screen.getByRole('button', { name: '重新加载' })).toBeTruthy()
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('请求失败时展示错误提示与重试按钮', async () => {
    mockedGetAttractions.mockRejectedValue(new Error('network down'))
    renderPage()
    expect(await screen.findByText('景点列表加载失败，请稍后重试。')).toBeTruthy()
    expect(screen.getByRole('alert')).toBeTruthy()
    expect(screen.getByRole('button', { name: '重试' })).toBeTruthy()
  })
})
