import { useState, type ReactNode } from 'react'
import { shareApi, type RouteShare } from '../api/share.api'
import { DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_PAGE_SIZE } from '../constants'

function SectionHeader({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return (
    <div className="mb-4 flex items-start justify-between gap-3">
      <div>
        <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
        <p className="mt-1 text-sm text-slate-500">{description}</p>
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  )
}

export function RouteSharePage() {
  const [genForm, setGenForm] = useState({ itemId: '', itemType: 'route' })
  const [genResult, setGenResult] = useState<RouteShare | null>(null)
  const [validateCode, setValidateCode] = useState('')
  const [validateResult, setValidateResult] = useState<boolean | null>(null)
  const [listUserId, setListUserId] = useState('')
  const [shareList, setShareList] = useState<RouteShare[]>([])
  const [popularList, setPopularList] = useState<RouteShare[]>([])
  const [statsId, setStatsId] = useState('')
  const [statsResult, setStatsResult] = useState<Record<string, any> | null>(null)
  const [batchIds, setBatchIds] = useState('')

  async function generateShare() {
    if (!genForm.itemId.trim()) return

    try {
      const response = await shareApi.generateShareCode(Number(genForm.itemId), genForm.itemType || 'route')
      setGenResult(response || null)
    } catch {
      setGenResult(null)
    }
  }

  async function validateShare() {
    if (!validateCode.trim()) return

    try {
      const response = await shareApi.validateShareCode(validateCode.trim())
      setValidateResult(Boolean(response))
    } catch {
      setValidateResult(false)
    }
  }

  async function fetchUserShares() {
    try {
      const response = await shareApi.getUserShares(Number(listUserId) || 0, DEFAULT_PAGE, DEFAULT_PAGE_SIZE)
      setShareList(Array.isArray(response) ? response : [])
    } catch {
      setShareList([])
    }
  }

  async function fetchPopular() {
    try {
      const response = await shareApi.getPopularShares(DEFAULT_LIMIT)
      setPopularList(Array.isArray(response) ? response : [])
    } catch {
      setPopularList([])
    }
  }

  async function fetchStats() {
    try {
      const response = await shareApi.getShareStatistics(Number(statsId) || 0)
      setStatsResult(response || null)
    } catch {
      setStatsResult(null)
    }
  }

  async function cancelShare(id?: number) {
    if (!id) return

    try {
      await shareApi.cancelShare(id)
      setShareList((current) => current.filter((item) => item.id !== id))
    } catch {
      // ignore
    }
  }

  async function batchCancel() {
    const ids = batchIds
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
      .map(Number)

    if (!ids.length) return

    try {
      await shareApi.batchCancelShares(ids)
      setShareList((current) => current.filter((item) => !ids.includes(Number(item.id))))
    } catch {
      // ignore
    }
  }

  const pendingBatchCount = batchIds
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean).length

  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />

        <div className="relative grid gap-6 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
          <div>
            <span className="section-kicker">路线分享</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">{'\u8def\u7ebf\u5206\u4eab'}</span>
              <span className="chip">{'\u9080\u8bf7\u7801'}</span>
              <span className="chip">{'\u8bbf\u95ee\u6570\u636e'}</span>
            </div>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">{'\u5206\u4eab\u4f60\u7684\u884c\u7a0b\u4e0e\u8def\u7ebf'}</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              {'\u751f\u6210\u5206\u4eab\u7801\uff0c\u6821\u9a8c\u94fe\u63a5\u6709\u6548\u6027\u3001\u67e5\u770b\u8bbf\u95ee\u7edf\u8ba1\u4e0e\u70ed\u95e8\u5206\u4eab\uff0c\u4fbf\u4e8e\u5728\u65c5\u9014\u4e2d\u5feb\u901f\u5206\u53d1\u8def\u7ebf\u3002'}
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u5df2\u751f\u6210'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{shareList.length}</div>
              </div>
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u70ed\u95e8\u6570'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{popularList.length}</div>
              </div>
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u5f85\u5904\u7406'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{pendingBatchCount}</div>
              </div>
            </div>
          </div>

          <div className="scenic-shell-soft edge-glow animate-fade-in p-5 sm:p-6">
            <SectionHeader title={'\u5206\u4eab\u8bf4\u660e'} description={'\u4e09\u6b65\u5b8c\u6210\u8def\u7ebf\u5206\u4eab'} />
            <div className="grid gap-2 text-sm text-slate-500 sm:grid-cols-2">
              <div className="travel-step-card">{'\u5148\u751f\u6210\u5206\u4eab\u7801\uff0c\u518d\u590d\u5236\u7ed9\u540c\u4f34'}</div>
              <div className="travel-step-card">{'\u652f\u6301\u6821\u9a8c\u3001\u7edf\u8ba1\u548c\u6279\u91cf\u53d6\u6d88'}</div>
            </div>
          </div>
        </div>
      </section>

      <div className="mt-8 grid gap-6 xl:grid-cols-2">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u751f\u6210\u5206\u4eab\u7801'} description={'\u8f93\u5165\u8def\u7ebf\u6216\u5185\u5bb9 ID \u751f\u6210\u5206\u4eab\u7801'} />
          <div className="mt-4 grid gap-3 sm:grid-cols-[1fr_auto]">
            <input
              value={genForm.itemId}
              onChange={(event) => setGenForm((current) => ({ ...current, itemId: event.target.value }))}
              type="text"
              placeholder={'\u8f93\u5165\u5185\u5bb9 ID'}
              className="search-input"
            />
            <button onClick={generateShare} className="btn-primary">{'\u751f\u6210'}</button>
          </div>
          <select
            value={genForm.itemType}
            onChange={(event) => setGenForm((current) => ({ ...current, itemType: event.target.value }))}
            className="mt-3 search-input"
          >
            <option value="route">{'\u8def\u7ebf'}</option>
            <option value="note">{'\u7b14\u8bb0'}</option>
          </select>
          {genResult ? <pre className="mt-4 whitespace-pre-wrap rounded-2xl bg-slate-50 p-4 text-xs text-slate-600">{JSON.stringify(genResult, null, 2)}</pre> : null}
        </div>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u6821\u9a8c\u5206\u4eab\u7801'} description={'\u8f93\u5165\u5206\u4eab\u7801\u6821\u9a8c\u662f\u5426\u6709\u6548'} />
          <div className="mt-4 flex gap-3">
            <input
              value={validateCode}
              onChange={(event) => setValidateCode(event.target.value)}
              type="text"
              placeholder={'\u8f93\u5165\u5206\u4eab\u7801'}
              className="search-input flex-1"
            />
            <button onClick={validateShare} className="btn-primary">{'\u6821\u9a8c'}</button>
          </div>
          {validateResult !== null ? (
            <div className="mt-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
              {validateResult ? '\u5206\u4eab\u7801\u6709\u6548' : '\u5206\u4eab\u7801\u65e0\u6548\u6216\u5df2\u5931\u6548'}
            </div>
          ) : null}
        </div>

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6 xl:col-span-2">
          <SectionHeader title={'\u6211\u7684\u5206\u4eab\u8bb0\u5f55'} description={'\u6309\u7528\u6237 ID \u67e5\u8be2\u5206\u4eab\u4e0e\u70ed\u95e8\u8bb0\u5f55'} action={<span className="chip">{shareList.length} {'\u6761'}</span>} />
          <div className="mb-4 grid gap-3 sm:grid-cols-[1fr_auto_auto]">
            <input
              value={listUserId}
              onChange={(event) => setListUserId(event.target.value)}
              type="text"
              placeholder={'\u8f93\u5165\u7528\u6237 ID'}
              className="search-input"
            />
            <button onClick={fetchUserShares} className="btn-secondary">{'\u67e5\u8be2\u6211\u7684\u5206\u4eab'}</button>
            <button onClick={fetchPopular} className="btn-secondary">{'\u67e5\u770b\u70ed\u95e8'}</button>
          </div>
          {shareList.length ? (
            <div className="space-y-4">
              {shareList.map((item) => (
                <article key={item.id} className="metric-card surface-card-hover">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <h3 className="text-base font-semibold text-slate-900">{item.shareCode || '\u672a\u547d\u540d\u7801'}</h3>
                      <p className="mt-2 text-sm leading-6 text-slate-500">{'\u7c7b\u578b'} {item.itemType || 'route'} {'\u00b7 \u5185\u5bb9 ID'} {item.itemId || item.routeId || '-'}</p>
                      <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-400">
                        <span className="chip">{'\u8bbf\u95ee'} {item.visitCount || 0}</span>
                        <span className="chip">{'\u5230\u671f'} {item.expireTime || '\u672a\u77e5'}</span>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => cancelShare(item.id)}
                      className="rounded-2xl border border-red-200 px-3 py-2 text-xs font-medium text-red-600 transition hover:bg-red-50"
                    >
                      {'\u53d6\u6d88\u5206\u4eab'}
                    </button>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className="py-8 text-center text-sm text-slate-400">{'\u6682\u65e0\u5206\u4eab\u8bb0\u5f55'}</div>
          )}
        </section>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u5206\u4eab\u7edf\u8ba1'} description={'\u6309\u5185\u5bb9 ID \u67e5\u770b\u5206\u4eab\u6982\u51b5'} />
          <div className="mt-4 flex gap-3">
            <input
              value={statsId}
              onChange={(event) => setStatsId(event.target.value)}
              type="text"
              placeholder={'\u8f93\u5165\u5185\u5bb9 ID'}
              className="search-input flex-1"
            />
            <button onClick={fetchStats} className="btn-primary">{'\u67e5\u770b'}</button>
          </div>
          {statsResult ? <pre className="mt-4 whitespace-pre-wrap rounded-2xl bg-slate-50 p-4 text-xs text-slate-600">{JSON.stringify(statsResult, null, 2)}</pre> : null}
        </div>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u6279\u91cf\u53d6\u6d88'} description={'\u8f93\u5165\u591a\u4e2a\u5206\u4eab ID\uff0c\u4ee5\u9017\u53f7\u5206\u9694'} />
          <div className="mt-4 space-y-3">
            <textarea
              value={batchIds}
              onChange={(event) => setBatchIds(event.target.value)}
              rows={5}
              placeholder={'\u8f93\u5165\u5206\u4eab ID\uff0c\u9017\u53f7\u5206\u9694'}
              className="search-input"
            />
            <button onClick={batchCancel} className="btn-primary">{'\u6279\u91cf\u53d6\u6d88'}</button>
          </div>
        </div>

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6 xl:col-span-2">
          <SectionHeader title={'\u70ed\u95e8\u5206\u4eab'} description={'\u67e5\u770b\u8bbf\u95ee\u91cf\u8f83\u9ad8\u7684\u5206\u4eab\u5185\u5bb9'} action={<span className="chip">{popularList.length} {'\u6761'}</span>} />
          {popularList.length ? (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {popularList.map((item) => (
                <article key={item.id} className="metric-card surface-card-hover">
                  <div className="text-sm font-semibold text-slate-900">{item.shareCode || '\u672a\u547d\u540d\u7801'}</div>
                  <div className="mt-2 text-sm text-slate-500">{'\u7c7b\u578b'} {item.itemType || 'route'} {'\u00b7 \u8bbf\u95ee'} {item.visitCount || 0}</div>
                </article>
              ))}
            </div>
          ) : (
            <div className="py-8 text-center text-sm text-slate-400">{'\u6682\u65e0\u70ed\u95e8\u5206\u4eab'}</div>
          )}
        </section>
      </div>
    </div>
  )
}
