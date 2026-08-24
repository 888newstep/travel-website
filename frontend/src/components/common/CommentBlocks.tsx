import type { ChangeEvent } from 'react'

interface CommentFeedItem {
  id: string | number
  author: string
  time: string
  content: string
  meta?: string
}

interface CommentFeedProps {
  title: string
  items: CommentFeedItem[]
  emptyText: string
}

interface CommentComposerProps {
  title: string
  value: string
  onChange: (value: string) => void
  onSubmit: () => void
  submitting: boolean
  disabled?: boolean
  placeholder: string
  submitText: string
  submittingText: string
  ratingValue?: string
  onRatingChange?: (value: string) => void
}

export function CommentFeed({ title, items, emptyText }: CommentFeedProps) {
  return (
    <div>
      <h3 className="text-base font-semibold text-slate-900">{title}</h3>
      {items.length ? (
        <div className="mt-3 space-y-3">
          {items.map((item) => (
            <article key={item.id} className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-600">
              <div className="flex items-center justify-between gap-3 text-xs text-slate-400">
                <span>{item.author}</span>
                <span>{item.time}</span>
              </div>
              {item.meta ? <div className="mt-2 text-xs text-sky-700">{item.meta}</div> : null}
              <p className="mt-2 leading-6">{item.content}</p>
            </article>
          ))}
        </div>
      ) : (
        <div className="mt-3 rounded-2xl border border-dashed border-slate-200 px-4 py-4 text-sm text-slate-400">{emptyText}</div>
      )}
    </div>
  )
}

export function CommentComposer({
  title,
  value,
  onChange,
  onSubmit,
  submitting,
  disabled = false,
  placeholder,
  submitText,
  submittingText,
  ratingValue,
  onRatingChange,
}: CommentComposerProps) {
  return (
    <div>
      <h3 className="text-base font-semibold text-slate-900">{title}</h3>
      <div className="mt-3 grid gap-3">
        {typeof ratingValue === 'string' && onRatingChange ? (
          <select
            value={ratingValue}
            onChange={(event: ChangeEvent<HTMLSelectElement>) => onRatingChange(event.target.value)}
            className="search-input"
            aria-label="评分"
          >
            <option value="5">5 星</option>
            <option value="4">4 星</option>
            <option value="3">3 星</option>
            <option value="2">2 星</option>
            <option value="1">1 星</option>
          </select>
        ) : null}
        <textarea
          value={value}
          onChange={(event: ChangeEvent<HTMLTextAreaElement>) => onChange(event.target.value)}
          rows={4}
          placeholder={placeholder}
          className="search-input resize-y"
        />
        <button
          type="button"
          onClick={onSubmit}
          disabled={disabled || !value.trim()}
          className="btn-primary w-full sm:w-auto disabled:cursor-not-allowed disabled:opacity-60"
        >
          {submitting ? submittingText : submitText}
        </button>
      </div>
    </div>
  )
}
