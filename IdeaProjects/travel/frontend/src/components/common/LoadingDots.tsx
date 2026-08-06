export function LoadingDots() {
  return (
    <div className="inline-flex items-center gap-1" role="status" aria-live="polite" aria-label="???">
      <span className="h-2 w-2 animate-bounce rounded-full bg-sky-400 [animation-delay:-0.3s]" />
      <span className="h-2 w-2 animate-bounce rounded-full bg-sky-400 [animation-delay:-0.15s]" />
      <span className="h-2 w-2 animate-bounce rounded-full bg-sky-400" />
    </div>
  )
}
