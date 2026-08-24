export function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center py-10" role="status" aria-live="polite">
      <div className="h-10 w-10 animate-spin rounded-full border-4 border-sky-100 border-t-sky-500" />
      <span className="sr-only">\u52a0\u8f7d\u4e2d</span>
    </div>
  )
}
