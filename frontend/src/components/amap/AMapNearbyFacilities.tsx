import { useCallback, useEffect, useState } from 'react'
import {
  attractionApi,
  type AMapFacilityCategory,
  type AMapNearbyFacilitiesResponse,
  type AMapWeatherResponse,
} from '../../api/attraction.api'
import { isRequestTimeoutError, withRequestTimeout } from '../../lib/request'

const CATEGORIES: Array<{ value: AMapFacilityCategory; label: string }> = [
  { value: 'restaurant', label: '餐饮' },
  { value: 'parking', label: '停车场' },
  { value: 'restroom', label: '公共厕所' },
  { value: 'transit', label: '公共交通' },
]

interface AMapNearbyFacilitiesProps {
  attractionId: number
}

export function AMapNearbyFacilities({ attractionId }: AMapNearbyFacilitiesProps) {
  const [category, setCategory] = useState<AMapFacilityCategory>('restaurant')
  const [response, setResponse] = useState<AMapNearbyFacilitiesResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [weather, setWeather] = useState<AMapWeatherResponse | null>(null)

  const loadFacilities = useCallback(async (nextCategory: AMapFacilityCategory) => {
    setLoading(true)
    setMessage(null)
    try {
      const result = await withRequestTimeout(attractionApi.getNearbyFacilities(attractionId, nextCategory, 1000))
      setResponse(result)
      if (!result.dataAvailable) setMessage(result.message || '周边设施暂不可用。')
    } catch (error) {
      setResponse(null)
      setMessage(isRequestTimeoutError(error) ? '周边设施加载超时。' : '周边设施加载失败。')
    } finally {
      setLoading(false)
    }
  }, [attractionId])

  useEffect(() => {
    setCategory('restaurant')
    void loadFacilities('restaurant')
    let active = true
    withRequestTimeout(attractionApi.getAttractionWeather(attractionId))
      .then((result) => {
        if (active) setWeather(result.dataAvailable ? result : null)
      })
      .catch(() => {
        if (active) setWeather(null)
      })
    return () => {
      active = false
    }
  }, [loadFacilities])

  function selectCategory(nextCategory: AMapFacilityCategory) {
    setCategory(nextCategory)
    void loadFacilities(nextCategory)
  }

  return (
    <section>
      {weather ? (
        <div className="mb-5 grid grid-cols-2 gap-3 rounded-lg border border-slate-200 p-4 sm:grid-cols-4">
          <div><div className="text-xs text-slate-400">当前天气</div><div className="mt-2 font-semibold text-slate-900">{weather.weather || '--'}</div></div>
          <div><div className="text-xs text-slate-400">温度</div><div className="mt-2 font-semibold text-slate-900">{typeof weather.temperature === 'number' ? `${weather.temperature}°C` : '--'}</div></div>
          <div><div className="text-xs text-slate-400">湿度</div><div className="mt-2 font-semibold text-slate-900">{weather.humidity ? `${weather.humidity}%` : '--'}</div></div>
          <div><div className="text-xs text-slate-400">风向风力</div><div className="mt-2 font-semibold text-slate-900">{[weather.windDirection, weather.windPower ? `${weather.windPower}级` : ''].filter(Boolean).join(' ') || '--'}</div></div>
        </div>
      ) : null}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold text-slate-900">周边设施</h3>
          <p className="mt-1 text-xs text-slate-400">高德开放平台 · 1 公里范围</p>
        </div>
        <div className="flex flex-wrap gap-2" role="tablist" aria-label="周边设施分类">
          {CATEGORIES.map((item) => (
            <button
              key={item.value}
              type="button"
              role="tab"
              aria-selected={category === item.value}
              className={category === item.value ? 'btn-primary px-3 py-2 text-xs' : 'btn-secondary px-3 py-2 text-xs'}
              onClick={() => selectCategory(item.value)}
              disabled={loading}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {loading ? <div className="mt-4 text-sm text-slate-500">正在加载...</div> : null}
      {!loading && message ? <div className="mt-4 text-sm text-amber-700">{message}</div> : null}
      {!loading && response?.dataAvailable && !response.items.length ? (
        <div className="mt-4 border-t border-dashed border-slate-200 py-4 text-sm text-slate-500">该范围内暂无{response.categoryLabel}。</div>
      ) : null}
      {!loading && response?.items.length ? (
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {response.items.slice(0, 6).map((item, index) => (
            <div key={item.poiId || `${item.name}-${index}`} className="rounded-lg border border-slate-200 p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="font-medium text-slate-900">{item.name}</div>
                {typeof item.distanceMeters === 'number' ? <span className="text-xs text-slate-400">{item.distanceMeters} 米</span> : null}
              </div>
              <div className="mt-2 text-sm leading-6 text-slate-500">{item.address || '暂无地址'}</div>
            </div>
          ))}
        </div>
      ) : null}
    </section>
  )
}
