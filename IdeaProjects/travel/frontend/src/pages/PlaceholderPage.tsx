import { Link } from 'react-router-dom'

interface PlaceholderPageProps {
  title: string
  description: string
}

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />

        <div className="relative">
          <span className="section-kicker">敬请期待</span>
          <div className="mt-3 flex flex-wrap gap-2">
            <span className="chip">功能规划中</span>
            <span className="chip">视觉统一</span>
            <span className="chip">接口联调</span>
          </div>
          <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">{title}</h1>
          <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">{description}</p>
          <div className="mt-8 scenic-shell-soft p-6 text-sm leading-7 text-slate-500">
            这个页面正在补齐中。你可以先回到首页、路线页或 AI 助手页继续浏览旅行内容。
          </div>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/" className="btn-primary">
              返回首页
            </Link>
            <Link to="/routes" className="btn-secondary">
              查看路线
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}
