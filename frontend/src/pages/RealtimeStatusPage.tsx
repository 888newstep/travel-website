import { useMemo, useState, type ReactNode } from 'react'
import { realtimeApi } from '../api/realtime.api'
import { RealtimeCard } from '../components/RealtimeCard'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { SearchEmptyState } from '../components/common/SearchFeedback'
import { type StatusNoticeTone, StatusNotice } from '../components/common/StatusNotice'
import { CROWD_LEVEL_MEDIUM } from '../constants'

const DEFAULT_SYNC_MINUTES = 30

type QuerySection = 'single' | 'batch' | 'crowded' | 'needSync'

interface InlineNotice {
  tone: StatusNoticeTone
  message: string
}

function getWarnLevelLabel(severity?: string) {
  if (severity === 'high') return '高风险'
  if (severity === 'medium') return '中风险'
  return '低风险'
}

function getWarnLevelClass(severity?: string) {
  if (severity === 'high') return 'bg-red-200 text-red-700'
  if (severity === 'medium') return 'bg-sky-100 text-sky-700'
  return 'bg-blue-200 text-blue-700'
}

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

function StatTile({ label, value }: { label: string; value: number }) {
  return (
    <div className="metric-card rounded-2xl px-4 py-4">
      <div className="text-xs text-slate-500">{label}</div>
      <div className="mt-2 text-2xl font-semibold text-slate-900">{value}</div>
    </div>
  )
}

