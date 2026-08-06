import { useMemo, useState, type ReactNode } from 'react'
import { realtimeApi } from '../api/realtime.api'
import { RealtimeCard } from '../components/RealtimeCard'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { CROWD_LEVEL_MEDIUM } from '../constants'

const DEFAULT_SYNC_MINUTES = 30

function getTrafficStatusClass(value: Record<string, any> | null) {
  const status = String(value?.status || value?.level || '').toLowerCase()

  if (status.includes('拥堵') || status.includes('严重') || status.includes('severe') || status.includes('busy')) {
    return 'text-red-600'
  }

  if (status.includes('缓慢') || status.includes('slow') || status.includes('medium')) {
    return 'text-sky-600'
  }

  return 'text-emerald-600'
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
  const [historicalAvgId, setHistoricalAvgId] = useState('')
  const [historicalAvgResult, setHistoricalAvgResult] = useState<number | null>(null)
  const [sevenDaysAvgId, setSevenDaysAvgId] = useState('')
  const [sevenDaysAvgResult, setSevenDaysAvgResult] = useState<number | null>(null)
  const [warns, setWarns] = useState<Record<string, any>[]>([])
  const [warnsLoading, setWarnsLoading] = useState(false)
  const [needSyncMinutes, setNeedSyncMinutes] = useState(String(DEFAULT_SYNC_MINUTES))
  const [needSyncList, setNeedSyncList] = useState<Record<string, any>[]>([])
  const [trafficAttractionId, setTrafficAttractionId] = useState('')
  const [trafficResult, setTrafficResult] = useState<Record<string, any> | null>(null)
  const [batchUpdateLoading, setBatchUpdateLoading] = useState(false)
  const [batchUpdateResult, setBatchUpdateResult] = useState('')
  const [loading, setLoading] = useState(false)

  const trafficStatusColor = useMemo(() => getTrafficStatusClass(trafficResult), [trafficResult])

  async function fetchSingleStatus() {
    const id = singleAttractionId.trim()
    if (!id) return

    setLoading(true)
    try {
      const result = await realtimeApi.getAttractionRealtimeStatus(Number(id))
      setSingleResult(result || null)
    } catch {
      setSingleResult(null)
    } finally {
      setLoading(false)
    }
  }

  async function fetchBatchStatus() {
    const ids = batchIds
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
      .map(Number)

    if (!ids.length) return

    setLoading(true)
    try {
      const result = await realtimeApi.getBatchRealtimeStatus(ids)
      setBatchResults(Array.isArray(result) ? result : [])
    } catch {
      setBatchResults([])
    } finally {
      setLoading(false)
    }
  }

  async function fetchCrowdedList() {
    const level = Number(minCrowdLevel) || CROWD_LEVEL_MEDIUM

    setLoading(true)
    try {
      const result = await realtimeApi.getCrowdedAttractions(level)
      setCrowdedList(Array.isArray(result) ? result : [])
    } catch {
      setCrowdedList([])
    } finally {
      setLoading(false)
    }
  }

  async function fetchHistoricalAvg() {
    const id = historicalAvgId.trim()
    if (!id) return

    try {
      const result = await realtimeApi.getHistoricalAvgCrowdCount(Number(id))
      setHistoricalAvgResult(Number(result) || 0)
    } catch {
      setHistoricalAvgResult(null)
    }
  }

  async function fetchSevenDaysAvg() {
    const id = sevenDaysAvgId.trim()
    if (!id) return

    try {
      const result = await realtimeApi.get7DaysAvgCrowdCount(Number(id))
      setSevenDaysAvgResult(Number(result) || 0)
    } catch {
      setSevenDaysAvgResult(null)
    }
  }

  async function fetchWarns() {
    setWarnsLoading(true)
    try {
      const result = await realtimeApi.getActiveWarns()
      setWarns(Array.isArray(result) ? result : [])
    } catch {
      setWarns([])
    } finally {
      setWarnsLoading(false)
    }
  }

  async function fetchNeedSync() {
    const minutes = Number(needSyncMinutes) || DEFAULT_SYNC_MINUTES

    try {
      const result = await realtimeApi.getNeedSyncStatus(minutes)
      setNeedSyncList(Array.isArray(result) ? result : [])
    } catch {
      setNeedSyncList([])
    }
  }

  async function fetchTraffic() {
    const id = trafficAttractionId.trim()
    if (!id) return

    setTrafficResult(null)
    try {
      const result = await realtimeApi.getTrafficInfo(Number(id))
      setTrafficResult(result || null)
    } catch {
      setTrafficResult(null)
    }
  }

  async function batchUpdateStatus() {
    setBatchUpdateResult('')
    setBatchUpdateLoading(true)
    try {
      await realtimeApi.triggerBatchUpdate()
      setBatchUpdateResult('批量状态刷新成功')
    } catch {
      setBatchUpdateResult('批量状态刷新失败')
    } finally {
      setBatchUpdateLoading(false)
    }
  }

  const overviewCards = [
    { label: '批量状态', value: batchResults.length },
    { label: '拥挤景点', value: crowdedList.length },
    { label: '活动预警', value: warns.length },
    { label: '待同步项', value: needSyncList.length },
  ]

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
              <span className="chip">交通状态</span>
            </div>
            <h1 className="text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">把景点状态、拥挤度和交通信息放在同一块看</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              这一页现在更适合做出发前和途中判断：先查单个景点，再看批量状态、拥挤列表、活动预警和交通信息，减少来回切页的成本。
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
                <div className="mt-1 text-xl font-semibold text-slate-900">先刷新状态，再决定是否调整路线</div>
              </div>
              <span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-sky-700">
                Live Board
              </span>
            </div>
            <div className="mt-4 grid gap-3 text-sm text-slate-600">
              <div className="travel-step-card">单景点查询适合出发前确认排队和天气。</div>
              <div className="travel-step-card">拥挤列表和预警适合中途决定是否临时换点。</div>
              <div className="travel-step-card">交通状态可以辅助判断是否继续按原路线前进。</div>
            </div>
            <div className="mt-5 flex flex-wrap gap-3">
              <button type="button" onClick={batchUpdateStatus} className="btn-primary">
                {batchUpdateLoading ? '刷新中...' : '批量刷新状态'}
              </button>
              {batchUpdateResult ? <span className="text-sm text-slate-500">{batchUpdateResult}</span> : null}
            </div>
          </div>
        </div>
        </div>
      </section>

      <section className="mt-8 grid gap-6 xl:grid-cols-[1fr_1fr]">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="单景点状态" description="输入景点 ID，查看当前客流、等待时间、天气与开闭园状态。" />
          <div className="flex gap-3">
            <input
              value={singleAttractionId}
              onChange={(event) => setSingleAttractionId(event.target.value)}
              type="text"
              placeholder="输入 attractionId"
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchSingleStatus}>查询</button>
          </div>
          <div className="mt-4">{loading ? <LoadingSpinner /> : null}</div>
          {singleResult ? <div className="mt-4"><RealtimeCard data={singleResult} /></div> : <p className="mt-4 text-sm text-slate-400">暂无单景点实时数据</p>}
        </div>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="批量状态" description="多个景点一起查询，更适合对比当天行程中几个候选点。" />
          <div className="flex gap-3">
            <input
              value={batchIds}
              onChange={(event) => setBatchIds(event.target.value)}
              type="text"
              placeholder="输入多个 attractionId，用逗号分隔"
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchBatchStatus}>查询</button>
          </div>
          {batchResults.length ? (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {batchResults.map((item, index) => (
                <RealtimeCard key={item.id || `${item.attractionId}-${index}`} data={item} />
              ))}
            </div>
          ) : (
            <p className="mt-4 text-sm text-slate-400">暂无批量状态结果</p>
          )}
        </div>
      </section>

      <section className="mt-6 grid gap-6 xl:grid-cols-[1fr_1fr]">
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
          {crowdedList.length ? (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {crowdedList.map((item, index) => (
                <RealtimeCard key={item.id || `${item.attractionId}-${index}`} data={item} />
              ))}
            </div>
          ) : (
            <p className="mt-4 text-sm text-slate-400">暂无拥挤景点结果</p>
          )}
        </div>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="历史客流参考" description="把历史平均值和近 7 天均值放在一起，辅助判断今天是否异常拥挤。" />
          <div className="grid gap-4 md:grid-cols-2">
            <div className="metric-card">
              <div className="text-xs text-slate-500">历史平均客流</div>
              <div className="mt-3 flex gap-3">
                <input
                  value={historicalAvgId}
                  onChange={(event) => setHistoricalAvgId(event.target.value)}
                  type="text"
                  placeholder="输入 attractionId"
                  className="search-input flex-1"
                />
                <button className="btn-secondary" onClick={fetchHistoricalAvg}>查询</button>
              </div>
              <div className="mt-4 text-sm text-slate-600">结果：{historicalAvgResult ?? '--'}</div>
            </div>
            <div className="metric-card">
              <div className="text-xs text-slate-500">近 7 天平均客流</div>
              <div className="mt-3 flex gap-3">
                <input
                  value={sevenDaysAvgId}
                  onChange={(event) => setSevenDaysAvgId(event.target.value)}
                  type="text"
                  placeholder="输入 attractionId"
                  className="search-input flex-1"
                />
                <button className="btn-secondary" onClick={fetchSevenDaysAvg}>查询</button>
              </div>
              <div className="mt-4 text-sm text-slate-600">结果：{sevenDaysAvgResult ?? '--'}</div>
            </div>
          </div>
        </div>
      </section>

      <section className="mt-6 grid gap-6 xl:grid-cols-[1fr_1fr]">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="活动预警" description="查看当前激活的状态预警与风险提示。" action={<button className="btn-secondary" onClick={fetchWarns}>刷新</button>} />
          {warnsLoading ? <LoadingSpinner /> : null}
          {!warnsLoading && warns.length ? (
            <div className="space-y-4">
              {warns.map((warn, index) => (
                <article key={warn.id || index} className="rounded-[1.5rem] border border-slate-200 bg-slate-50 px-5 py-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="text-base font-semibold text-slate-900">{warn.title || warn.name || `预警 #${index + 1}`}</h3>
                      <p className="mt-2 text-sm leading-6 text-slate-500">{warn.description || warn.message || '暂无更多描述'}</p>
                    </div>
                    <span className={`rounded-full px-3 py-1 text-xs font-medium ${getWarnLevelClass(warn.severity)}`}>
                      {getWarnLevelLabel(warn.severity)}
                    </span>
                  </div>
                </article>
              ))}
            </div>
          ) : null}
          {!warnsLoading && !warns.length ? <p className="py-8 text-center text-sm text-slate-400">暂无活动预警</p> : null}
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
          {needSyncList.length ? (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {needSyncList.map((item, index) => (
                <RealtimeCard key={item.id || `${item.attractionId}-${index}`} data={item} />
              ))}
            </div>
          ) : (
            <p className="mt-4 text-sm text-slate-400">暂无待同步记录</p>
          )}
        </div>
      </section>

      <section className="mt-6 grid gap-6 xl:grid-cols-[1fr_1fr]">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="交通信息查询" description="快速查看指定景点周边交通状态、速度和拥堵指数。" />
          <div className="flex gap-3">
            <input
              value={trafficAttractionId}
              onChange={(event) => setTrafficAttractionId(event.target.value)}
              type="text"
              placeholder="输入 attractionId"
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchTraffic}>查询</button>
          </div>
          {trafficResult ? (
            <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="space-y-2 text-sm">
                <p className="text-slate-700">
                  交通状态：<span className={trafficStatusColor}>{String(trafficResult.status || trafficResult.level || '未知')}</span>
                </p>
                {trafficResult.speed ? <p className="text-slate-600">平均速度：{trafficResult.speed} km/h</p> : null}
                {trafficResult.congestion ? <p className="text-slate-600">拥堵指数：{trafficResult.congestion}</p> : null}
                {trafficResult.description ? <p className="text-xs text-slate-500">{trafficResult.description}</p> : null}
              </div>
            </div>
          ) : (
            <p className="mt-4 text-sm text-slate-400">暂无交通查询结果</p>
          )}
        </div>
      </section>
    </div>
  )
}


