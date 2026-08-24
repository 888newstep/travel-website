import { useState } from 'react'
import { attractionApi, type AMapPlaceCandidate } from '../../api/attraction.api'
import { isRequestTimeoutError, withRequestTimeout } from '../../lib/request'
import { LoadingSpinner } from '../common/LoadingSpinner'
import { StatusNotice } from '../common/StatusNotice'

interface AMapPlaceSearchPanelProps {
  keyword: string
}

export function AMapPlaceSearchPanel({ keyword }: AMapPlaceSearchPanelProps) {
  const normalizedKeyword = keyword.trim()
  const [items, setItems] = useState<AMapPlaceCandidate[]>([])
  const [searchedKeyword, setSearchedKeyword] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const showingCurrentKeyword = searchedKeyword === normalizedKeyword

  async function search() {
    if (normalizedKeyword.length < 2 || loading) return
    setLoading(true)
    setMessage(null)
    try {
      const response = await withRequestTimeout(attractionApi.searchExternalPlaces(normalizedKeyword))
      setItems(Array.isArray(response.items) ? response.items : [])
      setSearchedKeyword(normalizedKeyword)
      if (!response.dataAvailable) {
        setMessage(response.message || '高德地点搜索暂不可用。')
      }
    } catch (error) {
      setItems([])
      setSearchedKeyword(normalizedKeyword)
      setMessage(isRequestTimeoutError(error) ? '高德地点搜索超时，请稍后重试。' : '高德地点搜索失败，请稍后重试。')
    } finally {
      setLoading(false)
    }
  }

  if (normalizedKeyword.length < 2) return null

  return (
    <section className="mt-8 border-t border-slate-200 pt-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">高德地点候选</h2>
          <p className="mt-1 text-sm text-slate-500">数据来源：高德开放平台</p>
        </div>
        <button type="button" className="btn-secondary" disabled={loading} onClick={search}>
          {loading ? '搜索中...' : '搜索高德地点'}
        </button>
      </div>

      {loading ? <div className="mt-5"><LoadingSpinner /></div> : null}
      {message ? <div className="mt-4"><StatusNotice tone="warning" message={message} /></div> : null}
      {!loading && showingCurrentKeyword && searchedKeyword && !message && !items.length ? (
        <div className="mt-4 border-t border-dashed border-slate-200 py-5 text-sm text-slate-500">未找到相关高德地点。</div>
      ) : null}

      {showingCurrentKeyword && items.length ? (
        <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {items.map((item, index) => (
            <article key={item.poiId || `${item.name}-${index}`} className="overflow-hidden rounded-lg border border-slate-200 bg-white">
              {item.imageUrl ? <img src={item.imageUrl} alt={item.name} className="h-36 w-full object-cover" /> : null}
              <div className="p-4">
                <div className="flex items-start justify-between gap-3">
                  <h3 className="font-semibold text-slate-900">{item.name}</h3>
                  <span className="chip">高德</span>
                </div>
                <p className="mt-2 text-sm leading-6 text-slate-500">{item.address || [item.city, item.district].filter(Boolean).join(' ') || '暂无地址'}</p>
                {item.type ? <p className="mt-2 line-clamp-1 text-xs text-slate-400">{item.type}</p> : null}
              </div>
            </article>
          ))}
        </div>
      ) : null}
    </section>
  )
}