export function RealtimeStatusPage() {
  const [singleAttractionId, setSingleAttractionId] = useState('')
  const [singleResult, setSingleResult] = useState<Record<string, any> | null>(null)
  const [batchIds, setBatchIds] = useState('')
  const [batchResults, setBatchResults] = useState<Record<string, any>[]>([])
  const [minCrowdLevel, setMinCrowdLevel] = useState(String(CROWD_LEVEL_MEDIUM))
  const [crowdedList, setCrowdedList] = useState<Record<string, any>[]>([])
  const [warns, setWarns] = useState<Record<string, any>[]>([])
  const [warnsLoading, setWarnsLoading] = useState(false)
  const [needSyncMinutes, setNeedSyncMinutes] = useState(String(DEFAULT_SYNC_MINUTES))
  const [needSyncList, setNeedSyncList] = useState<Record<string, any>[]>([])
  const [loadingSection, setLoadingSection] = useState<QuerySection | null>(null)
  const [singleNotice, setSingleNotice] = useState<InlineNotice | null>(null)
  const [batchNotice, setBatchNotice] = useState<InlineNotice | null>(null)
  const [crowdedNotice, setCrowdedNotice] = useState<InlineNotice | null>(null)
  const [warnsNotice, setWarnsNotice] = useState<InlineNotice | null>(null)
  const [needSyncNotice, setNeedSyncNotice] = useState<InlineNotice | null>(null)
  const [hasFetchedSingle, setHasFetchedSingle] = useState(false)
  const [hasFetchedBatch, setHasFetchedBatch] = useState(false)
  const [hasFetchedCrowded, setHasFetchedCrowded] = useState(false)
  const [hasFetchedWarns, setHasFetchedWarns] = useState(false)
  const [hasFetchedNeedSync, setHasFetchedNeedSync] = useState(false)

  function isLoading(section: QuerySection) {
    return loadingSection === section
  }

  async function fetchSingleStatus() {
    const id = singleAttractionId.trim()
    if (!id) {
      setHasFetchedSingle(false)
      setSingleResult(null)
      setSingleNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u666f\u70b9\u7f16\u53f7\u3002' })
      return
    }

    setHasFetchedSingle(true)
    setSingleNotice(null)
    setLoadingSection('single')
    try {
      const result = await realtimeApi.getAttractionRealtimeStatus(Number(id))
      setSingleResult(result || null)
    } catch {
      setSingleResult(null)
      setSingleNotice({ tone: 'error', message: '\u5355\u666f\u70b9\u5b9e\u65f6\u72b6\u6001\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoadingSection(null)
    }
  }

  async function fetchBatchStatus() {
    const ids = batchIds
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
      .map(Number)

    if (!ids.length) {
      setHasFetchedBatch(false)
      setBatchResults([])
      setBatchNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u9700\u8981\u67e5\u8be2\u7684\u666f\u70b9\u7f16\u53f7\u3002' })
      return
    }

    setHasFetchedBatch(true)
    setBatchNotice(null)
    setLoadingSection('batch')
    try {
      const result = await realtimeApi.getBatchRealtimeStatus(ids)
      setBatchResults(Array.isArray(result) ? result : [])
    } catch {
      setBatchResults([])
      setBatchNotice({ tone: 'error', message: '\u6279\u91cf\u72b6\u6001\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoadingSection(null)
    }
  }

  async function fetchCrowdedList() {
    const level = Number(minCrowdLevel) || CROWD_LEVEL_MEDIUM

    setHasFetchedCrowded(true)
    setCrowdedNotice(null)
    setLoadingSection('crowded')
    try {
      const result = await realtimeApi.getCrowdedAttractions(level)
      setCrowdedList(Array.isArray(result) ? result : [])
    } catch {
      setCrowdedList([])
      setCrowdedNotice({ tone: 'error', message: '\u62e5\u6324\u666f\u70b9\u5217\u8868\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoadingSection(null)
    }
  }

  async function fetchWarns() {
    setHasFetchedWarns(true)
    setWarnsNotice(null)
    setWarnsLoading(true)
    try {
      const result = await realtimeApi.getActiveWarns()
      setWarns(Array.isArray(result) ? result : [])
    } catch {
      setWarns([])
      setWarnsNotice({ tone: 'error', message: '\u6d3b\u52a8\u9884\u8b66\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setWarnsLoading(false)
    }
  }

  async function fetchNeedSync() {
    const minutes = Number(needSyncMinutes) || DEFAULT_SYNC_MINUTES

    setHasFetchedNeedSync(true)
    setNeedSyncNotice(null)
    setLoadingSection('needSync')
    try {
      const result = await realtimeApi.getNeedSyncStatus(minutes)
      setNeedSyncList(Array.isArray(result) ? result : [])
    } catch {
      setNeedSyncList([])
      setNeedSyncNotice({ tone: 'error', message: '\u5f85\u540c\u6b65\u72b6\u6001\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoadingSection(null)
    }
  }

  const overviewCards = useMemo(() => [
    { label: '批量状态', value: batchResults.length },
    { label: '拥挤景点', value: crowdedList.length },
    { label: '活动预警', value: warns.length },
    { label: '待同步项', value: needSyncList.length },
  ], [batchResults.length, crowdedList.length, needSyncList.length, warns.length])

  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />

        <div className="relative">
        <div className="grid gap-8 xl:grid-cols-[1.08fr_0.92fr] xl:items-center">
          <div>
            <span className="section-kicker">实时状态</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">实时动态</span>
              <span className="chip">客流与预警</span>
              <span className="chip">数据新鲜度</span>
            </div>
            <h1 className="text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">集中查看景点天气、客流和状态预警</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              这里展示当前景点状态快照、拥挤列表和活动预警，帮助判断是否需要调整当天安排。
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {overviewCards.map((item) => (
                <StatTile key={item.label} label={item.label} value={item.value} />
              ))}
            </div>
          </div>

          <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
            <div className="flex items-center justify-between gap-3">
              <div>
                <div className="text-sm font-medium text-slate-500">快捷操作</div>
                <div className="mt-1 text-xl font-semibold text-slate-900">先核对当前快照，再决定是否调整路线</div>
              </div>
              <span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-sky-700">
                {'\u5b9e\u65f6\u770b\u677f'}
              </span>
            </div>
            <div className="mt-4 grid gap-3 text-sm text-slate-600">
              <div className="travel-step-card">单景点查询适合出发前确认排队和天气。</div>
              <div className="travel-step-card">拥挤列表和预警适合中途决定是否临时换点。</div>
              <div className="travel-step-card">待同步列表用于识别数据是否已经超过设定的新鲜度阈值。</div>
            </div>
          </div>
        </div>
        </div>
      </section>

      <section className="mt-8 grid gap-6 xl:grid-cols-[1fr_1fr]">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="单景点状态" description="输入景点编号，查看当前客流、等待时间、天气与开闭园状态。" />
          <div className="flex gap-3">
            <input
              value={singleAttractionId}
              onChange={(event) => setSingleAttractionId(event.target.value)}
              type="text"
              placeholder={'\u8f93\u5165\u666f\u70b9\u7f16\u53f7'}
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchSingleStatus}>查询</button>
          </div>
          {singleNotice ? (
            <StatusNotice
              tone={singleNotice.tone}
              message={singleNotice.message}
              actionLabel={singleNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={singleNotice.tone === 'error' ? fetchSingleStatus : undefined}
              className="mt-4"
            />
          ) : null}
          {isLoading('single') ? <div className="mt-4"><LoadingSpinner /></div> : null}
          {!isLoading('single') && singleResult ? <div className="mt-4"><RealtimeCard data={singleResult} /></div> : null}
          {!isLoading('single') && hasFetchedSingle && !singleResult && !singleNotice ? (
            <SearchEmptyState className="mt-4" message={'\u6682\u672a\u67e5\u8be2\u5230\u8be5\u666f\u70b9\u7684\u5b9e\u65f6\u6570\u636e\u3002'} actionLabel={'\u91cd\u65b0\u67e5\u8be2'} onAction={fetchSingleStatus} />
          ) : null}
        </div>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="批量状态" description="多个景点一起查询，更适合对比当天行程中几个候选点。" />
          <div className="flex gap-3">
            <input
              value={batchIds}
              onChange={(event) => setBatchIds(event.target.value)}
              type="text"
              placeholder={'\u8f93\u5165\u591a\u4e2a\u666f\u70b9\u7f16\u53f7\uff0c\u7528\u9017\u53f7\u5206\u9694'}
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchBatchStatus}>查询</button>
          </div>
          {batchNotice ? (
            <StatusNotice
              tone={batchNotice.tone}
              message={batchNotice.message}
              actionLabel={batchNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={batchNotice.tone === 'error' ? fetchBatchStatus : undefined}
              className="mt-4"
            />
          ) : null}
          {isLoading('batch') ? <div className="mt-4"><LoadingSpinner /></div> : null}
          {!isLoading('batch') && batchResults.length ? (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {batchResults.map((item, index) => (
                <RealtimeCard key={item.id || `${item.attractionId}-${index}`} data={item} />
              ))}
            </div>
          ) : null}
          {!isLoading('batch') && hasFetchedBatch && !batchResults.length && !batchNotice ? (
            <SearchEmptyState className="mt-4" message={'\u5f53\u524d\u6ca1\u6709\u53ef\u5c55\u793a\u7684\u6279\u91cf\u72b6\u6001\u7ed3\u679c\u3002'} actionLabel={'\u91cd\u65b0\u67e5\u8be2'} onAction={fetchBatchStatus} />
          ) : null}
        </div>
      </section>

      <section className="mt-6">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="拥挤景点" description="按最低拥挤等级筛出当前更需要回避的景点。" />
          <div className="flex gap-3">
            <input
              value={minCrowdLevel}
              onChange={(event) => setMinCrowdLevel(event.target.value)}
              type="text"
              placeholder="输入最小拥挤等级"
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchCrowdedList}>查询</button>
          </div>
          {crowdedNotice ? (
            <StatusNotice
              tone={crowdedNotice.tone}
              message={crowdedNotice.message}
              actionLabel={crowdedNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={crowdedNotice.tone === 'error' ? fetchCrowdedList : undefined}
              className="mt-4"
            />
          ) : null}
          {isLoading('crowded') ? <div className="mt-4"><LoadingSpinner /></div> : null}
          {!isLoading('crowded') && crowdedList.length ? (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {crowdedList.map((item, index) => (
                <RealtimeCard key={item.id || `${item.attractionId}-${index}`} data={item} />
              ))}
            </div>
          ) : null}
          {!isLoading('crowded') && hasFetchedCrowded && !crowdedList.length && !crowdedNotice ? (
            <SearchEmptyState className="mt-4" message={'\u5f53\u524d\u6ca1\u6709\u8fbe\u5230\u8be5\u62e5\u6324\u7b49\u7ea7\u7684\u666f\u70b9\u3002'} actionLabel={'\u91cd\u65b0\u67e5\u8be2'} onAction={fetchCrowdedList} />
          ) : null}
        </div>

      </section>

      <section className="mt-6 grid gap-6 xl:grid-cols-[1fr_1fr]">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="活动预警" description="查看当前激活的状态预警与风险提示。" action={<button className="btn-secondary" onClick={fetchWarns}>刷新</button>} />
          {warnsNotice ? (
            <StatusNotice
              tone={warnsNotice.tone}
              message={warnsNotice.message}
              actionLabel={warnsNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={warnsNotice.tone === 'error' ? fetchWarns : undefined}
              className="mb-4"
            />
          ) : null}
          {warnsLoading ? <LoadingSpinner /> : null}
          {!warnsLoading && warns.length ? (
            <div className="space-y-4">
              {warns.map((warn, index) => (
                <article key={warn.id || index} className="rounded-[1.5rem] border border-slate-200 bg-slate-50 px-5 py-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="text-base font-semibold text-slate-900">{warn.title || warn.name || `\u9884\u8b66\u7f16\u53f7 ${index + 1}`}</h3>
                      <p className="mt-2 text-sm leading-6 text-slate-500">{warn.description || warn.message || '\u6682\u65e0\u66f4\u591a\u63cf\u8ff0'}</p>
                    </div>
                    <span className={`rounded-full px-3 py-1 text-xs font-medium ${getWarnLevelClass(warn.severity)}`}>
                      {getWarnLevelLabel(warn.severity)}
                    </span>
                  </div>
                </article>
              ))}
            </div>
          ) : null}
          {!warnsLoading && hasFetchedWarns && !warns.length && !warnsNotice ? (
            <SearchEmptyState className="mt-4" message={'\u5f53\u524d\u6682\u65e0\u6d3b\u52a8\u9884\u8b66\u3002'} actionLabel={'\u91cd\u65b0\u5237\u65b0'} onAction={fetchWarns} />
          ) : null}
        </div>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="待同步状态" description="查询最近一段时间内需要重新同步的实时状态记录。" />
          <div className="flex gap-3">
            <input
              value={needSyncMinutes}
              onChange={(event) => setNeedSyncMinutes(event.target.value)}
              type="text"
              placeholder="输入分钟数，例如 30"
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchNeedSync}>查询</button>
          </div>
          {needSyncNotice ? (
            <StatusNotice
              tone={needSyncNotice.tone}
              message={needSyncNotice.message}
              actionLabel={needSyncNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={needSyncNotice.tone === 'error' ? fetchNeedSync : undefined}
              className="mt-4"
            />
          ) : null}
          {isLoading('needSync') ? <div className="mt-4"><LoadingSpinner /></div> : null}
          {!isLoading('needSync') && needSyncList.length ? (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {needSyncList.map((item, index) => (
                <RealtimeCard key={item.id || `${item.attractionId}-${index}`} data={item} />
              ))}
            </div>
          ) : null}
          {!isLoading('needSync') && hasFetchedNeedSync && !needSyncList.length && !needSyncNotice ? (
            <SearchEmptyState className="mt-4" message={'\u5f53\u524d\u6682\u65e0\u5f85\u540c\u6b65\u72b6\u6001\u8bb0\u5f55\u3002'} actionLabel={'\u91cd\u65b0\u67e5\u8be2'} onAction={fetchNeedSync} />
          ) : null}
        </div>
      </section>

    </div>
  )
}


