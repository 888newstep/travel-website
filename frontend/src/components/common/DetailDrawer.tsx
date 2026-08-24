import type { PropsWithChildren, ReactNode } from 'react'

interface DetailDrawerProps extends PropsWithChildren {
  open: boolean
  title: string
  subtitle?: string
  loading?: boolean
  footer?: ReactNode
  onClose: () => void
}

export function DetailDrawer({
  open,
  title,
  subtitle,
  loading = false,
  footer,
  onClose,
  children,
}: DetailDrawerProps) {
  if (!open) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 supports-[backdrop-filter]:backdrop-blur-[2px]">
      <div className="absolute inset-y-0 right-0 flex w-full justify-end">
        <button
          type="button"
          aria-label="关闭详情抽屉"
          className="h-full flex-1 cursor-default"
          onClick={onClose}
        />
        <section className="drawer-shell relative h-full w-full max-w-2xl overflow-y-auto border-l border-white/70 bg-white/96 p-5 shadow-2xl shadow-slate-900/20 backdrop-blur sm:p-6 md:p-7">
          <div className="drawer-header sticky top-0 z-10 -mx-5 -mt-5 border-b border-slate-200/80 bg-white/92 px-5 py-4 backdrop-blur sm:-mx-6 sm:-mt-6 sm:px-6 md:-mx-7 md:-mt-7 md:px-7 md:py-5">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="text-xl font-semibold text-slate-900">{title}</h2>
                {subtitle ? <p className="mt-1 text-sm text-slate-500">{subtitle}</p> : null}
              </div>
              <button
                type="button"
                onClick={onClose}
                className="drawer-close rounded-full border border-slate-200 px-3 py-1 text-sm text-slate-500 transition hover:bg-slate-50"
              >
                关闭
              </button>
            </div>
          </div>

          <div className="drawer-body mt-6">
            {loading ? (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
                正在加载内容...
              </div>
            ) : (
              children
            )}
          </div>

          {footer ? (
            <div className="drawer-footer sticky bottom-0 mt-6 -mx-5 border-t border-slate-200/80 bg-white/94 px-5 py-4 backdrop-blur sm:-mx-6 sm:px-6 md:-mx-7 md:px-7">
              {footer}
            </div>
          ) : null}
        </section>
      </div>
    </div>
  )
}